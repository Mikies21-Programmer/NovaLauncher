package com.daybreak.animelauncher.theming

import com.daybreak.animelauncher.AdvancedStyleConfig
import com.daybreak.animelauncher.theming.ThemeProposalGenerator.calculateContrastRatio
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * UI-1 Fase 3: tests del pipeline AUTO para fondos de imagen (A-L del gate).
 * Loaders falsos inyectables → deterministas, sin Android, sin IO real.
 * El caso K (decode real fuera de Main) vive en [AdaptiveThemeEngineRobotest].
 */
class AdaptiveThemeAutoTest {

    private val config = AdvancedStyleConfig()

    @org.junit.Before
    fun clearPaletteCacheBetweenTests() {
        // Los tests comparten el singleton: aislar la caché por URI entre tests.
        AdaptiveThemeEngine.clearPaletteCache()
    }

    private val darkPalette = ThemePalette(
        dominantColor = 0xFF101820.toInt(),
        vibrantColor = 0xFF3D5AFE.toInt(),
        mutedColor = 0xFF263238.toInt(),
        isDark = true,
        averageLuminance = 0.08f
    )

    private val lightPalette = ThemePalette(
        dominantColor = 0xFFF5F5F5.toInt(),
        vibrantColor = 0xFFB0BEC5.toInt(),
        mutedColor = 0xFFE0E0E0.toInt(),
        isDark = false,
        averageLuminance = 0.85f
    )

    private val saturatedPalette = ThemePalette(
        dominantColor = 0xFFD32F2F.toInt(),
        vibrantColor = 0xFFFF1744.toInt(),
        mutedColor = 0xFF8E0000.toInt(),
        isDark = true,
        averageLuminance = 0.15f
    )

    /** Loader falso contable: uri → paleta; registra invocaciones e hilo. */
    private inner class FakeLoader(
        vararg palettes: Pair<String, ThemePalette>
    ) {
        private val map = palettes.toMap()
        val invocations = ConcurrentHashMap<String, AtomicInteger>()
        val threads = ConcurrentHashMap<String, String>()

        suspend fun load(uri: String): ThemePalette? {
            invocations.computeIfAbsent(uri) { AtomicInteger(0) }.incrementAndGet()
            threads[uri] = Thread.currentThread().name
            return map[uri] ?: darkPalette
        }
    }

    private fun engineResolve(
        uri: String,
        loader: FakeLoader,
        styleConfig: AdvancedStyleConfig = config.copy(themeMode = ThemeMode.AUTO),
        pageIndex: Int = 0
    ): PageTheme = runBlocking {
        AdaptiveThemeEngine.resolveAuto(
            pageIndex = pageIndex,
            backgroundUri = uri,
            styleConfig = styleConfig,
            context = null,
            paletteLoader = { u -> loader.load(u) }
        )
    }

    // =========================================================================
    // A + L. Misma imagen → un solo análisis, misma paleta (caché por URI)
    // =========================================================================

    @Test
    fun test01_MismaUri_UnSoloAnalisis_MismaPaleta() {
        val loader = FakeLoader("img://a" to darkPalette)
        val p1 = engineResolve("img://a", loader)
        val p2 = engineResolve("img://a", loader)
        assertEquals(1, loader.invocations["img://a"]!!.get())
        assertEquals("La caché debe servir la misma paleta", p1.palette, p2.palette)
        assertEquals(p1.tokens, p2.tokens)
    }

    // =========================================================================
    // B. URI diferente → nuevo análisis
    // =========================================================================

    @Test
    fun test02_UriDistinto_NuevoAnalisis() {
        val loader = FakeLoader("img://a" to darkPalette, "img://b" to lightPalette)
        val pa = engineResolve("img://a", loader)
        val pb = engineResolve("img://b", loader)
        assertEquals(1, loader.invocations["img://a"]!!.get())
        assertEquals(1, loader.invocations["img://b"]!!.get())
        assertNotEquals(pa.palette, pb.palette)
        assertEquals(darkPalette, pa.palette)
        assertEquals(lightPalette, pb.palette)
    }

    // =========================================================================
    // C. MANUAL no analiza
    // =========================================================================

