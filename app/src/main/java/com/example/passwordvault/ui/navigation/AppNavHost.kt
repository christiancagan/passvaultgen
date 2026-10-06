package com.example.passwordvault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.passwordvault.ui.auth.AuthViewModel
import com.example.passwordvault.ui.auth.CreateMasterPasswordScreen
import com.example.passwordvault.ui.auth.UnlockScreen
import com.example.passwordvault.ui.auth.WelcomeScreen
import com.example.passwordvault.ui.backup.BackupScreen
import com.example.passwordvault.ui.generator.GeneratorScreen
import com.example.passwordvault.ui.home.HomeScreen
import com.example.passwordvault.ui.settings.ChangePasswordScreen
import com.example.passwordvault.ui.settings.SettingsScreen
import com.example.passwordvault.ui.settings.SettingsViewModel
import com.example.passwordvault.ui.vault.AddEditEntryScreen
import com.example.passwordvault.ui.vault.EntryDetailsScreen

object Routes {
    const val WELCOME = "welcome"
    const val CREATE = "create"
    const val UNLOCK = "unlock"
    const val HOME = "home"
    const val ADD = "add"
    const val EDIT = "edit/{id}"
    const val DETAILS = "details/{id}"
    const val GENERATOR = "generator"
    const val SETTINGS = "settings"
    const val BACKUP = "backup"
    const val CHANGE_PASSWORD = "change_password"

    fun edit(id: String) = "edit/$id"
    fun details(id: String) = "details/$id"
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    val hasVault by authViewModel.hasVault.collectAsStateWithLifecycle()
    val isUnlocked by authViewModel.isUnlocked.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

    // Lock the vault when the app goes to the background (if auto-lock is on).
    val lifecycleOwner = LocalLifecycleOwner.current
    val autoLockEnabled by rememberUpdatedState(settings.autoLockEnabled)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && autoLockEnabled) {
                authViewModel.lock()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.WELCOME,
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onNavigateToCreate = { navController.navigate(Routes.CREATE) },
                onNavigateToUnlock = { navController.navigate(Routes.UNLOCK) },
            )
        }
        composable(Routes.CREATE) {
            CreateMasterPasswordScreen(
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.UNLOCK) {
            UnlockScreen(
                onUnlocked = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onAdd = { navController.navigate(Routes.ADD) },
                onDetails = { id -> navController.navigate(Routes.details(id)) },
                onGenerator = { navController.navigate(Routes.GENERATOR) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onLock = { authViewModel.lock() },
            )
        }
        composable(Routes.ADD) {
            AddEditEntryScreen(
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            Routes.EDIT,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id").orEmpty()
            AddEditEntryScreen(
                entryId = id,
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            Routes.DETAILS,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id").orEmpty()
            EntryDetailsScreen(
                entryId = id,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.edit(id)) },
            )
        }
        composable(Routes.GENERATOR) {
            GeneratorScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onBackup = { navController.navigate(Routes.BACKUP) },
                onChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                onVaultDeleted = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.CHANGE_PASSWORD) {
            ChangePasswordScreen(onDone = { navController.popBackStack() })
        }
        composable(Routes.BACKUP) {
            BackupScreen(onBack = { navController.popBackStack() })
        }
    }

    // Route to the correct screen based on vault state.
    LaunchedEffect(hasVault, isUnlocked) {
        val vaultExists = hasVault
        val unlocked = isUnlocked
        when {
            vaultExists == null -> Unit // still loading
            !vaultExists -> {
                navController.navigate(Routes.WELCOME) {
                    popUpTo(Routes.WELCOME) { inclusive = true }
                }
            }
            vaultExists && !unlocked -> {
                navController.navigate(Routes.UNLOCK) {
                    popUpTo(Routes.WELCOME) { inclusive = true }
                }
            }
            vaultExists && unlocked -> {
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.WELCOME) { inclusive = true }
                }
            }
        }
    }
}
