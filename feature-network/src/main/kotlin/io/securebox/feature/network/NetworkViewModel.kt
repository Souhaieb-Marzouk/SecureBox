package io.securebox.feature.network

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import io.securebox.core.domain.network.AnomalyRepository
import io.securebox.core.domain.network.AnomalySnapshot
import io.securebox.core.domain.network.AppTraffic
import io.securebox.core.domain.network.FirewallPreset
import io.securebox.core.domain.network.FirewallRepository
import io.securebox.core.domain.network.FirewallState
import io.securebox.core.domain.network.PortRepository
import io.securebox.core.domain.network.PortScanResult
import io.securebox.core.domain.network.TrafficRange
import io.securebox.core.domain.network.TrafficRepository
import io.securebox.core.domain.permission.PlatformPermissions
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class NetworkError {
    VPN_UNAVAILABLE,
    SERVICE_FAILED,
}

data class AppRow(
    val packageName: String,
    val label: String,
    val uid: Int,
    val rxBytes: Long,
    val txBytes: Long,
    val rateBytesPerSecond: Long,
    val blocked: Boolean,
    val isSystem: Boolean,
    val isSelf: Boolean,
) {
    val totalBytes: Long get() = rxBytes + txBytes
}

data class NetworkUiState(
    val loading: Boolean = true,
    val usageAccessGranted: Boolean = false,
    val range: TrafficRange = TrafficRange.LIVE,
    val apps: List<AppRow> = emptyList(),
    val firewall: FirewallState = FirewallState(),
    val ports: PortScanResult = PortScanResult(available = true, ports = emptyList()),
    val anomaly: AnomalySnapshot = AnomalySnapshot(),
    val error: NetworkError? = null,
)

