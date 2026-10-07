package com.example.passwordvault

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.ui.components.GradientBackground
import com.example.passwordvault.ui.navigation.AppNavHost
import com.example.passwordvault.ui.settings.SettingsViewModel
import com.example.passwordvault.ui.theme.PassVaultTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prevent screenshots and screen recording across the app. A password
        // manager treats essentially every screen as sensitive, so FLAG_SECURE
        // is applied globally as a secure default. Debug builds skip it purely
        // so QA/emulator tooling can capture screenshots; release is unchanged.
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debuggable) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            PassVaultTheme(mode = settings.themeMode) {
                GradientBackground {
                    AppNavHost()
                }
            }
        }
    }
}
