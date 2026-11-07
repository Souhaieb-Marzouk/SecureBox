package io.securebox.core.domain.security

/** A verified threat-intel feed. Only feeds whose detached Ed25519 signature checked out are stored. */
public data class ThreatFeed(
    val version: Int,
    val generatedAt: String,
    val rules: List<SignatureRule>,
    val trackerPrefixes: List<String>,
    val domains: List<String>,
)

public data class ThreatFeedStatus(
    val version: Int,
    val generatedAt: String,
    val ruleCount: Int,
    val trackerCount: Int,
    val domainCount: Int,
    val installedAtMillis: Long,
    val lastCheckedAtMillis: Long?,
    val signatureValid: Boolean,
)

public sealed interface FeedUpdateResult {
    public data class Updated(val status: ThreatFeedStatus) : FeedUpdateResult

    public data object UpToDate : FeedUpdateResult

    /** No feed URL is configured in this build. */
    public data object NotConfigured : FeedUpdateResult

    /** The payload or its signature failed verification; nothing was stored. */
    public data object InvalidSignature : FeedUpdateResult

    public data class Failed(val reason: String) : FeedUpdateResult
}

/**
 * Downloads, verifies and stores the signed threat feed.
 *
 * Implementations must reject any feed whose signature does not match the embedded public key,
 * and must store the payload atomically so a partial download never becomes active.
 */
public interface ThreatFeedRepository {
    public suspend fun status(): ThreatFeedStatus?

    public suspend fun activeFeed(): ThreatFeed?

    public suspend fun update(force: Boolean = false): FeedUpdateResult
}
