package com.daybreak.animelauncher.theming

import com.daybreak.animelauncher.AdvancedStyleConfig

/**
 * Controlador puro de lógica de flujo, conversión y transiciones de estado para
 * Adaptive Wallpaper Theming (Fase 2D).
 *
 * Responsabilidad:
 * - Conversión segura y bidireccional entre [ThemeProposal] y [AdvancedStyleConfig].
 * - Determinación de estados resultantes para las acciones de Aplicar, Descartar, Volver y Personalizar.
 * - Manejo robusto de fallbacks en ausencia de propuestas válidas o errores de análisis.
 * - Desacoplado 100% de Compose y del framework de Android para máxima testabilidad unitaria.
 */
object ThemeProposalFlowHandler {

    /**
     * Convierte una [proposal] en un [AdvancedStyleConfig], preservando los valores
     * configurados por el usuario en [base] (opacidades, radios de esquinas, transparencias).
     */
    fun proposalToAdvancedStyleConfig(
        proposal: ThemeProposal,
        base: AdvancedStyleConfig
    ): AdvancedStyleConfig {
        val tokens = proposal.proposedTokens
        val accentHex = colorToHex(tokens.accentColor)
        val surfaceHex = colorToHex(tokens.surfaceColor)
        val textPrimaryHex = colorToHex(tokens.textPrimaryColor)

        val miTextHex = if (proposal.isDark) "#000000" else "#FFFFFF"

        return base.copy(
            // Acentos y resaltes
            accentColor = accentHex,
            miButtonColor = accentHex,
            triangleColor = accentHex,
            clockColor = accentHex,
            batteryColor = accentHex,
            messagesColor = accentHex,
            dateColor = accentHex,

            // Textos principales
            appDrawerTextColor = textPrimaryHex,
            customIconColor = textPrimaryHex,
            miTextColor = miTextHex,

            // Superficies y contenedores
            sidebarColor = surfaceHex,
            diagonalBarColor = surfaceHex,
            appDrawerBgColor = surfaceHex
        )
    }

    /**
     * Resuelve el [AdvancedStyleConfig] que debe persistirse al pulsar "Aplicar".
     */
    fun handleApply(
        proposal: ThemeProposal,
        base: AdvancedStyleConfig
    ): AdvancedStyleConfig {
        return proposalToAdvancedStyleConfig(proposal, base)
    }

    /**
     * Resuelve el [AdvancedStyleConfig] que debe restaurarse al pulsar "Descartar".
     * Devuelve el estilo original intacto previo a cualquier previsualización transitoria.
     */
    fun handleDiscard(initialConfig: AdvancedStyleConfig): AdvancedStyleConfig {
        return initialConfig
    }

    /**
     * Resuelve el [AdvancedStyleConfig] que debe restaurarse al pulsar "Back" / Volver.
     * Devuelve el estilo original intacto.
     */
    fun handleBack(initialConfig: AdvancedStyleConfig): AdvancedStyleConfig {
        return initialConfig
    }

    /**
     * Resuelve el [AdvancedStyleConfig] que servirá como punto de partida al pulsar "Personalizar".
     */
    fun handleCustomize(
        proposal: ThemeProposal,
        base: AdvancedStyleConfig
    ): AdvancedStyleConfig {
        return proposalToAdvancedStyleConfig(proposal, base)
    }

    /**
     * Resuelve de forma segura la lista de propuestas a partir del [palette] analizado.
     * Si la paleta es nula, vacía o inválida, genera propuestas utilizando [ThemePalette.DEFAULT].
     */
    fun resolveProposals(palette: ThemePalette?): List<ThemeProposal> {
        val safePalette = palette ?: ThemePalette.DEFAULT
        val generated = ThemeProposalGenerator.generateProposals(safePalette)
        return if (generated.isNotEmpty()) {
            generated
        } else {
            ThemeProposalGenerator.generateProposals(ThemePalette.DEFAULT)
        }
    }

    /**
     * Convierte un entero ARGB de 32 bits a una cadena hexadecimal estándar "#RRGGBB".
     */
    fun colorToHex(color: Int): String {
        return String.format("#%06X", 0x00FFFFFF and color)
    }
}
