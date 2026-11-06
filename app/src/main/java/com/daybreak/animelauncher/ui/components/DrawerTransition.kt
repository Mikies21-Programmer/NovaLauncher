package com.daybreak.animelauncher.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Drawer-only visibility lifetime: retain content through exit and reversals.
 * Target intent, current state and running motion all participate in retention.
 * No extra layout, clipping, coroutine, or hidden screen is kept after exit.
 */
@Composable
internal fun DrawerTransition(
    visible: Boolean,
    content: @Composable (Transition<Boolean>, Modifier) -> Unit
) {
    val motion = updateTransition(visible, label = "drawer_visibility")
    val progress = motion.animateFloat(
        transitionSpec = {
            if (targetState) tween(160, easing = FastOutSlowInEasing)
            else tween(120, easing = FastOutLinearInEasing)
        },
        label = "drawer_progress"
    ) { shown -> if (shown) 1f else 0f }
    val distancePx = with(LocalDensity.current) { 8.dp.toPx() }

    if (motion.currentState || motion.targetState || motion.isRunning) {
        content(
            motion,
            Modifier.graphicsLayer {
                // One layer, one progress value; no animated reads in composition.
                val fraction = progress.value.coerceIn(0f, 1f)
                alpha = fraction
                translationY = (1f - fraction) * distancePx
            }
        )
    }
}

