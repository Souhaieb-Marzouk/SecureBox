package io.securebox.feature.threats

import java.text.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.securebox.core.domain.security.AppRiskReport
import io.securebox.core.domain.security.RiskLevel
import io.securebox.core.ui.component.BannerSeverity
import io.securebox.core.ui.component.ScreenHeading
import io.securebox.core.ui.component.SecureCard
import io.securebox.core.ui.component.StatusBanner
import java.util.Date

@Composable
fun ThreatsRoute(viewModel: ThreatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ThreatsScreen(
        state = state,
        onScan = viewModel::scanNow,
        onScheduled = viewModel::setScheduled,
    )
}

@Composable
fun ThreatsScreen(
    state: ThreatsUiState,
    onScan: () -> Unit,
    onScheduled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ScreenHeading(
                title = stringResource(R.string.threats_title),
                subtitle = stringResource(R.string.threats_subtitle),
            )
        }
        item {
            SecureCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = stringResource(R.string.threats_scan_title), style = MaterialTheme.typography.titleLarge)
                        Text(
                            text =
                                when {
                                    state.scanning -> stringResource(R.string.threats_scanning)
                                    state.summary == null -> stringResource(R.string.threats_no_scan)
                                    else -> {
                                        val formatted =
                                            DateFormat
                                                .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                                .format(Date(state.summary.finishedAtMillis))
                                        stringResource(
                                            R.string.threats_last_scan,
                                            formatted,
                                            state.summary.scannedApps,
                                        )
                                    }
                                },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = onScan, enabled = !state.scanning) {
                        Text(stringResource(if (state.scanning) R.string.threats_scanning_short else R.string.threats_scan_now))
                    }
                }
                if (state.scanning) {
                    LinearProgressIndicator(
                        progress = if (state.total > 0) state.analyzed.toFloat() / state.total.toFloat() else 0f,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    Text(
                        text = stringResource(R.string.threats_scan_progress, state.analyzed, state.total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.summary?.let { summary ->
                    if (!state.scanning) {
                        Text(
                            text =
                                stringResource(
                                    R.string.threats_summary,
                                    summary.highRiskApps,
                                    summary.findingsCount,
                                    summary.rulesVersion,
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (summary.highRiskApps > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        item {
            StatusBanner(
                severity = BannerSeverity.INFO,
                message = stringResource(R.string.threats_on_device),
            )
        }

        item {
            Text(text = stringResource(R.string.threats_results_title), style = MaterialTheme.typography.titleLarge)
        }

        val risky = state.reports.filter { it.level.ordinal >= RiskLevel.MEDIUM.ordinal }.take(10)
        if (risky.isEmpty()) {
            item {
                Text(
                    text = stringResource(if (state.summary == null) R.string.threats_run_first else R.string.threats_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(risky, key = { it.app.packageName }) { report ->
                RiskyRow(report)
            }
        }

        item {
            SecureCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = stringResource(R.string.threats_scheduled_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.threats_scheduled_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.scheduled, onCheckedChange = onScheduled)
                }
            }
        }

        item {
            Text(
                text =
                    if (state.rulesVersion == 0) {
                        stringResource(R.string.threats_rules_unavailable)
                    } else {
                        stringResource(R.string.threats_rules_version, state.rulesVersion)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RiskyRow(report: AppRiskReport) {
    val color =
        when (report.level) {
            RiskLevel.CRITICAL, RiskLevel.HIGH -> MaterialTheme.colorScheme.error
            RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = report.app.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.threats_findings_count, report.findings.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.threats_score_badge, report.score, levelLabel(report.level)),
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
        }
    }
}

@Composable
private fun levelLabel(level: RiskLevel): String =
    stringResource(
        when (level) {
            RiskLevel.SAFE -> R.string.threats_level_safe
            RiskLevel.LOW -> R.string.threats_level_low
            RiskLevel.MEDIUM -> R.string.threats_level_medium
            RiskLevel.HIGH -> R.string.threats_level_high
            RiskLevel.CRITICAL -> R.string.threats_level_critical
        },
    )

