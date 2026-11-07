package io.securebox.core.domain.security

public enum class IntegrityIssue {
    DEBUGGER_ATTACHED,
    EMULATOR,
    ROOTED,
    SIGNATURE_MISMATCH,
}

public data class IntegrityReport(
    val issues: List<IntegrityIssue>,
    val checkedAtMillis: Long,
) {
    val compromised: Boolean
        get() = issues.any {
            it == IntegrityIssue.DEBUGGER_ATTACHED ||
                it == IntegrityIssue.ROOTED ||
                it == IntegrityIssue.SIGNATURE_MISMATCH
        }
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

