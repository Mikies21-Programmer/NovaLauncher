package com.daybreak.animelauncher.theming

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * UI-1 Fase 4: Glass Premium adaptativo por capas, derivado de [GlassParams].
 *
 * Capas (en orden de dibujo):
 * 1. Tint/superficie adaptativa (scrimColor @ scrimAlpha) — superficie de lectura.
 * 2. Gradiente vertical sutil (highlight arriba → fade abajo) — profundidad.
 * 3. Gradiente inferior (color de superficie al fondo).
 * 4. Borde fino (borderColor @ borderAlpha).
 * 5. Highlight superior (línea de luz con highlightColor @ highlightAlpha).
 *
 * Glow externo: intencionalmente NO se dibuja (costo/riesgo de exageración);
 * el highlight superior cumple el rol perceptual.
 *
 * Reglas: sin trabajo por frame (Brush estáticos por composición); MANUAL
 * (adaptive=false) devuelve el modifier intacto para conservar la paridad
 * pixel-perfect histórica.
 */
fun Modifier.glassPremium(
    glass: GlassParams,
    shape: Shape = RectangleShape,
    topHighlightHeight: Dp = 1.dp
): Modifier {
    if (!glass.adaptive) return this

    val scrim = Color(glass.scrimColor).copy(alpha = glass.scrimAlpha)
    val highlight = Color(glass.highlightColor).copy(alpha = glass.highlightAlpha)
    val gradientTop = Color(glass.gradientTopColor).copy(alpha = glass.gradientAlpha)
    val gradientBottom = Color(glass.gradientBottomColor).copy(alpha = glass.gradientAlpha * 0.4f)
    val border = Color(glass.borderColor).copy(alpha = glass.borderAlpha)
    val lineFade = (topHighlightHeight.value / 4000f).coerceIn(0.0005f, 0.01f)

    val mainStops: Array<Pair<Float, Color>> = arrayOf(
        0f to highlight,
        lineFade to scrim,
        0.7f to scrim,
        1f to gradientBottom
    )
    val bottomStops: Array<Pair<Float, Color>> = arrayOf(
        0.75f to Color.Transparent,
        1f to gradientTop
    )

    return this
        // Capas 1+2+5: tint de lectura, gradiente y highlight superior integrados.
        .background(brush = Brush.verticalGradient(colorStops = mainStops), shape = shape)
        // Capa 3: gradiente inferior sutil.
        .background(brush = Brush.verticalGradient(colorStops = bottomStops), shape = shape)
        // Capa 4: borde fino.
        .border(width = 0.5.dp, color = border, shape = shape)
}
