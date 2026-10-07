package com.example.passwordvault.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.passwordvault.R
import com.example.passwordvault.ui.theme.LocalIsDark

/**
 * Branded app backdrop: the zap artwork sits in the lower zone of the screen
 * (away from headings and body text) with a theme-aware scrim on top.
 *
 * Dark mode: the art shows through clearly (the glow band is kept away from
 * ink-heavy areas). Light mode: the image is faded to a watermark and a
 * stronger light scrim keeps dark ink at full contrast.
 */
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dark = LocalIsDark.current
    val scrim = if (dark) {
        Brush.verticalGradient(
            listOf(
                scheme.background.copy(alpha = 0.55f),
                scheme.background.copy(alpha = 0.72f),
                scheme.surfaceContainerLow.copy(alpha = 0.42f),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                scheme.background.copy(alpha = 0.68f),
                scheme.background.copy(alpha = 0.85f),
                scheme.surfaceContainerLow.copy(alpha = 0.62f),
            ),
        )
    }
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_zap),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (dark) 1f else 0.5f },
        )
        Box(modifier = Modifier.fillMaxSize().background(scrim))
        content()
    }
}
