package com.daybreak.animelauncher.theming

import com.daybreak.animelauncher.AdvancedStyleConfig
import org.junit.Assert.*
import org.junit.Test

/**
 * UI-1 Fase 6: tests del logo configurable (A-N del gate). Puros, sin Compose.
 */
class LauncherLogoTest {

    private val config = AdvancedStyleConfig()
    private val manualTokens = LauncherThemeTokens.fromAdvancedStyleConfig(config)

    private fun resolve(
        styleConfig: AdvancedStyleConfig = config,
        tokens: LauncherThemeTokens = manualTokens
    ) = LauncherLogoResolver.resolve(styleConfig, tokens)

    // A. default mode = ICON_ONLY
    @Test
    fun test01_DefaultModeIconOnly() {
        assertEquals(LogoMode.ICON_ONLY, config.logoMode)
        assertEquals(LogoMode.ICON_ONLY, resolve().mode)
    }

    // B. default icon = AutoAwesome
    @Test
    fun test02_DefaultIconAutoAwesome() {
        assertEquals(LogoCatalog.DEFAULT_ICON, "AutoAwesome")
        assertEquals("AutoAwesome", config.logoIcon)
        assertEquals(Icons_Outlined_AutoAwesome(), resolve().icon)
    }

    private fun Icons_Outlined_AutoAwesome() = LogoCatalog.vector("AutoAwesome")

    // C/D. Validación de longitud en Settings
    @Test
    fun test03_MaxDiezCaracteres() {
        assertNull(logoTextError("ABCDEFGHIJ"))     // 10 exactos
        assertNotNull(logoTextError("ABCDEFGHIJK")) // 11 rechazados
        assertEquals(
            "Máximo 10 caracteres",
            logoTextError("ABCDEFGHIJK", isEs = true)
        )
    }

    // D. contador / validación con 7 chars (formato del contador en Settings)
    @Test
    fun test04_SieteCaracteresValidos() {
        assertNull(logoTextError("GABRIELA"))
        assertEquals("GABRIELA", sanitizeLogoText("GABRIELA"))
    }

    // E. whitespace-only rechazado
    @Test
    fun test05_WhitespaceOnlyRechazado() {
        assertNotNull(logoTextError("   "))
        // y la sanitización runtime lo normaliza a vacío
        assertEquals("", sanitizeLogoText("   "))
    }

    // F. runtime take(10)
    @Test
    fun test06_RuntimeTakeDiez() {
        assertEquals("ABCDEFGHIJ", sanitizeLogoText("ABCDEFGHIJKLMNO"))
        // y espacios internos se conservan; trim de extremos
        assertEquals("NOVA OS", sanitizeLogoText("  NOVA OS  "))
    }

    // G. MANUAL compatibility: círculo con el color histórico (miButtonColor,
    // mismo patrón que el engine aplica a elementColors.miButton)
    @Test
    fun test07_ManualCompatibility() {
        val custom = config.copy(miButtonColor = "#FF00AA")
        val tokens = LauncherThemeTokens.fromAdvancedStyleConfig(custom)
        val spec = resolve(custom, tokens)
        assertEquals(0xFFFF00AA.toInt(), spec.circleColor)
        assertEquals(tokens.textOnAccentColor, spec.glyphColor)
        assertEquals(tokens.textPrimaryColor, spec.textColor)
        // AUTO: el círculo es el accent adaptativo de los tokens, no el campo histórico
        val autoTokens = LauncherThemeTokens(
            accentColor = 0xFF466A8D.toInt(),
            surfaceColor = 0xFF08080C.toInt(),
            textPrimaryColor = 0xFFFFFFFF.toInt(),
            textSecondaryColor = 0xFFD0D4DC.toInt(),
            borderColor = 0xFF466A8D.toInt()
        )
        val autoSpec = resolve(custom.copy(themeMode = ThemeMode.AUTO), autoTokens)
        assertEquals(autoTokens.accentColor, autoSpec.circleColor)
        // AUTO_WITH_OVERRIDES: misma identidad accent que AUTO
        val ovrSpec = resolve(custom.copy(themeMode = ThemeMode.AUTO_WITH_OVERRIDES), autoTokens)
        assertEquals(autoTokens.accentColor, ovrSpec.circleColor)
    }

    // H. AUTO usa textPrimary para el texto
    @Test
    fun test08_AutoTextoUsaTextPrimary() {
        val autoTokens = LauncherThemeTokens(
            accentColor = 0xFF466A8D.toInt(),
            surfaceColor = 0xFF08080C.toInt(),
            textPrimaryColor = 0xFFFFFFFF.toInt(),
            textSecondaryColor = 0xFFD0D4DC.toInt(),
            borderColor = 0xFF466A8D.toInt()
        )
        val spec = resolve(config.copy(themeMode = ThemeMode.AUTO), autoTokens)
        assertEquals(0xFFFFFFFF.toInt(), spec.textColor)
        assertEquals(autoTokens.textPrimaryColor, spec.textColor)
    }

