package com.example.passwordvault.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Layered app backdrop: a subtle vertical tonal gradient with two large soft
 * radial glows for depth. Uses gradients instead of blur for battery and
 * readability.
 *
 * The two glows drift on a slow (~14s, reversed) loop so the backdrop feels
 * alive without perceptible motion sickness risk; amplitude is a few percent
 * of the canvas. The drift is drawn, not composited, so it costs one extra
 * draw pass rather than a layer.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "glow-drift")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow-phase",
    )
    // Secondary glow breathes on a slightly different phase so the pair
    // never moves in lockstep.
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(11_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow-breath",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            scheme.background,
                            scheme.surfaceContainerLow,
                            scheme.background,
                        ),
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(scheme.primary.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(
                            size.width * (0.86f + 0.05f * drift),
                            size.height * (0.03f + 0.04f * drift),
                        ),
                        radius = size.minDimension * (0.72f + 0.07f * breath),
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(scheme.secondary.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(
                            size.width * (0.08f - 0.04f * drift),
                            size.height * (0.94f - 0.03f * breath),
                        ),
                        radius = size.minDimension * (0.82f + 0.07f * drift),
                    ),
                )
            },
    ) {
        content()
    }
}
