package io.securebox.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.securebox.core.domain.model.AppSettings
import io.securebox.core.domain.model.DnsPreset
import io.securebox.core.domain.model.TrackingProtectionMode
import io.securebox.core.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShieldUiState(
    val settings: AppSettings = AppSettings(),
    val privateDnsHost: String? = null,
    val dnsTest: DohResult? = null,
    val dnsTesting: Boolean = false,
    val dnsError: String? = null,
)

@HiltViewModel
class ShieldViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ShieldUiState())
        val state: StateFlow<ShieldUiState> = _state.asStateFlow()

        init {
            viewModelScope.launch {
                settingsRepository.settings.collect { settings ->
                    _state.update { it.copy(settings = settings) }
                }
            }
            refreshPrivateDns()
        }

        fun refreshPrivateDns() {
            _state.update { it.copy(privateDnsHost = currentPrivateDnsHost(context)) }
        }

        fun setTrackingProtection(mode: TrackingProtectionMode) {
            viewModelScope.launch { settingsRepository.setTrackingProtection(mode) }
        }

        fun setBlockAdsAndTrackers(enabled: Boolean) {
            viewModelScope.launch { settingsRepository.setBlockAdsAndTrackers(enabled) }
        }

        fun setHideAnnoyances(enabled: Boolean) {
            viewModelScope.launch { settingsRepository.setHideAnnoyances(enabled) }
        }

        fun setSafeBrowsing(enabled: Boolean) {
            viewModelScope.launch { settingsRepository.setSafeBrowsing(enabled) }
        }

        fun setDnsPreset(preset: DnsPreset) {
            viewModelScope.launch { settingsRepository.setDnsPreset(preset) }
        }

        fun testDoh() {
            val preset = dnsPresetInfo(_state.value.settings.dnsPreset)
            if (_state.value.dnsTesting) return
            _state.update { it.copy(dnsTesting = true, dnsError = null, dnsTest = null) }
            viewModelScope.launch {
                DohClient
                    .test(preset.dohUrl)
                    .onSuccess { result ->
                        _state.update { it.copy(dnsTesting = false, dnsTest = result) }
                    }.onFailure { error ->
                        _state.update {
                            it.copy(
                                dnsTesting = false,
                                dnsError = error.message ?: "DNS request failed",
                            )
                        }
                    }
            }
        }
    }
