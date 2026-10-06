package com.example.passwordvault.ui.home

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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.passwordvault.ui.components.PassVaultTopBar
import com.example.passwordvault.ui.dashboard.DashboardPane
import com.example.passwordvault.ui.dashboard.SecurityDashboardViewModel
import com.example.passwordvault.ui.vault.VaultPane

private enum class HomeTab { DASHBOARD, VAULT }

/**
 * Post-unlock home: bottom navigation with the security dashboard first
 * and the credential vault second.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onDetails: (String) -> Unit,
    onGenerator: () -> Unit,
    onSettings: () -> Unit,
    onLock: () -> Unit,
    dashboardViewModel: SecurityDashboardViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.DASHBOARD) }

    Scaffold(
        topBar = {
            if (tab == HomeTab.DASHBOARD) {
                PassVaultTopBar(
                    title = "Security Dashboard",
                    actions = {
                        IconButton(onClick = { dashboardViewModel.refresh() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    },
                )
            } else {
                PassVaultTopBar(
                    title = "Vault",
                    actions = {
                        IconButton(onClick = onGenerator) {
                            Icon(Icons.Filled.Shuffle, contentDescription = "Password generator")
                        }
                        IconButton(onClick = onSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                        IconButton(onClick = onLock) {
                            Icon(Icons.Filled.Lock, contentDescription = "Lock vault")
                        }
                    },
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == HomeTab.DASHBOARD,
                    onClick = { tab = HomeTab.DASHBOARD },
                    icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
                    label = { Text("Dashboard") },
                )
                NavigationBarItem(
                    selected = tab == HomeTab.VAULT,
                    onClick = { tab = HomeTab.VAULT },
                    icon = { Icon(Icons.Filled.VpnKey, contentDescription = null) },
                    label = { Text("Vault") },
                )
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
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(padding),
        ) {
            if (tab == HomeTab.DASHBOARD) {
                DashboardPane(viewModel = dashboardViewModel)
            } else {
                VaultPane(onDetails = onDetails)
            }
        }
    }
}
