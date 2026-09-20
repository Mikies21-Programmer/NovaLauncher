package com.daybreak.animelauncher.theming

import androidx.annotation.ColorInt
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Generador puro, determinista y matemáticamente validado de propuestas de tema
 * para NovaLauncher (Adaptive Wallpaper Theming - Fase 2C).
 *
 * Responsabilidad:
 * Recibir un [ThemePalette] y sintetizar entre 1 y 4 [ThemeProposal] semánticas
 * (Dominante, Vibrante, Equilibrado), garantizando contrastes WCAG 2.1 (>= 4.5:1 para
 * texto principal, >= 3:1 para secundario y acento), deduplicación perceptual y
 * aislamiento absoluto respecto a Compose y al framework de UI de Android.
 */
object ThemeProposalGenerator {

    private const val MIN_CONTRAST_TEXT_PRIMARY = 4.5f
    private const val MIN_CONTRAST_TEXT_SECONDARY = 3.0f
    private const val MIN_CONTRAST_ACCENT = 3.0f
    private const val MIN_CONTRAST_BORDER = 1.5f

    // Fallbacks base para superficies
    private const val DARK_SURFACE_BASE = 0xFF08080C.toInt()   // ObsidianCard NovaLauncher
    private const val LIGHT_SURFACE_BASE = 0xFFF5F6F8.toInt()  // Light Glass Card

    // Fallbacks base para texto
    private const val DARK_TEXT_PRIMARY = 0xFF111216.toInt()
    private const val DARK_TEXT_SECONDARY = 0xFF585C64.toInt()
    private const val LIGHT_TEXT_PRIMARY = 0xFFFFFFFF.toInt()
    private const val LIGHT_TEXT_SECONDARY = 0xFFD0D4DC.toInt()

    /**
     * Genera una lista ordenada y deduplicada de propuestas de tema a partir de una [palette].
     *
     * @param palette Paleta analizada extraída del wallpaper.
     * @return Lista inmutable de [ThemeProposal] válidas y contrastadas (1 a 4 elementos).
     */
    fun generateProposals(palette: ThemePalette): List<ThemeProposal> {
        val safePalette = sanitizePalette(palette)
        val isDark = safePalette.isDark

        val candidates = mutableListOf<ThemeProposal>()

        // 1. Propuesta A: Dominante (identidad del wallpaper)
        buildDominantProposal(safePalette, isDark)?.let { candidates.add(it) }

        // 2. Propuesta B: Vibrante (acento llamativo / estilo cyberpunk)
        buildVibrantProposal(safePalette, isDark)?.let { candidates.add(it) }

        // 3. Propuesta C: Equilibrada (tonos atenuados / sobrios)
        buildMutedProposal(safePalette, isDark)?.let { candidates.add(it) }

        // Deduplicación perceptual determinista
        val deduplicated = deduplicateProposals(candidates, safePalette)

        // Fallback garantizado: si todas las variantes fueron rechazadas o duplicadas
        if (deduplicated.isEmpty()) {
            return listOf(createFallbackProposal(safePalette))
        }

        // Reasignar orden 1-indexed correlativo
        return deduplicated.mapIndexed { index, proposal ->
            proposal.copy(order = index + 1)
        }
    }

    // =========================================================================
    // Constructores semánticos de propuestas
    // =========================================================================

    private fun buildDominantProposal(palette: ThemePalette, isDark: Boolean): ThemeProposal? {
        val baseColor = palette.dominantColor
        val surface = createSurface(baseColor, isDark, blendRatio = 0.10f)
        val accent = ensureContrast(baseColor, surface, MIN_CONTRAST_ACCENT, isDark)

        val textPrimary = if (isDark) {
            ensureContrast(LIGHT_TEXT_PRIMARY, surface, MIN_CONTRAST_TEXT_PRIMARY, true)
        } else {
            ensureContrast(DARK_TEXT_PRIMARY, surface, MIN_CONTRAST_TEXT_PRIMARY, false)
        }

        val textSecondary = if (isDark) {
            ensureContrast(LIGHT_TEXT_SECONDARY, surface, MIN_CONTRAST_TEXT_SECONDARY, true)
        } else {
            ensureContrast(DARK_TEXT_SECONDARY, surface, MIN_CONTRAST_TEXT_SECONDARY, false)
        }

        val border = ensureContrast(accent, surface, MIN_CONTRAST_BORDER, isDark)

        val tokens = LauncherThemeTokens(
            accentColor = accent,
            surfaceColor = surface,
            textPrimaryColor = textPrimary,
            textSecondaryColor = textSecondary,
            borderColor = border
        )

        return if (validateTokens(tokens)) {
            ThemeProposal(
                id = "proposal_dominant",
                palette = palette,
                proposedTokens = tokens,
                isDark = isDark,
                label = "Dominante",
                order = 1
            )
        } else null
    }

