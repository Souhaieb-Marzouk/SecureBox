package io.securebox.feature.network

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.net.VpnService
import android.provider.Settings
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.securebox.core.domain.network.AnomalyKind
import io.securebox.core.domain.network.AnomalySnapshot
import io.securebox.core.domain.network.FirewallPreset
import io.securebox.core.domain.network.ListeningPort
import io.securebox.core.domain.network.PortScanResult
import io.securebox.core.domain.network.TrafficRange
import io.securebox.core.ui.component.BannerSeverity
import io.securebox.core.ui.component.FocusAnchor
import io.securebox.core.ui.component.ScreenHeading
import io.securebox.core.ui.component.SecureCard
import io.securebox.core.ui.component.StatusBanner
import io.securebox.core.ui.icon.SecureBoxIcons
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NetworkRoute(viewModel: NetworkViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val vpnLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.setFirewallEnabled(true)
            } else {
                viewModel.onVpnPermissionDenied()
            }
        }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NetworkScreen(
        state = state,
        onRange = viewModel::setRange,
        onToggleFirewall = { enabled ->
            if (!enabled) {
                viewModel.setFirewallEnabled(false)
            } else {
                val prepare = VpnService.prepare(context)
                if (prepare == null) viewModel.setFirewallEnabled(true) else vpnLauncher.launch(prepare)
            }
        },
        onPreset = viewModel::setPreset,
        onBlockUnknown = viewModel::blockUnknownApps,
        onToggleBlock = viewModel::toggleBlock,
        onRefresh = viewModel::refresh,
        onClearError = viewModel::clearError,
    )
}

