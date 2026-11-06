package io.securebox.feature.onboarding

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.securebox.core.domain.model.AppLanguage
import io.securebox.core.domain.model.DeviceMode
import io.securebox.core.ui.component.ScoreGauge
import io.securebox.core.ui.component.ScoreGradeLabels
import io.securebox.core.ui.icon.SecureBoxIcons

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    // Usage access and "install unknown apps" are granted in system screens that
                    // cannot return a result, so we re-read the real state on every resume.
                    viewModel.refreshPermissions()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    OnboardingScreen(
        state = state,
        onSelectLanguage = viewModel::selectLanguage,
        onContinueLanguage = viewModel::continueFromLanguage,
        onConfirmDevice = viewModel::confirmDeviceMode,
        onBack = viewModel::goBack,
        onNotificationResult = viewModel::onNotificationPermissionResult,
        onVpnResult = viewModel::onVpnPrepared,
        onContinuePermissions = viewModel::continueFromPermissions,
        onFinish = viewModel::finish,
    )
}

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    onSelectLanguage: (AppLanguage) -> Unit,
    onContinueLanguage: () -> Unit,
    onConfirmDevice: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNotificationResult: (Boolean) -> Unit,
    onVpnResult: (Boolean) -> Unit,
    onContinuePermissions: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Brand()
            Spacer(modifier = Modifier.height(24.dp))
            if (state.step != OnboardingStep.DONE) {
                StepIndicator(state)
                Spacer(modifier = Modifier.height(32.dp))
            }

            Column(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (state.step) {
                    OnboardingStep.LANGUAGE ->
                        LanguageStep(
                            selected = state.language,
                            onSelect = onSelectLanguage,
                            onContinue = onContinueLanguage,
                        )

                    OnboardingStep.DEVICE ->
                        DeviceStep(
                            state = state,
                            onConfirm = onConfirmDevice,
                            onBack = onBack,
                        )

                    OnboardingStep.PERMISSIONS ->
                        PermissionsStep(
                            state = state,
                            onNotificationResult = onNotificationResult,
                            onVpnResult = onVpnResult,
                            onContinue = onContinuePermissions,
                            onBack = onBack,
                        )

                    OnboardingStep.DONE ->
                        DoneStep(state = state, onFinish = onFinish)
                }
            }
        }
    }
}

