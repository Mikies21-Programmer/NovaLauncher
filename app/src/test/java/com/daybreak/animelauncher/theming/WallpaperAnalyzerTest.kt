package com.daybreak.animelauncher.theming

import android.graphics.Bitmap
import android.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pruebas unitarias de aislamiento para [WallpaperAnalyzer] y [ThemePalette]
 * correspondientes a la Fase 1 de Adaptive Wallpaper Theming.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WallpaperAnalyzerTest {

    private fun createSolidBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height) { color }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    private fun createQuadrantBitmap(width: Int, height: Int, c1: Int, c2: Int, c3: Int, c4: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val midX = width / 2
        val midY = height / 2
        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = when {
                    x < midX && y < midY -> c1
                    x >= midX && y < midY -> c2
                    x < midX && y >= midY -> c3
                    else -> c4
                }
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }

    @Test
    fun `1 imagen colorida produce colores consistentes y validos`() {
        val bitmap = createQuadrantBitmap(
            100, 100,
            Color.rgb(255, 0, 0),     // Rojo vibrante
            Color.rgb(0, 255, 0),     // Verde vibrante
            Color.rgb(0, 0, 255),     // Azul vibrante
            Color.rgb(255, 255, 0)    // Amarillo
        )

        val palette = WallpaperAnalyzer.analyzeSync(bitmap)

        assertNotNull(palette)
        assertTrue("Luminancia debe estar entre 0.0 y 1.0", palette.averageLuminance in 0.0f..1.0f)
        assertTrue("Color dominante debe ser opaco", Color.alpha(palette.dominantColor) > 0)
        assertTrue("Color vibrante debe ser opaco", Color.alpha(palette.vibrantColor) > 0)
        assertTrue("Color muted debe ser opaco", Color.alpha(palette.mutedColor) > 0)
    }

    @Test
    fun `2 imagen predominantemente oscura se clasifica como isDark true`() {
        val bitmap = createSolidBitmap(100, 100, Color.rgb(15, 15, 20))

        val palette = WallpaperAnalyzer.analyzeSync(bitmap)

        assertTrue("Fondo oscuro debe tener isDark = true", palette.isDark)
        assertTrue("Luminancia oscura debe ser baja (< 0.2)", palette.averageLuminance < 0.2f)
    }

    @Test
    fun `3 imagen predominantemente clara se clasifica como isDark false`() {
        val bitmap = createSolidBitmap(100, 100, Color.rgb(240, 245, 250))

        val palette = WallpaperAnalyzer.analyzeSync(bitmap)

        assertFalse("Fondo claro debe tener isDark = false", palette.isDark)
        assertTrue("Luminancia clara debe ser alta (> 0.7)", palette.averageLuminance > 0.7f)
    }

    @Test
    fun `4 imagen monocromatica no crashea y genera paleta utilizable`() {
        val bitmap = createSolidBitmap(80, 80, Color.rgb(128, 128, 128))

        val palette = WallpaperAnalyzer.analyzeSync(bitmap)

        assertNotNull(palette)
        assertTrue("Luminancia de gris medio debe estar en rango medio", palette.averageLuminance in 0.15f..0.35f)
        assertTrue("isDark consistente", palette.isDark)
        // En imagen gris puro, el fallback para vibrante debe resolver a un color usable sin excepciones
        assertTrue("Color vibrante fallback debe ser opaco", Color.alpha(palette.vibrantColor) > 0)
    }

    @Test
    fun `5 ausencia de vibrant swatch usa fallback coherente sin crash`() {
        // Un bitmap con solo tonos de gris y sepia tenue sin saturación suficiente para swatch vibrante
        val bitmap = createQuadrantBitmap(
            60, 60,
            Color.rgb(80, 80, 80),
            Color.rgb(100, 100, 100),
            Color.rgb(120, 120, 120),
            Color.rgb(90, 90, 90)
        )

        val rawPalette = Palette.from(bitmap).generate()
        val themePalette = WallpaperAnalyzer.extractPalette(rawPalette)

        assertNotNull(themePalette)
        assertTrue("Vibrant color con fallback debe tener valor no cero", themePalette.vibrantColor != 0)
        assertEquals("Dominante y vibrante deben coincidir o tener fallback seguro", themePalette.dominantColor, themePalette.vibrantColor)
    }

    @Test
    fun `6 ausencia de muted swatch usa fallback coherente sin crash`() {
        // Colores 100% saturados puros sin presencia de tonos tenues o apagados
        val bitmap = createQuadrantBitmap(
            60, 60,
            Color.rgb(255, 0, 0),
            Color.rgb(0, 255, 0),
            Color.rgb(0, 0, 255),
            Color.rgb(255, 255, 0)
        )

        val rawPalette = Palette.from(bitmap).generate()
        val themePalette = WallpaperAnalyzer.extractPalette(rawPalette)

        assertNotNull(themePalette)
        assertTrue("Muted color con fallback debe tener valor no cero", themePalette.mutedColor != 0)
        assertTrue("Alpha de muted debe ser opaco", Color.alpha(themePalette.mutedColor) > 0)
    }

    @Test
    fun `7 resolucion pequena no crashea ni sufre desbordamiento`() {
        val tinyBitmap = createSolidBitmap(2, 2, Color.rgb(200, 50, 50))

        val palette = WallpaperAnalyzer.analyzeSync(tinyBitmap)

        assertNotNull(palette)
        assertTrue("Luminancia calculada en bitmap 2x2", palette.averageLuminance in 0.0f..1.0f)
    }

    @Test
    fun `8 valores de ThemePalette son consistentes y reproducibles`() {
        val bitmap1 = createSolidBitmap(50, 50, Color.rgb(30, 30, 80))
        val bitmap2 = createSolidBitmap(50, 50, Color.rgb(30, 30, 80))

        val palette1 = WallpaperAnalyzer.analyzeSync(bitmap1)
        val palette2 = WallpaperAnalyzer.analyzeSync(bitmap2)

        assertEquals("Dominant color debe ser determinista", palette1.dominantColor, palette2.dominantColor)
        assertEquals("Vibrant color debe ser determinista", palette1.vibrantColor, palette2.vibrantColor)
        assertEquals("isDark debe ser idéntico", palette1.isDark, palette2.isDark)
        assertEquals("Luminancia debe ser idéntica", palette1.averageLuminance, palette2.averageLuminance, 0.001f)
    }

    @Test
    fun `9 bitmap nulo o reciclado devuelve ThemePalette DEFAULT`() {
        val nullResult = WallpaperAnalyzer.analyzeSync(null)
        assertEquals(ThemePalette.DEFAULT, nullResult)

        val bitmap = createSolidBitmap(10, 10, Color.RED)
        bitmap.recycle()
        val recycledResult = WallpaperAnalyzer.analyzeSync(bitmap)
        assertEquals(ThemePalette.DEFAULT, recycledResult)
    }

    @Test
    fun `10 bitmap de gran resolucion se redimensiona a 200px max`() {
        val largeBitmap = createSolidBitmap(800, 1200, Color.rgb(10, 10, 50))
        val (scaled, wasScaled) = WallpaperAnalyzer.downscaleIfNeeded(largeBitmap, 200)

        assertTrue("Debe haberse generado un bitmap escalado", wasScaled)
        assertTrue("Ancho escalado debe ser <= 200", scaled.width <= 200)
        assertTrue("Alto escalado debe ser <= 200", scaled.height <= 200)
        assertEquals("Alto escalado debe ser exactamente 200", 200, scaled.height)

        scaled.recycle()
        largeBitmap.recycle()
    }

    @Test
    fun `11 ejecucion asincrona con analyze corrutina en Dispatchers Default`() = runBlocking {
        val bitmap = createSolidBitmap(40, 40, Color.rgb(0, 240, 255))
        val palette = WallpaperAnalyzer.analyze(bitmap)

        assertNotNull(palette)
        assertFalse("Cyan brillante no debe ser isDark", palette.isDark)
    }
}