@Composable
fun NetworkScreen(
    state: NetworkUiState,
    onRange: (TrafficRange) -> Unit,
    onToggleFirewall: (Boolean) -> Unit,
    onPreset: (FirewallPreset) -> Unit,
    onBlockUnknown: () -> Unit,
    onToggleBlock: (String, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var usageHelp by remember { mutableStateOf(false) }
    val isTelevision =
        remember(context) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
                context.getSystemService(UiModeManager::class.java)?.currentModeType ==
                Configuration.UI_MODE_TYPE_TELEVISION
        }
    val maxMetric = state.apps.maxOfOrNull { if (state.range == TrafficRange.LIVE) it.rateBytesPerSecond else it.totalBytes }?.coerceAtLeast(1L) ?: 1L

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { FocusAnchor() }
        item {
            ScreenHeading(
                title = stringResource(R.string.network_title),
                subtitle = stringResource(R.string.network_subtitle),
            )
        }

        if (!state.usageAccessGranted) {
            item {
                UsageAccessCard(
                    onOpenSettings = {
                        // On TVs the phone-style Usage access activity often resolves but shows a
                        // blank or unrelated screen. Show the manual path and offer generic Settings.
                        if (isTelevision || !openUsageAccessSettings(context)) usageHelp = true
                    },
                )
            }
        }

        state.error?.let { error ->
            item {
                StatusBanner(
                    severity = BannerSeverity.DANGER,
                    message =
                        stringResource(
                            when (error) {
                                NetworkError.VPN_UNAVAILABLE -> R.string.network_error_vpn
                                NetworkError.SERVICE_FAILED -> R.string.network_error_service
                            },
                        ),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButtonLabel(text = stringResource(R.string.network_dismiss), onClick = onClearError)
                    TextButtonLabel(text = stringResource(R.string.network_refresh), onClick = onRefresh)
                }
            }
        }

        item {
            FirewallCard(
                state = state,
                onToggle = onToggleFirewall,
                onPreset = onPreset,
                onBlockUnknown = onBlockUnknown,
            )
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.network_bandwidth_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                TextButtonLabel(text = stringResource(R.string.network_refresh), onClick = onRefresh)
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { RangeChip(TrafficRange.LIVE, state.range, onRange) }
                item { RangeChip(TrafficRange.DAY, state.range, onRange) }
                item { RangeChip(TrafficRange.WEEK, state.range, onRange) }
                item { RangeChip(TrafficRange.MONTH, state.range, onRange) }
            }
        }

        item {
            Text(
                text =
                    stringResource(
                        if (state.range == TrafficRange.LIVE) {
                            R.string.network_live_hint
                        } else {
                            R.string.network_history_hint
                        },
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.apps.isEmpty()) {
            item {
                SecureCard {
                    Text(
                        text = stringResource(R.string.network_no_usage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            items(state.apps, key = { it.packageName }) { app ->
                AppTrafficRow(
                    app = app,
                    maxMetric = maxMetric,
                    live = state.range == TrafficRange.LIVE,
                    onToggleBlock = onToggleBlock,
                )
            }
        }

        item { PortsCard(state.ports, onRefresh = onRefresh) }
        item { AnomalyCard(state.anomaly) }
        item { FocusAnchor() }
    }

    if (usageHelp) {
        AlertDialog(
            onDismissRequest = { usageHelp = false },
            title = { Text(text = stringResource(R.string.network_usage_manual_title)) },
            text = { Text(text = stringResource(R.string.network_usage_manual_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        usageHelp = false
                        openGenericSettings(context)
                    },
                ) {
                    Text(text = stringResource(R.string.network_usage_manual_open))
                }
            },
            dismissButton = {
                TextButton(onClick = { usageHelp = false }) {
                    Text(text = stringResource(R.string.network_usage_manual_close))
                }
            },
        )
    }
}

@Composable
private fun UsageAccessCard(onOpenSettings: () -> Unit) {
    SecureCard {
        Text(text = stringResource(R.string.network_usage_needed_title), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.network_usage_needed_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButtonLabel(text = stringResource(R.string.network_open_settings), onClick = onOpenSettings)
    }
}

@Composable
private fun FirewallCard(
    state: NetworkUiState,
    onToggle: (Boolean) -> Unit,
    onPreset: (FirewallPreset) -> Unit,
    onBlockUnknown: () -> Unit,
) {
    val firewall = state.firewall
    SecureCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(R.string.network_firewall_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    text =
                        when {
                            !firewall.enabled -> stringResource(R.string.network_firewall_off)
                            firewall.running -> stringResource(R.string.network_firewall_on, firewall.effectiveBlockedCount)
                            else -> stringResource(R.string.network_firewall_waiting)
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (firewall.running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = firewall.enabled, onCheckedChange = onToggle)
        }

        if (firewall.otherVpnActive) {
            StatusBanner(severity = BannerSeverity.WARNING, message = stringResource(R.string.network_firewall_other_vpn))
        }

        Text(
            text = stringResource(R.string.network_firewall_local_only),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(4.dp))
        PresetRow(
            label = stringResource(R.string.network_preset_allow_all),
            selected = firewall.preset == FirewallPreset.ALLOW_ALL,
            onClick = { onPreset(FirewallPreset.ALLOW_ALL) },
        )
        PresetRow(
            label = stringResource(R.string.network_preset_block_unknown),
            selected = firewall.preset == FirewallPreset.BLOCK_UNKNOWN_APPS,
            onClick = { onPreset(FirewallPreset.BLOCK_UNKNOWN_APPS) },
        )
        Text(
            text = stringResource(R.string.network_preset_block_unknown_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (firewall.preset == FirewallPreset.BLOCK_UNKNOWN_APPS && !firewall.enabled) {
            TextButtonLabel(text = stringResource(R.string.network_block_unknown_action), onClick = onBlockUnknown)
        }
    }
}

@Composable
private fun PresetRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(start = 6.dp),
        )
    }
}

@Composable
private fun RangeChip(
    range: TrafficRange,
    selected: TrafficRange,
    onSelect: (TrafficRange) -> Unit,
) {
    FilterChip(
        selected = range == selected,
        onClick = { onSelect(range) },
        label = {
            Text(
                text =
                    stringResource(
                        when (range) {
                            TrafficRange.LIVE -> R.string.network_range_live
                            TrafficRange.DAY -> R.string.network_range_day
                            TrafficRange.WEEK -> R.string.network_range_week
                            TrafficRange.MONTH -> R.string.network_range_month
                        },
                    ),
            )
        },
    )
}

@Composable
private fun AppTrafficRow(
    app: AppRow,
    maxMetric: Long,
    live: Boolean,
    onToggleBlock: (String, Boolean) -> Unit,
) {
    val fraction = ((if (live) app.rateBytesPerSecond else app.totalBytes).toFloat() / maxMetric.toFloat()).coerceIn(0f, 1f)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIcon(packageName = app.packageName)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text =
                            if (live) {
                                stringResource(
                                    R.string.network_row_live_stats,
                                    formatBytes(app.rateBytesPerSecond),
                                    formatBytes(app.totalBytes),
                                )
                            } else {
                                stringResource(
                                    R.string.network_row_history_stats,
                                    formatBytes(app.rxBytes),
                                    formatBytes(app.txBytes),
                                )
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = app.blocked,
                    onCheckedChange = { checked -> onToggleBlock(app.packageName, checked) },
                    enabled = !app.isSelf,
                )
            }
            LinearProgressIndicator(
                progress = fraction,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(4.dp),
            )
        }
    }
}

@Composable
private fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value =
            withContext(Dispatchers.IO) {
                runCatching {
                    context.packageManager.getApplicationIcon(packageName).toBitmap(72, 72).asImageBitmap()
                }.getOrNull()
            }
    }
    val image = bitmap
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = modifier.size(38.dp).clip(CircleShape),
        )
    } else {
        Box(
            modifier =
                modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = SecureBoxIcons.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun PortsCard(
    ports: PortScanResult,
    onRefresh: () -> Unit,
) {
    SecureCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.network_ports_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            TextButtonLabel(text = stringResource(R.string.network_ports_rescan), onClick = onRefresh)
        }
        Text(
            text = stringResource(R.string.network_ports_explainer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            !ports.available -> {
                Text(
                    text = stringResource(R.string.network_ports_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ports.ports.isEmpty() -> {
                Text(
                    text = stringResource(R.string.network_ports_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                ports.ports.take(8).forEach { port -> PortRow(port) }
            }
        }
    }
}

@Composable
private fun PortRow(port: ListeningPort) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.network_port_label, port.protocol.name, port.port),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = port.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(if (port.exposed) R.string.network_ports_exposed else R.string.network_ports_local),
            style = MaterialTheme.typography.labelSmall,
            color = if (port.exposed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AnomalyCard(snapshot: AnomalySnapshot) {
    SecureCard {
        Text(text = stringResource(R.string.network_anomaly_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(R.string.network_anomaly_explainer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val lastChecked = snapshot.lastCheckedAtMillis
        Text(
            text =
                if (lastChecked == null) {
                    stringResource(R.string.network_anomaly_never)
                } else {
                    val formatted =
                        DateFormat
                            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                            .format(Date(lastChecked))
                    stringResource(R.string.network_anomaly_last, formatted)
                },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (snapshot.events.isEmpty()) {
            Text(
                text = stringResource(R.string.network_anomaly_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            snapshot.events.take(3).forEach { event ->
                Text(
                    text =
                        if (event.kind == AnomalyKind.HARD_THRESHOLD) {
                            stringResource(R.string.network_anomaly_hard, event.label, formatBytes(event.observedBytes))
                        } else {
                            stringResource(
                                R.string.network_anomaly_spike,
                                event.label,
                                formatBytes(event.observedBytes),
                                formatBytes(event.baselineBytesPerHour),
                            )
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun TextButtonLabel(
    text: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

private fun openGenericSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun openUsageAccessSettings(context: Context): Boolean {
    // Android TV boxes often omit the phone-style Usage access screen, and some OEM settings apps
    // resolve the action but fail when started. Try the specific screen, then generic Settings, then
    // the app's own info page, and report failure so the UI can show manual steps.
    val candidates =
        listOf(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
        )
    for (intent in candidates) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) continue
        if (runCatching { context.startActivity(intent) }.isSuccess) return true
    }
    return false
}

@Composable
private fun formatBytes(bytes: Long): String = Formatter.formatFileSize(LocalContext.current, bytes.coerceAtLeast(0L))
