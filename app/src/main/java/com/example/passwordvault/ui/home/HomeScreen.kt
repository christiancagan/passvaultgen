package com.example.passwordvault.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.passwordvault.ui.components.GradientBackground
import com.example.passwordvault.ui.components.PassVaultTopBar
import com.example.passwordvault.ui.dashboard.DashboardPane
import com.example.passwordvault.ui.dashboard.SecurityDashboardViewModel
import com.example.passwordvault.ui.generator.GeneratorPane
import com.example.passwordvault.ui.settings.SettingsPane
import com.example.passwordvault.ui.settings.SettingsViewModel
import com.example.passwordvault.ui.vault.VaultPane

private enum class HomeTab(
    val label: String,
    val icon: ImageVector,
    val topBarTitle: String,
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, "Security Dashboard"),
    VAULT("Vault", Icons.Filled.VpnKey, "Vault"),
    GENERATOR("Generator", Icons.Filled.Shuffle, "Password Generator"),
    SETTINGS("Settings", Icons.Filled.Settings, "Settings"),
}

/**
 * Post-unlock home: a four-tab bottom navigation over the layered gradient
 * backdrop. The security dashboard is the landing tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onDetails: (String) -> Unit,
    onLock: () -> Unit,
    onBackup: () -> Unit,
    onChangePassword: () -> Unit,
    onVaultDeleted: () -> Unit,
    dashboardViewModel: SecurityDashboardViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            PassVaultTopBar(
                title = tab.topBarTitle,
                actions = {
                    when (tab) {
                        HomeTab.DASHBOARD -> IconButton(onClick = { dashboardViewModel.refresh() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                        HomeTab.VAULT -> IconButton(onClick = onLock) {
                            Icon(Icons.Filled.Lock, contentDescription = "Lock vault")
                        }
                        else -> Unit
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f),
            ) {
                HomeTab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Icon(entry.icon, contentDescription = null) },
                        label = { Text(entry.label) },
                        colors = NavigationBarItemDefaults.colors(),
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == HomeTab.VAULT) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Icon(Icons.Filled.Add, contentDescription = "Add credential") },
                    text = { Text("Add") },
                )
            }
        },
    ) { padding ->
        GradientBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 16 }) togetherWith
                            fadeOut(tween(120))
                    },
                    label = "home-tab",
                ) { target ->
                    when (target) {
                        HomeTab.DASHBOARD -> DashboardPane(viewModel = dashboardViewModel)
                        HomeTab.VAULT -> VaultPane(
                            onDetails = onDetails,
                            snackbarHostState = snackbarHostState,
                        )
                        HomeTab.GENERATOR -> GeneratorPane(snackbarHostState = snackbarHostState)
                        HomeTab.SETTINGS -> SettingsPane(
                            onBackup = onBackup,
                            onChangePassword = onChangePassword,
                            onVaultDeleted = onVaultDeleted,
                        )
                    }
                }
            }
        }
    }
}
