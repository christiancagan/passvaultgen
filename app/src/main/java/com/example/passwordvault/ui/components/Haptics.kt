package com.example.passwordvault.ui.components

import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Returns a callback that fires a click haptic; call inside composition. */
@Composable
fun rememberHapticTap(): () -> Unit {
    val feedback = LocalHapticFeedback.current
    return { feedback.performHapticFeedback(HapticFeedbackType.LongPress) }
}

/** Switch with a click haptic and Material 3's built-in animated thumb. */
@Composable
fun HapticSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tap = rememberHapticTap()
    Switch(
        checked = checked,
        onCheckedChange = {
            tap()
            onCheckedChange(it)
        },
        modifier = modifier,
    )
}

/** Checkbox with a click haptic. */
@Composable
fun HapticCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val tap = rememberHapticTap()
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange?.let { change ->
            {
                tap()
                change(it)
            }
        },
        modifier = modifier,
    )
}
