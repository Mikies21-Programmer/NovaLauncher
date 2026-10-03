package com.daybreak.animelauncher.ui.screens.hiddenapps

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.daybreak.animelauncher.theming.LocalLauncherThemeTokens
import com.daybreak.animelauncher.theming.accent
import com.daybreak.animelauncher.theming.border
import kotlin.math.hypot

/**
 * Componente Canvas interactivo y ligero para desbloqueo y configuración de patrón 3x3.
 * Opera de forma 100% nativa en Compose con los tokens de tema de NovaLauncher.
 */
@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    onPatternComplete: (List<Int>) -> Unit
) {
    val themeTokens = LocalLauncherThemeTokens.current
    val accentColor = if (isError) Color(0xFFFF5252) else themeTokens.accent
    val baseBorderColor = themeTokens.border.copy(alpha = 0.5f)

    var currentTouchPos by remember { mutableStateOf<Offset?>(null) }
    val selectedDots = remember { mutableStateListOf<Int>() }

    Box(
        modifier = modifier
            .padding(16.dp)
            .aspectRatio(1f)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedDots.clear()
                            currentTouchPos = offset
                            val hit = findHitDot(offset, size.width.toFloat(), size.height.toFloat())
                            if (hit != null) {
                                selectedDots.add(hit)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentTouchPos = change.position
                            val hit = findHitDot(change.position, size.width.toFloat(), size.height.toFloat())
                            if (hit != null && hit !in selectedDots) {
                                selectedDots.add(hit)
                            }
                        },
                        onDragEnd = {
                            currentTouchPos = null
                            if (selectedDots.isNotEmpty()) {
                                onPatternComplete(selectedDots.toList())
                            }
                        },
                        onDragCancel = {
                            currentTouchPos = null
                            selectedDots.clear()
                        }
                    )
                }
        ) {
            val cellWidth = size.width / 3f
            val cellHeight = size.height / 3f
            val dotRadius = 10.dp.toPx()
            val hitRadius = cellWidth * 0.4f
            val strokeWidth = 5.dp.toPx()

            fun getDotCenter(index: Int): Offset {
                val row = index / 3
                val col = index % 3
                return Offset(
                    x = col * cellWidth + cellWidth / 2f,
                    y = row * cellHeight + cellHeight / 2f
                )
            }

            // 1. Dibujar líneas conectoras entre puntos seleccionados
            if (selectedDots.size > 1) {
                for (i in 0 until selectedDots.size - 1) {
                    val p1 = getDotCenter(selectedDots[i])
                    val p2 = getDotCenter(selectedDots[i + 1])
                    drawLine(
                        color = accentColor,
                        start = p1,
                        end = p2,
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 2. Línea flotante hacia la posición actual del dedo
            val lastDot = selectedDots.lastOrNull()
            val touch = currentTouchPos
            if (lastDot != null && touch != null) {
                drawLine(
                    color = accentColor.copy(alpha = 0.7f),
                    start = getDotCenter(lastDot),
                    end = touch,
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }

            // 3. Dibujar los 9 círculos de la cuadrícula
            for (i in 0 until 9) {
                val center = getDotCenter(i)
                val isSelected = i in selectedDots

                if (isSelected) {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.25f),
                        radius = hitRadius * 0.7f,
                        center = center
                    )
                    drawCircle(
                        color = accentColor,
                        radius = dotRadius * 1.2f,
                        center = center
                    )
                } else {
                    drawCircle(
                        color = baseBorderColor,
                        radius = dotRadius * 0.8f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = baseBorderColor.copy(alpha = 0.4f),
                        radius = dotRadius * 0.4f,
                        center = center
                    )
                }
            }
        }
    }
}

private fun findHitDot(pos: Offset, width: Float, height: Float): Int? {
    val cellWidth = width / 3f
    val cellHeight = height / 3f
    val threshold = cellWidth * 0.38f

    for (i in 0 until 9) {
        val row = i / 3
        val col = i % 3
        val centerX = col * cellWidth + cellWidth / 2f
        val centerY = row * cellHeight + cellHeight / 2f

        val dist = hypot(pos.x - centerX, pos.y - centerY)
        if (dist <= threshold) {
            return i
        }
    }
    return null
}
