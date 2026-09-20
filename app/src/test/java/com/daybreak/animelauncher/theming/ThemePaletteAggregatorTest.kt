package com.daybreak.animelauncher.theming

import org.junit.Assert.*
import org.junit.Test

/**
 * Pruebas unitarias para [ThemePaletteAggregator] (Adaptive Wallpaper Theming - Fase 2E).
 *
 * Valida la robustez matemática, determinismo, rechazo de outliers y ausencia de interpolación
 * artificial al agregar paletas de frames de video.
 */
class ThemePaletteAggregatorTest {

    @Test
    fun `lista vacia retorna ThemePalette DEFAULT`() {
        val result = ThemePaletteAggregator.aggregate(emptyList())
        assertEquals(ThemePalette.DEFAULT, result)
    }

    @Test
    fun `una sola paleta se retorna de forma segura e identica`() {
        val single = ThemePalette(
            dominantColor = 0xFF336699.toInt(),
            vibrantColor = 0xFF00FFCC.toInt(),
            mutedColor = 0xFF556677.toInt(),
            isDark = true,
            averageLuminance = 0.25f
        )
        val result = ThemePaletteAggregator.aggregate(listOf(single))
        assertEquals(single, result)
    }

    @Test
    fun `dos paletas seleccionan representante central y promedian luminancia`() {
        val p1 = ThemePalette(
            dominantColor = 0xFF102030.toInt(),
            vibrantColor = 0xFF00E5FF.toInt(),
            mutedColor = 0xFF405060.toInt(),
            isDark = true,
            averageLuminance = 0.10f
        )
        val p2 = ThemePalette(
            dominantColor = 0xFF203040.toInt(),
            vibrantColor = 0xFF00B0FF.toInt(),
            mutedColor = 0xFF506070.toInt(),
            isDark = true,
            averageLuminance = 0.20f
        )

        val result = ThemePaletteAggregator.aggregate(listOf(p1, p2))

        // Determinismo ante empate en 2 candidatos: selecciona el primer candidato
        assertEquals(p1.dominantColor, result.dominantColor)
        assertEquals(p1.vibrantColor, result.vibrantColor)
        assertEquals(p1.mutedColor, result.mutedColor)
        assertEquals(0.15f, result.averageLuminance, 0.001f)
        assertTrue(result.isDark)
    }

    @Test
    fun `tres paletas similares seleccionan el medoide central sin promediar RGB`() {
        // Tres tonos azules cercanos
        val colorA = 0xFF002244.toInt() // r=0, g=34, b=68
        val colorB = 0xFF00264A.toInt() // r=0, g=38, b=74  <- Medoide más cercano a A y C
        val colorC = 0xFF002A50.toInt() // r=0, g=42, b=80

        val p1 = ThemePalette(colorA, 0xFF00AAEE.toInt(), 0xFF334455.toInt(), true, 0.12f)
        val p2 = ThemePalette(colorB, 0xFF00BBEE.toInt(), 0xFF354657.toInt(), true, 0.14f)
        val p3 = ThemePalette(colorC, 0xFF00CCEE.toInt(), 0xFF374859.toInt(), true, 0.16f)

        val result = ThemePaletteAggregator.aggregate(listOf(p1, p2, p3))

        // El color resultante debe ser exactamente uno de los colores candidatos existentes
        assertTrue(
            result.dominantColor == colorA || result.dominantColor == colorB || result.dominantColor == colorC
        )
        assertEquals(colorB, result.dominantColor)
        assertEquals(0.14f, result.averageLuminance, 0.001f)
    }

    @Test
    fun `tres paletas con un outlier extremo descartan el outlier`() {
        // Frame 1: Azul oscuro nocturno
        val darkBlue1 = 0xFF0A1428.toInt()
        // Frame 2: Azul oscuro muy similar
        val darkBlue2 = 0xFF0E1A30.toInt()
        // Frame 3: Flash blanco / transición atípica (OUTLIER)
        val whiteOutlier = 0xFFFFFFFF.toInt()

        val p1 = ThemePalette(darkBlue1, 0xFF00E5FF.toInt(), 0xFF2A3648.toInt(), true, 0.08f)
        val p2 = ThemePalette(darkBlue2, 0xFF00B0FF.toInt(), 0xFF2E3A4C.toInt(), true, 0.10f)
        val p3 = ThemePalette(whiteOutlier, 0xFFFFFFFF.toInt(), 0xFFCCCCCC.toInt(), false, 0.95f)

        val result = ThemePaletteAggregator.aggregate(listOf(p1, p2, p3))

        // El outlier no debe ganar bajo ninguna circunstancia
        assertNotEquals(whiteOutlier, result.dominantColor)
        assertNotEquals(0xFFFFFFFF.toInt(), result.vibrantColor)
        assertTrue(result.dominantColor == darkBlue1 || result.dominantColor == darkBlue2)
    }

    @Test
    fun `ejecucion determinista sin variaciones aleatorias`() {
        val p1 = ThemePalette(0xFF112233.toInt(), 0xFF445566.toInt(), 0xFF778899.toInt(), true, 0.20f)
        val p2 = ThemePalette(0xFF223344.toInt(), 0xFF556677.toInt(), 0xFF8899AA.toInt(), true, 0.30f)
        val p3 = ThemePalette(0xFF334455.toInt(), 0xFF667788.toInt(), 0xFF99AABB.toInt(), true, 0.40f)

        val run1 = ThemePaletteAggregator.aggregate(listOf(p1, p2, p3))
        val run2 = ThemePaletteAggregator.aggregate(listOf(p1, p2, p3))

        assertEquals(run1.dominantColor, run2.dominantColor)
        assertEquals(run1.vibrantColor, run2.vibrantColor)
        assertEquals(run1.mutedColor, run2.mutedColor)
        assertEquals(run1.averageLuminance, run2.averageLuminance, 0.0001f)
        assertEquals(run1.isDark, run2.isDark)
    }

    @Test
    fun `clasificacion isDark respeta el umbral de 0,5`() {
        val darkPalette = ThemePalette(0xFF101010.toInt(), 0xFF00F0FF.toInt(), 0xFF505050.toInt(), true, 0.49f)
        val lightPalette = ThemePalette(0xFFF0F0F0.toInt(), 0xFF0077AA.toInt(), 0xFFAAAAAA.toInt(), false, 0.51f)

        val aggregatedDark = ThemePaletteAggregator.aggregate(listOf(darkPalette))
        assertTrue(aggregatedDark.isDark)

        val aggregatedLight = ThemePaletteAggregator.aggregate(listOf(lightPalette))
        assertFalse(aggregatedLight.isDark)
    }
}