@Composable
private fun Brand() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            imageVector = SecureBoxIcons.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = stringResource(R.string.onboarding_brand_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun StepIndicator(state: OnboardingUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(state.totalSteps) { index ->
            val active = index < state.stepNumber
            Box(
                modifier =
                    Modifier
                        .size(if (index == state.stepNumber - 1) 12.dp else 8.dp)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        ),
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.onboarding_step_indicator, state.stepNumber, state.totalSteps),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun LanguageStep(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onContinue: () -> Unit,
) {
    Text(
        text = stringResource(R.string.onboarding_language_title),
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.onboarding_language_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(32.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        LanguageCard(
            code = stringResource(R.string.onboarding_language_code_en),
            name = stringResource(R.string.onboarding_language_english),
            selected = selected == AppLanguage.ENGLISH,
            onClick = { onSelect(AppLanguage.ENGLISH) },
            modifier = Modifier.weight(1f),
        )
        LanguageCard(
            code = stringResource(R.string.onboarding_language_code_fr),
            name = stringResource(R.string.onboarding_language_french),
            selected = selected == AppLanguage.FRENCH,
            onClick = { onSelect(AppLanguage.FRENCH) },
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(modifier = Modifier.height(40.dp))
    Button(
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(text = stringResource(R.string.onboarding_continue), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LanguageCard(
    code: String,
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(160.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
        border =
            if (selected) {
                BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun DeviceStep(
    state: OnboardingUiState,
    onConfirm: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Icon(
        imageVector = if (state.detectedTv) SecureBoxIcons.Tv else SecureBoxIcons.Phone,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(96.dp),
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.onboarding_device_title),
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text =
            stringResource(
                if (state.detectedTv) R.string.onboarding_detected_tv else R.string.onboarding_detected_phone,
            ),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = { onConfirm(state.detectedTv) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(text = stringResource(R.string.onboarding_device_yes), style = MaterialTheme.typography.labelLarge)
    }
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(
        onClick = { onConfirm(!state.detectedTv) },
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(
            text =
                stringResource(
                    if (state.detectedTv) R.string.onboarding_device_switch_phone else R.string.onboarding_device_switch_tv,
                ),
            style = MaterialTheme.typography.labelLarge,
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.onboarding_device_hint),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = onBack) {
        Text(text = stringResource(R.string.onboarding_back))
    }
}

@Composable
private fun PermissionsStep(
    state: OnboardingUiState,
    onNotificationResult: (Boolean) -> Unit,
    onVpnResult: (Boolean) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            onNotificationResult(granted)
        }
    val vpnLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onVpnResult(result.resultCode == Activity.RESULT_OK)
        }

    Text(
        text = stringResource(R.string.onboarding_permissions_title),
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.onboarding_permissions_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(28.dp))

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PermissionCard(
            title = stringResource(R.string.onboarding_permission_usage_title),
            why = stringResource(R.string.onboarding_permission_usage_why),
            granted = state.usageAccessGranted,
            actionLabel = stringResource(R.string.onboarding_permission_allow),
            onAction = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        )

        PermissionCard(
            title = stringResource(R.string.onboarding_permission_vpn_title),
            why = stringResource(R.string.onboarding_permission_vpn_why),
            granted = state.vpnPrepared,
            actionLabel = stringResource(R.string.onboarding_permission_allow),
            onAction = {
                val prepareIntent = VpnService.prepare(context)
                if (prepareIntent == null) onVpnResult(true) else vpnLauncher.launch(prepareIntent)
            },
        )

        PermissionCard(
            title = stringResource(R.string.onboarding_permission_notifications_title),
            why = stringResource(R.string.onboarding_permission_notifications_why),
            granted = state.notificationsGranted,
            actionLabel = stringResource(R.string.onboarding_permission_allow),
            onAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onNotificationResult(true)
                }
            },
        )

        PermissionCard(
            title = stringResource(R.string.onboarding_permission_install_title),
            why = stringResource(R.string.onboarding_permission_install_why),
            granted = state.installPackagesGranted,
            actionLabel = stringResource(R.string.onboarding_permission_allow),
            onAction = {
                val intent =
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        android.net.Uri.parse("package:${context.packageName}"),
                    )
                context.startActivity(intent)
            },
            footer = if (state.installPackagesGranted) null else stringResource(R.string.onboarding_permission_install_howto),
        )
    }

    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(text = stringResource(R.string.onboarding_continue), style = MaterialTheme.typography.labelLarge)
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = onBack) {
        Text(text = stringResource(R.string.onboarding_back))
    }
}

@Composable
private fun PermissionCard(
    title: String,
    why: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    footer: String? = null,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = why,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (footer != null) {
                Text(
                    text = footer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = SecureBoxIcons.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.onboarding_permission_allowed),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Button(onClick = onAction) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Composable
private fun DoneStep(
    state: OnboardingUiState,
    onFinish: () -> Unit,
) {
    Spacer(modifier = Modifier.height(8.dp))
    ScoreGauge(
        score = state.score.value,
        grade = state.score.grade,
        gradeLabel = stringResource(ScoreGradeLabels.of(state.score.grade)),
        contentDescription = stringResource(R.string.onboarding_done_score_description, state.score.value),
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.onboarding_done_title),
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.onboarding_done_subtitle),
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(36.dp))
    Button(
        onClick = onFinish,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(text = stringResource(R.string.onboarding_done_open), style = MaterialTheme.typography.labelLarge)
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.onboarding_device_mode_current, deviceModeLabel(state.deviceMode)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun deviceModeLabel(mode: DeviceMode): String =
    stringResource(
        if (mode == DeviceMode.TV) R.string.onboarding_device_tv_short else R.string.onboarding_device_phone_short,
    )
