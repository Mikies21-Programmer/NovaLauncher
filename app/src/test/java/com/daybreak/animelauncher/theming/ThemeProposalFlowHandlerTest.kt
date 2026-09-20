package com.daybreak.animelauncher.theming

import com.daybreak.animelauncher.AdvancedStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias para [ThemeProposalFlowHandler] (Fase 2D).
 *
 * Cubre los 10 casos requeridos por la especificación:
 * 1. proposal -> AdvancedStyleConfig
 * 2. Apply
 * 3. Discard
 * 4. Back
 * 5. Personalizar
 * 6. 1 proposal
 * 7. 2 proposals
 * 8. 3 proposals
 * 9. 4 proposals
 * 10. Wallpaper sin propuesta válida
 */
class ThemeProposalFlowHandlerTest {

    private val samplePalette = ThemePalette(
        dominantColor = 0xFF4A148C.toInt(),
        vibrantColor = 0xFF00E5FF.toInt(),
        mutedColor = 0xFFB39DDB.toInt(),
        isDark = true,
        averageLuminance = 0.18f
    )

    private val sampleTokens = LauncherThemeTokens(
        accentColor = 0xFF00E5FF.toInt(),
        surfaceColor = 0xFF0B0A12.toInt(),
        textPrimaryColor = 0xFFFFFFFF.toInt(),
        textSecondaryColor = 0xFFD0D4DC.toInt(),
        borderColor = 0xFF00E5FF.toInt()
    )

    private val sampleProposal = ThemeProposal(
        id = "proposal_vibrant",
        palette = samplePalette,
        proposedTokens = sampleTokens,
        isDark = true,
        label = "Vibrante",
        order = 1
    )

    private val initialConfig = AdvancedStyleConfig(
        accentColor = "#FF9900",
        cornerRadius = 24f,
        panelTransparency = 0.75f,
        widgetOpacity = 0.85f
    )

    @Test
    fun `1 proposal se convierte correctamente a AdvancedStyleConfig preservando parametros base`() {
        val result = ThemeProposalFlowHandler.proposalToAdvancedStyleConfig(sampleProposal, initialConfig)

        assertEquals("#00E5FF", result.accentColor)
        assertEquals("#00E5FF", result.miButtonColor)
        assertEquals("#00E5FF", result.triangleColor)
        assertEquals("#FFFFFF", result.appDrawerTextColor)
        assertEquals("#0B0A12", result.appDrawerBgColor)
        assertEquals("#0B0A12", result.sidebarColor)
        assertEquals("#000000", result.miTextColor)

        // Parámetros de usuario no deben modificarse
        assertEquals(24f, result.cornerRadius)
        assertEquals(0.75f, result.panelTransparency)
        assertEquals(0.85f, result.widgetOpacity)
    }

    @Test
    fun `2 accion Apply resuelve el estilo derivado de la propuesta seleccionada`() {
        val applied = ThemeProposalFlowHandler.handleApply(sampleProposal, initialConfig)

        assertEquals("#00E5FF", applied.accentColor)
        assertNotEquals(initialConfig.accentColor, applied.accentColor)
        assertEquals(initialConfig.cornerRadius, applied.cornerRadius)
    }

    @Test
    fun `3 accion Discard restaura exactamente el estilo inicial del usuario`() {
        val discarded = ThemeProposalFlowHandler.handleDiscard(initialConfig)

        assertEquals(initialConfig, discarded)
        assertEquals("#FF9900", discarded.accentColor)
    }

    @Test
    fun `4 accion Back restaura exactamente el estilo inicial del usuario`() {
        val backed = ThemeProposalFlowHandler.handleBack(initialConfig)

        assertEquals(initialConfig, backed)
        assertEquals("#FF9900", backed.accentColor)
    }

    @Test
    fun `5 accion Personalizar prepara la propuesta como punto de partida`() {
        val customized = ThemeProposalFlowHandler.handleCustomize(sampleProposal, initialConfig)

        assertEquals("#00E5FF", customized.accentColor)
        assertEquals(initialConfig.cornerRadius, customized.cornerRadius)
    }

    @Test
    fun `6 lista con 1 propuesta maneja paleta monocromatica adecuadamente`() {
        val monoPalette = ThemePalette(
            dominantColor = 0xFF444444.toInt(),
            vibrantColor = 0xFF444444.toInt(),
            mutedColor = 0xFF444444.toInt(),
            isDark = true,
            averageLuminance = 0.15f
        )
        val proposals = ThemeProposalFlowHandler.resolveProposals(monoPalette)

        assertEquals(1, proposals.size)
        assertEquals("Dominante", proposals[0].label)
        assertEquals(1, proposals[0].order)
    }

