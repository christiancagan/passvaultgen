package com.example.passwordvault.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.passwordvault.ui.components.ThemeToggleButton
import com.example.passwordvault.ui.theme.brandGradient

@Composable
fun WelcomeScreen(
    onNavigateToCreate: () -> Unit,
    onNavigateToUnlock: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ThemeToggleButton()
        }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.size(128.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            CircleShape,
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(brandGradient(), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = "PassVaultGen",
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier.widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "A private, offline password vault.\nYour data never leaves this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onNavigateToCreate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(),
                ) {
                    Text("Create a new vault")
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onNavigateToUnlock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text("I already have a vault")
                }
            }
        }
}