    private fun buildVibrantProposal(palette: ThemePalette, isDark: Boolean): ThemeProposal? {
        val baseColor = palette.vibrantColor
        // La superficie se fundamenta en dominant o vibrant para mantener armonía
        val surface = createSurface(palette.dominantColor, isDark, blendRatio = 0.08f)

        // Potenciamos saturación moderada en vibrant manteniendo armonía
        val vibrantBoosted = adjustSaturation(baseColor, minSaturation = 0.55f)
        val accent = ensureContrast(vibrantBoosted, surface, MIN_CONTRAST_ACCENT, isDark)

        val textPrimary = if (isDark) {
            ensureContrast(LIGHT_TEXT_PRIMARY, surface, MIN_CONTRAST_TEXT_PRIMARY, true)
        } else {
            ensureContrast(DARK_TEXT_PRIMARY, surface, MIN_CONTRAST_TEXT_PRIMARY, false)
        }

        val textSecondary = if (isDark) {
            ensureContrast(LIGHT_TEXT_SECONDARY, surface, MIN_CONTRAST_TEXT_SECONDARY, true)
        } else {
            ensureContrast(DARK_TEXT_SECONDARY, surface, MIN_CONTRAST_TEXT_SECONDARY, false)
        }

        // Borde neón a juego con el acento
        val border = ensureContrast(accent, surface, MIN_CONTRAST_BORDER, isDark)

        val tokens = LauncherThemeTokens(
            accentColor = accent,
            surfaceColor = surface,
            textPrimaryColor = textPrimary,
            textSecondaryColor = textSecondary,
            borderColor = border
        )

        return if (validateTokens(tokens)) {
            ThemeProposal(
                id = "proposal_vibrant",
                palette = palette,
                proposedTokens = tokens,
                isDark = isDark,
                label = "Vibrante",
                order = 2
            )
        } else null
    }

    private fun buildMutedProposal(palette: ThemePalette, isDark: Boolean): ThemeProposal? {
        val baseColor = palette.mutedColor
        val surface = createSurface(baseColor, isDark, blendRatio = 0.08f)

        // Atenuamos saturación para mantener perfil sobrio y elegante
        val mutedSoft = adjustSaturation(baseColor, maxSaturation = 0.40f)
        val accent = ensureContrast(mutedSoft, surface, MIN_CONTRAST_ACCENT, isDark)

        val textPrimary = if (isDark) {
            ensureContrast(LIGHT_TEXT_PRIMARY, surface, MIN_CONTRAST_TEXT_PRIMARY, true)
        } else {
            ensureContrast(DARK_TEXT_PRIMARY, surface, MIN_CONTRAST_TEXT_PRIMARY, false)
        }

        val textSecondary = if (isDark) {
            ensureContrast(LIGHT_TEXT_SECONDARY, surface, MIN_CONTRAST_TEXT_SECONDARY, true)
        } else {
            ensureContrast(DARK_TEXT_SECONDARY, surface, MIN_CONTRAST_TEXT_SECONDARY, false)
        }

        // Delimitador suave derivado del muted
        val borderDerived = blendColors(surface, accent, 0.45f)
        val border = ensureContrast(borderDerived, surface, MIN_CONTRAST_BORDER, isDark)

        val tokens = LauncherThemeTokens(
            accentColor = accent,
            surfaceColor = surface,
            textPrimaryColor = textPrimary,
            textSecondaryColor = textSecondary,
            borderColor = border
        )

        return if (validateTokens(tokens)) {
            ThemeProposal(
                id = "proposal_muted",
                palette = palette,
                proposedTokens = tokens,
                isDark = isDark,
                label = "Equilibrado",
                order = 3
            )
        } else null
    }

    private fun createFallbackProposal(palette: ThemePalette): ThemeProposal {
        return ThemeProposal(
            id = "proposal_fallback",
            palette = palette,
            proposedTokens = LauncherThemeTokens.DEFAULT,
            isDark = true,
            label = "Por defecto",
            order = 1
        )
    }

    // =========================================================================
    // Deduplicación perceptual
    // =========================================================================

    private fun deduplicateProposals(
        candidates: List<ThemeProposal>,
        palette: ThemePalette
    ): List<ThemeProposal> {
        val accepted = mutableListOf<ThemeProposal>()

        for (candidate in candidates) {
            val isDuplicate = accepted.any { existing ->
                areTokensPerceptuallySimilar(candidate.proposedTokens, existing.proposedTokens)
            }
            if (!isDuplicate) {
                accepted.add(candidate)
            }
            // Límite estricto de máximo 4 propuestas
            if (accepted.size >= 4) break
        }

        return accepted
    }

