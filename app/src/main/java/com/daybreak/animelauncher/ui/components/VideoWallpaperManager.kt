package com.daybreak.animelauncher.ui.components

import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Gestor centralizado y ultra-optimizado de ExoPlayer para fondos de video.
 *
 * Arquitectura de Pool Fijo de 2 Decodificadores (Separación de Estados):
 * 1. Estado del Player: Dos instancias persistentes de ExoPlayer (Slot 0 y Slot 1) que nunca se destruyen.
 * 2. Estado de la Surface: `isSurfaceBound` y vinculación explícita con PlayerView (`TextureView`).
 * 3. Página asignada: Índice de escritorio asociado a cada slot.
 * 4. Recurso/URI actual: `currentUriKey` para detectar cambios reales de archivo multimedia.
 * 5. Estado de Foreground/Background: `isAppForeground` para pausar decodificación y evitar consumo.
 * 6. Detección de primer frame: `hasRenderedSinceLastBind` + watchdog condicional ligero de 500ms.
 */
object VideoWallpaperManager {

    private class PlayerSlot(val id: Int, val player: ExoPlayer) {
        var assignedPage: Int? = null
        var currentUriKey: String? = null
        var isSurfaceBound: Boolean = false
        var hasRenderedSinceLastBind: Boolean = false
        var watchdogJob: Job? = null
    }

    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var slotA: PlayerSlot? = null
    private var slotB: PlayerSlot? = null

    data class PageBinding(
        val playerView: PlayerView,
        val rawResId: Int,
        val videoUri: String?
    )

    private val pageBindings = HashMap<Int, PageBinding>()
    private val savedPositions = HashMap<String, Long>()

    private var appContext: Context? = null
    private var currentPageIndex = 0
    private var isAppForeground = true

    private fun createOptimizedPlayer(context: Context, slotGetter: () -> PlayerSlot?): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 1500,
                /* maxBufferMs = */ 4000,
                /* bufferForPlaybackMs = */ 400,
                /* bufferForPlaybackAfterRebufferMs = */ 800
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        return ExoPlayer.Builder(context.applicationContext)
            .setLoadControl(loadControl)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                trackSelectionParameters = trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()

