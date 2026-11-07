package io.securebox.feature.threats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.securebox.core.domain.repository.SettingsRepository
import io.securebox.core.domain.security.AppRiskReport
import io.securebox.core.domain.security.ScanSummary
import io.securebox.core.domain.security.SecurityRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ThreatsUiState(
    val loading: Boolean = true,
    val scanning: Boolean = false,
    val analyzed: Int = 0,
    val total: Int = 0,
    val summary: ScanSummary? = null,
    val reports: List<AppRiskReport> = emptyList(),
    val scheduled: Boolean = true,
    val rulesVersion: Int = 0,
)

@HiltViewModel
class ThreatsViewModel
    @Inject
    constructor(
        private val securityRepository: SecurityRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ThreatsUiState())
        val state: StateFlow<ThreatsUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                settingsRepository.settings.collect { settings ->
                    _state.update { it.copy(scheduled = settings.scheduledScansEnabled) }
                }
            }
            load()
        }

        fun load() {
            viewModelScope.launch {
                val summary = runCatching { securityRepository.lastScan() }.getOrNull()
                val reports = runCatching { securityRepository.storedReports() }.getOrDefault(emptyList())
                val rules = runCatching { securityRepository.rulesVersion() }.getOrDefault(0)
                _state.update {
                    it.copy(
                        loading = false,
                        summary = summary,
                        reports = reports.sortedBy { report -> report.score },
                        rulesVersion = rules,
                    )
                }
            }
        }

        fun scanNow() {
            if (_state.value.scanning) return
            viewModelScope.launch {
                _state.update { it.copy(scanning = true, analyzed = 0, total = 0) }
                val summary =
                    runCatching {
                        securityRepository.scanAll { done, total ->
                            _state.update { current -> current.copy(analyzed = done, total = total) }
                        }
                    }.getOrNull()
                val reports = runCatching { securityRepository.storedReports() }.getOrDefault(emptyList())
                _state.update {
                    it.copy(
                        scanning = false,
                        summary = summary ?: it.summary,
                        reports = reports.sortedBy { report -> report.score },
                    )
                }
            }
        }

        fun setScheduled(enabled: Boolean) {
            viewModelScope.launch {
                settingsRepository.setScheduledScansEnabled(enabled)
            }
        }
    }