    @Test
    fun test03_ManualNuncaInvocaElLoader() {
        val loader = FakeLoader("img://a" to darkPalette)
        val page = runBlocking {
            AdaptiveThemeEngine.resolveAuto(0, "img://a", config.copy(themeMode = ThemeMode.MANUAL), null, paletteLoader = { u -> loader.load(u) })
        }
        assertEquals(0, loader.invocations.size)
        assertEquals(ThemePalette.DEFAULT, page.palette)
    }

    // =========================================================================
    // D. AUTO produce tokens contrastados (autoridad: generador)
    // =========================================================================

    @Test
    fun test04_AutoOscuro_TextPrimaryContrastado() {
        val page = engineResolve("img://dark", FakeLoader("img://dark" to darkPalette))
        val ratio = calculateContrastRatio(page.tokens.textPrimaryColor, page.tokens.surfaceColor)
        assertTrue("textPrimary vs surface debe ser >= 4.5:1, ratio=$ratio", ratio >= 4.5f)
    }

    // =========================================================================
    // E. Fondo claro → texto oscuro
    // =========================================================================

    @Test
    fun test05_FondoClaro_TextoOscuro() {
        val page = engineResolve("img://light", FakeLoader("img://light" to lightPalette))
        assertFalse("Propuesta para fondo claro debe ser esquema claro", page.isDark)
        val lum = (0.2126f * ((page.tokens.textPrimaryColor shr 16 and 0xFF) / 255f) +
            0.7152f * ((page.tokens.textPrimaryColor shr 8 and 0xFF) / 255f) +
            0.0722f * ((page.tokens.textPrimaryColor and 0xFF) / 255f))
        assertTrue("Texto primario sobre fondo claro debe ser oscuro (lum=$lum)", lum < 0.5f)
        val ratio = calculateContrastRatio(page.tokens.textPrimaryColor, page.tokens.surfaceColor)
        assertTrue("Contraste >= 4.5:1, ratio=$ratio", ratio >= 4.5f)
    }

    // =========================================================================
    // F. Fondo oscuro → texto claro
    // =========================================================================

    @Test
    fun test06_FondoOscuro_TextoClaro() {
        val page = engineResolve("img://dark", FakeLoader("img://dark" to darkPalette))
        assertTrue(page.isDark)
        val lum = (0.2126f * ((page.tokens.textPrimaryColor shr 16 and 0xFF) / 255f) +
            0.7152f * ((page.tokens.textPrimaryColor shr 8 and 0xFF) / 255f) +
            0.0722f * ((page.tokens.textPrimaryColor and 0xFF) / 255f))
        assertTrue("Texto primario sobre fondo oscuro debe ser claro (lum=$lum)", lum > 0.5f)
        val ratio = calculateContrastRatio(page.tokens.textPrimaryColor, page.tokens.surfaceColor)
        assertTrue("Contraste >= 4.5:1, ratio=$ratio", ratio >= 4.5f)
    }

    // =========================================================================
    // G. El accent NO se convierte en textPrimary en AUTO
    // =========================================================================

    @Test
    fun test07_Auto_AccentNuncaEsTextPrimary() {
        val page = engineResolve("img://sat", FakeLoader("img://sat" to saturatedPalette))
        assertEquals(
            "El reloj/status consumen textPrimary (anti-patrón eliminado)",
            page.tokens.textPrimaryColor, page.elementColors.clock
        )
        assertNotEquals(
            "El accent generado no debe usarse como color de texto principal",
            page.tokens.accentColor, page.elementColors.clock
        )
        assertNotEquals(page.tokens.accentColor, page.tokens.textPrimaryColor)
    }

    // =========================================================================
    // H. Override explícito cambia solamente el elemento seleccionado
    // =========================================================================

    @Test
    fun test08_OverrideSoloCambiaElElementoSeleccionado() {
        val style = config.copy(
            themeMode = ThemeMode.AUTO_WITH_OVERRIDES,
            adaptiveOverrides = com.daybreak.animelauncher.theming.AdaptiveColorOverrides(clockColor = "#FF00FF")
        )
        val page = engineResolve("img://sat", FakeLoader("img://sat" to saturatedPalette), style)
        val autoPure = engineResolve("img://sat", FakeLoader("img://sat" to saturatedPalette))
        assertEquals(0xFFFF00FF.toInt(), page.elementColors.clock)
        assertEquals("El resto de elementos permanece AUTO", autoPure.elementColors.battery, page.elementColors.battery)
        assertEquals(autoPure.elementColors.messages, page.elementColors.messages)
        assertEquals(autoPure.elementColors.date, page.elementColors.date)
    }

