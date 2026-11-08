package io.securebox.core.domain.security

/**
 * Permission-first risk scoring.
 *
 * The score and level depend only on permissions the app currently holds. Risk signals (trackers,
 * exported components, signature rules) remain visible as context but never change the score.
 *
 * 100 means no sensitive permission is currently granted. Heavier and combined capabilities lower
 * the score; an SMS + screen-control combination is forced into the critical band.
 */
public class RiskScorer {
    private data class PermissionCategory(
        val names: Set<String>,
        val penalty: Int,
    )

    private val categories =
        listOf(
            PermissionCategory(setOf("READ_SMS", "SEND_SMS", "RECEIVE_SMS", "READ_MMS", "SEND_MMS"), 55),
            PermissionCategory(setOf("READ_CALL_LOG", "WRITE_CALL_LOG"), 50),
            PermissionCategory(setOf("BIND_ACCESSIBILITY_SERVICE", "ACCESSIBILITY"), 60),
            PermissionCategory(setOf("BIND_DEVICE_ADMIN", "DEVICE_ADMIN"), 60),
            PermissionCategory(setOf("BIND_NOTIFICATION_LISTENER_SERVICE", "NOTIFICATION_LISTENER"), 50),
            PermissionCategory(setOf("SYSTEM_ALERT_WINDOW"), 55),
            PermissionCategory(setOf("MANAGE_EXTERNAL_STORAGE"), 30),
            PermissionCategory(setOf("READ_CONTACTS", "WRITE_CONTACTS"), 20),
            PermissionCategory(setOf("ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION"), 20),
            PermissionCategory(setOf("ACCESS_BACKGROUND_LOCATION"), 10),
            PermissionCategory(setOf("CAMERA"), 20),
            PermissionCategory(setOf("RECORD_AUDIO"), 25),
            PermissionCategory(setOf("REQUEST_INSTALL_PACKAGES"), 20),
            PermissionCategory(setOf("PACKAGE_USAGE_STATS"), 10),
            PermissionCategory(setOf("QUERY_ALL_PACKAGES"), 8),
            PermissionCategory(
                setOf("READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE", "READ_MEDIA_IMAGES", "READ_MEDIA_VIDEO", "READ_MEDIA_AUDIO"),
                5,
            ),
            PermissionCategory(setOf("READ_PHONE_STATE"), 15),
            PermissionCategory(setOf("CALL_PHONE", "ANSWER_PHONE_CALLS"), 15),
            PermissionCategory(setOf("BODY_SENSORS"), 10),
            PermissionCategory(setOf("READ_CALENDAR", "WRITE_CALENDAR"), 5),
            PermissionCategory(setOf("BLUETOOTH_CONNECT", "BLUETOOTH_SCAN"), 5),
            PermissionCategory(setOf("REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"), 5),
            PermissionCategory(setOf("SCHEDULE_EXACT_ALARM"), 5),
        )

    public fun score(permissions: Collection<String>): Int {
        val simple = permissions.map { it.substringAfterLast('.') }.toSet()

        fun has(vararg names: String): Boolean = names.any { it in simple }

        val matched = categories.filter { category -> category.names.any { it in simple } }
        val strongest = matched.maxOfOrNull { it.penalty } ?: 0
        val breadth = (matched.size - 1).coerceAtLeast(0) * 5
        var penalty = strongest + breadth

        val sms = has("READ_SMS", "SEND_SMS", "RECEIVE_SMS", "READ_MMS", "SEND_MMS")
        val accessibility = has("BIND_ACCESSIBILITY_SERVICE", "ACCESSIBILITY")
        val deviceAdmin = has("BIND_DEVICE_ADMIN", "DEVICE_ADMIN")
        val overlay = has("SYSTEM_ALERT_WINDOW")

        // Dangerous combinations: reading messages plus controlling the screen, or drawing over
        // other apps while controlling the screen.
        if (sms && (accessibility || deviceAdmin || overlay)) penalty += 45
        if (overlay && accessibility) penalty += 45

        return (100 - penalty).coerceIn(0, 100)
    }

    public fun level(score: Int): RiskLevel =
        when {
            score >= 85 -> RiskLevel.SAFE
            score >= 70 -> RiskLevel.LOW
            score >= 50 -> RiskLevel.MEDIUM
            score >= 30 -> RiskLevel.HIGH
            else -> RiskLevel.CRITICAL
        }

    public fun report(
        app: AppSecurityInfo,
        findings: List<Finding>,
    ): AppRiskReport {
        val value = score(app.permissions)
        val valueLevel = level(value)
        return AppRiskReport(
            app = app,
            score = value,
            level = valueLevel,
            findings = findings,
            recommendation = recommendation(app, valueLevel),
        )
    }

    /**
     * The recommendation is deliberately conservative for system software: removing or disabling a
     * built-in app can break calls, Wi-Fi, updates or the launcher.
     */
    public fun recommendation(
        app: AppSecurityInfo,
        level: RiskLevel,
    ): AppRecommendation =
        when {
            app.isSystem -> AppRecommendation.KEEP
            level == RiskLevel.CRITICAL -> AppRecommendation.UNINSTALL
            level == RiskLevel.HIGH &&
                (app.source == InstallSource.SIDELOADED ||
                    app.source == InstallSource.OTHER_STORE ||
                    app.source == InstallSource.UNKNOWN) -> AppRecommendation.UNINSTALL

            level == RiskLevel.HIGH || level == RiskLevel.MEDIUM -> AppRecommendation.REDUCE_PERMISSIONS
            else -> AppRecommendation.KEEP
        }
}
