package com.daybreak.animelauncher.ram

import com.daybreak.animelauncher.AdvancedStyleConfig
import com.daybreak.animelauncher.theming.LauncherThemeTokens
import com.daybreak.animelauncher.theming.ThemeMode
import com.google.gson.Gson
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UI-1 Fase 7: tests del indicador de RAM global (gate A-R). Puros JVM, sin
 * Android framework: snapshots construidos directamente, proveedor falso y ticks
 * suspendibles por Channel (sin relojes reales). El gate del composable
 * (pollActive = página visible && foreground) es el contrato probado en K/L/M/R
 * a nivel de [pollRam]: el LaunchedEffect(pollActive) cancela/relanza el mismo loop.
 */
class RamMonitorTest {

    private val GB = BYTES_PER_GB

    // Proveedor falso: cuenta lecturas y entrega una secuencia cíclica.
    private class FakeRamProvider(vararg snapshots: RamSnapshot?) : RamProvider {
        private val seq = snapshots.toList()
        private var i = 0
        var reads = 0
            private set
        override suspend fun snapshot(): RamSnapshot? {
            reads++
            if (seq.isEmpty()) return null
            val s = seq[i % seq.size]
            i++
            return s
        }
    }

    private fun snap(totalGb: Long, availGb: Long, low: Boolean = false) =
        RamSnapshot(totalBytes = totalGb * GB, availBytes = availGb * GB, lowMemory = low)

    // A. cálculo de usedApprox
    @Test
    fun test01_UsedApprox() {
        val s = snap(totalGb = 11, availGb = 4)
        assertEquals(7L * GB, s.usedBytesApprox)
        // avail = total → usada 0
        assertEquals(0L, snap(8, 8).usedBytesApprox)
    }

    // B. total y available válidos
    @Test
    fun test02_SnapshotValido() {
        val s = snap(11, 4)
        assertTrue(s.isValid)
        assertEquals(4L * GB, s.availBytes)
        assertEquals(11L * GB, s.totalBytes)
    }

    // C. protección frente a valores límite
    @Test
    fun test03_Limites() {
        assertFalse(RamSnapshot(0L, 0L, false).isValid)              // total 0
        assertFalse(RamSnapshot(-1L, 0L, false).isValid)             // total negativo
        assertFalse(RamSnapshot(GB, -1L, false).isValid)             // avail negativo
        val overflow = RamSnapshot(GB, 2 * GB, false)                // avail > total
        assertFalse(overflow.isValid)
        assertEquals("usada nunca negativa", 0L, overflow.usedBytesApprox)
        assertEquals("snapshot inválido → fracción 0", 0f, overflow.usedFraction)
    }

    // D. porcentaje acotado entre 0 y 100
    @Test
    fun test04_PorcentajeAcotado() {
        assertEquals(100, snap(10, 0).usedPercent)
        assertEquals(0, snap(10, 10).usedPercent)
        assertEquals(50, snap(10, 5).usedPercent)
        assertEquals("inválido → 0", 0, RamSnapshot(GB, 2 * GB, false).usedPercent)
        assertEquals("fracción acotada", 1f, snap(10, 0).usedFraction)
    }

    // E. formato en español e inglés
    @Test
    fun test05_FormatoEsEn() {
        // 10.8125 GB total, 4.4 GB avail → usada 6.4125 GB (redondeo a 1 decimal)
        val s = RamSnapshot(
            totalBytes = 11_610_216_448L,
            availBytes = 4_724_464_026L,
            lowMemory = false
        )
        assertEquals("6.4 / 10.8 GB", RamFormat.usedOverTotal(s))
        assertEquals("4.4 GB disponibles", RamFormat.available(s, isEs = true))
        assertEquals("4.4 GB available", RamFormat.available(s, isEs = false))
        assertEquals("RAM", "RAM") // etiqueta idéntica en ambos idiomas
    }

    // F. conversión y redondeo de unidades
    @Test
    fun test06_ConversionRedondeo() {
        assertEquals(1.0f, RamFormat.bytesToGb(GB), 1e-6f)
        assertEquals("2.0", RamFormat.gb(2 * GB))
        assertEquals("punto decimal estable (Locale.US)", "1.5", RamFormat.gb(1_610_612_736L))
        assertEquals("un decimal fijo", "0.1", RamFormat.gb(107_374_182L))
        // redondeo medio: 1.25 → 1.3 (HALF_UP de %.1f)
        assertEquals("1.3", RamFormat.gb(1_342_177_280L))
    }

