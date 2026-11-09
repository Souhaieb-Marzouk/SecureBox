package io.securebox.core.domain.model

/**
 * The two UI shells SecureBox can present. Detection happens once during onboarding and can be
 * overridden later in Settings, because some TV boxes do not advertise Leanback support.
 */
public enum class DeviceMode {
    PHONE,
    TV,
}

/** Languages supported by the app. The tag is the BCP-47 language code used by Android. */
public enum class AppLanguage(
    public val tag: String,
) {
    ENGLISH("en"),
    FRENCH("fr"),
    ;

    public companion object {
        public fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag.equals(tag?.trim(), ignoreCase = true) } ?: ENGLISH
    }
}

/** Theme preference. Dark is the default because SecureBox is used at night on TV boxes too. */
public enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM,
}

/** How aggressively the private browser blocks tracking. */
public enum class TrackingProtectionMode {
    STANDARD,
    HIGH,
}

/** DNS preset offered by the Privacy Shield; the user applies it in Android Private DNS. */
public enum class DnsPreset(
    public val dohUrl: String,
    public val dotHost: String,
) {
    CLOUDFLARE_FAMILIES(
        dohUrl = "https://family.cloudflare-dns.com/dns-query",
        dotHost = "family.cloudflare-dns.com",
    ),
    ADGUARD(
        dohUrl = "https://dns.adguard-dns.com/dns-query",
        dotHost = "dns.adguard-dns.com",
    ),
    QUAD9(
        dohUrl = "https://dns.quad9.net/dns-query",
        dotHost = "dns.quad9.net",
    ),
}

/** Global firewall presets for the local VPN (implemented in Milestone 2). */
public enum class FirewallPreset {
    ALLOW_ALL,
    BLOCK_UNKNOWN_APPS,
}

/**
 * Everything the user can configure, persisted in DataStore.
 *
 * A single immutable snapshot keeps the two shells and the settings screens consistent: any
 * change flows through one source of truth.
 */
public data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val deviceMode: DeviceMode = DeviceMode.PHONE,
    val deviceModeConfirmedByUser: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val firewallEnabled: Boolean = false,
    val firewallPreset: FirewallPreset = FirewallPreset.ALLOW_ALL,
    val threatIntelEnabled: Boolean = true,
    val threatIntelAutoUpdate: Boolean = true,
    val scheduledScansEnabled: Boolean = true,
    val updateCheckIntervalHours: Long = DEFAULT_UPDATE_INTERVAL_HOURS,
    val usageAccessGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val vpnPrepared: Boolean = false,
    val notificationsOptOut: Boolean = false,
    val trackingProtection: TrackingProtectionMode = TrackingProtectionMode.STANDARD,
    val blockAdsAndTrackers: Boolean = true,
    val hideAnnoyances: Boolean = true,
    val safeBrowsing: Boolean = true,
    val dnsPreset: DnsPreset = DnsPreset.ADGUARD,
    /** True when the combined all-app DNS filter is active alongside the firewall. */
    val dnsShieldEnabled: Boolean = false,
    /** Apps explicitly kept outside the SecureBox VPN tunnel (video players that crash under it). */
    val vpnBypassPackages: Set<String> = emptySet(),
) {
    public companion object {
        public const val DEFAULT_UPDATE_INTERVAL_HOURS: Long = 12L
    }
}

/** The installed build, used to compare against GitHub release tags. */
public data class AppVersion(
    val versionName: String,
    val versionCode: Int,
)
