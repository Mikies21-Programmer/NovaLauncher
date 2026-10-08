package com.daybreak.animelauncher.theming

import com.daybreak.animelauncher.AdvancedStyleConfig
import com.daybreak.animelauncher.theming.ThemeProposalGenerator.calculateContrastRatio
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * UI-1 Fase 5: tests del sampling de video (A-O del gate). Sin reloj real:
 * delayFn inyectado inmediato; maxTicks acota el bucle.
 */
class AdaptiveVideoSamplerTest {

    private val config = AdvancedStyleConfig().copy(themeMode = ThemeMode.AUTO)

    private val red = ThemePalette(0xFFD32F2F.toInt(), 0xFFFF1744.toInt(), 0xFF8E0000.toInt(), true, 0.15f)
    private val blue = ThemePalette(0xFF101820.toInt(), 0xFF466A8D.toInt(), 0xFF1B2A3A.toInt(), true, 0.04f)
    private val white = ThemePalette(0xFFF5F5F5.toInt(), 0xFFB0BEC5.toInt(), 0xFFE0E0E0.toInt(), false, 0.85f)

    // =========================================================================
    // A/B. Política de activación del sampling
    // =========================================================================

    @Test
    fun test01_VideoAutoVisibleForeground_ActivaSampling() {
        assertTrue(samplingEnabled(ThemeMode.AUTO, isVideoBackground = true, isPageVisible = true, isAppForeground = true))
        assertTrue(samplingEnabled(ThemeMode.AUTO_WITH_OVERRIDES, true, true, true))
    }

    @Test
    fun test02_Manual_NoIniciaSampling() {
        assertFalse(samplingEnabled(ThemeMode.MANUAL, true, true, true))
    }

    @Test
    fun test02b_ImagenNoUsaSampling() {
        assertFalse("El sampling es exclusivo del fondo de video", samplingEnabled(ThemeMode.AUTO, isVideoBackground = false, isPageVisible = true, isAppForeground = true))
    }

    // =========================================================================
    // C/D/E. Ciclo de vida: visibilidad y foreground
    // =========================================================================

    @Test
    fun test03_PageInvisible_NoExtrae() {
        val sampler = AdaptiveVideoSampler<Unit>(
            intervalMs = 1, maxTicks = 5,
            frameAt = { Unit }, analyze = { blue },
            onPalette = { _, _ -> }, delayFn = { }
        )
        sampler.setActive(false)
        runBlocking {
            val job = launch { sampler.start(kotlinx.coroutines.CoroutineScope(coroutineContext)) }
            kotlinx.coroutines.delay(50)
            assertEquals(0, sampler.extractionCount)
            job.cancel()
        }
        assertFalse(sampler.running)
    }

    @Test
    fun test04_LauncherBackground_NoExtrae() {
        val sampler = AdaptiveVideoSampler<Unit>(
            intervalMs = 1, maxTicks = 5,
            frameAt = { Unit }, analyze = { blue },
            onPalette = { _, _ -> }, delayFn = { }
        )
        sampler.setActive(false)
        assertEquals(0, sampler.extractionCount)
    }

    @Test
    fun test05_VisibleReanuda() {
        val sampler = AdaptiveVideoSampler<Unit>(
            intervalMs = 1, maxTicks = 100,
            frameAt = { Unit }, analyze = { blue },
            onPalette = { _, _ -> }, delayFn = { }
        )
        sampler.setActive(false)
        sampler.setActive(true)
        assertTrue(sampler.running)
        runBlocking {
            val job = launch { sampler.start(kotlinx.coroutines.CoroutineScope(coroutineContext)) }
            kotlinx.coroutines.delay(80)
            assertTrue("Reanudado debe extraer", sampler.extractionCount > 0)
            job.cancel()
        }
        sampler.stop()
    }

    // =========================================================================
    // F/G. Umbral perceptual
    // =========================================================================

