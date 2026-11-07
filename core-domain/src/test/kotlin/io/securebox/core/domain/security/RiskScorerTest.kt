package io.securebox.core.domain.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskScorerTest {
    private val scorer = RiskScorer()

    private val app =
        AppSecurityInfo(
            packageName = "com.example",
            label = "Example",
            uid = 1000,
            versionName = "1.0",
            isSystem = false,
            source = InstallSource.PLAY_STORE,
            installer = "com.android.vending",
            firstInstallTimeMillis = 0,
            lastUpdateTimeMillis = 0,
            permissions = emptyList(),
        )

    @Test
    fun cleanAppScoresOneHundredAndSafe() {
        val report = scorer.report(app, emptyList())
        assertEquals(100, report.score)
        assertEquals(RiskLevel.SAFE, report.level)
    }

    @Test
    fun criticalFindingForcesCriticalLevel() {
        val report = scorer.report(app, listOf(Finding(FindingCode.COMBO_SMS_ACCESSIBILITY, RiskLevel.CRITICAL)))
        assertEquals(RiskLevel.CRITICAL, report.level)
        assertTrue(report.score < 30)
    }

    @Test
    fun scoreIsClampedAtZero() {
        val many = (1..5).map { Finding(FindingCode.SIGNATURE_RULE, RiskLevel.CRITICAL, "r$it") }
        assertEquals(0, scorer.score(many))
    }

    @Test
    fun mediumFindingsStayMedium() {
        val report = scorer.report(app, listOf(Finding(FindingCode.TRACKER_SDK, RiskLevel.LOW, "sdk")))
        assertEquals(RiskLevel.SAFE, report.level)
        assertEquals(96, report.score)
    }
}