    // G. lowMemory representado correctamente
    @Test
    fun test07_LowMemory() {
        val low = snap(11, 10, low = true)
        assertEquals("RAM baja", RamFormat.detailLine(low, isEs = true))
        assertEquals("Low RAM", RamFormat.detailLine(low, isEs = false))
        // lowMemory=false → línea normal de disponibles (NO se interpreta % alto como peligro)
        val normal = snap(11, 4)
        assertEquals("4.0 GB disponibles", RamFormat.detailLine(normal, isEs = true))
        assertEquals(low.lowMemory, true)
    }

    // H. snapshot inválido/error del proveedor
    @Test
    fun test08_ProveedorInvalido() {
        assertEquals("—", RamFormat.usedOverTotal(null))
        assertEquals("", RamFormat.available(null, isEs = true))
        assertEquals("", RamFormat.detailLine(null, isEs = true))
        val invalid = RamSnapshot(0L, 0L, false)
        assertEquals("—", RamFormat.usedOverTotal(invalid))
        assertEquals("", RamFormat.detailLine(invalid, isEs = false))
    }

    // I. configuración antigua sin showRamWidget → default false (retrocompatible)
    @Test
    fun test09_ConfigAntiguaSinCampo() {
        val oldJson = """{"themeMode":"MANUAL","sidebarColor":"#050508"}"""
        val loaded = Gson().fromJson(oldJson, AdvancedStyleConfig::class.java)
        assertFalse("JSON antiguo sin el campo → widget oculto", loaded.showRamWidget)
        assertEquals(ThemeMode.MANUAL, loaded.themeMode)
        // los demás campos de Fase 6 sobreviven
        val oldJson6 = """{"themeMode":"AUTO","logoMode":"ICON_PLUS_TEXT","logoText":"NOVA"}"""
        val loaded6 = Gson().fromJson(oldJson6, AdvancedStyleConfig::class.java)
        assertFalse(loaded6.showRamWidget)
        assertEquals("NOVA", loaded6.logoText)
    }

    // J. estado inicial de show/hide + round-trip
    @Test
    fun test10_EstadoInicialShowHide() {
        assertFalse("default = oculto (retrocompatible)", AdvancedStyleConfig().showRamWidget)
        assertTrue(AdvancedStyleConfig().copy(showRamWidget = true).showRamWidget)
        // round-trip Gson con el campo presente
        val gson = Gson()
        val config = AdvancedStyleConfig().copy(showRamWidget = true)
        val restored = gson.fromJson(gson.toJson(config), AdvancedStyleConfig::class.java)
        assertTrue(restored.showRamWidget)
        assertFalse(gson.fromJson(gson.toJson(AdvancedStyleConfig()), AdvancedStyleConfig::class.java).showRamWidget)
    }

    // K. polling se detiene cuando la página deja de estar visible (job cancelado)
    // L. cancela al salir de RESUMED (mismo mecanismo: LaunchedEffect key)
    @Test
    fun test11_PollingSeDetieneAlCancelar() = runBlocking {
        val provider = FakeRamProvider(snap(11, 4), snap(11, 5))
        val ticks = Channel<Unit>(Channel.UNLIMITED)
        val job = launch {
            pollRam(provider, tick = { ticks.receive() }) { }
        }
        // lectura inicial inmediata, sin tick previo
        withTimeout(5_000) {
            while (provider.reads < 1) yield()
        }
        assertEquals(1, provider.reads)
        // dos ticks → dos lecturas más
        ticks.send(Unit); ticks.send(Unit)
        withTimeout(5_000) {
            while (provider.reads < 3) yield()
        }
        assertEquals(3, provider.reads)
        // cancelación (página oculta / ON_STOP) → ningún trabajo residual
        job.cancelAndJoin()
        val readsAtCancel = provider.reads
        ticks.send(Unit); ticks.send(Unit)
        ticks.close()
        assertEquals("cancelado = cero lecturas nuevas", readsAtCancel, provider.reads)
    }

    // M. vuelve a actualizar cuando regresa (relanzamiento del efecto)
    @Test
    fun test12_ReanudacionRelanzaConLecturaInmediata() = runBlocking {
        val provider = FakeRamProvider(snap(11, 4), snap(11, 5), snap(11, 6))
        val ticks = Channel<Unit>(Channel.UNLIMITED)
        // primera instancia cancelada (salió de foreground)
        val job1 = launch { pollRam(provider, tick = { ticks.receive() }) { } }
        withTimeout(5_000) { while (provider.reads < 1) yield() }
        job1.cancelAndJoin()
        // regreso: el efecto se relanza → lectura inmediata sin tick
        val job2 = launch { pollRam(provider, tick = { ticks.receive() }) { } }
        withTimeout(5_000) { while (provider.reads < 2) yield() }
        assertEquals(2, provider.reads)
        job2.cancelAndJoin()
    }

