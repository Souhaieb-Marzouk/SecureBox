package io.securebox.feature.apps

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.securebox.core.domain.security.AppRecommendation
import io.securebox.core.domain.security.AppRiskReport
import io.securebox.core.domain.security.Finding
import io.securebox.core.domain.security.FindingCode
import io.securebox.core.domain.security.InstallSource
import io.securebox.core.domain.security.RiskLevel
import io.securebox.core.ui.component.BannerSeverity
import io.securebox.core.ui.component.FocusAnchor
import io.securebox.core.ui.component.ScreenHeading
import io.securebox.core.ui.component.SecureCard
import io.securebox.core.ui.component.PullToRefreshBox
import io.securebox.core.ui.component.StatusBanner
import io.securebox.core.ui.icon.SecureBoxIcons
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppsRoute(viewModel: AppsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    // Returning from Android's permission settings must re-read granted permissions.
                    viewModel.load()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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
                AppsFilter.SYSTEM -> report.app.source == InstallSource.SYSTEM
                AppsFilter.PLAY_STORE -> report.app.source == InstallSource.PLAY_STORE
                AppsFilter.OTHER_STORE -> report.app.source == InstallSource.OTHER_STORE
                AppsFilter.MANUAL -> report.app.source == InstallSource.SIDELOADED
            }
        }

    PullToRefreshBox(refreshing = state.loading, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("apps_list"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        item { FocusAnchor() }
        item {
            ScreenHeading(
                title = stringResource(R.string.apps_title),
                subtitle = stringResource(R.string.apps_subtitle),
            )
        }
        item {
            SecureCard {
                Text(text = stringResource(R.string.apps_how_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.apps_how_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.apps_legend),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.reports.isNotEmpty()) {
                    val attention = state.reports.count { it.level.ordinal >= RiskLevel.MEDIUM.ordinal }
                    Text(
                        text = stringResource(R.string.apps_summary, state.reports.size, attention),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.filter == AppsFilter.ALL,
                        onClick = { onFilter(AppsFilter.ALL) },
                        label = { Text(stringResource(R.string.apps_filter_all)) },
                    )
                }
                item {
                    FilterChip(
                        selected = state.filter == AppsFilter.RISKY,
                        onClick = { onFilter(AppsFilter.RISKY) },
                        label = { Text(stringResource(R.string.apps_filter_risky)) },
                    )
                }
                item {
                    FilterChip(
                        selected = state.filter == AppsFilter.SYSTEM,
                        onClick = { onFilter(AppsFilter.SYSTEM) },
                        label = { Text(stringResource(R.string.apps_filter_system)) },
                    )
                }
                item {
                    FilterChip(
                        selected = state.filter == AppsFilter.PLAY_STORE,
                        onClick = { onFilter(AppsFilter.PLAY_STORE) },
                        label = { Text(stringResource(R.string.apps_filter_play)) },
                    )
                }
                item {
                    FilterChip(
                        selected = state.filter == AppsFilter.OTHER_STORE,
                        onClick = { onFilter(AppsFilter.OTHER_STORE) },
                        label = { Text(stringResource(R.string.apps_filter_other_store)) },
                    )
                }
                item {
                    FilterChip(
                        selected = state.filter == AppsFilter.MANUAL,
                        onClick = { onFilter(AppsFilter.MANUAL) },
                        label = { Text(stringResource(R.string.apps_filter_manual)) },
                    )
                }
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
        item { FocusAnchor() }
        }
    }

    state.selected?.let { report ->
        AppDetailDialog(report = report, onDismiss = { onSelect(null) }, onRefresh = onRefresh)
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
                SourceBadge(source = report.app.source)
                Text(
                    text = stringResource(recommendationShortRes(report)),
                    style = MaterialTheme.typography.labelSmall,
                    color = recommendationColor(report),
                )
            }
            RiskBadge(report.level, report.score)
        }
    }
}

