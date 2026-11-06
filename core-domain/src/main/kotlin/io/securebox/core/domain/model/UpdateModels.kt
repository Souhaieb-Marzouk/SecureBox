package io.securebox.core.domain.model

import io.securebox.core.domain.version.SemVer
import java.io.File

/** One downloadable file attached to a GitHub release. */
public data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
)

/**
 * A published GitHub release, already mapped from the API payload.
 *
 * [version] is null when the tag does not follow semantic versioning; the updater then refuses to
 * install it rather than guessing.
 */
public data class ReleaseInfo(
    val tagName: String,
    val title: String,
    val notes: String,
    val publishedAt: String,
    val version: SemVer?,
    val versionCode: Int?,
    val prerelease: Boolean,
    val assets: List<ReleaseAsset>,
) {
    public fun asset(name: String): ReleaseAsset? = assets.firstOrNull { it.name == name }

    public fun findChecksumAsset(): ReleaseAsset? = asset(SHA256SUMS_NAME)

    public companion object {
        public const val SHA256SUMS_NAME: String = "SHA256SUMS"
        public const val UNIVERSAL_ASSET_NAME: String = "securebox-universal-release.apk"
    }
}

/** Result of asking GitHub for the latest release. */
public sealed interface CheckResult {
    public data class Fetched(
        val release: ReleaseInfo,
        val fromCache: Boolean,
    ) : CheckResult

    /** GitHub returned 403: the anonymous hourly quota is exhausted. */
    public data object RateLimited : CheckResult

    /** The repository exists but has no published release yet. */
    public data object NoRelease : CheckResult

    public data class Failed(
        val reason: String,
    ) : CheckResult
}

/** Whether the installed build is older than the published release. */
public sealed interface UpdateDecision {
    public data object UpToDate : UpdateDecision

    public data class UpdateAvailable(
        val release: ReleaseInfo,
        val current: AppVersion,
    ) : UpdateDecision

    public data class Incomparable(
        val reason: String,
    ) : UpdateDecision
}

public data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
) {
    public val fraction: Float
        get() = if (totalBytes <= 0L) 0f else (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
}

/** Result of downloading and verifying an update package. */
public sealed interface DownloadResult {
    public data class Verified(
        val file: File,
        val sha256: String,
    ) : DownloadResult

    /** The downloaded bytes did not match the published checksum. The file has been deleted. */
    public data class ChecksumMismatch(
        val expected: String,
        val actual: String,
    ) : DownloadResult

    /** No checksum could be found in the release, so the package is never trusted. */
    public data object MissingChecksum : DownloadResult

    public data class Failed(
        val reason: String,
    ) : DownloadResult
}
