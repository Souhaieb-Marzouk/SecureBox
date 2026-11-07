package io.securebox.feature.threatintel

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.securebox.core.domain.FeatureFlags
import io.securebox.core.ui.component.BannerSeverity
import io.securebox.core.ui.component.ScreenHeading
import io.securebox.core.ui.component.SecureCard
import io.securebox.core.ui.component.StatusBanner
import java.util.Date

@Composable
fun ThreatIntelRoute(
    onBack: () -> Unit,
    viewModel: ThreatIntelViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ThreatIntelScreen(
        state = state,
        onBack = onBack,
        onUpdate = viewModel::updateNow,
        onAutoUpdate = viewModel::setAutoUpdate,
    )
}

@Composable
fun ThreatIntelScreen(
    state: ThreatIntelUiState,
    onBack: () -> Unit,
    onUpdate: () -> Unit,
    onAutoUpdate: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormat = DateFormat.getDateFormat(context)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ScreenHeading(
                    title = stringResource(R.string.threat_intel_title),
                    subtitle = stringResource(R.string.threat_intel_subtitle),
                    modifier = Modifier.weight(1f),
                )
                TrialBadge()
            }
        }

        item {
            SecureCard {
                val status = state.status
                if (status == null) {
                    Text(
                        text = stringResource(R.string.threat_intel_no_feed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.threat_intel_version, status.version),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(R.string.threat_intel_rules, status.ruleCount),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.threat_intel_trackers, status.trackerCount),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (status.domainCount > 0) {
                        Text(
                            text = stringResource(R.string.threat_intel_domains, status.domainCount),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Text(
                        text = stringResource(R.string.threat_intel_installed, dateFormat.format(Date(status.installedAtMillis))),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text =
                            status.lastCheckedAtMillis?.let {
                                stringResource(R.string.threat_intel_checked, dateFormat.format(Date(it)))
                            } ?: stringResource(R.string.threat_intel_never),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.threat_intel_signature),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onUpdate, enabled = !state.updating) {
                        Text(
                            text = stringResource(
                                if (state.updating) R.string.threat_intel_updating else R.string.threat_intel_update,
                            ),
                        )
                    }
                    if (state.updating) CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                }

                state.result?.let { result ->
                    Text(
                        text = stringResource(
                            when (result) {
                                FeedResultKind.UPDATED -> R.string.threat_intel_result_updated
                                FeedResultKind.UP_TO_DATE -> R.string.threat_intel_result_up_to_date
                                FeedResultKind.INVALID_SIGNATURE -> R.string.threat_intel_result_invalid
                                FeedResultKind.NOT_CONFIGURED -> R.string.threat_intel_result_not_configured
                                FeedResultKind.FAILED -> R.string.threat_intel_result_failed
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            if (result == FeedResultKind.UPDATED || result == FeedResultKind.UP_TO_DATE) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                    )
                    state.detail?.let { detail ->
                        Text(text = detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item {
            SecureCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = stringResource(R.string.threat_intel_auto_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.threat_intel_auto_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = state.autoUpdate, onCheckedChange = onAutoUpdate)
                }
            }
        }

        item {
            StatusBanner(
                severity = BannerSeverity.INFO,
                message = stringResource(R.string.threat_intel_local),
            )
        }

        if (FeatureFlags.PAID_TIER_AVAILABLE) {
            item {
                SecureCard {
                    Text(text = stringResource(R.string.threat_intel_paid_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.threat_intel_paid_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = {}, enabled = false) {
                        Text(text = stringResource(R.string.threat_intel_paid_cta))
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.threat_intel_back)) }
        }
    }
}
