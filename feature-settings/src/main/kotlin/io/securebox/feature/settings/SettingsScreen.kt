package io.securebox.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import io.securebox.core.domain.model.AppLanguage
import io.securebox.core.domain.model.DeviceMode
import io.securebox.core.domain.model.ThemeMode
import io.securebox.core.domain.security.IntegrityIssue
import io.securebox.core.ui.component.BrandMark
import io.securebox.core.ui.component.ScreenHeading
import io.securebox.core.ui.component.SecureCard
import io.securebox.core.ui.icon.SecureBoxIcons
import io.securebox.feature.update.UpdateSection

private const val GITHUB_URL = "https://github.com/Souhaieb-Marzouk/SecureBox"
private const val LICENSE_URL = "$GITHUB_URL/blob/main/LICENSE"

@Composable
fun SettingsRoute(
    onOpenPrivacy: () -> Unit,
    onOpenThreatIntel: () -> Unit,
    onOpenHelp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onLanguage = viewModel::setLanguage,
        onTheme = viewModel::setTheme,
        onDeviceMode = viewModel::setDeviceMode,
        onDetectAgain = viewModel::detectAgain,
        onThreatIntelEnabled = viewModel::setThreatIntelEnabled,
        onRefreshIntegrity = viewModel::refreshIntegrity,
        onOpenPrivacy = onOpenPrivacy,
        onOpenThreatIntel = onOpenThreatIntel,
        onOpenHelp = onOpenHelp,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onLanguage: (AppLanguage) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onDeviceMode: (DeviceMode) -> Unit,
    onDetectAgain: () -> Unit,
    onThreatIntelEnabled: (Boolean) -> Unit,
    onRefreshIntegrity: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenThreatIntel: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 20.dp),
    ) {
        item {
            ScreenHeading(
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.settings_subtitle),
            )
        }

        item {
            SecureCard(modifier = Modifier.clickable(onClick = onOpenHelp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    BrandMark(size = 44.dp)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_help_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.settings_help_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = SecureBoxIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        item {
            SettingsSection(
                title = stringResource(R.string.settings_section_language),
                hint = stringResource(R.string.settings_section_language_hint),
            ) {
                RadioRow(
                    label = stringResource(R.string.settings_language_english),
                    selected = state.settings.language == AppLanguage.ENGLISH,
                    onClick = { onLanguage(AppLanguage.ENGLISH) },
                )
                RadioRow(
                    label = stringResource(R.string.settings_language_french),
                    selected = state.settings.language == AppLanguage.FRENCH,
                    onClick = { onLanguage(AppLanguage.FRENCH) },
                )
            }
        }

        item {
            SettingsSection(
                title = stringResource(R.string.settings_section_device),
                hint = stringResource(R.string.settings_section_device_hint),
            ) {
                RadioRow(
                    label = stringResource(R.string.settings_device_phone),
                    selected = state.settings.deviceMode == DeviceMode.PHONE,
                    onClick = { onDeviceMode(DeviceMode.PHONE) },
                )
                RadioRow(
                    label = stringResource(R.string.settings_device_tv),
                    selected = state.settings.deviceMode == DeviceMode.TV,
                    onClick = { onDeviceMode(DeviceMode.TV) },
                )
                Spacer(modifier = Modifier.padding(top = 4.dp))
                Button(onClick = onDetectAgain) {
                    Text(text = stringResource(R.string.settings_device_detect_again))
                }
                state.detectionMessage?.let { detected ->
                    Text(
                        text =
                            stringResource(
                                if (detected == DeviceMode.TV) {
                                    R.string.settings_device_detected_tv
                                } else {
                                    R.string.settings_device_detected_phone
                                },
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item {
            SettingsSection(
                title = stringResource(R.string.settings_section_theme),
                hint = stringResource(R.string.settings_section_theme_hint),
            ) {
                RadioRow(
                    label = stringResource(R.string.settings_theme_dark),
                    selected = state.settings.themeMode == ThemeMode.DARK,
                    onClick = { onTheme(ThemeMode.DARK) },
                )
                RadioRow(
                    label = stringResource(R.string.settings_theme_light),
                    selected = state.settings.themeMode == ThemeMode.LIGHT,
                    onClick = { onTheme(ThemeMode.LIGHT) },
                )
                RadioRow(
                    label = stringResource(R.string.settings_theme_system),
                    selected = state.settings.themeMode == ThemeMode.SYSTEM,
                    onClick = { onTheme(ThemeMode.SYSTEM) },
                )
            }
        }

        item { SectionTitle(text = stringResource(R.string.settings_section_protection)) }

        item {
            SecureCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = stringResource(R.string.settings_firewall_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.settings_firewall_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_firewall_status),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }

        item {
            SecureCard {
                Text(text = stringResource(R.string.settings_threat_intel_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.settings_threat_intel_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.settings_threat_intel_toggle),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = state.settings.threatIntelEnabled, onCheckedChange = onThreatIntelEnabled)
                }
                TextButton(onClick = onOpenThreatIntel) {
                    Text(text = stringResource(R.string.settings_threat_intel_open))
                }
            }
        }

        item { SectionTitle(text = stringResource(R.string.settings_integrity_title)) }
        item {
            SecureCard {
                val report = state.integrity
                if (report == null) {
                    Text(
                        text = stringResource(R.string.settings_integrity_checking),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val signatureMismatch = report.issues.contains(IntegrityIssue.SIGNATURE_MISMATCH)
                    val advisory = report.issues.isNotEmpty() && !signatureMismatch
                    Text(
                        text =
                            when {
                                signatureMismatch -> stringResource(R.string.settings_integrity_warning)
                                advisory -> stringResource(R.string.settings_integrity_advisory)
                                else -> stringResource(R.string.settings_integrity_ok)
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            when {
                                signatureMismatch -> MaterialTheme.colorScheme.error
                                advisory -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            },
                    )
                    report.issues.forEach { issue ->
                        Text(
                            text = integrityIssueLabel(issue),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = onRefreshIntegrity) {
                        Text(stringResource(R.string.settings_integrity_recheck))
                    }
                }
            }
        }

        if (FeatureFlags.PAID_TIER_AVAILABLE) {
            item { SectionTitle(text = stringResource(R.string.settings_paid_title)) }
            item {
                SecureCard {
                    Text(
                        text = stringResource(R.string.settings_paid_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = {}, enabled = false) {
                        Text(stringResource(R.string.settings_paid_cta))
                    }
                }
            }
        }

        item { SectionTitle(text = stringResource(R.string.settings_section_updates)) }
        item { UpdateSection() }

        item {
            SecureCard(modifier = Modifier.clickable(onClick = onOpenPrivacy)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = SecureBoxIcons.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(text = stringResource(R.string.settings_privacy_row_title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(R.string.settings_privacy_row_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = SecureBoxIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { SectionTitle(text = stringResource(R.string.settings_section_about)) }
        item {
            SecureCard {
                Text(
                    text = stringResource(R.string.settings_about_version, state.installedVersion),
                    style = MaterialTheme.typography.bodyLarge,
                )
                LinkRow(
                    icon = SecureBoxIcons.Globe,
                    label = stringResource(R.string.settings_about_github),
                    onClick = { openUrl(GITHUB_URL) },
                )
                LinkRow(
                    icon = SecureBoxIcons.Shield,
                    label = stringResource(R.string.settings_about_license),
                    onClick = { openUrl(LICENSE_URL) },
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun SettingsSection(
    title: String,
    hint: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(text = title)
        Text(text = hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(modifier = Modifier.padding(12.dp), content = content)
        }
    }
}

@Composable
private fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
        )
    }
}

@Composable
private fun LinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}


@Composable
private fun integrityIssueLabel(issue: IntegrityIssue): String =
    stringResource(
        when (issue) {
            IntegrityIssue.DEBUGGER_ATTACHED -> R.string.settings_integrity_debugger
            IntegrityIssue.EMULATOR -> R.string.settings_integrity_emulator
            IntegrityIssue.ROOTED -> R.string.settings_integrity_rooted
            IntegrityIssue.TEST_KEYS -> R.string.settings_integrity_test_keys
            IntegrityIssue.SIGNATURE_MISMATCH -> R.string.settings_integrity_signature
        },
    )