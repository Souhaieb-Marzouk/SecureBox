package io.securebox.core.domain.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskScorerTest {
    private val scorer = RiskScorer()

    private fun app(
        permissions: List<String> = emptyList(),
        isSystem: Boolean = false,
        source: InstallSource = InstallSource.PLAY_STORE,
    ): AppSecurityInfo =
        AppSecurityInfo(
            packageName = "com.example",
            label = "Example",
            uid = 1000,
            versionName = "1.0",
            isSystem = isSystem,
            source = source,
            installer = if (source == InstallSource.PLAY_STORE) "com.android.vending" else null,
            firstInstallTimeMillis = 0,
            lastUpdateTimeMillis = 0,
            permissions = permissions,
        )

    @Test
    fun cleanAppScoresOneHundredAndSafe() {
        val report = scorer.report(app(), emptyList())
        assertEquals(100, report.score)
        assertEquals(RiskLevel.SAFE, report.level)
        assertEquals(AppRecommendation.KEEP, report.recommendation)
    }

    @Test
    fun singleSmsPermissionIsHighRisk() {
        val report = scorer.report(app(permissions = listOf("android.permission.READ_SMS")), emptyList())
        assertEquals(45, report.score)
        assertEquals(RiskLevel.HIGH, report.level)
        assertEquals(AppRecommendation.REDUCE_PERMISSIONS, report.recommendation)
    }

    @Test
    fun smsPlusAccessibilityIsCritical() {
        val report =
            scorer.report(
                app(
                    permissions =
                        listOf(
                            "android.permission.READ_SMS",
                            "android.permission.BIND_ACCESSIBILITY_SERVICE",
                        ),
                ),
                emptyList(),
            )
        assertEquals(RiskLevel.CRITICAL, report.level)
        assertTrue(report.score < 30)
        assertEquals(AppRecommendation.UNINSTALL, report.recommendation)
    }

    @Test
    fun findingsDoNotChangeThePermissionScore() {
        val report =
            scorer.report(
                app(permissions = emptyList()),
                listOf(Finding(FindingCode.SIGNATURE_RULE, RiskLevel.CRITICAL, "rule")),
            )
        assertEquals(100, report.score)
        assertEquals(RiskLevel.SAFE, report.level)
    }

    @Test
    fun systemAppsAreNeverRecommendedForUninstall() {
        val report =
            scorer.report(
                app(
                    permissions =
                        listOf(
                            "android.permission.READ_SMS",
                            "android.permission.BIND_ACCESSIBILITY_SERVICE",
                        ),
                    isSystem = true,
                ),
                emptyList(),
            )
        assertEquals(RiskLevel.CRITICAL, report.level)
        assertEquals(AppRecommendation.KEEP, report.recommendation)
    }

    @Test
    fun manualHighRiskAppIsRecommendedForUninstall() {
        val report =
            scorer.report(
                app(
                    permissions = listOf("android.permission.READ_SMS"),
                    source = InstallSource.SIDELOADED,
                ),
                emptyList(),
            )
        assertEquals(AppRecommendation.UNINSTALL, report.recommendation)
    }

    @Test
    fun scoreIsClampedAtZero() {
        val permissions =
            listOf(
                "android.permission.READ_SMS",
                "android.permission.READ_CALL_LOG",
                "android.permission.BIND_ACCESSIBILITY_SERVICE",
                "android.permission.BIND_DEVICE_ADMIN",
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.RECORD_AUDIO",
            )
        assertEquals(0, scorer.score(permissions))
    }
}
