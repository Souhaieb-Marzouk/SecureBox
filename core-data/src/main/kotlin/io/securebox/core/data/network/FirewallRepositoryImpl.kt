package io.securebox.core.data.network

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.securebox.core.domain.network.FirewallPreset
import io.securebox.core.domain.network.FirewallRepository
import io.securebox.core.domain.network.FirewallState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

@Singleton
class FirewallRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dataStore: DataStore<Preferences>,
        private val registry: AppRegistry,
    ) : FirewallRepository {
        private data class Runtime(
            val running: Boolean = false,
            val otherVpnActive: Boolean = false,
            val vpnPrepared: Boolean = false,
        )

        private val runtime = MutableStateFlow(Runtime())
        private val unknownPackages = MutableStateFlow<Set<String>>(emptySet())
        @Volatile private var ownVpnStoppedAt = 0L

        override val state: Flow<FirewallState> =
            combine(dataStore.data, runtime, unknownPackages) { preferences, runtime, unknown ->
                val preset = enumOrDefault(preferences[Keys.PRESET], FirewallPreset.ALLOW_ALL)
                val explicit = preferences[Keys.BLOCKED] ?: emptySet()
                val effective = if (preset == FirewallPreset.BLOCK_UNKNOWN_APPS) explicit + unknown else explicit
                FirewallState(
                    enabled = preferences[Keys.ENABLED] ?: false,
                    preset = preset,
                    blockedPackages = explicit,
                    effectiveBlockedPackages = effective,
                    running = runtime.running,
                    otherVpnActive = runtime.otherVpnActive,
                    vpnPrepared = runtime.vpnPrepared,
                )
            }.distinctUntilChanged()

        override suspend fun setEnabled(enabled: Boolean) {
            dataStore.edit { it[Keys.ENABLED] = enabled }
            if (!enabled) {
                val dnsEnabled = dataStore.data.first()[Keys.DNS_SHIELD_ENABLED] ?: false
                if (dnsEnabled) {
                    refreshStatus()
                } else {
                    stopService()
                }
            } else {
                refreshStatus()
                startService()
            }
        }

        override suspend fun setPreset(preset: FirewallPreset) {
            dataStore.edit { it[Keys.PRESET] = preset.name }
            if (preset == FirewallPreset.BLOCK_UNKNOWN_APPS) {
                unknownPackages.value = registry.unknownPackages()
            }
            // The running service observes the state flow and rebuilds its tunnel automatically.
            refreshStatus()
        }

        override suspend fun setBlocked(
            packageName: String,
            blocked: Boolean,
        ) {
            dataStore.edit { preferences ->
                val current = preferences[Keys.BLOCKED]?.toMutableSet() ?: mutableSetOf()
                if (blocked) current.add(packageName) else current.remove(packageName)
                preferences[Keys.BLOCKED] = current
            }
        }

        override suspend fun blockAll(packages: Set<String>) {
            if (packages.isEmpty()) return
            dataStore.edit { preferences ->
                val current = preferences[Keys.BLOCKED]?.toMutableSet() ?: mutableSetOf()
                current.addAll(packages)
                preferences[Keys.BLOCKED] = current
            }
        }

        override suspend fun refreshStatus() {
            val preferences = dataStore.data.first()
            val preset = enumOrDefault(preferences[Keys.PRESET], FirewallPreset.ALLOW_ALL)
            if (preset == FirewallPreset.BLOCK_UNKNOWN_APPS) {
                unknownPackages.value = registry.unknownPackages()
            }
            val running = SecureBoxVpnService.isRunning
            runtime.value =
                Runtime(
                    running = running,
                    otherVpnActive = !running && hasForeignVpn(),
                    vpnPrepared = VpnService.prepare(context) == null,
                )
        }

        override suspend fun startService() {
            val serviceIntent = Intent(context, SecureBoxVpnService::class.java)
            runCatching { ContextCompat.startForegroundService(context, serviceIntent) }
            runtime.update { it.copy(running = SecureBoxVpnService.isRunning) }
        }

        override suspend fun stopService() {
            ownVpnStoppedAt = SystemClock.elapsedRealtime()
            context.stopService(Intent(context, SecureBoxVpnService::class.java))
            SecureBoxVpnService.isRunning = false
            runtime.update { it.copy(running = false) }
        }

        override suspend fun unknownPackages(): Set<String> = registry.unknownPackages()

        override fun setServiceRunning(running: Boolean) {
            if (!running) ownVpnStoppedAt = SystemClock.elapsedRealtime()
            runtime.update { it.copy(running = running, otherVpnActive = if (running) false else it.otherVpnActive) }
        }

        private fun hasForeignVpn(): Boolean {
            // The kernel/ConnectivityService can keep the old VPN network visible for a moment after
            // stopService(). Do not report SecureBox as a foreign VPN while that teardown completes.
            if (SystemClock.elapsedRealtime() - ownVpnStoppedAt < 5_000) return false
            val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return false
            return connectivity.allNetworks.any { network ->
                connectivity.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            }
        }

        private inline fun <reified T : Enum<T>> enumOrDefault(
            raw: String?,
            default: T,
        ): T = raw?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default

        private object Keys {
            val ENABLED = booleanPreferencesKey("firewall_enabled")
            val PRESET = stringPreferencesKey("firewall_preset")
            val BLOCKED = stringSetPreferencesKey("firewall_blocked_packages")
            val DNS_SHIELD_ENABLED = booleanPreferencesKey("dns_shield_enabled")
        }
    }
