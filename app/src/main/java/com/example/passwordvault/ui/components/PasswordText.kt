package com.example.passwordvault.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.passwordvault.ui.theme.passwordStyle

/**
 * Monospace password display with characters tinted by class: letters use the
 * surface ink, digits the accent, symbols the tertiary violet. Never pass this
 * value through previews or logs.
 */
@Composable
fun ColoredPasswordText(
    password: String,
    modifier: Modifier = Modifier,
    masked: Boolean = false,
    size: TextUnit = 15.sp,
) {
    val scheme = MaterialTheme.colorScheme
    val style = passwordStyle(size)
    if (masked) {
        Text(text = "•".repeat(12), style = style, modifier = modifier)
    } else {
        Text(
            text = tintPassword(password, style, scheme.primary, scheme.tertiary),
            style = style,
            modifier = modifier,
        )
    }
}

private fun tintPassword(
    password: String,
    base: TextStyle,
    digitColor: Color,
    symbolColor: Color,
): AnnotatedString = AnnotatedString.Builder(password).apply {
    addStyle(SpanStyle(color = base.color), 0, password.length)
    var i = 0
    while (i < password.length) {
        val c = password[i]
        val cls = classOf(c)
        val color = when (cls) {
            1 -> digitColor
            2 -> symbolColor
            else -> null
        }
        if (color != null) {
            var j = i + 1
            while (j < password.length && classOf(password[j]) == cls) j++
            addStyle(SpanStyle(color = color), i, j)
            i = j
        } else {
            i++
        }
    }
}.toAnnotatedString()

private fun classOf(c: Char): Int = when {
    c.isDigit() -> 1
    c.isLetter() -> 0
    else -> 2
}
