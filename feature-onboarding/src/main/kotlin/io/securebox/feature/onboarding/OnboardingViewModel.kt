package io.securebox.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.securebox.core.domain.device.DeviceModeDetector
import io.securebox.core.domain.model.AppLanguage
import io.securebox.core.domain.model.DeviceMode
import io.securebox.core.domain.model.SecurityInputs
import io.securebox.core.domain.model.SecurityScore
import io.securebox.core.domain.model.CalculateSecurityScoreUseCase
import io.securebox.core.domain.permission.PlatformPermissions
import io.securebox.core.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep {
    LANGUAGE,
    DEVICE,
    PERMISSIONS,
    DONE,
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.LANGUAGE,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val detectedTv: Boolean = false,
    val deviceMode: DeviceMode = DeviceMode.PHONE,
    val usageAccessGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val installPackagesGranted: Boolean = false,
    val vpnPrepared: Boolean = false,
    val score: SecurityScore = SecurityScore.EMPTY,
) {
    val stepNumber: Int
        get() = step.ordinal + 1

    val totalSteps: Int
        get() = TOTAL_STEPS

    companion object {
        const val TOTAL_STEPS: Int = 3
    }
}

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val deviceModeDetector: DeviceModeDetector,
        private val platformPermissions: PlatformPermissions,
        private val calculateSecurityScore: CalculateSecurityScoreUseCase,
    ) : ViewModel() {
        private val detectedTv = deviceModeDetector.isTelevision()

        private val _state =
            MutableStateFlow(
                OnboardingUiState(
                    detectedTv = detectedTv,
                    deviceMode = if (detectedTv) DeviceMode.TV else DeviceMode.PHONE,
                ),
            )
        val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

        init {
            refreshPermissions()
        }

        fun selectLanguage(language: AppLanguage) {
            _state.update { it.copy(language = language) }
            // Applied immediately so the next screen is already translated.
            viewModelScope.launch { settingsRepository.setLanguage(language) }
        }

        fun continueFromLanguage() {
            _state.update { it.copy(step = OnboardingStep.DEVICE) }
        }

        fun confirmDeviceMode(isTv: Boolean) {
            val mode = if (isTv) DeviceMode.TV else DeviceMode.PHONE
            _state.update { it.copy(deviceMode = mode, step = OnboardingStep.PERMISSIONS) }
            viewModelScope.launch { settingsRepository.setDeviceMode(mode, confirmedByUser = true) }
        }

        fun goBack() {
            _state.update { current ->
                val previous =
                    when (current.step) {
                        OnboardingStep.DEVICE -> OnboardingStep.LANGUAGE
                        OnboardingStep.PERMISSIONS -> OnboardingStep.DEVICE
                        else -> current.step
                    }
                current.copy(step = previous)
            }
        }

        /** Called whenever the app resumes: special permissions cannot report a result directly. */
        fun refreshPermissions() {
            val usage = platformPermissions.hasUsageAccess()
            val notifications = platformPermissions.hasNotificationPermission()
            val install = platformPermissions.canInstallPackages()
            _state.update { current ->
                val updated = current.copy(
                    usageAccessGranted = usage,
                    notificationsGranted = notifications,
                    installPackagesGranted = install,
                )
                updated.copy(score = scoreFor(updated))
            }
        }

        fun onNotificationPermissionResult(granted: Boolean) {
            _state.update { current ->
                val updated = current.copy(notificationsGranted = granted || current.notificationsGranted)
                updated.copy(score = scoreFor(updated))
            }
            viewModelScope.launch { persistPermissionSnapshot() }
        }

        fun onVpnPrepared(prepared: Boolean) {
            _state.update { current ->
                val updated = current.copy(vpnPrepared = prepared)
                updated.copy(score = scoreFor(updated))
            }
            viewModelScope.launch { persistPermissionSnapshot() }
        }

        fun continueFromPermissions() {
            _state.update { it.copy(step = OnboardingStep.DONE) }
            viewModelScope.launch { persistPermissionSnapshot() }
        }

        fun finish() {
            viewModelScope.launch {
                persistPermissionSnapshot()
                settingsRepository.completeOnboarding()
            }
        }

        private suspend fun persistPermissionSnapshot() {
            val current = _state.value
            settingsRepository.recordPermissionSnapshot(
                usageAccessGranted = current.usageAccessGranted,
                notificationsGranted = current.notificationsGranted,
                vpnPrepared = current.vpnPrepared,
            )
        }

        private fun scoreFor(state: OnboardingUiState): SecurityScore =
            calculateSecurityScore(
                SecurityInputs(
                    usageAccessGranted = state.usageAccessGranted,
                    notificationsGranted = state.notificationsGranted,
                    firewallPrepared = state.vpnPrepared,
                    // A freshly installed build is by definition the latest one we know about.
                    appIsUpToDate = true,
                ),
            )
    }
