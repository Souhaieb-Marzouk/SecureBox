package io.securebox.core.network.github

import io.securebox.core.domain.model.ReleaseAsset
import io.securebox.core.domain.model.ReleaseInfo
import io.securebox.core.domain.version.SemVerParser
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request

internal sealed interface LatestReleaseResult {
    data class Success(
        val release: ReleaseInfo,
        val rawJson: String,
    ) : LatestReleaseResult

    data object RateLimited : LatestReleaseResult

    data object NoRelease : LatestReleaseResult

    data class Failure(
        val message: String,
    ) : LatestReleaseResult
}

/**
 * Talks to the public GitHub REST API directly: no proxy, no token, no telemetry.
 * The endpoint is documented at https://docs.github.com/rest/releases/releases.
 */
class GithubApiClient(
    private val client: OkHttpClient,
    private val owner: String,
    private val repo: String,
    private val apiBase: String,
) {
        internal suspend fun fetchLatestRelease(): LatestReleaseResult =
            withContext(Dispatchers.IO) {
                val request =
                    Request.Builder()
                        .url("$apiBase/repos/$owner/$repo/releases/latest")
                        .header("Accept", "application/vnd.github+json")
                        .header("X-GitHub-Api-Version", "2022-11-28")
                        .header("User-Agent", "SecureBox-Android")
                        .build()

                try {
                    client.newCall(request).execute().use { response ->
                        when {
                            response.code == 200 -> {
                                val body = response.body?.string().orEmpty()
                                if (body.isBlank()) {
                                    LatestReleaseResult.Failure("Empty response from GitHub.")
                                } else {
                                    val dto = secureBoxJson.decodeFromString<GithubReleaseDto>(body)
                                    if (dto.draft) {
                                        LatestReleaseResult.NoRelease
                                    } else {
                                        LatestReleaseResult.Success(dto.toDomain(), body)
                                    }
                                }
                            }

                            response.code == 403 || response.code == 429 -> LatestReleaseResult.RateLimited
                            response.code == 404 -> LatestReleaseResult.NoRelease
                            else -> LatestReleaseResult.Failure("GitHub returned HTTP ${response.code}.")
                        }
                    }
                } catch (error: IOException) {
                    LatestReleaseResult.Failure(error.message ?: "Network unavailable.")
                } catch (error: RuntimeException) {
                    LatestReleaseResult.Failure(error.message ?: "Unexpected response.")
                }
            }

        fun fetchText(url: String): String? =
            try {
                val request =
                    Request.Builder()
                        .url(url)
                        .header("User-Agent", "SecureBox-Android")
                        .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body?.string() else null
                }
            } catch (error: IOException) {
                null
            }

        private fun GithubReleaseDto.toDomain(): ReleaseInfo =
            ReleaseInfo(
                tagName = tagName,
                title = name?.takeIf { it.isNotBlank() } ?: tagName,
                notes = body.orEmpty(),
                publishedAt = publishedAt.orEmpty(),
                version = SemVerParser.parse(tagName),
                versionCode = SemVerParser.versionCodeFromTag(tagName),
                prerelease = prerelease,
                assets = assets.map { ReleaseAsset(name = it.name, downloadUrl = it.downloadUrl, sizeBytes = it.size) },
            )
    }
