package com.daybreak.animelauncher.theming

import androidx.annotation.ColorInt

/**
 * UI-1 Fase 3: overrides EXPLÍCITOS por elemento para AUTO_WITH_OVERRIDES.
 *
 * `null` = sin override (el elemento usa el color generado por AUTO).
 * Un color no-null = el usuario fijó explícitamente ese elemento (nunca se
 * interpreta un default heredado como override).
 *
 * Gson-safe: clase anidada con todos los campos con default; JSONs antiguos
 * sin este objeto resuelven al default (sin overrides).
 */
data class AdaptiveColorOverrides(
    val clockColor: String? = null,
    val batteryColor: String? = null,
    val messagesColor: String? = null,
    val dateColor: String? = null
)

/**
 * UI-1 Fase 3: colores resueltos por elemento, listos para consumo por
 * ViewOne/ViewTwo. El engine los deriva según el modo:
 * - MANUAL: desde AdvancedStyleConfig (paridad exacta).
 * - AUTO: desde los tokens generados (textPrimary/textSecondary/accent/textOnAccent).
 * - AUTO_WITH_OVERRIDES: AUTO + overrides no-null por elemento.
 */
data class AdaptiveElementColors(
    @get:ColorInt val clock: Int,
    @get:ColorInt val battery: Int,
    @get:ColorInt val messages: Int,
    @get:ColorInt val weekday: Int,
    @get:ColorInt val date: Int,
    @get:ColorInt val icon: Int,
    @get:ColorInt val miButton: Int,
    @get:ColorInt val miText: Int
)

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
    val cornerRadius: Float,
    // UI-1 Fase 4: capas de glass premium.
    val gradientAlpha: Float = 0f,
    val highlightAlpha: Float = 0f,
    @get:ColorInt val glowColor: Int = 0,
    val glowAlpha: Float = 0f,
    // true = glass premium adaptativo (AUTO); false = legacy (paridad MANUAL).
    val adaptive: Boolean = false
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
    val isDark: Boolean,
    val elementColors: AdaptiveElementColors
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
