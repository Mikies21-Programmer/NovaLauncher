package com.daybreak.animelauncher.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Per-interactive-icon state, with no gesture detector or global scope.
 * Compose applies the platform MotionDurationScale (including zero) to this tween.
 * The State must be read in the layer lambda, never in the composable body.
 */
@Composable
internal fun rememberIconPressScale(pressed: Boolean): State<Float> =
    animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(
            durationMillis = if (pressed) 80 else 120,
            easing = FastOutSlowInEasing
        ),
        label = "icon_press_scale"
    )

/** Apply to visual content only: the outer gesture/semantics bounds remain unscaled. */
internal fun Modifier.iconPressScale(scale: State<Float>?): Modifier =
    if (scale == null) this else graphicsLayer {
        val value = scale.value
        scaleX = value
        scaleY = value
    }

