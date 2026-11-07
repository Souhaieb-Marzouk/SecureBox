package io.securebox.core.domain.network

import kotlinx.coroutines.flow.Flow

/** Per-app traffic: live counters plus historical ranges from NetworkStatsManager. */
public interface TrafficRepository {
    /** Totals since boot for every visible app, read from TrafficStats. */
    public suspend fun liveTotals(): List<AppTraffic>

    /** Totals for [range] (day/week/month) from NetworkStatsManager. */
    public suspend fun historicalUsage(range: TrafficRange): List<AppTraffic>

    /** Upload bytes per package for the hour ending at [nowMillis]. */
    public suspend fun hourlyUploads(nowMillis: Long): Map<String, Long>

    /** 7-day average upload bytes per package, expressed per hour. */
    public suspend fun baselineHourlyUploads(nowMillis: Long): Map<String, Long>

    /** Package name to user-visible label, for every visible app. */
    public suspend fun appLabels(): Map<String, String>
}

/** Local-VPN firewall state and control. */
public interface FirewallRepository {
    public val state: Flow<FirewallState>

    public suspend fun setEnabled(enabled: Boolean)

    public suspend fun setPreset(preset: FirewallPreset)

    public suspend fun setBlocked(
        packageName: String,
        blocked: Boolean,
    )

    public suspend fun blockAll(packages: Set<String>)

    /** Recomputes running/other-VPN/prepared flags. */
    public suspend fun refreshStatus()

    public suspend fun startService()

    public suspend fun stopService()

    /** Called by the VpnService so the UI immediately reflects start/stop. */
    public fun setServiceRunning(running: Boolean)

    /** Apps installed outside the Play Store, used by the BLOCK_UNKNOWN_APPS preset. */
    public suspend fun unknownPackages(): Set<String>
}

/** Listening sockets owned by apps. */
public interface PortRepository {
    public suspend fun listeningPorts(): PortScanResult
}

/** Runs anomaly checks and remembers the most recent findings. */
public interface AnomalyRepository {
    public suspend fun runCheck(): List<AnomalyEvent>

    public suspend fun snapshot(): AnomalySnapshot

    public suspend fun record(
        events: List<AnomalyEvent>,
        checkedAtMillis: Long,
    )
}