    private fun areTokensPerceptuallySimilar(
        t1: LauncherThemeTokens,
        t2: LauncherThemeTokens
    ): Boolean {
        val accentDist = rgbManhattanDistance(t1.accentColor, t2.accentColor)
        val surfaceDist = rgbManhattanDistance(t1.surfaceColor, t2.surfaceColor)

        // Dos propuestas se consideran duplicadas si su acento y superficie son esencialmente indistinguibles
        return accentDist < 30 && surfaceDist < 25
    }

    private fun rgbManhattanDistance(c1: Int, c2: Int): Int {
        val r1 = (c1 ushr 16) and 0xFF
        val g1 = (c1 ushr 8) and 0xFF
        val b1 = c1 and 0xFF

        val r2 = (c2 ushr 16) and 0xFF
        val g2 = (c2 ushr 8) and 0xFF
        val b2 = c2 and 0xFF

        return abs(r1 - r2) + abs(g1 - g2) + abs(b1 - b2)
    }

    // =========================================================================
    // Validación de contraste y tokens
    // =========================================================================

    private fun validateTokens(tokens: LauncherThemeTokens): Boolean {
        val textPrimaryRatio = calculateContrastRatio(tokens.textPrimaryColor, tokens.surfaceColor)
        val textSecondaryRatio = calculateContrastRatio(tokens.textSecondaryColor, tokens.surfaceColor)
        val accentRatio = calculateContrastRatio(tokens.accentColor, tokens.surfaceColor)
        val borderRatio = calculateContrastRatio(tokens.borderColor, tokens.surfaceColor)

        return textPrimaryRatio >= MIN_CONTRAST_TEXT_PRIMARY &&
                textSecondaryRatio >= MIN_CONTRAST_TEXT_SECONDARY &&
                accentRatio >= MIN_CONTRAST_ACCENT &&
                borderRatio >= MIN_CONTRAST_BORDER
    }

    // =========================================================================
    // Motor matemático de color y contraste puro
    // =========================================================================

    /**
     * Garantiza que [color] cumpla al menos [minContrast] frente a [surfaceColor].
     * Si no cumple, ajusta su luminancia iterativa y deterministamente manteniendo el tono (Hue).
     */
    fun ensureContrast(
        @ColorInt color: Int,
        @ColorInt surfaceColor: Int,
        minContrast: Float,
        isDarkSurface: Boolean
    ): Int {
        var current = sanitizeColor(color, if (isDarkSurface) LIGHT_TEXT_PRIMARY else DARK_TEXT_PRIMARY)
        if (calculateContrastRatio(current, surfaceColor) >= minContrast) {
            return current
        }

        val hsl = rgbToHsl(current)
        val h = hsl[0]
        val s = hsl[1]
        var l = hsl[2]

        val step = 0.04f
        val maxIterations = 20

        for (i in 0 until maxIterations) {
            l = if (isDarkSurface) {
                (l + step).coerceAtMost(0.96f)
            } else {
                (l - step).coerceAtLeast(0.06f)
            }
            current = hslToRgb(h, s, l)
            if (calculateContrastRatio(current, surfaceColor) >= minContrast) {
                return current
            }
        }

        // Si la saturación impide alcanzar el contraste, desaturar hacia extremo seguro
        return if (isDarkSurface) LIGHT_TEXT_PRIMARY else DARK_TEXT_PRIMARY
    }

