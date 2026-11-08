package io.securebox.core.domain.shield

import kotlinx.coroutines.flow.Flow

/** Cumulative, on-device protection counters. Never uploaded. */
public data class ShieldStats(
    val adsBlocked: Long = 0,
    val trackersBlocked: Long = 0,
    val annoyancesCleaned: Long = 0,
    val maliciousBlocked: Long = 0,
    val dnsBlocked: Long = 0,
) {
    public val total: Long
        get() = adsBlocked + trackersBlocked + annoyancesCleaned + maliciousBlocked + dnsBlocked
}

public interface ShieldStatsRepository {
    public val stats: Flow<ShieldStats>

    public suspend fun add(
        ads: Long = 0,
        trackers: Long = 0,
        annoyances: Long = 0,
        malicious: Long = 0,
        dns: Long = 0,
    )

    public suspend fun reset()
}
