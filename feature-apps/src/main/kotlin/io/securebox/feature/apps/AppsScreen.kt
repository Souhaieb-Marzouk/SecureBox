package io.securebox.feature.apps

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.securebox.core.domain.security.AppRiskReport
import io.securebox.core.domain.security.Finding
import io.securebox.core.domain.security.FindingCode
import io.securebox.core.domain.security.InstallSource
import io.securebox.core.domain.security.RiskLevel
import io.securebox.core.ui.component.BannerSeverity
import io.securebox.core.ui.component.ScreenHeading
import io.securebox.core.ui.component.StatusBanner
import io.securebox.core.ui.icon.SecureBoxIcons
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppsRoute(viewModel: AppsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppsScreen(
        state = state,
        onFilter = viewModel::setFilter,
        onSelect = viewModel::select,
        onRefresh = viewModel::load,
    )
}

@Composable
fun AppsScreen(
    state: AppsUiState,
    onFilter: (AppsFilter) -> Unit,
    onSelect: (AppRiskReport?) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible =
        state.reports.filter { report ->
            when (state.filter) {
                AppsFilter.ALL -> true
                AppsFilter.RISKY -> report.level.ordinal >= RiskLevel.MEDIUM.ordinal
                AppsFilter.SIDELOADED -> report.app.source == InstallSource.SIDELOADED
            }
        }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeading(
                title = stringResource(R.string.apps_title),
                subtitle = stringResource(R.string.apps_subtitle),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.filter == AppsFilter.ALL,
                    onClick = { onFilter(AppsFilter.ALL) },
                    label = { Text(stringResource(R.string.apps_filter_all)) },
                )
                FilterChip(
                    selected = state.filter == AppsFilter.RISKY,
                    onClick = { onFilter(AppsFilter.RISKY) },
                    label = { Text(stringResource(R.string.apps_filter_risky)) },
                )
                FilterChip(
                    selected = state.filter == AppsFilter.SIDELOADED,
                    onClick = { onFilter(AppsFilter.SIDELOADED) },
                    label = { Text(stringResource(R.string.apps_filter_sideloaded)) },
                )
            }
        }
        if (state.loading) {
            item {
                StatusBanner(
                    severity = BannerSeverity.INFO,
                    message =
                        if (state.total > 0) {
                            stringResource(R.string.apps_progress, state.analyzed, state.total)
                        } else {
                            stringResource(R.string.apps_loading)
                        },
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.apps_count, visible.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRefresh) { Text(stringResource(R.string.apps_refresh)) }
            }
        }
        if (visible.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.apps_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(visible, key = { it.app.packageName }) { report ->
            AppRow(report = report, onClick = { onSelect(report) })
        }
    }

    state.selected?.let { report ->
        AppDetailDialog(report = report, onDismiss = { onSelect(null) })
    }
}

@Composable
private fun AppRow(
    report: AppRiskReport,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppIcon(packageName = report.app.packageName)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = report.app.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = sourceLabel(report.app.source),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RiskBadge(report.level, report.score)
        }
    }
}

