package io.securebox.core.network.github

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.securebox.core.domain.model.AppVersion
import io.securebox.core.domain.model.CheckResult
import io.securebox.core.domain.model.DownloadProgress
import io.securebox.core.domain.model.DownloadResult
import io.securebox.core.domain.model.ReleaseAsset
import io.securebox.core.domain.model.ReleaseInfo
import io.securebox.core.domain.repository.SettingsRepository
import io.securebox.core.domain.repository.UpdateRepository
import io.securebox.core.domain.security.Sha256
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class GithubUpdateRepository
    @Inject
    constructor(
        private val api: GithubApiClient,
        private val client: OkHttpClient,
        private val dataStore: DataStore<Preferences>,
        private val settingsRepository: SettingsRepository,
        @ApplicationContext private val context: Context,
    ) : UpdateRepository {
        override suspend fun fetchLatestRelease(
            force: Boolean,
            current: AppVersion,
        ): CheckResult {
            val intervalHours = settingsRepository.settings.first().updateCheckIntervalHours
            val lastChecked = lastCheckedAtMillis()
            val cachedJson = dataStore.data.first()[Keys.CACHED_RELEASE]

            if (!force && cachedJson != null && lastChecked != null) {
                val ageMillis = System.currentTimeMillis() - lastChecked
                if (ageMillis in 0 until intervalHours * 60 * 60 * 1000) {
                    parseCached(cachedJson)?.let { return CheckResult.Fetched(it, fromCache = true) }
                }
            }

            return when (val result = api.fetchLatestRelease()) {
                is LatestReleaseResult.Success -> {
                    dataStore.edit {
                        it[Keys.CACHED_RELEASE] = result.rawJson
                        it[Keys.LAST_CHECK] = System.currentTimeMillis()
                    }
                    CheckResult.Fetched(result.release, fromCache = false)
                }

                LatestReleaseResult.RateLimited -> CheckResult.RateLimited
                LatestReleaseResult.NoRelease -> CheckResult.NoRelease
                is LatestReleaseResult.Failure -> CheckResult.Failed(result.message)
            }
        }

        override suspend fun lastCheckedAtMillis(): Long? = dataStore.data.first()[Keys.LAST_CHECK]

        override suspend fun downloadApk(
            release: ReleaseInfo,
            primaryAbi: String?,
            onProgress: (DownloadProgress) -> Unit,
        ): DownloadResult =
            withContext(Dispatchers.IO) {
                val asset =
                    chooseAsset(release, primaryAbi)
                        ?: return@withContext DownloadResult.Failed(
                            "No compatible APK is attached to this release.",
                        )

                if (asset.sizeBytes > MAX_APK_BYTES) {
                    return@withContext DownloadResult.Failed("Release asset is unexpectedly large.")
                }

                val expectedHash = publishedChecksum(release, asset)
                    ?: return@withContext DownloadResult.MissingChecksum

                val targetDirectory = File(context.cacheDir, "updates").apply { mkdirs() }
                val target = File(targetDirectory, asset.name)
                if (target.exists()) target.delete()

                val failure = downloadTo(asset, target, onProgress)
                if (failure != null) {
                    target.delete()
                    return@withContext DownloadResult.Failed(failure)
                }

                val actualHash = Sha256.of(target)
                if (!Sha256.matches(expectedHash, actualHash)) {
                    target.delete()
                    return@withContext DownloadResult.ChecksumMismatch(
                        expected = Sha256.normalize(expectedHash),
                        actual = actualHash,
                    )
                }

                DownloadResult.Verified(file = target, sha256 = actualHash)
            }

        private fun chooseAsset(
            release: ReleaseInfo,
            primaryAbi: String?,
        ): ReleaseAsset? =
            preferredAssetNames(primaryAbi)
                .firstNotNullOfOrNull { name -> release.asset(name) }

        private fun preferredAssetNames(primaryAbi: String?): List<String> =
            buildList {
                when (primaryAbi) {
                    "arm64-v8a" -> add("securebox-arm64-v8a-release.apk")
                    "armeabi-v7a" -> add("securebox-armeabi-v7a-release.apk")
                    "x86_64" -> add("securebox-x86_64-release.apk")
                    "x86" -> add("securebox-x86-release.apk")
                }
                add(ReleaseInfo.UNIVERSAL_ASSET_NAME)
            }

        /**
         * The checksum is required. It is read from the `SHA256SUMS` asset first and, if that asset
         * is missing, from the release body. A release without a checksum is never installable.
         */
        private fun publishedChecksum(
            release: ReleaseInfo,
            asset: ReleaseAsset,
        ): String? {
            val checksumAsset = release.findChecksumAsset()
            if (checksumAsset != null) {
                val content = api.fetchText(checksumAsset.downloadUrl)
                if (content != null) {
                    Sha256.parseChecksumFile(content, asset.name)?.let { return it }
                }
            }
            return Sha256.parseChecksumFile(release.notes, asset.name)
        }

        private fun downloadTo(
            asset: ReleaseAsset,
            target: File,
            onProgress: (DownloadProgress) -> Unit,
        ): String? {
            val request = Request.Builder().url(asset.downloadUrl).header("User-Agent", "SecureBox-Android").build()
            return try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return "Download failed with HTTP ${response.code}."
                    val body = response.body ?: return "Empty download response."
                    val total = body.contentLength().takeIf { it > 0 } ?: asset.sizeBytes
                    var downloaded = 0L
                    body.byteStream().use { input ->
                        target.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (downloaded > MAX_APK_BYTES) return "Download exceeded the size limit."
                                onProgress(DownloadProgress(downloaded, total))
                            }
                        }
                    }
                    null
                }
            } catch (error: IOException) {
                error.message ?: "Download interrupted."
            }
        }

        private fun parseCached(rawJson: String): ReleaseInfo? =
            try {
                val dto = secureBoxJson.decodeFromString<GithubReleaseDto>(rawJson)
                ReleaseInfo(
                    tagName = dto.tagName,
                    title = dto.name?.takeIf { it.isNotBlank() } ?: dto.tagName,
                    notes = dto.body.orEmpty(),
                    publishedAt = dto.publishedAt.orEmpty(),
                    version = io.securebox.core.domain.version.SemVerParser.parse(dto.tagName),
                    versionCode = io.securebox.core.domain.version.SemVerParser.versionCodeFromTag(dto.tagName),
                    prerelease = dto.prerelease,
                    assets = dto.assets.map { ReleaseAsset(it.name, it.downloadUrl, it.size) },
                )
            } catch (error: RuntimeException) {
                null
            }

        private object Keys {
            val LAST_CHECK = longPreferencesKey("update_last_checked_at")
            val CACHED_RELEASE = stringPreferencesKey("update_cached_release")
        }

        private companion object {
            const val MAX_APK_BYTES: Long = 200L * 1024 * 1024
        }
    }
