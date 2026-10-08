package com.example.passwordvault.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.R
import com.example.passwordvault.ui.components.PassVaultCard
import com.example.passwordvault.ui.components.SecurePasswordField
import com.example.passwordvault.ui.components.ThemeToggleButton
import com.example.passwordvault.ui.theme.LocalIsDark

@Composable
fun UnlockScreen(
    onUnlocked: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var password by rememberSaveable { mutableStateOf("") }
    var biometricsError by rememberSaveable { mutableStateOf<String?>(null) }
    val activity = LocalContext.current as? FragmentActivity
    val scheme = MaterialTheme.colorScheme
    val dark = LocalIsDark.current

    // Lighter at the top so the header artwork reads clearly, heavier toward the
    // lower half so the heading and card keep full contrast.
    val scrim = if (dark) {
        Brush.verticalGradient(
            listOf(
                scheme.background.copy(alpha = 0.12f),
                scheme.background.copy(alpha = 0.66f),
                scheme.surfaceContainerLow.copy(alpha = 0.80f),
            ),
        )
    } else {
        Brush.verticalGradient(
            listOf(
                scheme.background.copy(alpha = 0.45f),
                scheme.background.copy(alpha = 0.82f),
                scheme.surfaceContainerLow.copy(alpha = 0.88f),
            ),
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val artReserve = maxHeight * 0.44f

        Image(
            painter = painterResource(id = R.drawable.bg_unlock),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = if (dark) 1f else 0.5f },
        )
        Box(modifier = Modifier.fillMaxSize().background(scrim))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(artReserve))
            Text("Unlock Vault", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PassVaultCard {
                    SecurePasswordField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Master password",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))

                    uiState.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { viewModel.unlock(password) },
                        enabled = !uiState.isLoading && password.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        } else {
                            Text("Unlock")
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Fingerprint trigger: replaces the old "Unlock with biometrics" button.
                // Always visible; tapping when biometrics are unavailable shows an error.
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        )
                        .clickable {
                            biometricsError = null
                            val act = activity
                            if (act == null) {
                                biometricsError = "Biometrics not available on this device"
                            } else {
                                viewModel.unlockWithBiometrics(act) { success ->
                                    if (success) {
                                        onUnlocked()
                                    } else {
                                        biometricsError =
                                            "Biometric unlock failed or not available"
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Fingerprint,
                        contentDescription = "Unlock with biometrics",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp),
                    )
                }

                biometricsError?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        ) {
            ThemeToggleButton()
        }
    }
}
