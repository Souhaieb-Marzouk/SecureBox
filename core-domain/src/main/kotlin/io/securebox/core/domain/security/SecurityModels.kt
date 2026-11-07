package io.securebox.core.domain.security

/** How risky an app looks, from the user's point of view. */
public enum class RiskLevel {
    SAFE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}

public enum class InstallSource {
    PLAY_STORE,
    SYSTEM,
    SIDELOADED,
    UNKNOWN,
}

/** Stable codes so the UI can localise every finding. */
public enum class FindingCode {
    SMS_ACCESS,
    CALL_LOG_ACCESS,
    CONTACTS_ACCESS,
    LOCATION_ACCESS,
    CAMERA_ACCESS,
    MICROPHONE_ACCESS,
    OVERLAY,
    ACCESSIBILITY,
    DEVICE_ADMIN,
    NOTIFICATION_LISTENER,
    INSTALL_PACKAGES,
    USAGE_ACCESS,
    TRACKER_SDK,
    SUSPICIOUS_SDK,
    SIDELOADED,
    EXPORTED_COMPONENT,
    HIDDEN_LAUNCHER,
    SIGNATURE_RULE,
    COMBO_SMS_ACCESSIBILITY,
    COMBO_OVERLAY_ACCESSIBILITY,
    COMBO_SMS_ADMIN,
}

public data class Finding(
    val code: FindingCode,
    val severity: RiskLevel,
    /** SDK name, component name, rule name or permission, depending on [code]. */
    val detail: String? = null,
)

public data class AppSecurityInfo(
    val packageName: String,
    val label: String,
    val uid: Int,
    val versionName: String,
    val isSystem: Boolean,
    val source: InstallSource,
    val installer: String?,
    val firstInstallTimeMillis: Long,
    val lastUpdateTimeMillis: Long,
    val permissions: List<String>,
)

public data class AppRiskReport(
    val app: AppSecurityInfo,
    val score: Int,
    val level: RiskLevel,
    val findings: List<Finding>,
)

public data class ScanSummary(
    val startedAtMillis: Long,
    val finishedAtMillis: Long,
    val scannedApps: Int,
    val highRiskApps: Int,
    val findingsCount: Int,
    val rulesVersion: Int,
)

/** One signature rule. Patterns are plain strings matched against extracted DEX strings. */
public data class SignatureRule(
    val id: String,
    val name: String,
    val severity: RiskLevel,
    val patterns: List<String>,
    val allRequired: Boolean = false,
)

public data class RuleSet(
    val version: Int,
    val rules: List<SignatureRule>,
    val trackerPrefixes: List<String>,
    val suspiciousPackages: List<String>,
)

