package com.daybreak.animelauncher.theming

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.daybreak.animelauncher.AdvancedStyleConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.ArrayDeque

/**
 * UI-1: resolución del tema por página.
 *
 * - MANUAL: resolución síncrona determinista (paridad exacta, sin IO).
 * - AUTO + imagen: análisis asíncrono (Fase 3) con fail-safe MANUAL inicial.
 * - AUTO + video (Fase 5): sampling periódico amortiguado cada 8 s con
 *   [VideoFrameExtractor.extractFrameAt] + [WallpaperAnalyzer] + ventana de
 *   3 muestras ([ThemePaletteAggregator]) + [VideoPaletteGate] (umbral
 *   perceptual). El sampling SOLO corre con página visible y app en foreground;
 *   se cancela al ocultar/background y reanuda al volver. Sin trabajo por frame
 *   del player (extrae 1 frame cada 8 s vía MediaMetadataRetriever, nunca toca
 *   el pool de reproducción).
 *
 * [onResolved] entrega cada PageTheme resuelto (para overlays globales como el
 * Drawer que necesitan el tema de la página activa sin duplicar samplers).
 */
@Composable
fun rememberAdaptivePageTheme(
    pageIndex: Int,
    backgroundUri: String?,
    styleConfig: AdvancedStyleConfig,
    appContext: Context,
    isPageVisible: Boolean = true,
    isAppForeground: Boolean = true,
    onResolved: ((PageTheme) -> Unit)? = null
): State<PageTheme> {
    // Clasificación del fondo: misma política que DynamicBackground (getType +
    // fallback por extensión; null-safe Y excepción-segura por separado, porque
    // getType puede LANZAR para file:// sin provider).
    val isImageBackground = remember(backgroundUri) {
        val type = runCatching {
            appContext.contentResolver.getType(Uri.parse(backgroundUri))
        }.getOrNull()
        if (type != null) {
            type.startsWith("image/")
        } else {
            val lower = backgroundUri?.lowercase() ?: ""
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp")
        }
    }
    val isVideoBackground = !backgroundUri.isNullOrBlank() && !isImageBackground

    val failSafe = remember(pageIndex, backgroundUri, styleConfig.themeMode) {
        AdaptiveThemeEngine.resolve(pageIndex, backgroundUri, styleConfig.themeMode, styleConfig)
    }

    // Esta versión de Compose no tiene overload de 4 keys: se empaquetan en una
    // data class (equals/hashCode estructurales → reinicio del productor correcto).
    val themeKeys = ThemeKeys(backgroundUri, styleConfig, isPageVisible, isAppForeground)

    return produceState(
        initialValue = failSafe,
        key1 = themeKeys
    ) {
        val mode = styleConfig.themeMode
        when {
            mode == ThemeMode.MANUAL -> {
                value = AdaptiveThemeEngine.resolve(pageIndex, backgroundUri, mode, styleConfig)
                onResolved?.invoke(value)
            }

            isImageBackground -> {
                val resolved = AdaptiveThemeEngine.resolveAuto(
                    pageIndex = pageIndex,
                    backgroundUri = backgroundUri,
                    styleConfig = styleConfig,
                    context = appContext
                )
                value = resolved
                logResolved("image", pageIndex, mode, resolved)
                onResolved?.invoke(resolved)
            }

            isVideoBackground && samplingEnabled(mode, true, isPageVisible, isAppForeground) -> {
                val videoUri = Uri.parse(backgroundUri)
                val durationMs = VideoFrameExtractor.videoDurationMs(appContext, videoUri) ?: 0L
                val window = ArrayDeque<ThemePalette>()
                var applied: ThemePalette? = null
                var tick = 0
                // El bucle se cancela al cambiar keys o al dispose (isActive false).
                while (isActive) {
                    val positionMs = if (durationMs > 0) (tick * 2000L) % durationMs else 0L
                    val frame = VideoFrameExtractor.extractFrameAt(appContext, videoUri, positionMs)
                    val palette = WallpaperAnalyzer.analyzeSync(frame).also { frame?.recycle() }
                    window.addLast(palette)
                    while (window.size > 3) window.removeFirst()
                    val candidate = if (window.size == 1) palette
                    else ThemePaletteAggregator.aggregate(window.toList())
                    if (VideoPaletteGate.shouldUpdate(applied, candidate)) {
                        applied = candidate
                        val resolved = AdaptiveThemeEngine.buildAutoPageTheme(pageIndex, candidate, styleConfig)
                        value = resolved
                        logResolved("video", pageIndex, mode, resolved, tick)
                        onResolved?.invoke(resolved)
                    }
                    tick++
                    delay(8000L)
                }
            }

            else -> {
                // Página invisible o launcher en background: valor SIN CAMBIOS,
                // cero extracciones y cero análisis (criterios C/D de Fase 5).
            }
        }
    }
}

private fun logResolved(
    source: String,
    pageIndex: Int,
    mode: ThemeMode,
    theme: PageTheme,
    tick: Int = -1
) {
    val contrast = ThemeProposalGenerator.calculateContrastRatio(
        theme.tokens.textPrimaryColor, theme.tokens.surfaceColor
    )
    android.util.Log.i(
        "AdaptiveTheme",
        "source=$source page=$pageIndex mode=$mode tick=$tick " +
            "lum=${"%.2f".format(theme.palette.averageLuminance)} isDark=${theme.isDark} " +
            "accent=#${Integer.toHexString(theme.tokens.accentColor)} " +
            "textPrimary=#${Integer.toHexString(theme.tokens.textPrimaryColor)} " +
            "contrastTP=${"%.2f".format(contrast)} scrimAlpha=${"%.2f".format(theme.glass.scrimAlpha)}"
    )
}

/** Clasificación imagen (getType + fallback por extensión; null-safe y excepción-segura). */
internal fun isImageUri(backgroundUri: String?, context: Context): Boolean {
    if (backgroundUri.isNullOrBlank()) return false
    val type = runCatching {
        context.contentResolver.getType(Uri.parse(backgroundUri))
    }.getOrNull()
    if (type != null) return type.startsWith("image/")
    val lower = backgroundUri.lowercase()
    return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
        lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp")
}


/** UI-1 Fase 5: clave compuesta del productor de tema (equals/hashCode estructurales). */
private data class ThemeKeys(
    val backgroundUri: String?,
    val styleConfig: AdvancedStyleConfig,
    val isPageVisible: Boolean,
    val isAppForeground: Boolean
)
