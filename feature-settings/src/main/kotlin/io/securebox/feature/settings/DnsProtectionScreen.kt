package io.securebox.feature.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.securebox.core.domain.model.DnsPreset
import io.securebox.core.ui.component.ScreenHeading
import io.securebox.core.ui.component.SecureCard

@Composable
fun DnsProtectionRoute(
    onBack: () -> Unit,
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
    DnsProtectionScreen(
        state = state,
        onBack = onBack,
        onPreset = viewModel::setDnsPreset,
        onTest = viewModel::testDoh,
        onRefresh = viewModel::refreshPrivateDns,
    )
}

@Composable
fun DnsProtectionScreen(
    state: ShieldUiState,
    onBack: () -> Unit,
    onPreset: (DnsPreset) -> Unit,
    onTest: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var settingsError by remember { mutableStateOf(false) }
    val selected = dnsPresetInfo(state.settings.dnsPreset)

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
            title = stringResource(R.string.shield_dns_title_screen),
            subtitle = stringResource(R.string.shield_dns_subtitle),
        )

        SecureCard {
            Text(text = stringResource(R.string.shield_dns_current), style = MaterialTheme.typography.titleMedium)
            val host = state.privateDnsHost
            Text(
                text =
                    if (host.isNullOrBlank()) {
                        stringResource(R.string.shield_dns_current_off)
                    } else {
                        host
                    },
                style = MaterialTheme.typography.bodyLarge,
                color = if (host.isNullOrBlank()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            )
            TextButton(onClick = onRefresh) {
                Text(text = stringResource(R.string.shield_dns_check))
            }
        }

        Text(text = stringResource(R.string.shield_dns_presets), style = MaterialTheme.typography.titleLarge)
        dnsPresetInfos.forEach { info ->
            DnsPresetCard(
                info = info,
                selected = info.preset == state.settings.dnsPreset,
                onClick = { onPreset(info.preset) },
            )
        }

        SecureCard {
            Text(text = stringResource(R.string.shield_dns_how_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.shield_dns_step1), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.shield_dns_step2), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.shield_dns_step3), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.shield_dns_step4), style = MaterialTheme.typography.bodyMedium)
        }

        SecureCard {
            Text(text = stringResource(R.string.shield_dns_dot, selected.dotHost), style = MaterialTheme.typography.bodySmall)
            Text(text = stringResource(R.string.shield_dns_doh, selected.dohUrl), style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(selected.dotHost))
                        Toast.makeText(context, R.string.shield_dns_copied, Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text(stringResource(R.string.shield_dns_copy))
                }
                OutlinedButton(
                    onClick = {
                        settingsError = !openPrivateDnsSettings(context)
                    },
                ) {
                    Text(stringResource(R.string.shield_dns_open))
                }
            }
            if (settingsError) {
                Text(
                    text = stringResource(R.string.shield_dns_open_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedButton(onClick = onTest, enabled = !state.dnsTesting) {
                Text(
                    stringResource(
                        if (state.dnsTesting) R.string.shield_dns_testing_detail else R.string.shield_dns_test_title,
                    ),
                )
            }
            state.dnsTest?.let { result ->
                Text(
                    text = stringResource(R.string.shield_dns_latency, result.latencyMs.toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.shield_dns_addresses, result.addresses.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.dnsError?.let { error ->
                Text(
                    text = stringResource(R.string.shield_dns_error, error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = stringResource(R.string.shield_dns_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DnsPresetCard(
    info: DnsPresetInfo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(info.titleRes), style = MaterialTheme.typography.titleMedium)
                if (info.preset == DnsPreset.ADGUARD) {
                    Text(
                        text = stringResource(R.string.shield_dns_recommended),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (info.preset == DnsPreset.CLOUDFLARE_FAMILIES) {
                    Text(
                        text = stringResource(R.string.shield_dns_family),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Text(
                text = stringResource(info.bodyRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = info.dohUrl,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = info.dotHost,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