    /**
     * Calcula la relación de contraste WCAG 2.1 entre dos colores: (L1 + 0.05) / (L2 + 0.05).
     * Rango: 1.0 (sin contraste) a 21.0 (máximo contraste).
     */
    fun calculateContrastRatio(@ColorInt foreground: Int, @ColorInt background: Int): Float {
        val l1 = calculateRelativeLuminance(foreground)
        val l2 = calculateRelativeLuminance(background)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    /**
     * Calcula la luminancia relativa WCAG 2.1 de un color ARGB en el rango [0.0, 1.0].
     */
    fun calculateRelativeLuminance(@ColorInt color: Int): Float {
        val r = ((color ushr 16) and 0xFF) / 255f
        val g = ((color ushr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f

        val rLin = linearizeChannel(r)
        val gLin = linearizeChannel(g)
        val bLin = linearizeChannel(b)

        return 0.2126f * rLin + 0.7152f * gLin + 0.0722f * bLin
    }

    private fun linearizeChannel(channel: Float): Float {
        return if (channel <= 0.04045f) {
            channel / 12.92f
        } else {
            ((channel + 0.055f) / 1.055f).pow(2.4f)
        }
    }

    /**
     * Genera una superficie refinada mezclando el color base con una superficie neutra.
     */
    private fun createSurface(@ColorInt baseColor: Int, isDark: Boolean, blendRatio: Float): Int {
        val baseSurface = if (isDark) DARK_SURFACE_BASE else LIGHT_SURFACE_BASE
        return blendColors(baseSurface, baseColor, blendRatio.coerceIn(0f, 0.20f))
    }

    /**
     * Mezcla linealmente dos colores ARGB opacos: (1 - ratio) * c1 + ratio * c2.
     */
    fun blendColors(@ColorInt c1: Int, @ColorInt c2: Int, ratio: Float): Int {
        val clampedRatio = ratio.coerceIn(0f, 1f)
        val invRatio = 1f - clampedRatio

        val r1 = (c1 ushr 16) and 0xFF
        val g1 = (c1 ushr 8) and 0xFF
        val b1 = c1 and 0xFF

        val r2 = (c2 ushr 16) and 0xFF
        val g2 = (c2 ushr 8) and 0xFF
        val b2 = c2 and 0xFF

        val r = (invRatio * r1 + clampedRatio * r2).roundToInt().coerceIn(0, 255)
        val g = (invRatio * g1 + clampedRatio * g2).roundToInt().coerceIn(0, 255)
        val b = (invRatio * b1 + clampedRatio * b2).roundToInt().coerceIn(0, 255)

        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun adjustSaturation(@ColorInt color: Int, minSaturation: Float = 0f, maxSaturation: Float = 1f): Int {
        val hsl = rgbToHsl(color)
        val originalS = hsl[1]
        // Si el color es acromático (gris/monocromático con saturación cercana a cero),
        // no forzar saturación mínima artificial que inventaría un tono inexistente.
        if (originalS < 0.05f && minSaturation > 0f) {
            return color
        }
        val clampedS = originalS.coerceIn(minSaturation, maxSaturation)
        return hslToRgb(hsl[0], clampedS, hsl[2])
    }

    /**
     * Sanitiza un color asegurando formato ARGB de 32 bits y opacidad completa (alpha = 0xFF).
     */
    fun sanitizeColor(@ColorInt color: Int, fallback: Int): Int {
        if (color == 0) return (0xFF shl 24) or (fallback and 0x00FFFFFF)
        return (0xFF shl 24) or (color and 0x00FFFFFF)
    }

    private fun sanitizePalette(palette: ThemePalette): ThemePalette {
        val dominant = sanitizeColor(palette.dominantColor, ThemePalette.DEFAULT.dominantColor)
        val vibrant = sanitizeColor(palette.vibrantColor, dominant)
        val muted = sanitizeColor(palette.mutedColor, dominant)

        val luminance = if (palette.averageLuminance.isNaN() || palette.averageLuminance.isInfinite()) {
            calculateRelativeLuminance(dominant)
        } else {
            palette.averageLuminance.coerceIn(0f, 1f)
        }

        return ThemePalette(
            dominantColor = dominant,
            vibrantColor = vibrant,
            mutedColor = muted,
            isDark = palette.isDark,
            averageLuminance = luminance
        )
    }

    // =========================================================================
    // Conversiones puras RGB <-> HSL
    // =========================================================================

    /**
     * Convierte un color ARGB a HSL en un FloatArray: [Hue 0..360, Saturation 0..1, Lightness 0..1].
     */
    fun rgbToHsl(@ColorInt color: Int): FloatArray {
        val r = ((color ushr 16) and 0xFF) / 255f
        val g = ((color ushr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        val l = (max + min) / 2f
        val s = if (delta == 0f) {
            0f
        } else {
            if (l <= 0.5f) delta / (max + min) else delta / (2f - max - min)
        }

        val h = if (delta == 0f) {
            0f
        } else {
            val rawH = when (max) {
                r -> ((g - b) / delta) % 6f
                g -> ((b - r) / delta) + 2f
                else -> ((r - g) / delta) + 4f
            }
            val deg = rawH * 60f
            if (deg < 0f) deg + 360f else deg
        }

        return floatArrayOf(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
    }

    /**
     * Convierte valores HSL a un entero ARGB opaco (alpha = 0xFF).
     */
    fun hslToRgb(h: Float, s: Float, l: Float): Int {
        val normalizedH = ((h % 360f) + 360f) % 360f
        val clampedS = s.coerceIn(0f, 1f)
        val clampedL = l.coerceIn(0f, 1f)

        val c = (1f - abs(2f * clampedL - 1f)) * clampedS
        val x = c * (1f - abs((normalizedH / 60f) % 2f - 1f))
        val m = clampedL - c / 2f

        val (rPrime, gPrime, bPrime) = when {
            normalizedH < 60f -> Triple(c, x, 0f)
            normalizedH < 120f -> Triple(x, c, 0f)
            normalizedH < 180f -> Triple(0f, c, x)
            normalizedH < 240f -> Triple(0f, x, c)
            normalizedH < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        val r = ((rPrime + m) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((gPrime + m) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((bPrime + m) * 255f).roundToInt().coerceIn(0, 255)

        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
