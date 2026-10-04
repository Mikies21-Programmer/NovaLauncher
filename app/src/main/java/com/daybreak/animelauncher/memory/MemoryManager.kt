package com.daybreak.animelauncher.memory

import android.content.ComponentCallbacks2
import android.content.Context
import coil.imageLoader

/**
 * Gestor central de memoria a nivel de proceso.
 *
 * La RECEPCIÓN de los trims vive en [com.daybreak.animelauncher.NovaApplication]
 * (override de `Application.onTrimMemory`): los callbacks registrados vía
 * `Context.registerComponentCallbacks` no reciben `onTrimMemory` en Android 16
 * (verificado físicamente 2026-10-04); solo reciben cambios de configuración.
 *
 * Niveles gestionados:
 * - **L1 (esta fase):** `TRIM_MEMORY_UI_HIDDEN` — libera caches reconstruibles y de
 *   bajo riesgo (Coil memory cache) cuando la UI deja de estar visible.
 * - **L2/L3 (futuras):** niveles de presión de background; la estructura ya los
 *   soporta vía [registerBackgroundAction]/[registerCriticalAction], sin implementar.
 *
 * Garantías: idempotente, thread-safe, nunca lanza excepciones, seguro con caches
 * vacías y con despachos repetidos. No libera players, surfaces, widgets, ViewModel
 * ni estado de configuración.
 */
object MemoryManager {

    private val lock = Any()
    private var coilActionRegistered = false

    private val uiHiddenActions = LinkedHashMap<String, () -> Unit>()
    private val backgroundActions = LinkedHashMap<String, () -> Unit>()
    private val criticalActions = LinkedHashMap<String, () -> Unit>()

    /**
     * Registra (o reemplaza por nombre) una acción para `TRIM_MEMORY_UI_HIDDEN`.
     * Las acciones deben ser idempotentes, baratas y no tocar zonas protegidas.
     */
    fun registerUiHiddenAction(name: String, action: () -> Unit) {
        synchronized(lock) { uiHiddenActions[name] = action }
    }

    fun unregisterUiHiddenAction(name: String) {
        synchronized(lock) { uiHiddenActions.remove(name) }
    }

    fun registerBackgroundAction(name: String, action: () -> Unit) {
        synchronized(lock) { backgroundActions[name] = action }
    }

    fun unregisterBackgroundAction(name: String) {
        synchronized(lock) { backgroundActions.remove(name) }
    }

    fun registerCriticalAction(name: String, action: () -> Unit) {
        synchronized(lock) { criticalActions[name] = action }
    }

    fun unregisterCriticalAction(name: String) {
        synchronized(lock) { criticalActions.remove(name) }
    }

    /**
     * Registra la acción de Coil (cache de imágenes). Idempotente: se llama desde
     * MainActivity.onCreate y múltiples recreaciones no duplican la acción.
     */
    fun attach(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            if (coilActionRegistered) return
            coilActionRegistered = true
        }
        registerUiHiddenAction(ACTION_COIL_MEMORY_CACHE) {
            // Coil 2.x: vacía la memory cache de imágenes; se reconstruye on-demand
            // cuando la UI vuelva a pedir los drawables. No toca disk cache ni el
            // pipeline de video.
            appContext.imageLoader.memoryCache?.clear()
        }
    }

    /** Entrada de trims desde [com.daybreak.animelauncher.NovaApplication]. */
    fun onProcessTrimMemory(level: Int) {
        val actions: List<() -> Unit> = when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN ->
                synchronized(lock) { uiHiddenActions.values.toList() }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE ->
                // L2/L3: infraestructura lista, sin acciones registradas en esta fase.
                synchronized(lock) { backgroundActions.values.toList() + criticalActions.values.toList() }
            else -> emptyList()
        }
        runActionsForTesting(actions)
    }

    /** Visible para tests: ejecuta acciones sin filtrar por nivel. */
    internal fun runActionsForTesting(actions: Collection<() -> Unit>) {
        for (action in actions) {
            try {
                action()
            } catch (_: Exception) {
                // Una acción fallida nunca debe propagar ni bloquear a las demás.
            }
        }
    }

    const val ACTION_COIL_MEMORY_CACHE = "coil_memory_cache"
}
