package io.securebox.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.securebox.core.domain.model.AppLanguage
import io.securebox.core.domain.model.AppSettings
import io.securebox.core.domain.model.DeviceMode
import io.securebox.core.domain.model.FirewallPreset
import io.securebox.core.domain.model.ThemeMode
import io.securebox.core.domain.repository.SettingsRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal val Context.secureBoxDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "securebox_settings",
)

@Singleton
class SettingsRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : SettingsRepository {
        override val settings: Flow<AppSettings> =
            dataStore.data
                .catch { error ->
                    if (error is IOException) emit(emptyPreferences()) else throw error
                }.map { preferences -> preferences.toSettings() }

        override suspend fun setLanguage(language: AppLanguage) {
            dataStore.edit { it[Keys.LANGUAGE] = language.tag }
        }

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            dataStore.edit { it[Keys.THEME] = themeMode.name }
        }

        override suspend fun setDeviceMode(
            mode: DeviceMode,
            confirmedByUser: Boolean,
        ) {
            dataStore.edit {
                it[Keys.DEVICE_MODE] = mode.name
                it[Keys.DEVICE_MODE_CONFIRMED] = confirmedByUser
            }
        }

        override suspend fun completeOnboarding() {
            dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = true }
        }

        override suspend fun setFirewallEnabled(enabled: Boolean) {
            dataStore.edit { it[Keys.FIREWALL_ENABLED] = enabled }
        }

        override suspend fun setFirewallPreset(preset: FirewallPreset) {
            dataStore.edit { it[Keys.FIREWALL_PRESET] = preset.name }
        }

        override suspend fun setThreatIntelEnabled(enabled: Boolean) {
            dataStore.edit { it[Keys.THREAT_INTEL_ENABLED] = enabled }
        }

        override suspend fun setThreatIntelAutoUpdate(enabled: Boolean) {
            dataStore.edit { it[Keys.THREAT_INTEL_AUTO_UPDATE] = enabled }
        }

        override suspend fun setScheduledScansEnabled(enabled: Boolean) {
            dataStore.edit { it[Keys.SCHEDULED_SCANS] = enabled }
        }

        override suspend fun recordPermissionSnapshot(
            usageAccessGranted: Boolean,
            notificationsGranted: Boolean,
            vpnPrepared: Boolean,
        ) {
            dataStore.edit {
                it[Keys.USAGE_ACCESS] = usageAccessGranted
                it[Keys.NOTIFICATIONS] = notificationsGranted
                it[Keys.VPN_PREPARED] = vpnPrepared
            }
        }

        private fun Preferences.toSettings(): AppSettings =
            AppSettings(
                onboardingCompleted = this[Keys.ONBOARDING_COMPLETED] ?: false,
                language = AppLanguage.fromTag(this[Keys.LANGUAGE]),
                deviceMode = enumOrDefault(this[Keys.DEVICE_MODE], DeviceMode.PHONE),
                deviceModeConfirmedByUser = this[Keys.DEVICE_MODE_CONFIRMED] ?: false,
                themeMode = enumOrDefault(this[Keys.THEME], ThemeMode.DARK),
                firewallEnabled = this[Keys.FIREWALL_ENABLED] ?: false,
                firewallPreset = enumOrDefault(this[Keys.FIREWALL_PRESET], FirewallPreset.ALLOW_ALL),
                threatIntelEnabled = this[Keys.THREAT_INTEL_ENABLED] ?: true,
                threatIntelAutoUpdate = this[Keys.THREAT_INTEL_AUTO_UPDATE] ?: true,
                scheduledScansEnabled = this[Keys.SCHEDULED_SCANS] ?: true,
                updateCheckIntervalHours = this[Keys.UPDATE_INTERVAL] ?: AppSettings.DEFAULT_UPDATE_INTERVAL_HOURS,
                usageAccessGranted = this[Keys.USAGE_ACCESS] ?: false,
                notificationsGranted = this[Keys.NOTIFICATIONS] ?: false,
                vpnPrepared = this[Keys.VPN_PREPARED] ?: false,
            )

        private inline fun <reified T : Enum<T>> enumOrDefault(
            raw: String?,
            default: T,
        ): T = raw?.let { value -> enumValues<T>().firstOrNull { it.name == value } } ?: default

        private object Keys {
            val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
            val LANGUAGE = stringPreferencesKey("language")
            val DEVICE_MODE = stringPreferencesKey("device_mode")
            val DEVICE_MODE_CONFIRMED = booleanPreferencesKey("device_mode_confirmed")
            val THEME = stringPreferencesKey("theme_mode")
            val FIREWALL_ENABLED = booleanPreferencesKey("firewall_enabled")
            val FIREWALL_PRESET = stringPreferencesKey("firewall_preset")
            val THREAT_INTEL_ENABLED = booleanPreferencesKey("threat_intel_enabled")
            val THREAT_INTEL_AUTO_UPDATE = booleanPreferencesKey("threat_intel_auto_update")
            val SCHEDULED_SCANS = booleanPreferencesKey("scheduled_scans_enabled")
            val UPDATE_INTERVAL = longPreferencesKey("update_check_interval_hours")
            val USAGE_ACCESS = booleanPreferencesKey("usage_access_granted")
            val NOTIFICATIONS = booleanPreferencesKey("notifications_granted")
            val VPN_PREPARED = booleanPreferencesKey("vpn_prepared")

            // Update cache shared with the update repository.
            val LAST_UPDATE_CHECK = longPreferencesKey("update_last_checked_at")
            val CACHED_RELEASE = stringPreferencesKey("update_cached_release")
        }
    }
