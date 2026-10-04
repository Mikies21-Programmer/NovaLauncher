package com.daybreak.animelauncher.theming

import com.daybreak.animelauncher.AdvancedStyleConfig
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

/**
 * UI-1 Fase 1: tests puros del núcleo runtime del tema adaptativo.
 * Sin Compose, sin Android UI, sin IO (MANUAL es determinista).
 */
class AdaptiveThemeEngineTest {

    private val config = AdvancedStyleConfig()

    // =========================================================================
    // A. ThemeMode defaults
    // =========================================================================

    @Test
    fun test01_ThemeModeDefaultEsManual() {
        assertEquals(ThemeMode.MANUAL, AdvancedStyleConfig().themeMode)
    }

    // =========================================================================
    // B. Resolución MANUAL: valores iguales a AdvancedStyleConfig
    // =========================================================================

    @Test
    fun test02_ManualResolution_MapeaConfigATokensYGlass() {
        val page = AdaptiveThemeEngine.resolve(0, null, ThemeMode.MANUAL, config)

        // Tokens: paridad con LauncherThemeTokens.fromAdvancedStyleConfig
        val expected = LauncherThemeTokens.fromAdvancedStyleConfig(config)
        assertEquals(expected, page.tokens)
        assertEquals(LauncherThemeTokens.parseHexColor(config.accentColor, -1), page.tokens.accentColor)
        assertEquals(config.miTextColor, "#000000")
        assertEquals(LauncherThemeTokens.DEFAULT_TEXT_ON_ACCENT, page.tokens.textOnAccentColor)

        // Glass: mapeo directo de los campos existentes (paridad, sin reinterpretar)
        assertEquals(config.sidebarOpacity, page.glass.scrimAlpha)
        assertEquals(config.glassBorderAlpha, page.glass.borderAlpha)
        assertEquals(config.cornerRadius, page.glass.cornerRadius)
        assertEquals(
            LauncherThemeTokens.parseHexColor(config.sidebarColor, -1),
            page.glass.scrimColor
        )
        assertEquals(
            LauncherThemeTokens.parseHexColor(config.diagonalBarColor, -1),
            page.glass.gradientTopColor
        )
    }

    @Test
    fun test02b_ManualRespeteaColoresPersonalizados() {
        val custom = config.copy(
            accentColor = "#FF00AA",
            miTextColor = "#112233"
        )
        val page = AdaptiveThemeEngine.resolve(0, null, ThemeMode.MANUAL, custom)
        assertEquals(0xFFFF00AA.toInt(), page.tokens.accentColor)
        assertEquals(0xFF112233.toInt(), page.tokens.textOnAccentColor)
        assertEquals(page.tokens.accentColor, page.glass.borderColor)
    }

    // =========================================================================
    // C. MANUAL no procesa el background
    // =========================================================================

    @Test
    fun test03_ManualIgnoraBackgroundUri() {
        val a = AdaptiveThemeEngine.resolve(0, null, ThemeMode.MANUAL, config)
        val b = AdaptiveThemeEngine.resolve(
            0, "content://media/external/video/media/999", ThemeMode.MANUAL, config
        )
        assertEquals("MANUAL no debe analizar el fondo", a, b)
        assertEquals("MANUAL usa la paleta estructural DEFAULT", ThemePalette.DEFAULT, a.palette)
    }

    // =========================================================================
    // D. DEFAULT tokens: deterministas y sin crash
    // =========================================================================

    @Test
    fun test04_DefaultTokensDeterministas() {
        val d1 = LauncherThemeTokens.DEFAULT
        val d2 = LauncherThemeTokens.DEFAULT
        assertEquals(d1, d2)
        assertEquals(0xFF00F0FF.toInt(), d1.accentColor)
        // statusColor por defecto = accent (paridad con los status widgets actuales)
        assertEquals(d1.accentColor, d1.statusColor)
        assertEquals(LauncherThemeTokens.DEFAULT_TEXT_ON_ACCENT, d1.textOnAccentColor)

        val page = AdaptiveThemeEngine.resolve(3, null, ThemeMode.MANUAL, AdvancedStyleConfig())
        assertEquals(LauncherThemeTokens.DEFAULT, page.tokens)
    }

    // =========================================================================
    // E. Colores inválidos: fallbacks seguros
    // =========================================================================

    @Test
    fun test05_ColorsInvalidos_FallbackSeguro() {
        val broken = config.copy(accentColor = "##ZZ", miTextColor = "")
        val page = AdaptiveThemeEngine.resolve(0, null, ThemeMode.MANUAL, broken)
        assertEquals(LauncherThemeTokens.DEFAULT.accentColor, page.tokens.accentColor)
        assertEquals(LauncherThemeTokens.DEFAULT_TEXT_ON_ACCENT, page.tokens.textOnAccentColor)
    }

    // =========================================================================
    // F. GlassParams dentro de rangos válidos
    // =========================================================================