                addListener(object : Player.Listener {
                    override fun onRenderedFirstFrame() {
                        val slot = slotGetter()
                        if (slot != null) {
                            slot.hasRenderedSinceLastBind = true
                            slot.watchdogJob?.cancel()
                            slot.watchdogJob = null
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        if (isAppForeground) {
                            try {
                                prepare()
                                playWhenReady = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                })
            }
    }

    private fun buildMediaUri(context: Context, @RawRes rawResId: Int, uriString: String?): Uri {
        return if (!uriString.isNullOrBlank()) {
            try {
                Uri.parse(uriString)
            } catch (e: Exception) {
                Uri.parse("android.resource://${context.packageName}/$rawResId")
            }
        } else {
            Uri.parse("android.resource://${context.packageName}/$rawResId")
        }
    }

    private fun ensureSlots(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
        val ctx = appContext ?: context.applicationContext
        if (slotA == null) {
            slotA = PlayerSlot(0, createOptimizedPlayer(ctx) { slotA })
        }
        if (slotB == null) {
            slotB = PlayerSlot(1, createOptimizedPlayer(ctx) { slotB })
        }
    }

    @Synchronized
    fun registerPlayerView(
        pageIndex: Int,
        playerView: PlayerView,
        @RawRes rawResId: Int,
        videoUri: String?
    ) {
        appContext = playerView.context.applicationContext
        ensureSlots(playerView.context)
        pageBindings[pageIndex] = PageBinding(playerView, rawResId, videoUri)
        syncSlots(playerView.context)
    }

    /**
     * Desregistra una vista cuando sale del composition tree.
     * CRÍTICO: Siempre detener playWhenReady antes de clearVideoSurface para evitar
     * que el decodificador intente seguir emitiendo buffers hacia una superficie huérfana.
     */
    @Synchronized
    fun unregisterPlayerView(pageIndex: Int, playerView: PlayerView? = null, source: String = "unknown") {
        val existing = pageBindings[pageIndex]
        if (existing != null && (playerView == null || existing.playerView === playerView)) {
            val syncContext = existing.playerView.context.applicationContext ?: appContext
            pageBindings.remove(pageIndex)
            existing.playerView.player = null
            val a = slotA
            if (a != null && a.assignedPage == pageIndex) {
                saveSlotPosition(a)
                a.watchdogJob?.cancel()
                a.watchdogJob = null
                a.player.playWhenReady = false
                a.player.clearVideoSurface()
                a.isSurfaceBound = false
                a.assignedPage = null
            }
            val b = slotB
            if (b != null && b.assignedPage == pageIndex) {
                saveSlotPosition(b)
                b.watchdogJob?.cancel()
                b.watchdogJob = null
                b.player.playWhenReady = false
                b.player.clearVideoSurface()
                b.isSurfaceBound = false
                b.assignedPage = null
            }
            // Forzar resincronización inmediata de los slots para que las páginas restantes no queden desvinculadas
            syncContext?.let { syncSlots(it) }
        }
    }

    @Synchronized
    fun onPageSelected(page: Int, context: Context? = null) {
        currentPageIndex = page
        val ctx = context ?: appContext
        if (ctx != null) {
            syncSlots(ctx)
        }
    }

    /**
     * Sincroniza los 2 slots de decodificación hardware con las páginas prioritarias:
     * - Target 1: Página actual visible.
     * - Target 2: Página contigua inmediata (pre-buffering del primer frame).
     */
    @Synchronized
    private fun syncSlots(context: Context) {
        ensureSlots(context)
        val sA = slotA ?: return
        val sB = slotB ?: return

        val target1 = currentPageIndex
        val target2 = if (pageBindings.containsKey(currentPageIndex + 1)) {
            currentPageIndex + 1
        } else if (currentPageIndex > 0 && pageBindings.containsKey(currentPageIndex - 1)) {
            currentPageIndex - 1
        } else {
            null
        }

        val targetPages = listOfNotNull(target1, target2).filter { pageBindings.containsKey(it) }
        val slots = listOf(sA, sB)

        for (target in targetPages) {
            val slot = slots.firstOrNull { it.assignedPage == target }
            if (slot != null) {
                // BUG 1 (Punto 4): La página ya está asignada a este slot. Comprobar si el recurso cambió
                val binding = pageBindings[target]
                if (binding != null) {
                    val mediaUri = buildMediaUri(context, binding.rawResId, binding.videoUri)
                    val uriKey = mediaUri.toString()
                    if (slot.currentUriKey != uriKey || binding.playerView.player !== slot.player) {
                        assignSlotToPage(context, slot, target)
                    }
                }
            } else {
                // Asignar un slot libre a este objetivo
                val freeSlot = slots.firstOrNull { it.assignedPage !in targetPages }
                if (freeSlot != null) {
                    assignSlotToPage(context, freeSlot, target)
                }
            }
        }

        // Liberar slots que ya no pertenezcan a los objetivos activos
        for (slot in slots) {
            val assigned = slot.assignedPage
            if (assigned != null && assigned !in targetPages) {
                saveSlotPosition(slot)
                slot.watchdogJob?.cancel()
                slot.watchdogJob = null
                slot.player.playWhenReady = false
                pageBindings[assigned]?.playerView?.player = null
                slot.player.clearVideoSurface()
                slot.isSurfaceBound = false
                slot.assignedPage = null
            }
        }

        // Aplicar estado de reproducción
        for (slot in slots) {
            val assigned = slot.assignedPage
            if (assigned != null && assigned in targetPages) {
                val binding = pageBindings[assigned]
                if (binding != null && binding.playerView.player !== slot.player) {
                    binding.playerView.player = slot.player
                    slot.isSurfaceBound = true
                }
                slot.player.playWhenReady = isAppForeground
            }
        }
    }

    /**
     * Vincula un slot de reproductor a una página determinada.
     */
    private fun assignSlotToPage(context: Context, slot: PlayerSlot, page: Int) {
        val binding = pageBindings[page] ?: return

        val oldPage = slot.assignedPage
        if (oldPage != null && oldPage != page) {
            saveSlotPosition(slot)
            slot.watchdogJob?.cancel()
            slot.watchdogJob = null
            slot.player.playWhenReady = false
            pageBindings[oldPage]?.playerView?.player = null
            slot.player.clearVideoSurface()
            slot.isSurfaceBound = false
        }

        slot.assignedPage = page

        // Desvincular primero si PlayerView ya apuntaba a este u otro player para forzar
        // a PlayerView a re-adjuntar su TextureView/Surface al slot.player
        if (binding.playerView.player === slot.player) {
            binding.playerView.player = null
        }
        binding.playerView.player = slot.player
        slot.isSurfaceBound = true
        slot.hasRenderedSinceLastBind = false

        val mediaUri = buildMediaUri(context, binding.rawResId, binding.videoUri)
        val uriKey = mediaUri.toString()

        if (slot.currentUriKey != uriKey) {
            slot.currentUriKey = uriKey
            slot.player.setMediaItem(MediaItem.fromUri(mediaUri))
            val startPos = savedPositions[uriKey] ?: 0L
            if (startPos > 0) {
                slot.player.seekTo(startPos)
            }
            slot.player.prepare()
        } else {
            // Mismo recurso multimedia ya cargado en este slot: re-bind de superficie
            val curPos = slot.player.currentPosition
            slot.player.seekTo(curPos)
            if (slot.player.playbackState == Player.STATE_IDLE || slot.player.playerError != null) {
                val startPos = savedPositions[uriKey] ?: 0L
                if (startPos > 0) {
                    slot.player.seekTo(startPos)
                }
                slot.player.prepare()
            }
        }

        slot.player.playWhenReady = isAppForeground

        // BUG 1 (Punto 3): Watchdog condicional ligero de 500 ms tras re-bind
        // Solo actúa si realmente el frame no se ha renderizado tras el tiempo prudencial
        slot.watchdogJob?.cancel()
        if (isAppForeground) {
            val targetPage = page
            slot.watchdogJob = managerScope.launch {
                delay(500)
                if (!slot.hasRenderedSinceLastBind && slot.isSurfaceBound && isAppForeground && slot.assignedPage == targetPage) {
                    // Recuperación localizada única para este slot sin afectar al resto del sistema
                    try {
                        val key = slot.currentUriKey
                        val pos = if (key != null) savedPositions[key] ?: slot.player.currentPosition else slot.player.currentPosition
                        val curBinding = pageBindings[targetPage]
                        if (curBinding != null && slot.assignedPage == targetPage) {
                            curBinding.playerView.player = null
                            curBinding.playerView.player = slot.player
                        }
                        slot.player.stop()
                        slot.player.prepare()
                        if (pos > 0) {
                            slot.player.seekTo(pos)
                        }
                        slot.player.playWhenReady = isAppForeground
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun saveSlotPosition(slot: PlayerSlot) {
        try {
            val key = slot.currentUriKey ?: return
            val pos = slot.player.currentPosition
            if (pos > 0) {
                savedPositions[key] = pos
            }
        } catch (e: Exception) {}
    }

    @Synchronized
    fun onPause() {
        isAppForeground = false
        slotA?.let {
            it.watchdogJob?.cancel()
            it.watchdogJob = null
            saveSlotPosition(it)
            it.player.playWhenReady = false
        }
        slotB?.let {
            it.watchdogJob?.cancel()
            it.watchdogJob = null
            saveSlotPosition(it)
            it.player.playWhenReady = false
        }
    }

    @Synchronized
    fun onResume(context: Context? = null) {
        isAppForeground = true
        val ctx = context ?: appContext
        if (ctx != null) {
            ensureSlots(ctx)
            syncSlots(ctx)
        }

        val slots = listOfNotNull(slotA, slotB)
        for (slot in slots) {
            if (slot.assignedPage != null) {
                if (slot.player.playerError != null || slot.player.playbackState == Player.STATE_IDLE) {
                    val key = slot.currentUriKey
                    val pos = if (key != null) savedPositions[key] ?: 0L else 0L
                    if (pos > 0) {
                        slot.player.seekTo(pos)
                    }
                    slot.player.prepare()
                }
                slot.player.playWhenReady = true
            }
        }
    }

    @Synchronized
    fun onWindowFocusChanged(hasFocus: Boolean, context: Context? = null) {
        if (hasFocus) {
            onResume(context)
        } else {
            onPause()
        }
    }

    @Synchronized
    fun releaseAll() {
        slotA?.let {
            it.watchdogJob?.cancel()
            saveSlotPosition(it)
            try {
                it.player.stop()
                it.player.clearMediaItems()
                it.player.release()
            } catch (e: Exception) {}
        }
        slotB?.let {
            it.watchdogJob?.cancel()
            saveSlotPosition(it)
            try {
                it.player.stop()
                it.player.clearMediaItems()
                it.player.release()
            } catch (e: Exception) {}
        }
        slotA = null
        slotB = null
        pageBindings.clear()
    }
}
