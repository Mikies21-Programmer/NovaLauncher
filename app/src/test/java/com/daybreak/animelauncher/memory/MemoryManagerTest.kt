package com.daybreak.animelauncher.memory

import android.content.ComponentCallbacks2
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tests del MemoryManager L1 (TRIM_MEMORY_UI_HIDDEN):
 * registro/desregistro, despacho por nivel, idempotencia, seguridad con
 * caches vacías y con despachos repetidos, y aislamiento de acciones fallidas.
 */
class MemoryManagerTest {

    @Test
    fun test01_UiHiddenActionSeEjecutaConElNivelCorrecto() {
        val counter = AtomicInteger(0)
        MemoryManager.registerUiHiddenAction("t1") { counter.incrementAndGet() }
        try {
            MemoryManager.onProcessTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
            assertEquals(1, counter.get())
        } finally {
            MemoryManager.unregisterUiHiddenAction("t1")
        }
    }

    @Test
    fun test02_OtrosNivelesNoDisparanAccionesL1() {
        val counter = AtomicInteger(0)
        MemoryManager.registerUiHiddenAction("t2") { counter.incrementAndGet() }
        try {
            MemoryManager.onProcessTrimMemory(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL)
            MemoryManager.onProcessTrimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
            MemoryManager.onProcessTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
            assertEquals("Las acciones L1 no deben ejecutarse con niveles de presión", 0, counter.get())
        } finally {
            MemoryManager.unregisterUiHiddenAction("t2")
        }
    }

    @Test
    fun test03_DesregistroEliminaLaAccion() {
        val counter = AtomicInteger(0)
        MemoryManager.registerUiHiddenAction("t3") { counter.incrementAndGet() }
        MemoryManager.unregisterUiHiddenAction("t3")
        MemoryManager.onProcessTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        assertEquals(0, counter.get())
    }

    @Test
    fun test04_DesregistroRepetidoYReemplazoPorNombre() {
        val first = AtomicInteger(0)
        val second = AtomicInteger(0)
        MemoryManager.registerUiHiddenAction("t4") { first.incrementAndGet() }
        MemoryManager.registerUiHiddenAction("t4") { second.incrementAndGet() }
        MemoryManager.unregisterUiHiddenAction("t4")
        MemoryManager.unregisterUiHiddenAction("t4") // doble unregister: seguro
        MemoryManager.onProcessTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
        assertEquals(0, first.get())
        assertEquals(0, second.get())
    }

    @Test
    fun test05_IdempotenciaDespachoRepetido() {
        val counter = AtomicInteger(0)
        MemoryManager.registerUiHiddenAction("t5") { counter.incrementAndGet() }
        try {
            repeat(5) { MemoryManager.onProcessTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) }
            assertEquals(5, counter.get()) // cada trim ejecuta una vez; nunca lanza
        } finally {
            MemoryManager.unregisterUiHiddenAction("t5")
        }
    }

    @Test
    fun test06_SeguroConRegistroVacio() {
        // Despacho sin ninguna acción registrada: no lanza, no hace nada.
        MemoryManager.onProcessTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
    }

    @Test
    fun test07_AccionQueLanzaNoPropagaNiBloqueaLasDemas() {
        val ok = AtomicInteger(0)
        MemoryManager.registerUiHiddenAction("t7a") { throw IllegalStateException("cache rota") }
        MemoryManager.registerUiHiddenAction("t7b") { ok.incrementAndGet() }
        try {
            MemoryManager.onProcessTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
            assertEquals("Las demás acciones deben ejecutarse tras una falla", 1, ok.get())
        } finally {
            MemoryManager.unregisterUiHiddenAction("t7a")
            MemoryManager.unregisterUiHiddenAction("t7b")
        }
    }

    @Test
    fun test08_AccionesDeFondoSeEjecutanSoloConNivelesDePresion() {
        val counter = AtomicInteger(0)
        MemoryManager.registerBackgroundAction("t8") { counter.incrementAndGet() }
        MemoryManager.registerCriticalAction("t8c") { counter.incrementAndGet() }
        try {
            MemoryManager.onProcessTrimMemory(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN)
            assertEquals("UI_HIDDEN no debe tocar acciones L2/L3 en esta fase", 0, counter.get())
            // Estructura lista para L2: los niveles de presión sí las alcanzan.
            MemoryManager.onProcessTrimMemory(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND)
            assertEquals(2, counter.get())
        } finally {
            MemoryManager.unregisterBackgroundAction("t8")
            MemoryManager.unregisterCriticalAction("t8c")
        }
    }
}
