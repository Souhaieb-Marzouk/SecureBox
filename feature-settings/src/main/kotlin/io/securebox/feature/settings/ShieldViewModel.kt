package io.securebox.feature.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.securebox.core.domain.model.AppSettings
import io.securebox.core.domain.model.DnsPreset
import io.securebox.core.domain.model.TrackingProtectionMode
import io.securebox.core.domain.repository.SettingsRepository
import io.securebox.core.domain.shield.ShieldStats
import io.securebox.core.domain.shield.ShieldStatsRepository
import io.securebox.core.data.network.DnsShieldVpnService
import io.securebox.core.data.network.SecureBoxVpnService
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
    val stats: ShieldStats = ShieldStats(),
    val dnsShieldRunning: Boolean = false,
    val firewallRunning: Boolean = false,
)

@HiltViewModel
class ShieldViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val statsRepository: ShieldStatsRepository,
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
            viewModelScope.launch {
                statsRepository.stats.collect { stats ->
                    _state.update { it.copy(stats = stats) }
                }
            }
            refreshPrivateDns()
            refreshDnsShield()
        }

        fun refreshDnsShield() {
            _state.update {
                it.copy(
                    dnsShieldRunning = DnsShieldVpnService.isRunning,
                    firewallRunning = SecureBoxVpnService.isRunning,
                )
            }
        }

        fun startDnsShield() {
            context.startForegroundService(Intent(context, DnsShieldVpnService::class.java))
            _state.update { it.copy(dnsShieldRunning = true) }
        }

        fun stopDnsShield() {
            context.startService(
                Intent(context, DnsShieldVpnService::class.java).setAction(DnsShieldVpnService.ACTION_STOP),
            )
            _state.update { it.copy(dnsShieldRunning = false) }
        }

        fun recordBrowserSession(
            ads: Long,
            trackers: Long,
            annoyances: Long,
            malicious: Long,
        ) {
            viewModelScope.launch {
                statsRepository.add(
                    ads = ads,
                    trackers = trackers,
                    annoyances = annoyances,
                    malicious = malicious,
                )
            }
        }

        fun resetStats() {
            viewModelScope.launch { statsRepository.reset() }
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