    // I. el icono usa accent como identidad (círculo), nunca como texto
    @Test
    fun test09_IconoUsaAccentComoIdentidad() {
        val autoTokens = LauncherThemeTokens(
            accentColor = 0xFFFF1744.toInt(),
            surfaceColor = 0xFF08080C.toInt(),
            textPrimaryColor = 0xFFFFFFFF.toInt(),
            textSecondaryColor = 0xFFD0D4DC.toInt(),
            borderColor = 0xFFFF1744.toInt()
        )
        val spec = resolve(config.copy(themeMode = ThemeMode.AUTO), autoTokens)
        assertEquals(autoTokens.accentColor, spec.circleColor)
        assertNotEquals("El glifo del círculo NO es el accent", autoTokens.accentColor, spec.glyphColor)
        assertNotEquals("El texto principal NO es el accent", autoTokens.accentColor, spec.textColor)
    }

    // J. ICON_ONLY: sin texto, con círculo
    @Test
    fun test10_IconOnly() {
        val spec = resolve(config.copy(logoMode = LogoMode.ICON_ONLY, logoText = "NOVA"))
        assertEquals(LogoMode.ICON_ONLY, spec.mode)
        assertTrue(spec.showCircle)
        assertEquals("", spec.text)
        assertNotNull(spec.icon)
    }

    // K. TEXT_ONLY: con texto, sin círculo, sin icono
    @Test
    fun test11_TextOnly() {
        val spec = resolve(config.copy(logoMode = LogoMode.TEXT_ONLY, logoText = "NOVA"))
        assertEquals(LogoMode.TEXT_ONLY, spec.mode)
        assertFalse(spec.showCircle)
        assertNull(spec.icon)
        assertEquals("NOVA", spec.text)
    }

    // L. ICON_PLUS_TEXT: ambos
    @Test
    fun test12_IconPlusText() {
        val spec = resolve(config.copy(logoMode = LogoMode.ICON_PLUS_TEXT, logoText = "NOVA"))
        assertEquals(LogoMode.ICON_PLUS_TEXT, spec.mode)
        assertTrue(spec.showCircle)
        assertNotNull(spec.icon)
        assertEquals("NOVA", spec.text)
    }

    // M. texto vacío en TEXT_ONLY → fallback a ICON_ONLY
    @Test
    fun test13_TextOnlyVacio_FallbackIconOnly() {
        val spec = resolve(config.copy(logoMode = LogoMode.TEXT_ONLY, logoText = ""))
        assertEquals(LogoMode.ICON_ONLY, spec.mode)
        assertNotNull(spec.icon)
        // whitespace-only también degrada (sanitize → vacío)
        val ws = resolve(config.copy(logoMode = LogoMode.TEXT_ONLY, logoText = "   "))
        assertEquals(LogoMode.ICON_ONLY, ws.mode)
    }

    // N. tokens deterministas
    @Test
    fun test14_TokensDeterministas() {
        val s1 = resolve()
        val s2 = resolve()
        assertEquals(s1, s2)
        assertEquals(s1.icon, s2.icon)
        val custom = config.copy(logoMode = LogoMode.ICON_PLUS_TEXT, logoText = "NEON")
        assertEquals(
            resolve(custom), resolve(custom)
        )
    }

    // Extra: claves del catálogo resuelven vectores distintos
    @Test
    fun test15_CatalogoClavesEstables() {
        assertEquals(
            listOf("AutoAwesome", "Star", "RocketLaunch", "Bolt", "Home", "Apps", "Palette", "Settings"),
            LogoCatalog.KEYS
        )
        val vectors = LogoCatalog.KEYS.map { LogoCatalog.vector(it) }
        assertEquals(vectors.size, vectors.distinctBy { it.name }.size)
    }

    // Extra: tamaños tipográficos iniciales por longitud (reducción progresiva)
    @Test
    fun test16_AutoSizeInicialPorLongitud() {
        assertEquals(14, logoInitialFontSizeSp("NOVA".length))
        assertEquals(12, logoInitialFontSizeSp("CYBER".length))
        assertEquals(10, logoInitialFontSizeSp("GABRIELA".length))
        assertEquals(10, logoInitialFontSizeSp(LOGO_TEXT_MAX_LENGTH))
    }

    // Extra: claves desconocidas del catálogo degradan al icono default (Gson-safe)
    @Test
    fun test17_IconoDesconocidoFallbackDefault() {
        val spec = resolve(config.copy(logoIcon = "Inexistente"))
        assertEquals(LogoCatalog.vector(LogoCatalog.DEFAULT_ICON), spec.icon)
    }
}
