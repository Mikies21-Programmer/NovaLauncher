package com.daybreak.animelauncher

import android.app.Application
import com.daybreak.animelauncher.memory.MemoryManager

/**
 * Application de NovaLauncher.
 *
 * MEMORY-L1: punto de recepción de los trims de memoria del sistema. Los callbacks
 * registrados vía `Context.registerComponentCallbacks` NO reciben `onTrimMemory`
 * (verificado en Android 16/HyperOS: solo reciben cambios de configuración); el
 * override directo en la Application es la vía de despacho garantizada.
 * La ejecución de acciones vive en [MemoryManager].
 */
class NovaApplication : Application() {

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        MemoryManager.onProcessTrimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // Nivel legacy; los niveles modernos llegan por onTrimMemory.
    }
}
