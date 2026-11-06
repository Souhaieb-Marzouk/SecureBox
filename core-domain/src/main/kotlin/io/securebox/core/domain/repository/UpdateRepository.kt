package io.securebox.core.domain.repository

import io.securebox.core.domain.model.AppVersion
import io.securebox.core.domain.model.CheckResult
import io.securebox.core.domain.model.DownloadProgress
import io.securebox.core.domain.model.DownloadResult
import io.securebox.core.domain.model.ReleaseInfo

/**
 * Fetches release metadata from GitHub and downloads the matching APK.
 *
 * Implementations must verify the SHA-256 published in the release before handing a file back.
 */
public interface UpdateRepository {
    public suspend fun fetchLatestRelease(
        force: Boolean,
        current: AppVersion,
    ): CheckResult

    public suspend fun downloadApk(
        release: ReleaseInfo,
        primaryAbi: String?,
        onProgress: (DownloadProgress) -> Unit,
    ): DownloadResult

    public suspend fun lastCheckedAtMillis(): Long?
}
