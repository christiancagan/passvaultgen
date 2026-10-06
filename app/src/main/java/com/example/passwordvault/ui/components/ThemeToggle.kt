package com.example.passwordvault.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.data.repository.ThemeMode
import com.example.passwordvault.ui.settings.SettingsViewModel

/**
 * One-tap dark/light toggle for top app bars. Pins an explicit LIGHT or DARK
 * choice: tapping while the UI is dark switches to LIGHT and vice versa.
 * The full System/Light/Dark choice lives in Settings.
 */
@Composable
fun ThemeToggleButton(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }
    IconButton(onClick = {
        viewModel.setThemeMode(if (isDark) ThemeMode.LIGHT else ThemeMode.DARK)
    }) {
        Icon(
            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
            contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode",
        )
    }
}

/**
 * System / Light / Dark selector row for the Settings appearance section.
 */
@Composable
fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeModeChip("System", selected == ThemeMode.SYSTEM, { onSelect(ThemeMode.SYSTEM) })
        ThemeModeChip("Light", selected == ThemeMode.LIGHT, { onSelect(ThemeMode.LIGHT) })
        ThemeModeChip("Dark", selected == ThemeMode.DARK, { onSelect(ThemeMode.DARK) })
    }
}

@Composable
private fun ThemeModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}
