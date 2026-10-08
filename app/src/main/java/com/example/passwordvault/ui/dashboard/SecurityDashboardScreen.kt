package com.example.passwordvault.ui.dashboard

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.passwordvault.ui.theme.brandGradient
import java.time.LocalDate

/**
 * Dashboard pane shown first inside the bottom-navigation home screen.
 * Health hero, stat tiles, and callouts — no scaffold of its own.
 */
@Composable
fun DashboardPane(
    viewModel: SecurityDashboardViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val issues = stats.weak + stats.reused + stats.old
    val health = if (stats.total == 0) 1f else
        ((stats.total - minOf(issues, stats.total)).toFloat() / stats.total).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroCard(
            total = stats.total,
            health = health,
            averageScore = stats.averageScore,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                label = "Weak",
                value = stats.weak.toString(),
                icon = Icons.Filled.Warning,
                color = MaterialTheme.colorScheme.onErrorContainer,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Reused",
                value = stats.reused.toString(),
                icon = Icons.Filled.ContentCopy,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                label = "Old (>180d)",
                value = stats.old.toString(),
                icon = Icons.Filled.Schedule,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Healthy",
                value = (stats.total - minOf(issues, stats.total)).toString(),
                icon = Icons.Filled.CheckCircle,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.weight(1f),
            )
        }
        if (issues > 0) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        "$issues credential${if (issues == 1) "" else "s"} need${if (issues == 1) "s" else ""} attention. Open an entry and use the Generate button for a stronger password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
        TipCard(tip = SecurityTips.forEpochDay(LocalDate.now().toEpochDay()))
        Text(
            "Passwords are never displayed here. Analysis runs entirely on-device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TipCard(tip: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Filled.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    "Security tip of the day",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    tip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun HeroCard(total: Int, health: Float, averageScore: Double) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brandGradient())
                .padding(24.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = scheme.onPrimary.copy(alpha = 0.18f),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Storage,
                                contentDescription = null,
                                tint = scheme.onPrimary,
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            "$total",
                            style = MaterialTheme.typography.displaySmall,
                            color = scheme.onPrimary,
                        )
                        Text(
                            "credentials protected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onPrimary.copy(alpha = 0.9f),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Vault health ${(health * 100).toInt()}% · Avg strength ${"%.1f".format(averageScore)}/4",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onPrimary.copy(alpha = 0.95f),
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { health },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = scheme.onPrimary,
                    trackColor = scheme.onPrimary.copy(alpha = 0.25f),
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = color,
                textAlign = TextAlign.Center,
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