@Composable
private fun RiskBadge(
    level: RiskLevel,
    score: Int,
) {
    val color =
        when (level) {
            RiskLevel.SAFE -> MaterialTheme.colorScheme.primary
            RiskLevel.LOW -> MaterialTheme.colorScheme.primary
            RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
            RiskLevel.HIGH, RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
        }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f)) {
        Text(
            text = stringResource(R.string.apps_score_badge, score, riskLabel(level)),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun AppDetailDialog(
    report: AppRiskReport,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val installed =
        DateFormat
            .getDateFormat(context)
            .format(Date(report.app.firstInstallTimeMillis))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(report.app.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.apps_detail_meta, report.app.versionName, installed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.apps_detail_rating, report.score, riskLabel(report.level)),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.apps_detail_findings),
                    style = MaterialTheme.typography.titleSmall,
                )
                if (report.findings.isEmpty()) {
                    Text(
                        text = stringResource(R.string.apps_detail_no_findings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    report.findings.take(10).forEach { finding ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier =
                                    Modifier
                                        .padding(top = 7.dp)
                                        .size(6.dp)
                                        .background(severityColor(finding.severity), CircleShape),
                            )
                            Text(
                                text = findingLabel(finding),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.apps_detail_permissions, report.app.permissions.size),
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.apps_detail_close)) }
        },
    )
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
        Image(bitmap = image, contentDescription = null, modifier = modifier.size(38.dp).clip(CircleShape))
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
private fun sourceLabel(source: InstallSource): String =
    stringResource(
        when (source) {
            InstallSource.PLAY_STORE -> R.string.apps_source_play
            InstallSource.SYSTEM -> R.string.apps_source_system
            InstallSource.SIDELOADED -> R.string.apps_source_sideloaded
            InstallSource.UNKNOWN -> R.string.apps_source_unknown
        },
    )

@Composable
private fun riskLabel(level: RiskLevel): String =
    stringResource(
        when (level) {
            RiskLevel.SAFE -> R.string.risk_safe
            RiskLevel.LOW -> R.string.risk_low
            RiskLevel.MEDIUM -> R.string.risk_medium
            RiskLevel.HIGH -> R.string.risk_high
            RiskLevel.CRITICAL -> R.string.risk_critical
        },
    )

@Composable
private fun severityColor(level: RiskLevel) =
    when (level) {
        RiskLevel.SAFE, RiskLevel.LOW -> MaterialTheme.colorScheme.primary
        RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiary
        RiskLevel.HIGH, RiskLevel.CRITICAL -> MaterialTheme.colorScheme.error
    }

@Composable
private fun findingLabel(finding: Finding): String {
    val detail = finding.detail.orEmpty()
    return when (finding.code) {
        FindingCode.SMS_ACCESS -> stringResource(R.string.finding_sms)
        FindingCode.CALL_LOG_ACCESS -> stringResource(R.string.finding_call_log)
        FindingCode.CONTACTS_ACCESS -> stringResource(R.string.finding_contacts)
        FindingCode.LOCATION_ACCESS -> stringResource(R.string.finding_location)
        FindingCode.CAMERA_ACCESS -> stringResource(R.string.finding_camera)
        FindingCode.MICROPHONE_ACCESS -> stringResource(R.string.finding_microphone)
        FindingCode.OVERLAY -> stringResource(R.string.finding_overlay)
        FindingCode.ACCESSIBILITY -> stringResource(R.string.finding_accessibility)
        FindingCode.DEVICE_ADMIN -> stringResource(R.string.finding_device_admin)
        FindingCode.NOTIFICATION_LISTENER -> stringResource(R.string.finding_notification_listener)
        FindingCode.INSTALL_PACKAGES -> stringResource(R.string.finding_install_packages)
        FindingCode.USAGE_ACCESS -> stringResource(R.string.finding_usage_access)
        FindingCode.TRACKER_SDK -> stringResource(R.string.finding_tracker, detail)
        FindingCode.SUSPICIOUS_SDK -> stringResource(R.string.finding_suspicious_sdk, detail)
        FindingCode.SIDELOADED -> stringResource(R.string.finding_sideloaded, detail)
        FindingCode.EXPORTED_COMPONENT -> stringResource(R.string.finding_exported, detail)
        FindingCode.HIDDEN_LAUNCHER -> stringResource(R.string.finding_hidden_launcher)
        FindingCode.SIGNATURE_RULE -> stringResource(R.string.finding_signature, detail)
        FindingCode.COMBO_SMS_ACCESSIBILITY -> stringResource(R.string.finding_combo_sms_accessibility)
        FindingCode.COMBO_OVERLAY_ACCESSIBILITY -> stringResource(R.string.finding_combo_overlay_accessibility)
        FindingCode.COMBO_SMS_ADMIN -> stringResource(R.string.finding_combo_sms_admin)
    }
}
