package com.example.passwordvault.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.passwordvault.data.repository.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF0A3D91),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A43),
    secondary = Color(0xFF3E5C8A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E4FF),
    onSecondaryContainer = Color(0xFF091B36),
    tertiary = Color(0xFF2E7D5B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB9EDCF),
    onTertiaryContainer = Color(0xFF00210F),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF171C26),
    surface = Color(0xFFF6F8FC),
    onSurface = Color(0xFF171C26),
    surfaceVariant = Color(0xFFE1E7F2),
    onSurfaceVariant = Color(0xFF424B5C),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF3FA),
    surfaceContainer = Color(0xFFE9EEF7),
    surfaceContainerHigh = Color(0xFFE3E9F4),
    surfaceContainerHighest = Color(0xFFDDE4F0),
    outline = Color(0xFF737D90),
    outlineVariant = Color(0xFFC2C9D8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C3FF),
    onPrimary = Color(0xFF002F67),
    primaryContainer = Color(0xFF0A3D91),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFF9FB4D8),
    onSecondary = Color(0xFF16283F),
    secondaryContainer = Color(0xFF2D435F),
    onSecondaryContainer = Color(0xFFD6E4FF),
    tertiary = Color(0xFF6FBF9A),
    onTertiary = Color(0xFF003823),
    tertiaryContainer = Color(0xFF00513A),
    onTertiaryContainer = Color(0xFFB9EDCF),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    background = Color(0xFF060D1A),
    onBackground = Color(0xFFE2E7F2),
    surface = Color(0xFF060D1A),
    onSurface = Color(0xFFE2E7F2),
    surfaceVariant = Color(0xFF1B2436),
    onSurfaceVariant = Color(0xFFC2C9D8),
    surfaceContainerLowest = Color(0xFF030710),
    surfaceContainerLow = Color(0xFF0A1222),
    surfaceContainer = Color(0xFF101A2E),
    surfaceContainerHigh = Color(0xFF18233A),
    surfaceContainerHighest = Color(0xFF222D44),
    outline = Color(0xFF8C96AB),
    outlineVariant = Color(0xFF35415A),
)

/**
 * App theme. Follows [mode]: an explicit LIGHT/DARK choice, or the system
 * setting when [ThemeMode.SYSTEM].
 */
@Composable
fun PassVaultTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

/**
 * Signature brand gradient (deep blue to slate blue) for hero panels.
 * Reads the active color scheme, so it adapts to light and dark mode.
 */
@Composable
fun brandGradient(): Brush {
    val scheme = MaterialTheme.colorScheme
    return Brush.horizontalGradient(listOf(scheme.primary, scheme.secondary))
}