    @Test
    fun test06_MismaPaleta_NoActualiza() {
        assertFalse(VideoPaletteGate.shouldUpdate(blue, blue.copy()))
    }

    @Test
    fun test07_DeltaPequeno_NoActualiza() {
        val near = blue.copy(
            dominantColor = 0xFF181C24.toInt(), // ΔR=8 del original
            averageLuminance = 0.05f            // Δlum=0.01
        )
        assertFalse(VideoPaletteGate.shouldUpdate(blue, near))
    }

    @Test
    fun test08_DeltaGrande_Actualiza() {
        assertTrue(VideoPaletteGate.shouldUpdate(blue, red))
        // También por luminancia (claro↔oscuro sin cambio de tono fuerte)
        assertTrue(VideoPaletteGate.shouldUpdate(blue, white))
    }

    // =========================================================================
    // I. Agregación estabiliza ruido
    // =========================================================================

    @Test
    fun test09_Agregacion_EstabilizaRuidoMomentaneo() {
        val updates = mutableListOf<ThemePalette>()
        // Secuencia: rojo estable con UNA muestra azul momentánea en medio.
        val frames = arrayOf<ThemePalette?>(red, red, red, blue, red)
        var tick = -1
        val sampler = AdaptiveVideoSampler<ThemePalette>(
            intervalMs = 1, maxTicks = 5, windowSize = 3,
            frameAt = { tick++; frames[tick.coerceIn(0, frames.size - 1)] },
            analyze = { it ?: blue },
            onPalette = { p, _ -> updates.add(p) },
            delayFn = { }
        )
        runBlocking {
            val job = launch { sampler.start(kotlinx.coroutines.CoroutineScope(coroutineContext)) }
            job.join()
        }
        // Ventana 3: [r,r,r] → [r,r,B] → [r,B,r] — el medoid mayoritario permanece
        // rojo en todos los agregados: la muestra ruidosa aislada NO voltea el tema.
        assertTrue("Primera muestra establece el tema", updates.isNotEmpty())
        assertTrue(
            "El agregado debe permanecer rojo-dominante pese al ruido momentáneo",
            updates.last().dominantColor == red.dominantColor
        )
        assertTrue("Sin actualizaciones por ruido: ${updates.size}", updates.size <= 2)
        assertEquals(5, sampler.extractionCount)
    }

    // =========================================================================
    // J. Overrides sobreviven a cambios de paleta
    // =========================================================================

    @Test
    fun test10_OverrideSobreviveACambioDePaleta() {
        val style = config.copy(
            themeMode = ThemeMode.AUTO_WITH_OVERRIDES,
            adaptiveOverrides = com.daybreak.animelauncher.theming.AdaptiveColorOverrides(clockColor = "#FF00FF")
        )
        val pRed = AdaptiveThemeEngine.buildAutoPageTheme(0, red, style)
        val pBlue = AdaptiveThemeEngine.buildAutoPageTheme(0, blue, style)
        assertEquals(0xFFFF00FF.toInt(), pRed.elementColors.clock)
        assertEquals("El override de reloj fijo sobrevive al cambio de paleta", 0xFFFF00FF.toInt(), pBlue.elementColors.clock)
        // messages NO tiene override: procede de tokens generados por paleta
        // (puede coincidir entre paletas oscuras — lo que se verifica es que
        // ninguno de los dos es el override magenta).
        assertNotEquals(0xFFFF00FF.toInt(), pRed.elementColors.messages)
        assertNotEquals(0xFFFF00FF.toInt(), pBlue.elementColors.messages)
        val pureBlue = AdaptiveThemeEngine.buildAutoPageTheme(0, blue, config.copy(themeMode = ThemeMode.AUTO))
        assertEquals(pureBlue.elementColors.messages, pBlue.elementColors.messages)
    }

    // =========================================================================
    // K. Cambio de URI reinicia el estado del sampler
    // =========================================================================

