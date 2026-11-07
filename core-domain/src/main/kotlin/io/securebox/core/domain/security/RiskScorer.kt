package io.securebox.core.domain.security

/**
 * Turns findings into a 0-100 risk score. A clean app scores 100; anything with a critical finding
 * is forced below 30 regardless of its other properties.
 */
public class RiskScorer {
    public fun score(findings: List<Finding>): Int {
        if (findings.isEmpty()) return 100
        var penalty = 0
        for (finding in findings) {
            penalty +=
                when (finding.severity) {
                    RiskLevel.CRITICAL -> 75
                    RiskLevel.HIGH -> 22
                    RiskLevel.MEDIUM -> 10
                    RiskLevel.LOW -> 4
                    RiskLevel.SAFE -> 0
                }
        }
        return (100 - penalty).coerceIn(0, 100)
    }

    public fun level(findings: List<Finding>, score: Int): RiskLevel {
        val worst = findings.maxByOrNull { severityRank(it.severity) }?.severity
        if (worst == RiskLevel.CRITICAL) return RiskLevel.CRITICAL
        return when {
            score >= 85 -> RiskLevel.SAFE
            score >= 70 -> RiskLevel.LOW
            score >= 50 -> RiskLevel.MEDIUM
            score >= 30 -> RiskLevel.HIGH
            else -> RiskLevel.CRITICAL
        }
    }

    public fun report(
        app: AppSecurityInfo,
        findings: List<Finding>,
    ): AppRiskReport {
        val value = score(findings)
        return AppRiskReport(app = app, score = value, level = level(findings, value), findings = findings)
    }

    private fun severityRank(level: RiskLevel): Int =
        when (level) {
            RiskLevel.SAFE -> 0
            RiskLevel.LOW -> 1
            RiskLevel.MEDIUM -> 2
            RiskLevel.HIGH -> 3
            RiskLevel.CRITICAL -> 4
        }
}

