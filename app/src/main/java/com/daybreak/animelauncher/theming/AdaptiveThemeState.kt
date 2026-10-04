package com.daybreak.animelauncher.theming

import androidx.annotation.ColorInt

/**
 * UI-1 Fase 1: parámetros de glass premium asociados a una página.
 *
 * Los defaults equivalen al diseño actual (sidebar/diagonal/accent de AdvancedStyleConfig).
 * Modelo puro e inmutable, sin dependencias de Compose ni Android UI.
 */
data class GlassParams(
    @get:ColorInt val scrimColor: Int,
    val scrimAlpha: Float,
    @get:ColorInt val gradientTopColor: Int,
    @get:ColorInt val gradientBottomColor: Int,
    @get:ColorInt val borderColor: Int,
    val borderAlpha: Float,
    @get:ColorInt val highlightColor: Int,
    val cornerRadius: Float
)

/**
 * UI-1 Fase 1: tema resuelto para UNA página del launcher.
 *
 * Inmutable y determinista: inputs iguales producen instancias iguales (data class).
 */
data class PageTheme(
    val pageIndex: Int,
    val tokens: LauncherThemeTokens,
    val glass: GlassParams,
    val palette: ThemePalette,
    val isDark: Boolean
)

/**
 * UI-1 Fase 1: estado runtime del tema adaptativo, resuelto por página.
 *
 * Permite background A → theme A y background B → theme B de forma independiente.
 * En Fase 1 todas las páginas resuelven igual (MANUAL parity); la estructura
 * soporta temas independientes por página para las fases siguientes.
 */
data class AdaptiveThemeState(
    val perPage: Map<Int, PageTheme>,
    val activePage: Int
) {
    /** Tema de la página activa, o el primero disponible como fallback seguro. */
    fun themeFor(pageIndex: Int): PageTheme? = perPage[pageIndex]
        ?: perPage[activePage]
        ?: perPage.values.firstOrNull()

    /** Tema expuesto a overlays globales (Drawer): el de la página activa. */
    fun themeForActive(): PageTheme? = themeFor(activePage)
}
