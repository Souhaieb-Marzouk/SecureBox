package io.securebox.core.domain.network

/** Per-app traffic totals. [rxBytes] is download, [txBytes] is upload. */
public data class AppTraffic(
    val packageName: String,
    val label: String,
    val uid: Int,
    val rxBytes: Long,
    val txBytes: Long,
    val isSystem: Boolean,
) {
    public val totalBytes: Long get() = rxBytes + txBytes
}

/** Windows offered by the bandwidth screen. */
public enum class TrafficRange {
    LIVE,
    DAY,
    WEEK,
    MONTH,
}

/** A socket listening on the device, attributed to an app where Android allows it. */
public data class ListeningPort(
    val packageName: String?,
    val label: String,
    val uid: Int,
    val port: Int,
    val protocol: Protocol,
    val exposed: Boolean,
) {
    public enum class Protocol {
        TCP,
        UDP,
    }
}

public data class PortScanResult(
    /** False when the OS denies access to /proc/net (common on recent Android releases). */
    val available: Boolean,
    val ports: List<ListeningPort>,
)

/** Global firewall modes. */
public enum class FirewallPreset {
    ALLOW_ALL,
    BLOCK_UNKNOWN_APPS,
}

public data class FirewallState(
    val enabled: Boolean = false,
    val preset: FirewallPreset = FirewallPreset.ALLOW_ALL,
    /** Apps the user explicitly blocked. */
    val blockedPackages: Set<String> = emptySet(),
    /** Apps actually routed into the local VPN (explicit + preset-derived). */
    val effectiveBlockedPackages: Set<String> = emptySet(),
    val running: Boolean = false,
    val otherVpnActive: Boolean = false,
    val vpnPrepared: Boolean = false,
) {
    public val effectiveBlockedCount: Int get() = effectiveBlockedPackages.size
}

public enum class AnomalyKind {
    UPLOAD_SPIKE,
    HARD_THRESHOLD,
}

/** A single suspicious-upload finding, ready for a notification or the dashboard. */
public data class AnomalyEvent(
    val packageName: String,
    val label: String,
    val kind: AnomalyKind,
    val observedBytes: Long,
    val baselineBytesPerHour: Long,
    val atMillis: Long,
)

/** Recent anomaly state persisted for the UI. */
public data class AnomalySnapshot(
    val lastCheckedAtMillis: Long? = null,
    val events: List<AnomalyEvent> = emptyList(),
)
