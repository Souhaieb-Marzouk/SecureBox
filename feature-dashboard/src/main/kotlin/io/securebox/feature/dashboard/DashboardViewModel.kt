package io.securebox.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.securebox.core.domain.model.AppVersion
import io.securebox.core.domain.model.CalculateSecurityScoreUseCase
import io.securebox.core.domain.model.CheckResult
import io.securebox.core.domain.model.DeviceMode
import io.securebox.core.domain.model.SecurityInputs
import io.securebox.core.domain.model.SecurityScore
import io.securebox.core.domain.model.UpdateDecision
import io.securebox.core.domain.repository.SettingsRepository
import io.securebox.core.domain.repository.UpdateRepository
import io.securebox.core.domain.update.UpdateEvaluator
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val score: SecurityScore = SecurityScore.EMPTY,
    val deviceMode: DeviceMode = DeviceMode.PHONE,
    val updateAvailable: Boolean = false,
)

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        settingsRepository: SettingsRepository,
        private val calculateSecurityScore: CalculateSecurityScoreUseCase,
        private val updateRepository: UpdateRepository,
        private val appVersion: AppVersion,
    ) : ViewModel() {
        private val updateAvailable = MutableStateFlow(false)

        init {
            viewModelScope.launch {
                when (val result = updateRepository.fetchLatestRelease(force = false, current = appVersion)) {
                    is CheckResult.Fetched -> {
                        updateAvailable.value =
                            UpdateEvaluator.evaluate(appVersion, result.release) is UpdateDecision.UpdateAvailable
                    }

                    else -> Unit
                }
            }
        }

        val state: StateFlow<DashboardUiState> =
            combine(settingsRepository.settings, updateAvailable) { settings, hasUpdate ->
                DashboardUiState(
                    score =
                        calculateSecurityScore(
                            SecurityInputs(
                                usageAccessGranted = settings.usageAccessGranted,
                                notificationsGranted = settings.notificationsGranted,
                                firewallPrepared = settings.vpnPrepared,
                                appIsUpToDate = !hasUpdate,
                            ),
                        ),
                    deviceMode = settings.deviceMode,
                    updateAvailable = hasUpdate,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DashboardUiState(),
            )
    }