    @Test
    fun test06_GlassParamsEnRangosValidos() {
        val page = AdaptiveThemeEngine.resolve(0, null, ThemeMode.MANUAL, config)
        assertTrue(page.glass.scrimAlpha in 0f..1f)
        assertTrue(page.glass.borderAlpha in 0f..1f)
        assertTrue(page.glass.cornerRadius >= 0f)

        val extreme = config.copy(sidebarOpacity = 1f, glassBorderAlpha = 0f, cornerRadius = 0f)
        val p2 = AdaptiveThemeEngine.resolve(0, null, ThemeMode.MANUAL, extreme)
        assertTrue(p2.glass.scrimAlpha in 0f..1f)
        assertTrue(p2.glass.borderAlpha in 0f..1f)
        assertTrue(p2.glass.cornerRadius >= 0f)
    }

    // =========================================================================
    // G. PageTheme: inmutable y estable para inputs iguales
    // =========================================================================

    @Test
    fun test07_PageThemeEstableParaInputsIguales() {
        val p1 = AdaptiveThemeEngine.resolve(0, null, ThemeMode.MANUAL, config)
        val p2 = AdaptiveThemeEngine.resolve(0, null, ThemeMode.MANUAL, config)
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
    }

    // =========================================================================
    // H. Estado multi-página: temas independientes por página
    // =========================================================================

    @Test
    fun test08_MultiPage_ThemesIndependientes() {
        val state = AdaptiveThemeEngine.resolveState(
            pageIndices = listOf(0, 1),
            backgroundUris = mapOf(0 to null, 1 to null),
            activePage = 0,
            mode = ThemeMode.MANUAL,
            styleConfig = config
        )
        assertEquals(2, state.perPage.size)
        assertEquals(0, state.perPage.getValue(0).pageIndex)
        assertEquals(1, state.perPage.getValue(1).pageIndex)
        // En MANUAL ambos resuelven idéntico (misma config), pero son instancias por página.
        assertEquals(state.perPage.getValue(0).tokens, state.perPage.getValue(1).tokens)
        assertEquals(0, state.activePage)
        assertEquals(state.perPage.getValue(1), state.themeFor(1))
    }

    @Test
    fun test08b_ThemeForFallbackSeguro() {
        val state = AdaptiveThemeState(perPage = emptyMap(), activePage = 0)
        assertNull(state.themeFor(0))
    }

    // =========================================================================
    // I. Compatibilidad Gson hacia atrás
    // =========================================================================

    @Test
    fun test09_GsonJsonAntiguoSinThemeMode_DefaultManual() {
        val gson = Gson()
        // JSON de una versión anterior: sin themeMode; el constructor no-arg de
        // AdvancedStyleConfig (todos los campos con default) preserva los defaults.
        val oldJson = """{"accentColor":"#FF00AA","clockColor":"#FF0000"}"""
        val parsed = gson.fromJson(oldJson, AdvancedStyleConfig::class.java)
        assertEquals(ThemeMode.MANUAL, parsed.themeMode)
        assertEquals("#FF00AA", parsed.accentColor)
        assertEquals("#FF0000", parsed.clockColor)
        // round-trip: el nuevo campo se serializa y deserializa estable
        val roundTrip = gson.fromJson(gson.toJson(config), AdvancedStyleConfig::class.java)
        assertEquals(ThemeMode.MANUAL, roundTrip.themeMode)
    }

    // =========================================================================
    // J. textOnAccent: contraste adecuado
    // =========================================================================

    @Test
    fun test10_TextOnAccentContrasteSobreAccent() {
        // Autoridad de contraste existente (sin duplicar): ThemeProposalGenerator.
        val page = AdaptiveThemeEngine.resolve(0, null, ThemeMode.MANUAL, config)
        val ratio = ThemeProposalGenerator.calculateContrastRatio(
            page.tokens.textOnAccentColor, page.tokens.accentColor
        )
        assertTrue("Texto sobre acento debe cumplir WCAG AA (>= 4.5:1), ratio=$ratio", ratio >= 4.5f)

        // Con un acento oscuro el fallback negro no alcanzará: el valor del usuario manda
        // en MANUAL (paridad); la garantía de contraste es responsabilidad del generador en AUTO.
        val darkAccent = config.copy(accentColor = "#001122", miTextColor = "#000000")
        val p2 = AdaptiveThemeEngine.resolve(0, null, ThemeMode.MANUAL, darkAccent)
        assertEquals(0xFF000000.toInt(), p2.tokens.textOnAccentColor)
    }

    // =========================================================================
    // Extrapolación: AUTO/AUTO_WITH_OVERRIDES fail-safe en Fase 1
    // =========================================================================

    @Test
    fun test11_AutoFailSafeIgualAManualEnFase1() {
        val manual = AdaptiveThemeEngine.resolve(0, null, ThemeMode.MANUAL, config)
        val auto = AdaptiveThemeEngine.resolve(0, "content://video/abc", ThemeMode.AUTO, config)
        val autoOverrides =
            AdaptiveThemeEngine.resolve(0, "content://video/abc", ThemeMode.AUTO_WITH_OVERRIDES, config)
        assertEquals("AUTO sin implementar debe resolver fail-safe igual que MANUAL", manual, auto)
        assertEquals(manual, autoOverrides)
    }
}