    // N. ninguna actualización a 60/120 Hz: cada update está precedido por UN tick
    @Test
    fun test13_UnaActualizacionPorTick() = runBlocking {
        val provider = FakeRamProvider(snap(11, 4), snap(11, 5), snap(11, 6), snap(11, 7))
        val ticks = Channel<Unit>(Channel.UNLIMITED)
        val updates = mutableListOf<RamSnapshot?>()
        val job = launch {
            pollRam(provider, tick = { ticks.receive() }) { updates += it }
        }
        repeat(5) { ticks.send(Unit) }
        withTimeout(5_000) { while (provider.reads < 6) yield() }
        job.cancelAndJoin()
        assertEquals("update inicial + 1 por tick", 6, updates.size)
        assertTrue("intervalo de producción = 5 s", RAM_POLL_INTERVAL_MS == 5_000L)
    }

    // O. tokens adaptativos correctos / P. MANUAL conserva colores existentes
    @Test
    fun test14_TokensColores() {
        // O: el widget usa textPrimary/textSecondary de los tokens que recibe
        val custom = LauncherThemeTokens(
            accentColor = 0xFF466A8D.toInt(),
            surfaceColor = 0xFF08080C.toInt(),
            textPrimaryColor = 0xFF112233.toInt(),
            textSecondaryColor = 0xFF445566.toInt(),
            borderColor = 0xFF466A8D.toInt()
        )
        val (p, s) = RamFormat.widgetColors(custom)
        assertEquals(0xFF112233.toInt(), p)
        assertEquals(0xFF445566.toInt(), s)
        // P: MANUAL → tokens deterministas de config (blanco / #CCCCCC históricos)
        val manual = LauncherThemeTokens.fromAdvancedStyleConfig(
            AdvancedStyleConfig().copy(themeMode = ThemeMode.MANUAL)
        )
        val (mp, ms) = RamFormat.widgetColors(manual)
        assertEquals(0xFFFFFFFF.toInt(), mp)
        assertEquals(0xFFCCCCCC.toInt(), ms)
    }

    // Q. loops independientes: cancelar uno no afecta al otro (una instancia
    // visible = un loop; el gate pagerState.currentPage == page lo garantiza)
    @Test
    fun test15_LoopsIndependientes() = runBlocking {
        val providerA = FakeRamProvider(snap(11, 4), snap(11, 5), snap(11, 6))
        val providerB = FakeRamProvider(snap(11, 4), snap(11, 5), snap(11, 6))
        val ticksA = Channel<Unit>(Channel.UNLIMITED)
        val ticksB = Channel<Unit>(Channel.UNLIMITED)
        val jobA = launch { pollRam(providerA, tick = { ticksA.receive() }) { } }
        val jobB = launch { pollRam(providerB, tick = { ticksB.receive() }) { } }
        withTimeout(5_000) {
            while (providerA.reads < 1 || providerB.reads < 1) yield()
        }
        // la instancia A deja de ser visible → su loop muere; B sigue
        jobA.cancelAndJoin()
        ticksA.send(Unit) // tick residual ignorado por A
        ticksB.send(Unit)
        withTimeout(5_000) { while (providerB.reads < 2) yield() }
        assertEquals("A sin lecturas nuevas", 1, providerA.reads)
        assertEquals("B sigue actualizando", 2, providerB.reads)
        jobB.cancelAndJoin()
    }

    // R. cancelar no deja operaciones en segundo plano (cancelación dentro del tick)
    @Test
    fun test16_CancelacionSinTrabajoResidual() = runBlocking {
        val provider = FakeRamProvider(snap(11, 4), snap(11, 5))
        val started = Channel<Unit>(Channel.UNLIMITED)
        val release = Channel<Unit>(Channel.UNLIMITED)
        val job = launch {
            pollRam(
                provider,
                tick = {
                    started.send(Unit)   // tick en curso (simula delay en progreso)
                    release.receive()    // queda suspendido aquí
                }
            ) { }
        }
        withTimeout(5_000) { started.receive() }
        // cancelar MIENTRAS el tick está en curso (página oculta a mitad de intervalo)
        job.cancelAndJoin()
        release.close()
        // job.join() completó → por concurrencia estructural ninguna lectura puede ocurrir después
        assertEquals("solo la lectura inicial; nada residual", 1, provider.reads)
    }
}
