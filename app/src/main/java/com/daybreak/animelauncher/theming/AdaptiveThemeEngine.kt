package com.daybreak.animelauncher.theming

import com.daybreak.animelauncher.AdvancedStyleConfig

/**
 * UI-1: motor de resolución del tema runtime por página.
 *
 * FASE 1 — solo [ThemeMode.MANUAL]:
 * - NO analiza wallpapers, NO abre MediaMetadataRetriever, NO ejecuta IO.
 * - NO modifica colores: produce tokens/glass equivalentes a la implementación
 *   actual a partir de [AdvancedStyleConfig] (paridad MANUAL exacta).
 * - `backgroundUri` se recibe por API pero NO se procesa en este modo.
 *
 * AUTO / AUTO_WITH_OVERRIDES: la API ya está preparada; en Fase 1 resuelven con el
 * mismo camino determinista de MANUAL (fail-safe). Las fases siguientes conectarán
 * aquí `WallpaperAnalyzer`/`VideoFrameExtractor` + `ThemeProposalGenerator`
 * (autoridad de contraste existente, sin duplicar) y la capa de overrides.
 *
 * Sin acoplamiento a Compose ni a Android UI: entrada/salida son modelos puros.
 */
object AdaptiveThemeEngine {

    /**
     * Resuelve el [PageTheme] de una página.
     *
     * @param pageIndex índice de página del Pager.
     * @param backgroundUri fondo de la página (sin procesar en Fase 1 / MANUAL).
     * @param mode modo de tema.
     * @param styleConfig configuración persistente (fuente de verdad en MANUAL).
     */
    fun resolve(
        pageIndex: Int,
        backgroundUri: String?,
        mode: ThemeMode,
        styleConfig: AdvancedStyleConfig
    ): PageTheme {
        return when (mode) {
            ThemeMode.MANUAL -> resolveManual(pageIndex, styleConfig)
            // Fase 2: AUTO analizará backgroundUri y AUTO_WITH_OVERRIDES aplicará
            // los overrides per-elemento sobre la paleta generada. Fail-safe actual:
            // resolución determinista MANUAL (sin IO, sin cambios de color).
            ThemeMode.AUTO -> resolveManual(pageIndex, styleConfig)
            ThemeMode.AUTO_WITH_OVERRIDES -> resolveManual(pageIndex, styleConfig)
        }
    }

    /** Resuelve el estado completo para un conjunto de páginas (mismo modo/config). */
    fun resolveState(
        pageIndices: List<Int>,
        backgroundUris: Map<Int, String?>,
        activePage: Int,
        mode: ThemeMode,
        styleConfig: AdvancedStyleConfig
    ): AdaptiveThemeState {
        val perPage = pageIndices.associateWith { index ->
            resolve(index, backgroundUris[index], mode, styleConfig)
        }
        return AdaptiveThemeState(perPage = perPage, activePage = activePage)
    }

    /**
     * Paridad MANUAL exacta con la implementación actual:
     * - tokens: `LauncherThemeTokens.fromAdvancedStyleConfig` (única fuente).
     * - glass: mapeo directo de los campos existentes (sidebar/diagonal/accent).
     * - palette: DEFAULT estructural (MANUAL no analiza fondos).
     */
    private fun resolveManual(pageIndex: Int, styleConfig: AdvancedStyleConfig): PageTheme {
        val tokens = LauncherThemeTokens.fromAdvancedStyleConfig(styleConfig)
        val glass = GlassParams(
            scrimColor = LauncherThemeTokens.parseHexColor(
                styleConfig.sidebarColor, LauncherThemeTokens.DEFAULT.surfaceColor
            ),
            scrimAlpha = styleConfig.sidebarOpacity,
            gradientTopColor = LauncherThemeTokens.parseHexColor(
                styleConfig.diagonalBarColor, LauncherThemeTokens.DEFAULT.surfaceColor
            ),
            gradientBottomColor = LauncherThemeTokens.parseHexColor(
                styleConfig.sidebarColor, LauncherThemeTokens.DEFAULT.surfaceColor
            ),
            borderColor = tokens.accentColor,
            borderAlpha = styleConfig.glassBorderAlpha,
            highlightColor = tokens.accentColor,
            cornerRadius = styleConfig.cornerRadius
        )
        return PageTheme(
            pageIndex = pageIndex,
            tokens = tokens,
            glass = glass,
            palette = ThemePalette.DEFAULT,
            isDark = ThemePalette.DEFAULT.isDark
        )
    }
}