    /** AUTO puro ignora overrides (diferenciación exigida por el mandato §5). */
    @Test
    fun test08b_AutoPuro_IgnoraOverrides() {
        val style = config.copy(
            themeMode = ThemeMode.AUTO,
            adaptiveOverrides = com.daybreak.animelauncher.theming.AdaptiveColorOverrides(clockColor = "#FF00FF")
        )
        val page = engineResolve("img://sat", FakeLoader("img://sat" to saturatedPalette), style)
        assertEquals(page.tokens.textPrimaryColor, page.elementColors.clock)
    }

    // =========================================================================
    // I. Usuario antiguo sin overrides: MANUAL paridad exacta
    // =========================================================================

    @Test
    fun test09_UsuarioAntiguo_ManualParidadStyle() {
        val style = config.copy(clockColor = "#123456", batteryColor = "#234567")
        val page = AdaptiveThemeEngine.resolve(0, null, ThemeMode.MANUAL, style)
        assertEquals(0xFF123456.toInt(), page.elementColors.clock)
        assertEquals(0xFF234567.toInt(), page.elementColors.battery)
        assertEquals(
            com.daybreak.animelauncher.theming.LauncherThemeTokens.parseHexColor(style.messagesColor, -1),
            page.elementColors.messages
        )
        assertEquals(
            com.daybreak.animelauncher.theming.LauncherThemeTokens.parseHexColor(style.dateColor, -1),
            page.elementColors.weekday
        )
        assertEquals(
            com.daybreak.animelauncher.theming.LauncherThemeTokens.parseHexColor(style.dateColor, -1),
            page.elementColors.date
        )
    }

    // =========================================================================
    // J. URI inválido / loader null → fallback seguro
    // =========================================================================

    @Test
    fun test10_UriInvalido_FallbackDefault() {
        val page = runBlocking {
            AdaptiveThemeEngine.resolveAuto(0, "img://roto", config.copy(themeMode = ThemeMode.AUTO), null, paletteLoader = { null })
        }
        assertEquals(ThemePalette.DEFAULT, page.palette)
        val expected = ThemeProposalGenerator.generateProposals(ThemePalette.DEFAULT)
            .firstOrNull { it.label == AdaptiveThemeEngine.AUTO_PROPOSAL_LABEL }
        assertEquals(expected!!.proposedTokens, page.tokens)
        // y sin uri
        val pageNoUri = runBlocking {
            AdaptiveThemeEngine.resolveAuto(0, null, config.copy(themeMode = ThemeMode.AUTO), null, paletteLoader = { null })
        }
        assertEquals(ThemePalette.DEFAULT, pageNoUri.palette)
    }

    // =========================================================================
    // K (estructura). El loader es suspend y el engine no bloquea Main:
    // verificado estructuralmente aquí; el decode real fuera de Main vive
    // en AdaptiveThemeEngineRobotest.
    // =========================================================================

    @Test
    fun test11_HiloDelLoaderRegistrado() {
        val loader = FakeLoader("img://a" to darkPalette)
        engineResolve("img://a", loader)
        assertTrue(loader.threads["img://a"]!!.isNotEmpty())
    }

    // =========================================================================
    // Overrides con el modo MANUAL: ignorados (paridad manda)
    // =========================================================================

    @Test
    fun test12_Manual_IgnoraOverrides() {
        val style = config.copy(
            themeMode = ThemeMode.MANUAL,
            adaptiveOverrides = com.daybreak.animelauncher.theming.AdaptiveColorOverrides(clockColor = "#FF00FF")
        )
        val page = AdaptiveThemeEngine.resolve(0, null, ThemeMode.MANUAL, style)
        assertEquals(
            com.daybreak.animelauncher.theming.LauncherThemeTokens.parseHexColor(style.clockColor, -1),
            page.elementColors.clock
        )
    }
}
