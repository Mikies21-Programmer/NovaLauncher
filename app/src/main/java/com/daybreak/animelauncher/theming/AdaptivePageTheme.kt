package com.daybreak.animelauncher.theming

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.daybreak.animelauncher.AdvancedStyleConfig

/**
 * UI-1 Fase 3: wrapper Compose de la resolución de tema por página.
 *
 * - MANUAL: resolución síncrona determinista (paridad exacta, sin IO).
 * - AUTO / AUTO_WITH_OVERRIDES con fondo de IMAGEN: resolución asíncrona
 *   ([AdaptiveThemeEngine.resolveAuto]) que parte del fail-safe MANUAL como
 *   estado inicial (sin flash, sin bloqueo) y actualiza al analizar.
 *   Un URI de video (o tipo desconocido) degrada a la resolución MANUAL —
 *   el sampling de video es una fase posterior.
 */
@Composable
fun rememberAdaptivePageTheme(
    pageIndex: Int,
    backgroundUri: String?,
    styleConfig: AdvancedStyleConfig,
    appContext: Context
): State<PageTheme> {
    // Misma clasificación que DynamicBackground: getType() + fallback por extensión
    // (los file:// y algunos content:// no resuelven MIME vía getType).
    val isImageBackground = remember(backgroundUri) {
        if (backgroundUri.isNullOrBlank()) false
        else {
            // getType puede LANZAR para file:// (sin provider): null-safe y
            // excepción-sega por separado, con fallback por extensión igual que
            // DynamicBackground.
            val type = runCatching {
                appContext.contentResolver.getType(android.net.Uri.parse(backgroundUri))
            }.getOrNull()
            if (type != null) {
                type.startsWith("image/")
            } else {
                val lower = backgroundUri.lowercase()
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                    lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".bmp")
            }
        }
    }

    return produceState(
        initialValue = AdaptiveThemeEngine.resolve(pageIndex, backgroundUri, styleConfig.themeMode, styleConfig),
        key1 = backgroundUri,
        key2 = styleConfig
    ) {
        val mode = styleConfig.themeMode
        if (mode != ThemeMode.MANUAL && isImageBackground) {
            val resolved = AdaptiveThemeEngine.resolveAuto(
                pageIndex = pageIndex,
                backgroundUri = backgroundUri,
                styleConfig = styleConfig,
                context = appContext
            )
            value = resolved
            // Diagnóstico del tema adaptativo (evidencia de tokens y contraste real).
            android.util.Log.i(
                "AdaptiveTheme",
                "page=$pageIndex mode=$mode lum=${"%.2f".format(resolved.palette.averageLuminance)} " +
                    "isDark=${resolved.isDark} accent=#${Integer.toHexString(resolved.tokens.accentColor)} " +
                    "textPrimary=#${Integer.toHexString(resolved.tokens.textPrimaryColor)} " +
                    "contrastTP=${"%.2f".format(
                        ThemeProposalGenerator.calculateContrastRatio(
                            resolved.tokens.textPrimaryColor, resolved.tokens.surfaceColor
                        )
                    )} scrimAlpha=${"%.2f".format(resolved.glass.scrimAlpha)}"
            )
        }
    }
}