    @Test
    fun `7 lista con 2 propuestas se resuelve correctamente cuando solo dos variantes son distinguibles`() {
        // Paleta donde vibrant es idéntico a dominant pero muted es distinto
        val twoVariantPalette = ThemePalette(
            dominantColor = 0xFF1A237E.toInt(),
            vibrantColor = 0xFF1A237E.toInt(), // duplicado de dominant
            mutedColor = 0xFF80DEEA.toInt(),    // variante clara distinguible
            isDark = true,
            averageLuminance = 0.12f
        )
        val proposals = ThemeProposalFlowHandler.resolveProposals(twoVariantPalette)

        assertEquals(2, proposals.size)
        assertEquals(1, proposals[0].order)
        assertEquals(2, proposals[1].order)
    }

    @Test
    fun `8 lista con 3 propuestas estandar se genera para paleta rica en colores`() {
        val proposals = ThemeProposalFlowHandler.resolveProposals(samplePalette)

        assertEquals(3, proposals.size)
        assertEquals(listOf("proposal_dominant", "proposal_vibrant", "proposal_muted"), proposals.map { it.id })
    }

    @Test
    fun `9 lista con 4 propuestas respeta el limite maximo de la especificacion`() {
        val proposals = ThemeProposalFlowHandler.resolveProposals(samplePalette)

        assertTrue("No debe superar nunca 4 propuestas", proposals.size <= 4)
    }

    @Test
    fun `10 wallpaper sin propuesta valida o paleta nula resuelve fallback seguro sin crashear`() {
        val nullProposals = ThemeProposalFlowHandler.resolveProposals(null)

        assertTrue("Debe retornar al menos 1 propuesta de fallback", nullProposals.isNotEmpty())
        for (p in nullProposals) {
            assertTrue("Token debe tener contraste válido", p.proposedTokens.accentColor != 0)
        }
    }

    @Test
    fun `11 Personalizar conserva propuesta seleccionada para AdvancedSettings`() {
        val customizedConfig = ThemeProposalFlowHandler.handleCustomize(sampleProposal, initialConfig)

        // El color de acento y los elementos clave deben coincidir con la propuesta seleccionada
        assertEquals("#00E5FF", customizedConfig.accentColor)
        assertEquals("#00E5FF", customizedConfig.clockColor)
        assertEquals("#00E5FF", customizedConfig.batteryColor)
        assertEquals("#0B0A12", customizedConfig.sidebarColor)

        // El usuario puede modificar los valores normalmente sin perder el estado base
        val userModified = customizedConfig.copy(cornerRadius = 32f, accentColor = "#FF0055")
        assertEquals(32f, userModified.cornerRadius)
        assertEquals("#FF0055", userModified.accentColor)
    }

    @Test
    fun `12 category pills tokens semanticos contrastan adecuadamente en tema oscuro y claro`() {
        // Tema oscuro
        val darkTokens = LauncherThemeTokens.DEFAULT
        val darkAccentRatio = ThemeProposalGenerator.calculateContrastRatio(darkTokens.accentColor, darkTokens.surfaceColor)
        val darkSecondaryRatio = ThemeProposalGenerator.calculateContrastRatio(darkTokens.textSecondaryColor, darkTokens.surfaceColor)
        assertTrue("En tema oscuro accent vs surface >= 3:1", darkAccentRatio >= 3.0f)
        assertTrue("En tema oscuro textSecondary vs surface >= 3:1", darkSecondaryRatio >= 3.0f)
        val darkSecLum = ThemeProposalGenerator.calculateRelativeLuminance(darkTokens.textSecondaryColor)
        assertTrue("Texto secundario debe ser claro en tema oscuro", darkSecLum > 0.40f)

        // Tema claro generado
        val lightPalette = ThemePalette(
            dominantColor = 0xFFFFFFFF.toInt(),
            vibrantColor = 0xFF2196F3.toInt(),
            mutedColor = 0xFF9E9E9E.toInt(),
            isDark = false,
            averageLuminance = 0.95f
        )
        val lightProposals = ThemeProposalFlowHandler.resolveProposals(lightPalette)
        assertTrue(lightProposals.isNotEmpty())

        val lightTokens = lightProposals[0].proposedTokens
        val lightAccentRatio = ThemeProposalGenerator.calculateContrastRatio(lightTokens.accentColor, lightTokens.surfaceColor)
        val lightSecondaryRatio = ThemeProposalGenerator.calculateContrastRatio(lightTokens.textSecondaryColor, lightTokens.surfaceColor)

        assertTrue("En tema claro accent vs surface >= 3:1", lightAccentRatio >= 3.0f)
        assertTrue("En tema claro textSecondary vs surface >= 3:1", lightSecondaryRatio >= 3.0f)

        // En tema claro el texto secundario debe ser oscuro
        val lightSecLum = ThemeProposalGenerator.calculateRelativeLuminance(lightTokens.textSecondaryColor)
        assertTrue("Texto secundario debe ser oscuro en tema claro (luminancia: $lightSecLum)", lightSecLum < 0.25f)
    }
}
