package com.daybreak.animelauncher.theming

import android.graphics.Bitmap
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Analizador de wallpapers para la extracción de paletas de color adaptativas (Fase 1: Fundamentos).
 *
 * Responsabilidad estricta:
 * - Análisis y extracción de color a partir de un [Bitmap].
 * - Reducción en memoria (~200x200 px) para alto rendimiento y bajo consumo de RAM.
 * - Ejecución asíncrona fuera del hilo principal mediante [Dispatchers.Default].
 * - Resiliencia ante swatches ausentes o imágenes monocromáticas mediante fallbacks robustos.
 *
 * No posee dependencias con Compose, vistas, ViewModels ni almacenamiento persistente.
 */
object WallpaperAnalyzer {

    /** Dimensión máxima recomendada para el bitmap temporal de análisis (~200x200 px). */
    private const val TARGET_MAX_DIMENSION = 200

    /**
     * Analiza de forma asíncrona el [bitmap] dado en el despachador [dispatcher] (por defecto [Dispatchers.Default]),
     * retornando un [ThemePalette] inmutable con los colores extraídos y calculados.
     */
    suspend fun analyze(
        bitmap: Bitmap?,
        dispatcher: CoroutineDispatcher = Dispatchers.Default
    ): ThemePalette = withContext(dispatcher) {
        analyzeSync(bitmap)
    }

    /**
     * Analiza de forma síncrona el [bitmap] extrayendo los swatches relevantes de [Palette].
     * Maneja bitmaps nulos, reciclados, monocromáticos o resoluciones extremas sin crashear.
     */
    fun analyzeSync(bitmap: Bitmap?): ThemePalette {
        if (bitmap == null || bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return ThemePalette.DEFAULT
        }

        val (scaledBitmap, needsRecycle) = downscaleIfNeeded(bitmap, TARGET_MAX_DIMENSION)

        return try {
            val sampleColor = try {
                scaledBitmap.getPixel(scaledBitmap.width / 2, scaledBitmap.height / 2)
            } catch (_: Throwable) {
                ThemePalette.DEFAULT.dominantColor
            }

            val palette = Palette.from(scaledBitmap)
                .clearFilters()
                .maximumColorCount(16)
                .generate()

            extractPalette(palette, sampleColor)
        } catch (e: Throwable) {
            ThemePalette.DEFAULT
        } finally {
            if (needsRecycle && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
        }
    }

    /**
     * Extrae y sintetiza un [ThemePalette] garantizando valores utilizables para cada componente
     * incluso cuando [Palette] no genere swatches específicos.
     */
    internal fun extractPalette(
        palette: Palette,
        @ColorInt fallbackColor: Int = ThemePalette.DEFAULT.dominantColor
    ): ThemePalette {
        val swatches = palette.swatches

        // 1. Swatch dominante
        val dominantSwatch = palette.dominantSwatch
            ?: swatches.maxByOrNull { it.population }

        val dominantColor = dominantSwatch?.rgb ?: fallbackColor

        // 2. Swatch vibrante (con fallback a light/dark vibrant, dominante o default)
        val vibrantSwatch = palette.vibrantSwatch
            ?: palette.lightVibrantSwatch
            ?: palette.darkVibrantSwatch

        val vibrantColor = vibrantSwatch?.rgb
            ?: dominantSwatch?.rgb
            ?: palette.mutedSwatch?.rgb
            ?: dominantColor

        // 3. Swatch apagado / muted (con fallback a dark/light muted, dominante o default)
        val mutedSwatch = palette.mutedSwatch
            ?: palette.darkMutedSwatch
            ?: palette.lightMutedSwatch

        val mutedColor = mutedSwatch?.rgb
            ?: dominantSwatch?.rgb
            ?: palette.vibrantSwatch?.rgb
            ?: dominantColor

        // 4. Cálculo de luminancia relativa promedio ponderada por la población de cada swatch
        val averageLuminance = calculateAverageLuminance(swatches, dominantColor)

        // 5. Clasificación clara u oscura
        val isDark = averageLuminance < 0.5f

        return ThemePalette(
            dominantColor = dominantColor,
            vibrantColor = vibrantColor,
            mutedColor = mutedColor,
            isDark = isDark,
            averageLuminance = averageLuminance
        )
    }

    /**
     * Calcula la luminancia relativa promedio ponderada según la población de píxeles
     * de los swatches generados. Si no hay swatches disponibles, deriva la luminancia del [dominantColor].
     */
    internal fun calculateAverageLuminance(
        swatches: List<Palette.Swatch>,
        @ColorInt dominantColor: Int
    ): Float {
        if (swatches.isEmpty()) {
            return ColorUtils.calculateLuminance(dominantColor).toFloat().coerceIn(0.0f, 1.0f)
        }

        var totalPopulation = 0L
        var weightedLuminanceSum = 0.0

        for (swatch in swatches) {
            val population = swatch.population
            if (population > 0) {
                totalPopulation += population
                val swatchLuminance = ColorUtils.calculateLuminance(swatch.rgb)
                weightedLuminanceSum += (swatchLuminance * population)
            }
        }

        return if (totalPopulation > 0) {
            (weightedLuminanceSum / totalPopulation).toFloat().coerceIn(0.0f, 1.0f)
        } else {
            ColorUtils.calculateLuminance(dominantColor).toFloat().coerceIn(0.0f, 1.0f)
        }
    }

    /**
     * Redimensiona proporcionalmente el [bitmap] para que ninguna de sus dimensiones supere [maxDimension].
     * Si el bitmap ya tiene dimensiones iguales o menores, se retorna tal cual indicando que no debe reciclarse.
     */
    internal fun downscaleIfNeeded(bitmap: Bitmap, maxDimension: Int): Pair<Bitmap, Boolean> {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return Pair(bitmap, false)
        }

        val scale = minOf(
            maxDimension.toFloat() / width,
            maxDimension.toFloat() / height
        )

        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)

        val scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        return Pair(scaled, scaled != bitmap)
    }
}
