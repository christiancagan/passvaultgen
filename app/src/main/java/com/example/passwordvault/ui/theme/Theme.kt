package com.example.passwordvault.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.passwordvault.data.repository.ThemeMode

/** True when the brand dark palette is active; used by strength text colors. */
val LocalIsDark = staticCompositionLocalOf { true }

private val BrandDark = darkColorScheme(
    primary = BrandTealDark,
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF0E5C48),
    onPrimaryContainer = Color(0xFFBDF5E5),
    secondary = BrandIndigoDark,
    onSecondary = Color(0xFF001453),
    secondaryContainer = Color(0xFF2A3A7E),
    onSecondaryContainer = Color(0xFFDEE2FF),
    tertiary = BrandVioletDark,
    onTertiary = Color(0xFF350070),
    tertiaryContainer = Color(0xFF4F1E96),
    onTertiaryContainer = Color(0xFFEDDCFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = NavyBackdrop,
    onBackground = Color(0xFFE1E7F1),
    surface = Color(0xFF0E1626),
    onSurface = Color(0xFFE1E7F1),
    surfaceVariant = Color(0xFF1B2536),
    onSurfaceVariant = Color(0xFFC1C8D6),
    surfaceContainerLowest = Color(0xFF070D18),
    surfaceContainerLow = Color(0xFF0A1220),
    surfaceContainer = Color(0xFF0E1626),
    surfaceContainerHigh = Color(0xFF151F31),
    surfaceContainerHighest = Color(0xFF1D283C),
    outline = Color(0xFF8B93A4),
    outlineVariant = Color(0xFF3A4558),
)

private val BrandLight = lightColorScheme(
    primary = BrandTealLight,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA9F2DE),
    onPrimaryContainer = Color(0xFF002019),
    secondary = BrandIndigoLight,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDE2FF),
    onSecondaryContainer = Color(0xFF00105C),
    tertiary = BrandVioletLight,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEADCFF),
    onTertiaryContainer = Color(0xFF24005B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF2F6FC),
    onBackground = Color(0xFF111825),
    surface = Color(0xFFF8FAFE),
    onSurface = Color(0xFF111825),
    surfaceVariant = Color(0xFFDDE5EE),
    onSurfaceVariant = Color(0xFF40495A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5FB),
    surfaceContainer = Color(0xFFEDF2F9),
    surfaceContainerHigh = Color(0xFFE7EDF6),
    surfaceContainerHighest = Color(0xFFE1E8F2),
    outline = Color(0xFF717A8A),
    outlineVariant = Color(0xFFC0C8D6),
)

/**
 * App theme. Dark is the design default; [mode] selects LIGHT/DARK/SYSTEM.
 *
 * [dynamicColor] is the user's explicit "Material You" toggle (Settings ->
 * Appearance): when enabled and the device is Android 12+, wallpaper-derived
 * dynamic colors replace the brand palette in every theme mode. When unset
 * the store defaults it to legacy behavior (dynamic color only in SYSTEM
 * mode), so upgrading users see no change until they touch the toggle.
 */
@Composable
fun PassVaultTheme(
    mode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val useDynamic = dynamicColor &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        darkTheme -> BrandDark
        else -> BrandLight
    }
    CompositionLocalProvider(
        LocalIsDark provides darkTheme,
        // Text() placed directly on the gradient (auth screens, panes) has no
        // Surface above it, so bind the correct ink globally: light ink on the
        // dark palette, dark ink on the light palette.
        androidx.compose.material3.LocalContentColor provides colorScheme.onBackground,
    ) {
        ApplyModeToWindow(darkTheme)
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

/**
 * Keeps the system-bar icons in sync with the in-app mode (which may differ
 * from the system setting when LIGHT/DARK is pinned): dark background gets
 * light icons and vice versa. The window background itself stays on the
 * theme's splash/gradient.
 */
@Composable
private fun ApplyModeToWindow(dark: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(dark, view) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
}

/**
 * Signature brand gradient (teal to electric indigo) for hero panels.
 * Reads the active color scheme, so it adapts to light, dark, and dynamic.
 */
@Composable
fun brandGradient(): Brush {
    val scheme = MaterialTheme.colorScheme
    return Brush.horizontalGradient(listOf(scheme.primary, scheme.secondary))
}