@HiltViewModel
class NetworkViewModel
    @Inject
    constructor(
        private val trafficRepository: TrafficRepository,
        private val firewallRepository: FirewallRepository,
        private val portRepository: PortRepository,
        private val anomalyRepository: AnomalyRepository,
        private val permissions: PlatformPermissions,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val _state = MutableStateFlow(NetworkUiState())
        val state: StateFlow<NetworkUiState> = _state.asStateFlow()

        private var lastTotals: Map<Int, AppTraffic> = emptyMap()
        private var lastSampleElapsed: Long = 0L
        private var firewallState: FirewallState = FirewallState()

        init {
            viewModelScope.launch {
                firewallRepository.state.collect { firewall ->
                    firewallState = firewall
                    _state.update { current ->
                        current.copy(
                            firewall = firewall,
                            apps = current.apps.map { it.copy(blocked = firewall.blockedPackages.contains(it.packageName)) },
                        )
                    }
                }
            }
            refresh()
            viewModelScope.launch {
                while (isActive) {
                    if (_state.value.range == TrafficRange.LIVE) loadLive()
                    delay(REFRESH_INTERVAL_MILLIS)
                }
            }
        }

        fun refresh() {
            viewModelScope.launch {
                val usageAccess = permissions.hasUsageAccess()
                _state.update { it.copy(usageAccessGranted = usageAccess, loading = false) }
                firewallRepository.refreshStatus()
                runCatching { anomalyRepository.snapshot() }.onSuccess { snapshot ->
                    _state.update { it.copy(anomaly = snapshot) }
                }
                runCatching { portRepository.listeningPorts() }.onSuccess { ports ->
                    _state.update { it.copy(ports = ports) }
                }
                if (usageAccess) loadRange() else _state.update { it.copy(apps = emptyList()) }
            }
        }

        fun setRange(range: TrafficRange) {
            if (_state.value.range == range) return
            _state.update { it.copy(range = range, apps = emptyList()) }
            if (range != TrafficRange.LIVE) {
                viewModelScope.launch { loadHistory(range) }
            }
        }

        fun setFirewallEnabled(enabled: Boolean) {
            viewModelScope.launch {
                runCatching { firewallRepository.setEnabled(enabled) }
                    .onFailure { _state.update { current -> current.copy(error = NetworkError.SERVICE_FAILED) } }
                firewallRepository.refreshStatus()
            }
        }

        fun setPreset(preset: FirewallPreset) {
            viewModelScope.launch {
                firewallRepository.setPreset(preset)
                firewallRepository.refreshStatus()
            }
        }

        fun toggleBlock(
            packageName: String,
            blocked: Boolean,
        ) {
            if (packageName == context.packageName) return
            viewModelScope.launch {
                firewallRepository.setBlocked(packageName, blocked)
            }
        }

        fun blockUnknownApps() {
            viewModelScope.launch {
                firewallRepository.setPreset(FirewallPreset.BLOCK_UNKNOWN_APPS)
                firewallRepository.setEnabled(true)
            }
        }

        fun onVpnPermissionDenied() {
            _state.update { it.copy(error = NetworkError.VPN_UNAVAILABLE) }
        }

        fun clearError() {
            _state.update { it.copy(error = null) }
        }

        private suspend fun loadRange() {
            when (_state.value.range) {
                TrafficRange.LIVE -> loadLive()
                TrafficRange.DAY -> loadHistory(TrafficRange.DAY)
                TrafficRange.WEEK -> loadHistory(TrafficRange.WEEK)
                TrafficRange.MONTH -> loadHistory(TrafficRange.MONTH)
            }
        }

        private suspend fun loadLive() {
            if (!permissions.hasUsageAccess()) return
            val totals = runCatching { trafficRepository.liveTotals() }.getOrDefault(emptyList())
            val elapsed = SystemClock.elapsedRealtime()
            val deltaMillis = (elapsed - lastSampleElapsed).coerceAtLeast(1L)
            val previous = lastTotals

            val rows =
                totals
                    .map { traffic ->
                        val old = previous[traffic.uid]
                        val deltaBytes =
                            if (old == null) {
                                0L
                            } else {
                                (traffic.rxBytes - old.rxBytes).coerceAtLeast(0L) +
                                    (traffic.txBytes - old.txBytes).coerceAtLeast(0L)
                            }
                        traffic.toRow(
                            rateBytesPerSecond = deltaBytes * 1000L / deltaMillis,
                            blocked = firewallState.blockedPackages.contains(traffic.packageName),
                        )
                    }.filter { it.totalBytes > 0 || it.rateBytesPerSecond > 0 }
                    .sortedWith(compareByDescending<AppRow> { it.rateBytesPerSecond }.thenByDescending { it.totalBytes })
                    .take(MAX_ROWS)

            lastTotals = totals.associateBy { it.uid }
            lastSampleElapsed = elapsed
            _state.update { it.copy(apps = rows) }
        }

        private suspend fun loadHistory(range: TrafficRange) {
            if (!permissions.hasUsageAccess()) return
            val totals = runCatching { trafficRepository.historicalUsage(range) }.getOrDefault(emptyList())
            val rows =
                totals
                    .map { it.toRow(rateBytesPerSecond = 0L, blocked = firewallState.blockedPackages.contains(it.packageName)) }
                    .sortedByDescending { it.totalBytes }
                    .take(MAX_ROWS)
            _state.update { it.copy(apps = rows) }
        }

        private fun AppTraffic.toRow(
            rateBytesPerSecond: Long,
            blocked: Boolean,
        ) = AppRow(
            packageName = packageName,
            label = label,
            uid = uid,
            rxBytes = rxBytes,
            txBytes = txBytes,
            rateBytesPerSecond = rateBytesPerSecond,
            blocked = blocked,
            isSystem = isSystem,
            isSelf = packageName == context.packageName,
        )

        private companion object {
            const val REFRESH_INTERVAL_MILLIS: Long = 2_000L
            const val MAX_ROWS: Int = 40
        }
    }
