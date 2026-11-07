package io.securebox.core.domain.security

/**
 * Permission-based heuristics. These are the combinations that matter in practice on TV boxes and
 * cheap phones: an app that can read SMS *and* draw over other apps, or that combines accessibility
 * with payment-related permissions, deserves a red badge.
 */
public class PermissionAuditor {
    public fun analyze(permissions: Collection<String>): List<Finding> {
        val set = permissions.map { it.substringAfterLast('.') }.toSet()
        val findings = mutableListOf<Finding>()

        fun add(
            code: FindingCode,
            severity: RiskLevel,
            enabled: Boolean,
        ) {
            if (enabled) findings += Finding(code, severity)
        }

        add(FindingCode.SMS_ACCESS, RiskLevel.HIGH, "READ_SMS" in set || "RECEIVE_SMS" in set || "SEND_SMS" in set)
        add(FindingCode.CALL_LOG_ACCESS, RiskLevel.HIGH, "READ_CALL_LOG" in set || "WRITE_CALL_LOG" in set)
        add(FindingCode.CONTACTS_ACCESS, RiskLevel.MEDIUM, "READ_CONTACTS" in set || "WRITE_CONTACTS" in set)
        add(FindingCode.LOCATION_ACCESS, RiskLevel.MEDIUM, "ACCESS_FINE_LOCATION" in set || "ACCESS_COARSE_LOCATION" in set)
        add(FindingCode.CAMERA_ACCESS, RiskLevel.MEDIUM, "CAMERA" in set)
        add(FindingCode.MICROPHONE_ACCESS, RiskLevel.MEDIUM, "RECORD_AUDIO" in set)
        add(FindingCode.OVERLAY, RiskLevel.HIGH, "SYSTEM_ALERT_WINDOW" in set)
        add(FindingCode.INSTALL_PACKAGES, RiskLevel.MEDIUM, "REQUEST_INSTALL_PACKAGES" in set)
        add(FindingCode.USAGE_ACCESS, RiskLevel.LOW, "PACKAGE_USAGE_STATS" in set)

        add(
            FindingCode.COMBO_SMS_ACCESSIBILITY,
            RiskLevel.CRITICAL,
            ("READ_SMS" in set || "RECEIVE_SMS" in set) && ("BIND_ACCESSIBILITY_SERVICE" in set || "ACCESSIBILITY" in set),
        )
        add(
            FindingCode.COMBO_OVERLAY_ACCESSIBILITY,
            RiskLevel.CRITICAL,
            "SYSTEM_ALERT_WINDOW" in set && ("BIND_ACCESSIBILITY_SERVICE" in set || "ACCESSIBILITY" in set),
        )
        add(
            FindingCode.COMBO_SMS_ADMIN,
            RiskLevel.HIGH,
            ("READ_SMS" in set || "RECEIVE_SMS" in set) && "BIND_DEVICE_ADMIN" in set,
        )

        return findings
    }
}

