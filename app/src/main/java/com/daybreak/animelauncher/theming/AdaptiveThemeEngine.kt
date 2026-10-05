package com.daybreak.animelauncher.theming

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.daybreak.animelauncher.AdvancedStyleConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * UI-1: motor de resolución del tema runtime por página.
 *
 * FASE 3 — AUTO para fondos de IMAGEN:
 * - Pipeline: backgroundUri → bitmap (decode reducido) → [WallpaperAnalyzer]
 *   → [ThemeProposalGenerator] (autoridad matemática, sin duplicar) → selección
 *   determinista ("Equilibrado", fallback primera propuesta) → tokens + colores
 *   por elemento (anti-patrón eliminado: status → textPrimary/secondary, NO accent).
 * - Caché pequeña por URI de PALETAS (no bitmaps); invalidación implícita por clave.
 * - Análisis SIEMPRE fuera del Main Thread ([resolveAuto] es suspend y decodifica
 *   en el dispatcher inyectado).
 *
 * MANUAL: sin IO, sin análisis — paridad exacta (Fase 1/2).
 * [resolve] (sync) mantiene AUTO/AUTO_WITH_OVERRIDES → fail-safe MANUAL: la
 * resolución real de AUTO es asíncrona ([resolveAuto]) y su wrapper Compose parte
 * de este fail-safe como estado inicial.
 */
object AdaptiveThemeEngine {

    /** Etiqueta de la propuesta determinista usada en AUTO. */
    const val AUTO_PROPOSAL_LABEL = "Equilibrado"

    private const val MAX_PALETTE_CACHE = 12
    private const val TARGET_DECODE_DIMENSION = 200

    private val lock = Any()
    private val paletteCache = LinkedHashMap<String, ThemePalette>(8, 0.75f, true)

    // =========================================================================
    // Resolución sync (MANUAL real; AUTO fail-safe)
    // =========================================================================

    fun resolve(
        pageIndex: Int,
        backgroundUri: String?,
        mode: ThemeMode,
        styleConfig: AdvancedStyleConfig
    ): PageTheme = resolveManual(pageIndex, styleConfig)

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

    // =========================================================================
    // Resolución AUTO (suspend; imágenes). Reutiliza WallpaperAnalyzer y
    // ThemeProposalGenerator — NO duplica matemática de color.
    // =========================================================================

    /**
     * Resuelve el tema de una página en modo AUTO/AUTO_WITH_OVERRIDES a partir del
     * fondo de imagen. [paletteLoader] permite inyectar la carga en tests; en
     * producción decodifica el URI con downsample y analiza con [WallpaperAnalyzer].
     *
     * Fail-safe: URI nulo/inválido o error de decodificación → paleta DEFAULT.
     */
    suspend fun resolveAuto(
        pageIndex: Int,
        backgroundUri: String?,
        styleConfig: AdvancedStyleConfig,
        context: Context? = null,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        paletteLoader: suspend (String) -> ThemePalette? = { uri ->
            context?.let { ctx -> loadPaletteFromUri(ctx, uri, dispatcher) }
        }
    ): PageTheme {
        // MANUAL se auto-contiene: sin análisis aunque se llame por error.
        if (styleConfig.themeMode == ThemeMode.MANUAL) {
            return resolveManual(pageIndex, styleConfig)
        }
        val palette = if (backgroundUri.isNullOrBlank()) {
            ThemePalette.DEFAULT
        } else {
            cachedPalette(backgroundUri) { paletteLoader(backgroundUri) } ?: ThemePalette.DEFAULT
        }
        return buildAutoPageTheme(pageIndex, palette, styleConfig)
    }

