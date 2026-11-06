package io.securebox.core.domain.model

/** Inputs that feed the Security Score. Each flag is something the user can fix in the app. */
public data class SecurityInputs(
    val usageAccessGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val firewallPrepared: Boolean = false,
    val appIsUpToDate: Boolean = false,
)

/** A single weighted contributor to the score, so the UI can explain "why this number". */
public data class ScoreFactor(
    val id: String,
    val weight: Int,
    val satisfied: Boolean,
)

public enum class ScoreGrade {
    EXCELLENT,
    GOOD,
    FAIR,
    AT_RISK,
}

public data class SecurityScore(
    val value: Int,
    val grade: ScoreGrade,
    val factors: List<ScoreFactor>,
) {
    public companion object {
        public val EMPTY: SecurityScore =
            SecurityScore(value = 0, grade = ScoreGrade.AT_RISK, factors = emptyList())
    }
}

/**
 * Turns permission and hygiene facts into a single 0-100 number that a non-technical user can act
 * on. Weights are deliberately simple and documented so the score never feels arbitrary:
 *
 * - Firewall prepared: 35 (the strongest protection this app offers)
 * - App up to date:    30 (known vulnerabilities are the most common real-world risk)
 * - Usage access:      20 (needed for per-app visibility)
 * - Notifications:     15 (needed to warn the user early)
 */
public class CalculateSecurityScoreUseCase {
    public operator fun invoke(inputs: SecurityInputs): SecurityScore {
        val factors =
            listOf(
                ScoreFactor(FACTOR_FIREWALL, weight = 35, satisfied = inputs.firewallPrepared),
                ScoreFactor(FACTOR_UPDATES, weight = 30, satisfied = inputs.appIsUpToDate),
                ScoreFactor(FACTOR_USAGE, weight = 20, satisfied = inputs.usageAccessGranted),
                ScoreFactor(FACTOR_NOTIFICATIONS, weight = 15, satisfied = inputs.notificationsGranted),
            )
        val raw = factors.filter { it.satisfied }.sumOf { it.weight }
        // Never show 0: an installed, running app has at least a baseline of protection.
        val value = raw.coerceIn(MINIMUM_SCORE, 100)
        return SecurityScore(value = value, grade = gradeFor(value), factors = factors)
    }

    private fun gradeFor(value: Int): ScoreGrade =
        when {
            value >= 85 -> ScoreGrade.EXCELLENT
            value >= 65 -> ScoreGrade.GOOD
            value >= 40 -> ScoreGrade.FAIR
            else -> ScoreGrade.AT_RISK
        }

    public companion object {
        public const val FACTOR_FIREWALL: String = "firewall"
        public const val FACTOR_UPDATES: String = "updates"
        public const val FACTOR_USAGE: String = "usage"
        public const val FACTOR_NOTIFICATIONS: String = "notifications"
        public const val MINIMUM_SCORE: Int = 5
    }
}
