package com.daybreak.animelauncher.theming

/**
 * UI-1 Fase 5: interpolación determinista de tokens/glass/elementos entre dos PageTheme.
 * Puro y testeable (sin Compose): el wrapper anima `fraction` 0→1 con tween y llama aquí.
 * `palette`, `isDark` y `pageIndex` se toman del target (b): la paleta es descriptiva
 * y el booleans no interpola; el color visible sí.
 */
object AdaptiveLerp {

    fun lerpColor(a: Int, b: Int, f: Float): Int {
        val cl = f.coerceIn(0f, 1f)
        val al = a.toLong() and 0xFFFFFFFFL
        val bl = b.toLong() and 0xFFFFFFFFL
        val aa = ((al shr 24) and 0xFF).toInt()
        val ar = ((al shr 16) and 0xFF).toInt()
        val ag = ((al shr 8) and 0xFF).toInt()
        val ab = (al and 0xFF).toInt()
        val ba = ((bl shr 24) and 0xFF).toInt()
        val br = ((bl shr 16) and 0xFF).toInt()
        val bg = ((bl shr 8) and 0xFF).toInt()
        val bb = (bl and 0xFF).toInt()
        fun m(x: Int, y: Int) = (x + ((y - x) * cl).toInt())
        return (m(aa, ba) shl 24) or (m(ar, br) shl 16) or (m(ag, bg) shl 8) or m(ab, bb)
    }

    fun lerpFloat(a: Float, b: Float, f: Float): Float =
        a + (b - a) * f.coerceIn(0f, 1f)

    fun lerpTokens(a: LauncherThemeTokens, b: LauncherThemeTokens, f: Float) = LauncherThemeTokens(
        accentColor = lerpColor(a.accentColor, b.accentColor, f),
        surfaceColor = lerpColor(a.surfaceColor, b.surfaceColor, f),
        textPrimaryColor = lerpColor(a.textPrimaryColor, b.textPrimaryColor, f),
        textSecondaryColor = lerpColor(a.textSecondaryColor, b.textSecondaryColor, f),
        borderColor = lerpColor(a.borderColor, b.borderColor, f),
        textOnAccentColor = lerpColor(a.textOnAccentColor, b.textOnAccentColor, f),
        statusColor = lerpColor(a.statusColor, b.statusColor, f)
    )

    fun lerpGlass(a: GlassParams, b: GlassParams, f: Float) = GlassParams(
        scrimColor = lerpColor(a.scrimColor, b.scrimColor, f),
        scrimAlpha = lerpFloat(a.scrimAlpha, b.scrimAlpha, f),
        gradientTopColor = lerpColor(a.gradientTopColor, b.gradientTopColor, f),
        gradientBottomColor = lerpColor(a.gradientBottomColor, b.gradientBottomColor, f),
        borderColor = lerpColor(a.borderColor, b.borderColor, f),
        borderAlpha = lerpFloat(a.borderAlpha, b.borderAlpha, f),
        highlightColor = lerpColor(a.highlightColor, b.highlightColor, f),
        cornerRadius = lerpFloat(a.cornerRadius, b.cornerRadius, f),
        gradientAlpha = lerpFloat(a.gradientAlpha, b.gradientAlpha, f),
        highlightAlpha = lerpFloat(a.highlightAlpha, b.highlightAlpha, f),
        glowColor = lerpColor(a.glowColor, b.glowColor, f),
        glowAlpha = lerpFloat(a.glowAlpha, b.glowAlpha, f),
        adaptive = b.adaptive
    )

    fun lerpElements(a: AdaptiveElementColors, b: AdaptiveElementColors, f: Float) =
        AdaptiveElementColors(
            clock = lerpColor(a.clock, b.clock, f),
            battery = lerpColor(a.battery, b.battery, f),
            messages = lerpColor(a.messages, b.messages, f),
            weekday = lerpColor(a.weekday, b.weekday, f),
            date = lerpColor(a.date, b.date, f),
            icon = lerpColor(a.icon, b.icon, f),
            miButton = lerpColor(a.miButton, b.miButton, f),
            miText = lerpColor(a.miText, b.miText, f)
        )

    fun lerpPageTheme(a: PageTheme, b: PageTheme, f: Float) = PageTheme(
        pageIndex = b.pageIndex,
        tokens = lerpTokens(a.tokens, b.tokens, f),
        glass = lerpGlass(a.glass, b.glass, f),
        palette = b.palette,
        isDark = b.isDark,
        elementColors = lerpElements(a.elementColors, b.elementColors, f)
    )
}
