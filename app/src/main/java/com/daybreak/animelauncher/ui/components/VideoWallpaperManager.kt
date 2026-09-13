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

/**
 * Gestor centralizado y ultra-optimizado de ExoPlayer para fondos de video.
 *
 * Arquitectura de Pool Fijo de 2 Decodificadores:
 * 1. MÁXIMO ESTRICTO DE 2 DECODIFICADORES HARDWARE: Solo existen 2 instancias de ExoPlayer
 *    en toda la vida de la app (Slot A y Slot B).
 * 2. CERO FUGAS Y CERO PANTALLAS NEGRAS: Los reproductores NUNCA se destruyen/liberan en tiempo
 *    de ejecución para evitar que PlayerViews queden huérfanas con reproductores muertos.
 * 3. Rotación inteligente: Página actual + página vecina inmediata (pre-buffering).
 * 4. Cero decodificación de audio (C.TRACK_TYPE_AUDIO deshabilitado).
 * 5. Reanudación condicional: NO re-prepara si el reproductor ya está listo.
 * 6. Pausa instantánea en pérdida de foco/onPause/onStop para cero consumo de CPU/GPU.
 */
object VideoWallpaperManager {

    private class PlayerSlot(val player: ExoPlayer) {
        var assignedPage: Int? = null
        var currentUriKey: String? = null
    }

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

    private fun createOptimizedPlayer(context: Context): ExoPlayer {
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
            slotA = PlayerSlot(createOptimizedPlayer(ctx))
        }
        if (slotB == null) {
            slotB = PlayerSlot(createOptimizedPlayer(ctx))
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

    @Synchronized
    fun unregisterPlayerView(pageIndex: Int, playerView: PlayerView? = null) {
        val existing = pageBindings[pageIndex]
        if (existing != null && (playerView == null || existing.playerView === playerView)) {
            pageBindings.remove(pageIndex)
            val a = slotA
            if (a != null && a.assignedPage == pageIndex) {
                saveSlotPosition(a)
                a.player.clearVideoSurface()
                a.assignedPage = null
            }
            val b = slotB
            if (b != null && b.assignedPage == pageIndex) {
                saveSlotPosition(b)
                b.player.clearVideoSurface()
                b.assignedPage = null
            }
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
     * Sincroniza los 2 reproductores hardware con las páginas más relevantes:
     * - Target 1: Página actual visible (reproducción activa).
     * - Target 2: Página siguiente inmediata o anterior (pre-buffering del primer frame).
     */
    @Synchronized
    private fun syncSlots(context: Context) {
        ensureSlots(context)
        val sA = slotA ?: return
        val sB = slotB ?: return

        // Determinar las 2 páginas objetivo prioritarias
        val target1 = currentPageIndex
        val target2 = if (pageBindings.containsKey(currentPageIndex + 1)) {
            currentPageIndex + 1
        } else if (currentPageIndex > 0 && pageBindings.containsKey(currentPageIndex - 1)) {
            currentPageIndex - 1
        } else {
            null
        }

        val targetPages = listOfNotNull(target1, target2).filter { pageBindings.containsKey(it) }

        // Mantener las asignaciones existentes si siguen siendo objetivos válidos
        val slots = listOf(sA, sB)
        for (target in targetPages) {
            val alreadyAssigned = slots.any { it.assignedPage == target }
            if (!alreadyAssigned) {
                // Buscar un slot disponible que no esté asignado a un objetivo actual
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
                slot.player.playWhenReady = false
                pageBindings[assigned]?.playerView?.player = null
                slot.player.clearVideoSurface()
                slot.assignedPage = null
            }
        }

        // Configurar estado de reproducción
        for (slot in slots) {
            val assigned = slot.assignedPage
            if (assigned != null && assigned in targetPages) {
                val binding = pageBindings[assigned]
                if (binding != null && binding.playerView.player !== slot.player) {
                    binding.playerView.player = slot.player
                }
                // Si la app está en primer plano, ambos targets se ponen en playWhenReady
                // para que el frame esté listo de inmediato y no haya pantalla negra al deslizar
                slot.player.playWhenReady = isAppForeground
            }
        }
    }

    private fun assignSlotToPage(context: Context, slot: PlayerSlot, page: Int) {
        val binding = pageBindings[page] ?: return

        // Si estaba en otra página, desacoplar limpiamente
        val oldPage = slot.assignedPage
        if (oldPage != null && oldPage != page) {
            saveSlotPosition(slot)
            pageBindings[oldPage]?.playerView?.player = null
            slot.player.clearVideoSurface()
        }

        slot.assignedPage = page
        binding.playerView.player = slot.player

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
            if (slot.player.playbackState == Player.STATE_IDLE || slot.player.playerError != null) {
                val startPos = savedPositions[uriKey] ?: 0L
                if (startPos > 0) {
                    slot.player.seekTo(startPos)
                }
                slot.player.prepare()
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
            saveSlotPosition(it)
            it.player.playWhenReady = false
        }
        slotB?.let {
            saveSlotPosition(it)
            it.player.playWhenReady = false
        }
    }

    /**
     * Reanuda la reproducción al regresar a primer plano.
     * CONDICIÓN 2: NO hace prepare() automáticamente en cada callback;
     * solo si realmente se perdió la superficie o el estado está en STATE_IDLE o con error.
     */
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
            // Ventana sin foco (ej. Recientes): pausar decodificación para ahorrar GPU/CPU
            slotA?.let {
                saveSlotPosition(it)
                it.player.playWhenReady = false
            }
            slotB?.let {
                saveSlotPosition(it)
                it.player.playWhenReady = false
            }
        }
    }

    @Synchronized
    fun releaseAll() {
        slotA?.let {
            saveSlotPosition(it)
            try {
                it.player.stop()
                it.player.clearMediaItems()
                it.player.release()
            } catch (e: Exception) {}
        }
        slotB?.let {
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
