package com.example.passwordvault.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Layered app backdrop: a subtle vertical tonal gradient with two large soft
 * radial glows for depth. Uses gradients instead of blur for battery and
 * readability.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
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
                        center = Offset(size.width * 0.88f, size.height * 0.04f),
                        radius = size.minDimension * 0.75f,
                    ),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(scheme.secondary.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.06f, size.height * 0.96f),
                        radius = size.minDimension * 0.85f,
                    ),
                )
            },
    ) {
        content()
    }
}
