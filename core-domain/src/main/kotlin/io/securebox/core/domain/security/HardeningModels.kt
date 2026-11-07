package io.securebox.core.domain.security

public enum class IntegrityIssue {
    DEBUGGER_ATTACHED,
    EMULATOR,
    ROOTED,
    TEST_KEYS,
    SIGNATURE_MISMATCH,
}

/**
 * Result of the local integrity checks. Only a signature mismatch (the APK was signed by someone
 * else) locks updates. Root, test-keys and emulator signals are advisory: they are common on cheap
 * TV boxes and do not mean the installed APK is tampered with.
 */
public data class IntegrityReport(
    val issues: List<IntegrityIssue>,
    val checkedAtMillis: Long,
) {
    val signatureMismatch: Boolean
        get() = issues.contains(IntegrityIssue.SIGNATURE_MISMATCH)

    val advisory: Boolean
        get() = issues.isNotEmpty() && !signatureMismatch

    val compromised: Boolean
        get() = signatureMismatch
}

public interface IntegrityRepository {
    public suspend fun check(): IntegrityReport
}

public enum class SettingId {
    DEVELOPER_OPTIONS,
    USB_DEBUGGING,
    ACCESSIBILITY,
    DEVICE_ADMIN,
    USER_CA_CERTIFICATES,
    INSTALL_UNKNOWN_APPS,
}

public enum class SettingSeverity {
    LOW,
    MEDIUM,
    HIGH,
}

public data class DangerousSetting(
    val id: SettingId,
    val severity: SettingSeverity,
    val isDangerous: Boolean,
    /** Number of items involved (admins, certificates, services), when relevant. */
    val count: Int = 0,
)

public interface DeviceSettingsRepository {
    public suspend fun audit(): List<DangerousSetting>
}

