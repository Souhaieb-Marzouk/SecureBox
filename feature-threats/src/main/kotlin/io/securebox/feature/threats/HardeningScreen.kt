package io.securebox.feature.threats

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import io.securebox.core.domain.security.DangerousSetting
import io.securebox.core.domain.security.SettingId
import io.securebox.core.domain.security.SettingSeverity
import io.securebox.core.ui.component.BannerSeverity
import io.securebox.core.ui.component.ScreenHeading
import io.securebox.core.ui.component.StatusBanner

@Composable
fun HardeningRoute(
    onBack: () -> Unit,
    viewModel: HardeningViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HardeningScreen(state = state, onBack = onBack, onRefresh = viewModel::refresh)
}

@Composable
fun HardeningScreen(
    state: HardeningUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dangerous = state.settings.filter { it.isDangerous }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeading(
                title = stringResource(R.string.hardening_title),
                subtitle = stringResource(R.string.hardening_subtitle),
            )
        }
        if (state.loading) {
            item { StatusBanner(severity = BannerSeverity.INFO, message = stringResource(R.string.hardening_checking)) }
        } else if (dangerous.isEmpty()) {
            item { StatusBanner(severity = BannerSeverity.INFO, message = stringResource(R.string.hardening_clear)) }
        } else {
            item {
                StatusBanner(
                    severity = BannerSeverity.WARNING,
                    message = stringResource(R.string.hardening_found, dangerous.size),
                )
            }
        }

        items(state.settings, key = { it.id.name }) { setting ->
            HardeningRow(
                setting = setting,
                onFix = {
                    val intent = intentFor(setting.id, context.packageName)
                    runCatching { context.startActivity(intent) }
                        .onFailure {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        }
                },
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack) { Text(stringResource(R.string.hardening_back)) }
                OutlinedButton(onClick = onRefresh) { Text(stringResource(R.string.hardening_recheck)) }
            }
        }
    }
}

@Composable
private fun HardeningRow(
    setting: DangerousSetting,
    onFix: () -> Unit,
) {
    val accent =
        when (setting.severity) {
            SettingSeverity.HIGH -> MaterialTheme.colorScheme.error
            SettingSeverity.MEDIUM -> MaterialTheme.colorScheme.tertiary
            SettingSeverity.LOW -> MaterialTheme.colorScheme.primary
        }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = settingTitle(setting.id), style = MaterialTheme.typography.titleMedium)
                Text(
                    text =
                        if (setting.isDangerous) {
                            if (setting.count > 1) stringResource(R.string.hardening_active_count, setting.count) else stringResource(R.string.hardening_active)
                        } else {
                            stringResource(R.string.hardening_inactive)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (setting.isDangerous) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (setting.isDangerous) {
                OutlinedButton(onClick = onFix) { Text(stringResource(R.string.hardening_fix)) }
            }
        }
    }
}

@Composable
private fun settingTitle(id: SettingId): String =
    stringResource(
        when (id) {
            SettingId.DEVELOPER_OPTIONS -> R.string.hardening_developer_options
            SettingId.USB_DEBUGGING -> R.string.hardening_usb_debugging
            SettingId.ACCESSIBILITY -> R.string.hardening_accessibility
            SettingId.DEVICE_ADMIN -> R.string.hardening_device_admin
            SettingId.USER_CA_CERTIFICATES -> R.string.hardening_user_ca
            SettingId.INSTALL_UNKNOWN_APPS -> R.string.hardening_install_unknown
        },
    )

private fun intentFor(
    id: SettingId,
    packageName: String,
): Intent =
    when (id) {
        SettingId.DEVELOPER_OPTIONS, SettingId.USB_DEBUGGING ->
            Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)

        SettingId.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        SettingId.DEVICE_ADMIN -> Intent(Settings.ACTION_SECURITY_SETTINGS)
        SettingId.USER_CA_CERTIFICATES -> Intent(Settings.ACTION_SECURITY_SETTINGS)
        SettingId.INSTALL_UNKNOWN_APPS ->
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
    }
