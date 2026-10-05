package com.daybreak.animelauncher.theming

import android.graphics.Bitmap
import com.daybreak.animelauncher.AdvancedStyleConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * UI-1 Fase 3, caso K: validación Robolectric de la cadena real de análisis.
 *
 * 1. `WallpaperAnalyzer.analyzeSync` sobre un bitmap REAL (sólido oscuro) produce
 *    una paleta propia (no la DEFAULT) con isDark correcto.
 * 2. `AdaptiveThemeEngine.buildAutoPageTheme` sobre esa paleta real produce tokens
 *    contrastados (>= 4.5:1) con el anti-patrón eliminado.
 * 3. `resolveAuto` con un URI inválido cae a DEFAULT sin lanzar.
 *
 * La garantía "análisis fuera del Main Thread" es estructural: `loadPaletteFromUri`
 * envuelve decode+análisis en `withContext(Dispatchers.Default)` y
 * `WallpaperAnalyzer.analyze` tiene Default como dispatcher (verificado en código).
 */
@RunWith(RobolectricTestRunner::class)
class AdaptiveThemeEngineRobotest {

    @Test
    fun analisisRealDeBitmapOscuro_TokensContrastados() {
        val darkBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(0xFF101820.toInt()) }

        val palette = WallpaperAnalyzer.analyzeSync(darkBitmap)
        assertNotEquals("El análisis real no debe caer a la paleta DEFAULT", ThemePalette.DEFAULT, palette)
        assertTrue("Bitmap oscuro → isDark", palette.isDark)

        val page = AdaptiveThemeEngine.buildAutoPageTheme(0, palette, AdvancedStyleConfig())
        val ratio = ThemeProposalGenerator.calculateContrastRatio(
            page.tokens.textPrimaryColor, page.tokens.surfaceColor
        )
        assertTrue("textPrimary vs surface >= 4.5:1, ratio=$ratio", ratio >= 4.5f)
        assertNotEquals(
            "Accent no debe usarse como textPrimary (anti-patrón)",
            page.tokens.accentColor, page.tokens.textPrimaryColor
        )
    }

    @Test
    fun analisisRealDeBitmapClaro_TokensContrastados() {
        val lightBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(0xFFF2F2F0.toInt()) }

        val palette = WallpaperAnalyzer.analyzeSync(lightBitmap)
        assertNotEquals(ThemePalette.DEFAULT, palette)

        val page = AdaptiveThemeEngine.buildAutoPageTheme(0, palette, AdvancedStyleConfig())
        val ratio = ThemeProposalGenerator.calculateContrastRatio(
            page.tokens.textPrimaryColor, page.tokens.surfaceColor
        )
        assertTrue("ratio=$ratio", ratio >= 4.5f)
    }

    @Test
    fun resolveAuto_SinUri_FallbackDefaultSinCrash() {
        // Ruta determinista sin ContentResolver (los fakes de Robolectric "decodifican"
        // cualquier stream, por lo que el caso URI-roto se cubre en el test puro J).
        val page = runBlocking {
            AdaptiveThemeEngine.resolveAuto(
                pageIndex = 0,
                backgroundUri = null,
                styleConfig = AdvancedStyleConfig().copy(themeMode = ThemeMode.AUTO),
                context = RuntimeEnvironment.getApplication()
            )
        }
        assertEquals(ThemePalette.DEFAULT, page.palette)
        assertTrue(page.elementColors.clock != 0)
    }
}
