package com.example.passwordvault.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Card row with hidden swipe actions: drag right to copy, drag left to
 * delete. Actions fire only past the threshold and the row springs back.
 * Vertical scrolling still wins over the horizontal gesture.
 */
@Composable
fun SwipeableRow(
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val threshold = with(density) { 104.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val feedback = androidx.compose.ui.platform.LocalHapticFeedback.current

    Box(modifier = modifier.fillMaxWidth()) {
        // Left action revealed when dragging right: Copy.
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
                .alpha((offset.value / threshold).coerceIn(0f, 1f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.ContentCopy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text("Copy", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        // Right action revealed when dragging left: Delete.
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .alpha((-offset.value / threshold).coerceIn(0f, 1f)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Delete", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offset.value }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val v = offset.value
                            scope.launch {
                                when {
                                    v >= threshold -> {
                                        offset.animateTo(0f, spring())
                                        feedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSwipeRight()
                                    }
                                    v <= -threshold -> {
                                        offset.animateTo(0f, spring())
                                        feedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSwipeLeft()
                                    }
                                    else -> offset.animateTo(0f, spring(dampingRatio = 0.7f))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(0f, spring()) }
                        },
                    ) { _, dragAmount ->
                        scope.launch {
                            offset.snapTo(
                                (offset.value + dragAmount).coerceIn(-threshold * 1.6f, threshold * 1.6f),
                            )
                        }
                    }
                },
        ) {
            content()
        }
    }
}
