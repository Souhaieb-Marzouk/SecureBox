package io.securebox.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.securebox.core.domain.shield.ShieldStats
import io.securebox.core.ui.component.FocusAnchor
import io.securebox.core.ui.component.ScreenHeading
import io.securebox.core.ui.component.SecureCard
import io.securebox.core.ui.theme.SecureBoxColors

@Composable
fun ShieldStatsRoute(
    onBack: () -> Unit,
    viewModel: ShieldViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ShieldStatsScreen(
        stats = state.stats,
        onBack = onBack,
        onReset = viewModel::resetStats,
    )
}

@Composable
fun ShieldStatsScreen(
    stats: ShieldStats,
    onBack: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmReset by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FocusAnchor()
        TextButton(onClick = onBack) { Text(stringResource(R.string.shield_back)) }
        ScreenHeading(
            title = stringResource(R.string.shield_stats_title),
            subtitle = stringResource(R.string.shield_stats_subtitle),
        )

        SecureCard {
            Text(
                text = stringResource(R.string.shield_stats_total),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stats.total.toString(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.shield_stats_summary, stats.total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        StatRow(label = stringResource(R.string.shield_stats_ads), value = stats.adsBlocked, color = SecureBoxColors.AuroraTeal)
        StatRow(label = stringResource(R.string.shield_stats_trackers), value = stats.trackersBlocked, color = SecureBoxColors.AuroraViolet)
        StatRow(label = stringResource(R.string.shield_stats_annoyances), value = stats.annoyancesCleaned, color = SecureBoxColors.AuroraAmber)
        StatRow(label = stringResource(R.string.shield_stats_malicious), value = stats.maliciousBlocked, color = SecureBoxColors.AuroraCoral)
        StatRow(label = stringResource(R.string.shield_stats_dns), value = stats.dnsBlocked, color = SecureBoxColors.AuroraCyan)

        OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.shield_stats_reset))
        }
        Text(
            text = stringResource(R.string.shield_stats_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FocusAnchor()
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.shield_stats_reset_title)) },
            text = { Text(stringResource(R.string.shield_stats_reset_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        onReset()
                    },
                ) {
                    Text(stringResource(R.string.shield_stats_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.shield_stats_cancel))
                }
            },
        )
    }
}

@Composable
private fun StatRow(
    label: String,
    value: Long,
    color: androidx.compose.ui.graphics.Color,
) {
    SecureCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = color,
            )
        }
    }
}