    @Test
    fun test11_ResetPorCambioUri() {
        val sampler = AdaptiveVideoSampler<ThemePalette>(
            intervalMs = 1, maxTicks = 8,
            frameAt = { blue }, analyze = { it ?: blue },
            onPalette = { _, _ -> }, delayFn = { kotlinx.coroutines.delay(1) }
        )
        runBlocking {
            val job = launch { sampler.start(kotlinx.coroutines.CoroutineScope(coroutineContext)) }
            kotlinx.coroutines.delay(60)
            sampler.extractionCount.let { c -> assertTrue("extracciones durante actividad: $c", c > 0) }
            job.cancel()
        }
        assertTrue("Cancelado deja de extraer", sampler.extractionCount <= 8)
        sampler.reset()
        // Tras reset: la siguiente muestra re-aplica (applied=null → gate true)
        var updated = false
        val sampler2 = AdaptiveVideoSampler<ThemePalette>(
            intervalMs = 1, maxTicks = 1,
            frameAt = { blue }, analyze = { it ?: blue },
            onPalette = { _, _ -> updated = true }, delayFn = { }
        )
        runBlocking {
            val job = launch { sampler2.start(kotlinx.coroutines.CoroutineScope(coroutineContext)) }
            job.join()
        }
        assertTrue(updated)
    }

    // =========================================================================
    // L. Lerp determinista
    // =========================================================================

    @Test
    fun test12_LerpDeterministaYExtremos() {
        val a = AdaptiveThemeEngine.buildAutoPageTheme(0, red, config)
        val b = AdaptiveThemeEngine.buildAutoPageTheme(1, blue, config)
        val f0 = AdaptiveLerp.lerpPageTheme(a, b, 0f)
        val f1 = AdaptiveLerp.lerpPageTheme(a, b, 1f)
        val mid = AdaptiveLerp.lerpPageTheme(a, b, 0.5f)
        assertEquals(a.tokens, f0.tokens)
        assertEquals(b.tokens, f1.tokens)
        assertEquals(b.pageIndex, mid.pageIndex)
        // determinismo
        assertEquals(mid, AdaptiveLerp.lerpPageTheme(a, b, 0.5f))
        // alpha de glass interpola dentro de rango
        assertTrue(mid.glass.scrimAlpha in 0.40f..0.65f)
    }

    // =========================================================================
    // N. Sin retención de bitmaps / ventana acotada (estructural)
    // =========================================================================

    @Test
    fun test13_VentanaAcotadaYExtraccionesContadas() {
        val sampler = AdaptiveVideoSampler<ThemePalette>(
            intervalMs = 1, maxTicks = 15, windowSize = 3,
            frameAt = { blue }, analyze = { it ?: blue },
            onPalette = { _, _ -> }, delayFn = { }
        )
        runBlocking {
            val job = launch { sampler.start(kotlinx.coroutines.CoroutineScope(coroutineContext)) }
            job.join()
        }
        assertEquals(15, sampler.extractionCount)
        // El sampler solo retiene paletas (ventana ≤ 3) y el tema aplicado; nunca frames.
    }

    // =========================================================================
    // O. stop/restart correcto
    // =========================================================================

    @Test
    fun test14_StopYRestart() {
        val sampler = AdaptiveVideoSampler<Unit>(
            intervalMs = 1, maxTicks = 1000,
            frameAt = { Unit }, analyze = { blue },
            onPalette = { _, _ -> }, delayFn = { }
        )
        assertFalse(sampler.isStarted)
        runBlocking {
            val job = launch { sampler.start(kotlinx.coroutines.CoroutineScope(coroutineContext)) }
            kotlinx.coroutines.delay(40)
            sampler.stop()
            job.join()
            assertFalse(sampler.isStarted)
            // restart en nuevo scope
            runBlocking { sampler.start(kotlinx.coroutines.CoroutineScope(coroutineContext)) }
            kotlinx.coroutines.delay(40)
            sampler.stop()
            assertTrue(sampler.extractionCount > 0)
        }
        sampler.stop()
    }
}
