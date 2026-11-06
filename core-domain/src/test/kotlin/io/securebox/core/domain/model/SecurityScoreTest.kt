package io.securebox.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityScoreTest {
    private val useCase = CalculateSecurityScoreUseCase()

    @Test
    fun everyProtectionOffStillGivesABaseline() {
        val score = useCase(SecurityInputs())
        assertEquals(CalculateSecurityScoreUseCase.MINIMUM_SCORE, score.value)
        assertEquals(ScoreGrade.AT_RISK, score.grade)
        assertTrue(score.factors.all { !it.satisfied })
    }

    @Test
    fun everythingEnabledIsOneHundred() {
        val score =
            useCase(
                SecurityInputs(
                    usageAccessGranted = true,
                    notificationsGranted = true,
                    firewallPrepared = true,
                    appIsUpToDate = true,
                ),
            )
        assertEquals(100, score.value)
        assertEquals(ScoreGrade.EXCELLENT, score.grade)
        assertTrue(score.factors.all { it.satisfied })
    }

    @Test
    fun weightsSumToHundred() {
        val total = useCase(SecurityInputs()).factors.sumOf { it.weight }
        assertEquals(100, total)
    }

    @Test
    fun scoreReflectsIndividualFactors() {
        val firewallOnly = useCase(SecurityInputs(firewallPrepared = true))
        assertEquals(35, firewallOnly.value)
        assertEquals(ScoreGrade.AT_RISK, firewallOnly.grade)

        val almostEverything =
            useCase(
                SecurityInputs(
                    usageAccessGranted = true,
                    notificationsGranted = true,
                    firewallPrepared = true,
                ),
            )
        assertEquals(70, almostEverything.value)
        assertEquals(ScoreGrade.GOOD, almostEverything.grade)
    }

    @Test
    fun valueNeverExceedsOneHundred() {
        val score = useCase(SecurityInputs(true, true, true, true))
        assertTrue(score.value in 0..100)
    }
}
