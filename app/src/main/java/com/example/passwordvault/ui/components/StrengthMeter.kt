package com.example.passwordvault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.passwordvault.ui.theme.strengthBarColor
import com.example.passwordvault.ui.theme.strengthTextColor

/** Maps estimated entropy (bits) to the 0..4 strength score. */
fun scoreFromEntropy(bits: Double): Int = when {
    bits < 36 -> 0
    bits < 60 -> 1
    bits < 80 -> 2
    bits < 100 -> 3
    else -> 4
}

/**
 * Continuous animated strength bar with a semantic color and label.
 * The fill width sweeps to the new score with a tween so changes read as
 * motion, not jumps.
 */
@Composable
fun StrengthMeter(
    score: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    val barColor = strengthBarColor(score)
    val textColor = strengthTextColor(score)
    val fill = animateFloatAsState(
        targetValue = (score + 1) / 5f,
        animationSpec = tween(500),
        label = "strength-fill",
    )
    val animatedColor by animateColorAsState(
        targetValue = barColor,
        animationSpec = tween(500),
        label = "strength-color",
    )
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(MaterialTheme.shapes.small)
                .background(animatedColor.copy(alpha = 0.22f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fill.value)
                    .background(animatedColor),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Strength: $label",
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
            )
        }
    }
}
