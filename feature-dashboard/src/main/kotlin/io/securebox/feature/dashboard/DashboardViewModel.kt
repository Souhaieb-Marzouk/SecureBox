package io.securebox.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.securebox.core.domain.model.CalculateSecurityScoreUseCase
import io.securebox.core.domain.model.DeviceMode
import io.securebox.core.domain.model.SecurityInputs
import io.securebox.core.domain.model.SecurityScore
import io.securebox.core.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DashboardUiState(
    val score: SecurityScore = SecurityScore.EMPTY,
    val deviceMode: DeviceMode = DeviceMode.PHONE,
)

@HiltViewModel
class DashboardViewModel
    @Inject
    constructor(
        settingsRepository: SettingsRepository,
        private val calculateSecurityScore: CalculateSecurityScoreUseCase,
    ) : ViewModel() {
        val state: StateFlow<DashboardUiState> =
            settingsRepository.settings
                .map { settings ->
                    DashboardUiState(
                        score =
                            calculateSecurityScore(
                                SecurityInputs(
                                    usageAccessGranted = settings.usageAccessGranted,
                                    notificationsGranted = settings.notificationsGranted,
                                    firewallPrepared = settings.vpnPrepared,
                                    // The updater marks this false through the shared score once an
                                    // update is known; a signed, current build starts as true.
                                    appIsUpToDate = true,
                                ),
                            ),
                        deviceMode = settings.deviceMode,
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = DashboardUiState(),
                )
    }
