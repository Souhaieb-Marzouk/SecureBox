package io.securebox.core.data.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.securebox.core.data.BuildConfig
import io.securebox.core.domain.security.RiskLevel
import io.securebox.core.domain.security.RuleSet
import io.securebox.core.domain.security.SignatureRule
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loads scanner rules from an AES-256-GCM encrypted asset.
 *
 * The key is compiled into the APK, so this is obfuscation rather than a secret. It keeps casual
 * rule tampering out and leaves room for the signed feed in v0.5.0, where rules will be signed with
 * Ed25519 and verified before use.
 */
@Singleton
class SecurityRulesRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        @Volatile
        private var cached: RuleSet? = null

        fun rules(): RuleSet {
            cached?.let { return it }
            return synchronized(this) {
                cached ?: load().also { cached = it }
            }
        }

        private fun load(): RuleSet =
            runCatching {
                val encrypted = context.assets.open(ASSET_NAME).use { it.readBytes() }
                if (encrypted.size <= IV_SIZE + TAG_SIZE) return@runCatching emptyRuleSet()

                val iv = encrypted.copyOfRange(0, IV_SIZE)
                val tag = encrypted.copyOfRange(encrypted.size - TAG_SIZE, encrypted.size)
                val ciphertext = encrypted.copyOfRange(IV_SIZE, encrypted.size - TAG_SIZE)

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(
                    Cipher.DECRYPT_MODE,
                    SecretKeySpec(BuildConfig.SCANNER_RULES_KEY.hexToBytes(), "AES"),
                    GCMParameterSpec(TAG_SIZE * 8, iv),
                )
                val json = String(cipher.doFinal(ciphertext + tag), Charsets.UTF_8)
                parse(json)
            }.getOrElse { emptyRuleSet() }

        private fun parse(json: String): RuleSet {
            val root = JSONObject(json)
            val rules = mutableListOf<SignatureRule>()
            val array = root.optJSONArray("rules") ?: JSONArray()
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val patterns = mutableListOf<String>()
                val patternArray = item.optJSONArray("patterns") ?: JSONArray()
                for (p in 0 until patternArray.length()) patterns += patternArray.getString(p)
                if (patterns.isEmpty()) continue
                rules +=
                    SignatureRule(
                        id = item.optString("id", "rule-$index"),
                        name = item.optString("name", "Unnamed rule"),
                        severity = parseSeverity(item.optString("severity")),
                        patterns = patterns,
                        allRequired = item.optBoolean("allRequired", false),
                    )
            }

            return RuleSet(
                version = root.optInt("version", 0),
                rules = rules,
                trackerPrefixes = root.optJSONArray("trackerPrefixes").toStringList(),
                suspiciousPackages = root.optJSONArray("suspiciousPackages").toStringList(),
            )
        }

        private fun parseSeverity(raw: String): RiskLevel =
            runCatching { RiskLevel.valueOf(raw.uppercase()) }.getOrDefault(RiskLevel.MEDIUM)

        private fun JSONArray?.toStringList(): List<String> {
            if (this == null) return emptyList()
            return buildList {
                for (index in 0 until length()) add(getString(index))
            }
        }

        private fun emptyRuleSet(): RuleSet = RuleSet(version = 0, rules = emptyList(), trackerPrefixes = emptyList(), suspiciousPackages = emptyList())

        private fun String.hexToBytes(): ByteArray =
            ByteArray(length / 2) { index ->
                ((this[index * 2].digitToInt(16) shl 4) or this[index * 2 + 1].digitToInt(16)).toByte()
            }

        private companion object {
            const val ASSET_NAME: String = "scanner_rules.enc"
            const val IV_SIZE: Int = 12
            const val TAG_SIZE: Int = 16
        }
    }
