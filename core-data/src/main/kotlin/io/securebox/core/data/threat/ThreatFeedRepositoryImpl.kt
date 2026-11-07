package io.securebox.core.data.threat

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.securebox.core.data.BuildConfig
import io.securebox.core.domain.security.FeedUpdateResult
import io.securebox.core.domain.security.RiskLevel
import io.securebox.core.domain.security.SignatureRule
import io.securebox.core.domain.security.ThreatFeed
import io.securebox.core.domain.security.ThreatFeedRepository
import io.securebox.core.domain.security.ThreatFeedStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Fetches `latest.json` and `latest.json.sig`, verifies the detached Ed25519 signature over the
 * exact payload bytes, parses the rules and stores the payload atomically.
 */
@Singleton
class ThreatFeedRepositoryImpl
    @Inject
    constructor(
        private val client: OkHttpClient,
        private val dataStore: DataStore<Preferences>,
        @ApplicationContext private val context: android.content.Context,
    ) : ThreatFeedRepository {
        override suspend fun status(): ThreatFeedStatus? =
            withContext(Dispatchers.IO) {
                val prefs = dataStore.data.first()
                val json = prefs[Keys.FEED_JSON] ?: return@withContext null
                val feed = runCatching { parse(json) }.getOrNull() ?: return@withContext null
                ThreatFeedStatus(
                    version = feed.version,
                    generatedAt = feed.generatedAt,
                    ruleCount = feed.rules.size,
                    trackerCount = feed.trackerPrefixes.size,
                    domainCount = feed.domains.size,
                    installedAtMillis = prefs[Keys.INSTALLED_AT] ?: 0L,
                    lastCheckedAtMillis = prefs[Keys.CHECKED_AT],
                    signatureValid = true,
                )
            }

        override suspend fun activeFeed(): ThreatFeed? =
            withContext(Dispatchers.IO) {
                val json = dataStore.data.first()[Keys.FEED_JSON] ?: return@withContext null
                runCatching { parse(json) }.getOrNull()
            }

        override suspend fun update(force: Boolean): FeedUpdateResult =
            withContext(Dispatchers.IO) {
                val url = BuildConfig.THREAT_FEED_URL
                if (url.isBlank()) return@withContext FeedUpdateResult.NotConfigured

                val prefs = dataStore.data.first()
                val checkedAt = prefs[Keys.CHECKED_AT]
                if (!force && checkedAt != null && System.currentTimeMillis() - checkedAt < CHECK_INTERVAL_MILLIS) {
                    return@withContext FeedUpdateResult.UpToDate
                }

                val payload = fetch(url)
                if (payload == null) {
                    dataStore.edit { it[Keys.CHECKED_AT] = System.currentTimeMillis() }
                    return@withContext FeedUpdateResult.Failed(context.getString(io.securebox.core.data.R.string.feed_error_network))
                }
                val signature = fetch("$url.sig")?.toString(Charsets.UTF_8)?.trim()
                if (signature.isNullOrBlank() || !verify(payload, signature)) {
                    return@withContext FeedUpdateResult.InvalidSignature
                }

                val feed =
                    runCatching { parse(payload.toString(Charsets.UTF_8)) }.getOrElse {
                        return@withContext FeedUpdateResult.Failed(context.getString(io.securebox.core.data.R.string.feed_error_parse))
                    }
                val currentVersion = prefs[Keys.FEED_JSON]?.let { runCatching { parse(it).version }.getOrNull() }
                if (currentVersion != null && feed.version <= currentVersion) {
                    dataStore.edit { it[Keys.CHECKED_AT] = System.currentTimeMillis() }
                    return@withContext FeedUpdateResult.UpToDate
                }

                // Atomic: a single edit writes the payload and both timestamps together.
                dataStore.edit {
                    it[Keys.FEED_JSON] = payload.toString(Charsets.UTF_8)
                    it[Keys.INSTALLED_AT] = System.currentTimeMillis()
                    it[Keys.CHECKED_AT] = System.currentTimeMillis()
                }
                val status = status() ?: return@withContext FeedUpdateResult.Failed("stored feed unreadable")
                FeedUpdateResult.Updated(status)
            }

        private fun fetch(url: String): ByteArray? =
            runCatching {
                val request = Request.Builder().url(url).header("User-Agent", "SecureBox-Android").build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return null
                    response.body?.bytes()
                }
            }.getOrNull()

        private fun verify(
            payload: ByteArray,
            signatureBase64: String,
        ): Boolean = Ed25519Verifier.verify(payload, signatureBase64, BuildConfig.THREAT_FEED_PUBLIC_KEY)
        private fun parse(json: String): ThreatFeed {
            val root = JSONObject(json)
            val rules = root.optJSONArray("rules").mapObjects { rule ->
                SignatureRule(
                    id = rule.optString("id", rule.optString("name")),
                    name = rule.optString("name"),
                    severity = runCatching { RiskLevel.valueOf(rule.optString("severity", "MEDIUM").uppercase()) }
                        .getOrDefault(RiskLevel.MEDIUM),
                    patterns = rule.optJSONArray("patterns").strings(),
                    allRequired = rule.optBoolean("allRequired", false),
                )
            }
            return ThreatFeed(
                version = root.optInt("version", 0),
                generatedAt = root.optString("generatedAt"),
                rules = rules.filter { it.name.isNotBlank() && it.patterns.isNotEmpty() },
                trackerPrefixes = root.optJSONArray("trackers").strings(),
                domains = root.optJSONArray("domains").strings(),
            )
        }

        private object Keys {
            val FEED_JSON = stringPreferencesKey("threat_feed_json")
            val INSTALLED_AT = longPreferencesKey("threat_feed_installed_at")
            val CHECKED_AT = longPreferencesKey("threat_feed_checked_at")
        }

        private companion object {
            const val CHECK_INTERVAL_MILLIS: Long = 24L * 60 * 60 * 1000
        }
    }

private fun JSONArray?.mapObjects(block: (JSONObject) -> SignatureRule): List<SignatureRule> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        runCatching { block(getJSONObject(index)) }.getOrNull()
    }
}

private fun JSONArray?.strings(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        runCatching { getString(index).takeIf { it.isNotBlank() } }.getOrNull()
    }
}

private fun String.hexToBytes(): ByteArray =
    chunked(2).map { it.toInt(16).toByte() }.toByteArray()