    /**
     * Construye el PageTheme AUTO a partir de una paleta ya resuelta (puro,
     * determinista, testeable sin Android).
     */
    fun buildAutoPageTheme(
        pageIndex: Int,
        palette: ThemePalette,
        styleConfig: AdvancedStyleConfig
    ): PageTheme {
        val proposals = ThemeProposalGenerator.generateProposals(palette)
        val proposal = proposals.firstOrNull { it.label == AUTO_PROPOSAL_LABEL }
            ?: proposals.firstOrNull()
            ?: ThemeProposalGenerator.generateProposals(ThemePalette.DEFAULT).first()
        val tokens = proposal.proposedTokens

        // Anti-patrón eliminado: status usa texto adaptativo; accent solo highlights.
        val baseElements = AdaptiveElementColors(
            clock = tokens.textPrimaryColor,
            battery = tokens.textPrimaryColor,
            messages = tokens.textSecondaryColor,
            weekday = tokens.textPrimaryColor,
            date = tokens.textSecondaryColor,
            icon = tokens.textPrimaryColor,
            miButton = tokens.accentColor,
            miText = tokens.textOnAccentColor
        )
        // Overrides EXPLÍCITOS: únicamente en AUTO_WITH_OVERRIDES (AUTO puro ignora
        // los overrides — diferenciación exigida por el mandato §5).
        val elements = if (styleConfig.themeMode == ThemeMode.AUTO_WITH_OVERRIDES) {
            applyOverrides(baseElements, styleConfig.adaptiveOverrides, tokens)
        } else {
            baseElements
        }

        // Glass premium adaptativo: scrim derivado de la paleta (fórmula
        // determinista: luminancia media + complejidad/varianza entre swatches).
        // - Base 0.40 (superficie de lectura estable).
        // - +0.30 × complejidad (fondos multicolor exigen más scrim).
        // - +0.15 × desvío de luminancia respecto del medio (extremos oscuros/
        //   claros necesitan más superficie para mantener el par de contraste).
        // Tope 0.65 para no opacar el fondo. El generador garantiza el par
        // textPrimary/textSecondary vs superficie >= 4.5/3.0:1.
        val complexity = paletteComplexity(palette)
        val scrimAlpha = (0.40f + 0.30f * complexity +
            0.15f * kotlin.math.abs(palette.averageLuminance - 0.5f)).coerceIn(0.40f, 0.65f)
        val glass = GlassParams(
            scrimColor = tokens.surfaceColor,
            scrimAlpha = scrimAlpha,
            gradientTopColor = tokens.surfaceColor,
            gradientBottomColor = tokens.surfaceColor,
            borderColor = tokens.accentColor,
            borderAlpha = styleConfig.glassBorderAlpha,
            highlightColor = tokens.accentColor,
            cornerRadius = styleConfig.cornerRadius,
            gradientAlpha = 0.5f,
            highlightAlpha = 0.35f,
            glowColor = tokens.accentColor,
            glowAlpha = 0.10f,
            adaptive = true
        )
        return PageTheme(
            pageIndex = pageIndex,
            tokens = tokens,
            glass = glass,
            palette = palette,
            isDark = proposal.isDark,
            elementColors = elements
        )
    }

    /**
     * Complejidad/varianza perceptual aproximada de la paleta (0..1): distancia
     * Manhattan normalizada entre los swatches dominant/vibrant/muted. Fondos
     * multicolor → mayor valor → más scrim de lectura.
     */
    fun paletteComplexity(palette: ThemePalette): Float {
        fun dist(c1: Int, c2: Int): Float {
            val dr = kotlin.math.abs((c1 ushr 16 and 0xFF) - (c2 ushr 16 and 0xFF))
            val dg = kotlin.math.abs((c1 ushr 8 and 0xFF) - (c2 ushr 8 and 0xFF))
            val db = kotlin.math.abs((c1 and 0xFF) - (c2 and 0xFF))
            return (dr + dg + db) / 765f
        }
        val d1 = dist(palette.dominantColor, palette.vibrantColor)
        val d2 = dist(palette.vibrantColor, palette.mutedColor)
        return ((d1 + d2) / 2f).coerceIn(0f, 1f)
    }