@Composable
private fun SourceBadge(source: InstallSource) {
    val color =
        when (source) {
            InstallSource.SYSTEM -> MaterialTheme.colorScheme.secondary
            InstallSource.PLAY_STORE -> MaterialTheme.colorScheme.primary
            InstallSource.OTHER_STORE -> MaterialTheme.colorScheme.tertiary
            InstallSource.SIDELOADED -> MaterialTheme.colorScheme.error
            InstallSource.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f)),
    ) {
        Text(
            text = sourceLabel(source),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun recommendationColor(report: AppRiskReport) =
    when (report.recommendation) {
        AppRecommendation.UNINSTALL -> MaterialTheme.colorScheme.error
        AppRecommendation.REDUCE_PERMISSIONS -> MaterialTheme.colorScheme.tertiary
        AppRecommendation.KEEP -> MaterialTheme.colorScheme.primary
    }

private fun recommendationShortRes(report: AppRiskReport): Int =
    when (report.recommendation) {
        AppRecommendation.KEEP ->
            if (report.app.isSystem) {
                R.string.apps_recommend_short_system
            } else {
                R.string.apps_recommend_short_keep
            }

        AppRecommendation.REDUCE_PERMISSIONS -> R.string.apps_recommend_short_reduce
        AppRecommendation.UNINSTALL -> R.string.apps_recommend_short_uninstall
    }

@Composable
private fun RecommendationCard(report: AppRiskReport) {
    val color = recommendationColor(report)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.30f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.apps_recommendation_label),
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
            Text(
                text = stringResource(recommendationTitleRes(report)),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(recommendationBodyRes(report)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(
                text = riskLabel(level),
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
            Text(
                text = stringResource(R.string.apps_score_badge, score),
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDetailDialog(
    report: AppRiskReport,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
) {
    val context = LocalContext.current
    val installed =
        DateFormat
            .getDateFormat(context)
            .format(Date(report.app.firstInstallTimeMillis))
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FocusAnchor()
            Text(text = report.app.label, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(R.string.apps_detail_meta, report.app.versionName, installed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.apps_detail_source, sourceLabel(report.app.source)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.apps_detail_rating, report.score, riskLabel(report.level)),
                style = MaterialTheme.typography.titleMedium,
            )
            RecommendationCard(report = report)
            Text(
                text = stringResource(R.string.apps_detail_what),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = riskMeaning(report.level),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.apps_detail_findings),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.apps_detail_findings_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                text = stringResource(R.string.apps_detail_permissions_title, report.app.permissions.size),
                style = MaterialTheme.typography.titleSmall,
            )
            val sortedPermissions = report.app.permissions.sortedByDescending { permissionWeight(it) }
            if (sortedPermissions.isEmpty()) {
                Text(
                    text = stringResource(R.string.apps_detail_no_permissions),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                sortedPermissions.forEach { permission ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier =
                                Modifier
                                    .padding(top = 7.dp)
                                    .size(5.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                        )
                        Text(
                            text = permissionLabel(permission),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.apps_detail_permissions_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRefresh) {
                Text(text = stringResource(R.string.apps_refresh))
            }

            Text(
                text = stringResource(R.string.apps_detail_actions),
                style = MaterialTheme.typography.titleSmall,
            )
            Button(
                onClick = { openAppSettings(context, report.app.packageName) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.apps_action_open_settings))
            }
            if (!report.app.isSystem && report.app.packageName != context.packageName) {
                OutlinedButton(
                    onClick = { uninstallApp(context, report.app.packageName) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.apps_action_uninstall))
                }
            }
            val codes = report.findings.map { it.code }.toSet()
            if (FindingCode.ACCESSIBILITY in codes ||
                FindingCode.COMBO_SMS_ACCESSIBILITY in codes ||
                FindingCode.COMBO_OVERLAY_ACCESSIBILITY in codes
            ) {
                TextButton(onClick = { openSystemSettings(context, Settings.ACTION_ACCESSIBILITY_SETTINGS) }) {
                    Text(stringResource(R.string.apps_action_accessibility))
                }
            }
            if (FindingCode.OVERLAY in codes || FindingCode.COMBO_OVERLAY_ACCESSIBILITY in codes) {
                TextButton(
                    onClick = {
                        openSystemSettings(
                            context,
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${report.app.packageName}"),
                        )
                    },
                ) {
                    Text(stringResource(R.string.apps_action_overlay))
                }
            }
            if (FindingCode.USAGE_ACCESS in codes) {
                TextButton(onClick = { openSystemSettings(context, Settings.ACTION_USAGE_ACCESS_SETTINGS) }) {
                    Text(stringResource(R.string.apps_action_usage))
                }
            }
            if (FindingCode.DEVICE_ADMIN in codes || FindingCode.COMBO_SMS_ADMIN in codes) {
                TextButton(onClick = { openSystemSettings(context, Settings.ACTION_SECURITY_SETTINGS) }) {
                    Text(stringResource(R.string.apps_action_admin))
                }
            }
            Text(
                text = stringResource(R.string.apps_action_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.apps_detail_close)) }
            FocusAnchor()
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
            InstallSource.OTHER_STORE -> R.string.apps_source_other_store
            InstallSource.SYSTEM -> R.string.apps_source_system
            InstallSource.SIDELOADED -> R.string.apps_source_sideloaded
            InstallSource.UNKNOWN -> R.string.apps_source_unknown
        },
    )

private fun recommendationTitleRes(report: AppRiskReport): Int =
    when (report.recommendation) {
        AppRecommendation.KEEP ->
            if (report.app.isSystem) {
                R.string.apps_recommend_system_title
            } else {
                R.string.apps_recommend_keep_title
            }

        AppRecommendation.REDUCE_PERMISSIONS -> R.string.apps_recommend_reduce_title
        AppRecommendation.UNINSTALL -> R.string.apps_recommend_uninstall_title
    }

private fun recommendationBodyRes(report: AppRiskReport): Int =
    when (report.recommendation) {
        AppRecommendation.KEEP ->
            if (report.app.isSystem) {
                R.string.apps_recommend_system_body
            } else {
                R.string.apps_recommend_keep_body
            }

        AppRecommendation.REDUCE_PERMISSIONS -> R.string.apps_recommend_reduce_body
        AppRecommendation.UNINSTALL -> R.string.apps_recommend_uninstall_body
    }

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
private fun riskMeaning(level: RiskLevel): String =
    stringResource(
        when (level) {
            RiskLevel.SAFE -> R.string.apps_detail_mean_safe
            RiskLevel.LOW -> R.string.apps_detail_mean_low
            RiskLevel.MEDIUM -> R.string.apps_detail_mean_medium
            RiskLevel.HIGH -> R.string.apps_detail_mean_high
            RiskLevel.CRITICAL -> R.string.apps_detail_mean_critical
        },
    )

private fun permissionWeight(permission: String): Int {
    val simple = permission.substringAfterLast('.')
    return when {
        simple.contains("SMS") || simple.contains("CALL_LOG") -> 100
        simple.contains("ACCESSIBILITY") || simple.contains("DEVICE_ADMIN") -> 95
        simple.contains("NOTIFICATION_LISTENER") -> 90
        simple.contains("SYSTEM_ALERT_WINDOW") -> 90
        simple.contains("CONTACTS") || simple.contains("CAMERA") || simple.contains("RECORD_AUDIO") -> 80
        simple.contains("LOCATION") -> 70
        simple.contains("INSTALL_PACKAGES") -> 65
        simple.contains("USAGE") -> 60
        else -> 0
    }
}

@Composable
private fun permissionLabel(permission: String): String {
    val simple = permission.substringAfterLast('.')
    return when (simple) {
        "CAMERA" -> stringResource(R.string.perm_camera)
        "RECORD_AUDIO" -> stringResource(R.string.perm_microphone)
        "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "ACCESS_BACKGROUND_LOCATION" -> stringResource(R.string.perm_location)
        "READ_CONTACTS", "WRITE_CONTACTS" -> stringResource(R.string.perm_contacts)
        "READ_SMS", "SEND_SMS", "RECEIVE_SMS", "READ_MMS", "SEND_MMS" -> stringResource(R.string.perm_sms)
        "READ_CALL_LOG", "WRITE_CALL_LOG" -> stringResource(R.string.perm_call_log)
        "READ_PHONE_STATE" -> stringResource(R.string.perm_phone)
        "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "READ_MEDIA_IMAGES", "READ_MEDIA_VIDEO", "READ_MEDIA_AUDIO",
        "MANAGE_EXTERNAL_STORAGE" -> stringResource(R.string.perm_storage)

        "POST_NOTIFICATIONS" -> stringResource(R.string.perm_notifications)
        "PACKAGE_USAGE_STATS" -> stringResource(R.string.perm_usage)
        "SYSTEM_ALERT_WINDOW" -> stringResource(R.string.perm_overlay)
        "BIND_ACCESSIBILITY_SERVICE" -> stringResource(R.string.perm_accessibility)
        "BIND_NOTIFICATION_LISTENER_SERVICE" -> stringResource(R.string.perm_notification_listener)
        "BIND_DEVICE_ADMIN" -> stringResource(R.string.perm_admin)
        "REQUEST_INSTALL_PACKAGES" -> stringResource(R.string.perm_install)
        "QUERY_ALL_PACKAGES" -> stringResource(R.string.perm_query_all)
        "INTERNET" -> stringResource(R.string.perm_internet)
        "ACCESS_NETWORK_STATE" -> stringResource(R.string.perm_network)
        "RECEIVE_BOOT_COMPLETED" -> stringResource(R.string.perm_boot)
        "FOREGROUND_SERVICE" -> stringResource(R.string.perm_foreground)
        "WAKE_LOCK" -> stringResource(R.string.perm_wake_lock)
        else -> simple.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }
}

private fun openAppSettings(
    context: Context,
    packageName: String,
) {
    openSystemSettings(context, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
}

private fun uninstallApp(
    context: Context,
    packageName: String,
) {
    openSystemSettings(context, Intent.ACTION_DELETE, Uri.parse("package:$packageName"))
}

private fun openSystemSettings(
    context: Context,
    action: String,
    data: Uri? = null,
) {
    val intent =
        Intent(action).apply {
            if (data != null) setData(data)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    runCatching { context.startActivity(intent) }
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
