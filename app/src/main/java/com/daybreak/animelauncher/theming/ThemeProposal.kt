package com.daybreak.animelauncher.theming

/**
 * Modelo inmutable y temporal que representa una propuesta de tema generada
 * a partir del análisis del wallpaper vía [ThemePalette].
 *
 * Este modelo es puramente en memoria y no se persiste directamente.
 *
 * @property id Identificador estable durante el ciclo de generación (ej. "proposal_dominant").
 * @property palette Paleta de origen extraída del wallpaper.
 * @property proposedTokens Conjunto de tokens visuales runtime listos para consumo por la UI.
 * @property isDark Indica si la propuesta corresponde a un esquema oscuro o claro.
 * @property label Etiqueta descriptiva y concisa para la futura UI (ej. "Dominante", "Vibrante", "Equilibrado").
 * @property order Orden relativo de presentación de la propuesta (1-indexed).
 */
data class ThemeProposal(
    val id: String,
    val palette: ThemePalette,
    val proposedTokens: LauncherThemeTokens,
    val isDark: Boolean,
    val label: String,
    val order: Int
)
