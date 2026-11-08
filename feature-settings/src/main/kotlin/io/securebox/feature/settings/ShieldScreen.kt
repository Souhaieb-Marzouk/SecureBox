package io.securebox.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.securebox.core.domain.model.TrackingProtectionMode
import io.securebox.core.ui.component.ActionCard
import io.securebox.core.ui.component.BrandMark
import io.securebox.core.ui.component.ScreenHeading
import io.securebox.core.ui.component.SecureCard
import io.securebox.core.ui.icon.SecureBoxIcons
import io.securebox.core.ui.theme.SecureBoxColors

@Composable
fun ShieldRoute(
    onBack: () -> Unit,
    onOpenBrowser: () -> Unit,
    onOpenDns: () -> Unit,
    viewModel: ShieldViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPrivateDns()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ShieldScreen(
        state = state,
        onBack = onBack,
        onOpenBrowser = onOpenBrowser,
        onOpenDns = onOpenDns,
        onMode = viewModel::setTrackingProtection,
        onBlockAds = viewModel::setBlockAdsAndTrackers,
        onHideAnnoyances = viewModel::setHideAnnoyances,
        onSafeBrowsing = viewModel::setSafeBrowsing,
        onTestDoh = viewModel::testDoh,
    )
}

@Composable
fun ShieldScreen(
    state: ShieldUiState,
    onBack: () -> Unit,
    onOpenBrowser: () -> Unit,
    onOpenDns: () -> Unit,
    onMode: (TrackingProtectionMode) -> Unit,
    onBlockAds: (Boolean) -> Unit,
    onHideAnnoyances: (Boolean) -> Unit,
    onSafeBrowsing: (Boolean) -> Unit,
    onTestDoh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings = state.settings
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.shield_back)) }
        ScreenHeading(
            title = stringResource(R.string.shield_title),
            subtitle = stringResource(R.string.shield_subtitle),
        )

        SecureCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier =
                        Modifier
                            .size(64.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(SecureBoxColors.AuroraTeal, SecureBoxColors.AuroraViolet),
                                ),
                                RoundedCornerShape(20.dp),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = SecureBoxIcons.Shield,
                        contentDescription = null,
                        tint = SecureBoxColors.Ink950,
                        modifier = Modifier.size(32.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = stringResource(R.string.shield_hero_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = stringResource(R.string.shield_hero_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(text = stringResource(R.string.shield_tracking_title), style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeCard(
                title = stringResource(R.string.shield_tracking_standard),
                body = stringResource(R.string.shield_tracking_standard_body),
                selected = settings.trackingProtection == TrackingProtectionMode.STANDARD,
                onClick = { onMode(TrackingProtectionMode.STANDARD) },
                modifier = Modifier.weight(1f),
            )
            ModeCard(
                title = stringResource(R.string.shield_tracking_high),
                body = stringResource(R.string.shield_tracking_high_body),
                selected = settings.trackingProtection == TrackingProtectionMode.HIGH,
                onClick = { onMode(TrackingProtectionMode.HIGH) },
                modifier = Modifier.weight(1f),
            )
        }

        Text(text = stringResource(R.string.shield_protections_title), style = MaterialTheme.typography.titleLarge)
        SecureCard {
            ToggleRow(
                title = stringResource(R.string.shield_block_ads),
                body = stringResource(R.string.shield_block_ads_body),
                checked = settings.blockAdsAndTrackers,
                onChecked = onBlockAds,
            )
            ToggleRow(
                title = stringResource(R.string.shield_hide_annoyances),
                body = stringResource(R.string.shield_hide_annoyances_body),
                checked = settings.hideAnnoyances,
                onChecked = onHideAnnoyances,
            )
            ToggleRow(
                title = stringResource(R.string.shield_safe_browsing),
                body = stringResource(R.string.shield_safe_browsing_body),
                checked = settings.safeBrowsing,
                onChecked = onSafeBrowsing,
            )
        }

        SecureCard {
            Text(text = stringResource(R.string.shield_dns_title), style = MaterialTheme.typography.titleMedium)
            val host = state.privateDnsHost
            Text(
                text =
                    if (host.isNullOrBlank()) {
                        stringResource(R.string.shield_dns_off)
                    } else {
                        stringResource(R.string.shield_dns_active, host)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = if (host.isNullOrBlank()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.shield_dns_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(dnsPresetInfo(settings.dnsPreset).titleRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onOpenDns) { Text(stringResource(R.string.shield_dns_configure)) }
                OutlinedButton(onClick = onTestDoh, enabled = !state.dnsTesting) {
                    Text(
                        stringResource(
                            if (state.dnsTesting) R.string.shield_dns_testing else R.string.shield_dns_test,
                        ),
                    )
                }
            }
            state.dnsTest?.let { result ->
                Text(
                    text =
                        stringResource(
                            R.string.shield_dns_test_ok,
                            result.latencyMs.toInt(),
                            result.addresses.firstOrNull().orEmpty(),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            state.dnsError?.let { error ->
                Text(
                    text = stringResource(R.string.shield_dns_test_fail, error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        ActionCard(
            icon = SecureBoxIcons.Globe,
            title = stringResource(R.string.shield_open_browser),
            description = stringResource(R.string.shield_open_browser_body),
            onClick = onOpenBrowser,
        )

        SecureCard {
            Text(text = stringResource(R.string.shield_limits_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.shield_limits_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ModeCard(
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    body: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
