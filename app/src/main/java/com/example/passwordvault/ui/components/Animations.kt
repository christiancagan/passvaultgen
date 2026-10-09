package com.example.passwordvault.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.roundToInt

/**
 * Scales the element down while [interactionSource] reports a press and
 * springs back on release. Pair with `clickable(interactionSource = ...)`
 * so the ripple and the scale share one gesture.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * One-shot entrance: fades and slides the element into place the first time
 * it composes, staggered by [index] (capped so long lists never wait long).
 *
 * Elements that re-enter composition later (re-inserted into a LazyColumn
 * after being scrolled away) replay the entrance — intentional: it makes
 * list changes read as motion.
 */
@Composable
fun Modifier.staggeredEntrance(
    index: Int,
    delayPerItemMillis: Int = 40,
    maxDelayItems: Int = 8,
): Modifier {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val delay = minOf(index, maxDelayItems) * delayPerItemMillis
    val alpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(320, delayMillis = delay, easing = FastOutSlowInEasing),
        label = "entrance-alpha",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (started) 0f else 32f,
        animationSpec = tween(320, delayMillis = delay, easing = FastOutSlowInEasing),
        label = "entrance-y",
    )
    return graphicsLayer {
        this.alpha = alpha
        translationY = offsetY
    }
}

/**
 * Animates from 0 up to [target], and re-animates whenever [target] changes
 * (from the currently shown value). Used for dashboard counters so numbers
 * read as live data instead of static text.
 */
@Composable
fun rememberCountUp(target: Int, durationMillis: Int = 700): Int {
    val animated = remember { Animatable(0f) }
    LaunchedEffect(target) {
        animated.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        )
    }
    return animated.value.roundToInt()
}
