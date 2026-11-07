package io.securebox.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.securebox.core.domain.device.DeviceModeDetector
import io.securebox.core.domain.model.AppLanguage
import io.securebox.core.domain.model.AppSettings
import io.securebox.core.domain.model.AppVersion
import io.securebox.core.domain.model.DeviceMode
import io.securebox.core.domain.model.ThemeMode
import io.securebox.core.domain.repository.SettingsRepository
import io.securebox.core.domain.security.IntegrityReport
import io.securebox.core.domain.security.IntegrityRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val installedVersion: String = "",
    val detectionMessage: DeviceMode? = null,
    val integrity: IntegrityReport? = null,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val deviceModeDetector: DeviceModeDetector,
        private val integrityRepository: IntegrityRepository,
        appVersion: AppVersion,
    ) : ViewModel() {
        private val _state = MutableStateFlow(SettingsUiState(installedVersion = appVersion.versionName))
        val state: StateFlow<SettingsUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                settingsRepository.settings.collect { settings ->
                    _state.update { it.copy(settings = settings) }
                }
            }
            refreshIntegrity()
        }

        fun refreshIntegrity() {
            viewModelScope.launch {
                runCatching { integrityRepository.check() }.onSuccess { report ->
                    _state.update { it.copy(integrity = report) }
                }
            }
        }

        fun setLanguage(language: AppLanguage) = update { settingsRepository.setLanguage(language) }

        fun setTheme(themeMode: ThemeMode) = update { settingsRepository.setThemeMode(themeMode) }

        fun setDeviceMode(mode: DeviceMode) = update { settingsRepository.setDeviceMode(mode, confirmedByUser = true) }

        fun detectAgain() {
            val tv = deviceModeDetector.isTelevision()
            val mode = if (tv) DeviceMode.TV else DeviceMode.PHONE
            _state.update { it.copy(detectionMessage = mode) }
            update { settingsRepository.setDeviceMode(mode, confirmedByUser = true) }
        }

        fun setThreatIntelEnabled(enabled: Boolean) = update { settingsRepository.setThreatIntelEnabled(enabled) }

        private fun update(action: suspend () -> Unit) {
            viewModelScope.launch { action() }
        }
    }
