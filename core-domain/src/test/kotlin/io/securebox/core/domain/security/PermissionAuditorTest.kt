package io.securebox.core.domain.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionAuditorTest {
    private val auditor = PermissionAuditor()

    @Test
    fun plainAppHasNoFindings() {
        assertTrue(auditor.analyze(listOf("android.permission.INTERNET")).isEmpty())
    }

    @Test
    fun smsAndAccessibilityIsCritical() {
        val findings =
            auditor.analyze(
                listOf(
                    "android.permission.READ_SMS",
                    "android.permission.BIND_ACCESSIBILITY_SERVICE",
                ),
            )
        assertTrue(findings.any { it.code == FindingCode.COMBO_SMS_ACCESSIBILITY && it.severity == RiskLevel.CRITICAL })
    }

    @Test
    fun overlayAndAccessibilityIsCritical() {
        val findings =
            auditor.analyze(
                listOf(
                    "android.permission.SYSTEM_ALERT_WINDOW",
                    "android.permission.BIND_ACCESSIBILITY_SERVICE",
                ),
            )
        assertTrue(findings.any { it.code == FindingCode.COMBO_OVERLAY_ACCESSIBILITY })
    }

    @Test
    fun individualDangerousPermissionsAreReported() {
        val findings =
            auditor.analyze(
                listOf(
                    "android.permission.READ_SMS",
                    "android.permission.CAMERA",
                    "android.permission.RECORD_AUDIO",
                ),
            )
        assertEquals(3, findings.size)
        assertTrue(findings.any { it.code == FindingCode.SMS_ACCESS })
        assertTrue(findings.any { it.code == FindingCode.CAMERA_ACCESS })
        assertTrue(findings.any { it.code == FindingCode.MICROPHONE_ACCESS })
    }
}
