package io.securebox.core.domain.repository

import io.securebox.core.domain.model.AppLanguage
import io.securebox.core.domain.model.AppSettings
import io.securebox.core.domain.model.DeviceMode
import io.securebox.core.domain.model.DnsPreset
import io.securebox.core.domain.model.FirewallPreset
import io.securebox.core.domain.model.ThemeMode
import io.securebox.core.domain.model.TrackingProtectionMode
import kotlinx.coroutines.flow.Flow

/** Single source of truth for user configuration. Persisted in DataStore, never uploaded. */
public interface SettingsRepository {
    public val settings: Flow<AppSettings>

    public suspend fun setLanguage(language: AppLanguage)

    public suspend fun setThemeMode(themeMode: ThemeMode)

    public suspend fun setDeviceMode(
        mode: DeviceMode,
        confirmedByUser: Boolean = true,
    )

    public suspend fun completeOnboarding()

    public suspend fun setFirewallEnabled(enabled: Boolean)

    public suspend fun setFirewallPreset(preset: FirewallPreset)

    public suspend fun setThreatIntelEnabled(enabled: Boolean)

    public suspend fun setThreatIntelAutoUpdate(enabled: Boolean)

    public suspend fun setScheduledScansEnabled(enabled: Boolean)

    public suspend fun setTrackingProtection(mode: TrackingProtectionMode)

    public suspend fun setBlockAdsAndTrackers(enabled: Boolean)

    public suspend fun setHideAnnoyances(enabled: Boolean)

    public suspend fun setSafeBrowsing(enabled: Boolean)

    public suspend fun setDnsPreset(preset: DnsPreset)

    public suspend fun setDnsShieldEnabled(enabled: Boolean)

    public suspend fun recordPermissionSnapshot(
        usageAccessGranted: Boolean,
        notificationsGranted: Boolean,
        vpnPrepared: Boolean,
    )
}