    /** Aplica solo los overrides explícitos (no-null); el resto queda AUTO. */
    fun applyOverrides(
        base: AdaptiveElementColors,
        overrides: AdaptiveColorOverrides?,
        tokens: LauncherThemeTokens
    ): AdaptiveElementColors {
        if (overrides == null) return base
        return base.copy(
            clock = overrides.clockColor?.let { LauncherThemeTokens.parseHexColor(it, base.clock) } ?: base.clock,
            battery = overrides.batteryColor?.let { LauncherThemeTokens.parseHexColor(it, base.battery) } ?: base.battery,
            messages = overrides.messagesColor?.let { LauncherThemeTokens.parseHexColor(it, base.messages) } ?: base.messages,
            date = overrides.dateColor?.let { LauncherThemeTokens.parseHexColor(it, base.date) } ?: base.date
        )
    }

    // =========================================================================
    // Caché por URI (solo paletas; pequeña, LRU, sin bitmaps)
    // =========================================================================

    private suspend fun cachedPalette(
        uri: String,
        loader: suspend () -> ThemePalette?
    ): ThemePalette? {
        synchronized(lock) { paletteCache[uri] }?.let { return it }
        val loaded = loader() ?: return null
        synchronized(lock) {
            paletteCache[uri] = loaded
            while (paletteCache.size > MAX_PALETTE_CACHE) {
                val eldest = paletteCache.keys.firstOrNull() ?: break
                paletteCache.remove(eldest)
            }
        }
        return loaded
    }

    /** Visible para tests: vaciado de la caché. */
    fun clearPaletteCache() = synchronized(lock) { paletteCache.clear() }

    /** Visible para tests: tamaño de la caché. */
    fun paletteCacheSize(): Int = synchronized(lock) { paletteCache.size }

    // =========================================================================
    // Decodificación reducida (producción) — fuera de Main vía dispatcher.
    // =========================================================================

    private suspend fun loadPaletteFromUri(
        context: Context,
        uriString: String,
        dispatcher: CoroutineDispatcher
    ): ThemePalette? = withContext(dispatcher) {
        try {
            val uri = Uri.parse(uriString)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { it: InputStream ->
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null

            var sample = 1
            while (bounds.outWidth / (sample * 2) >= TARGET_DECODE_DIMENSION ||
                bounds.outHeight / (sample * 2) >= TARGET_DECODE_DIMENSION
            ) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return@withContext null
            try {
                WallpaperAnalyzer.analyzeSync(bitmap)
            } finally {
                bitmap.recycle()
            }
        } catch (_: Exception) {
            null
        }
    }

    // =========================================================================
    // MANUAL (paridad exacta)
    // =========================================================================

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
        // Paridad: cada elemento conserva exactamente su color de AdvancedStyleConfig.
        val elements = AdaptiveElementColors(
            clock = LauncherThemeTokens.parseHexColor(styleConfig.clockColor, tokens.accentColor),
            battery = LauncherThemeTokens.parseHexColor(styleConfig.batteryColor, tokens.accentColor),
            messages = LauncherThemeTokens.parseHexColor(styleConfig.messagesColor, tokens.accentColor),
            weekday = LauncherThemeTokens.parseHexColor(styleConfig.dateColor, tokens.accentColor),
            date = LauncherThemeTokens.parseHexColor(styleConfig.dateColor, tokens.accentColor),
            icon = LauncherThemeTokens.parseHexColor(styleConfig.customIconColor, LauncherThemeTokens.DEFAULT.textPrimaryColor),
            miButton = LauncherThemeTokens.parseHexColor(styleConfig.miButtonColor, tokens.accentColor),
            miText = LauncherThemeTokens.parseHexColor(styleConfig.miTextColor, LauncherThemeTokens.DEFAULT_TEXT_ON_ACCENT)
        )
        return PageTheme(
            pageIndex = pageIndex,
            tokens = tokens,
            glass = glass,
            palette = ThemePalette.DEFAULT,
            isDark = ThemePalette.DEFAULT.isDark,
            elementColors = elements
        )
    }
}
