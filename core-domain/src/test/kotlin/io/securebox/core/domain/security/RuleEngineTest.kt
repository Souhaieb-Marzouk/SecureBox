package io.securebox.core.domain.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {
    private val rules =
        RuleSet(
            version = 3,
            rules =
                listOf(
                    SignatureRule("R1", "Runtime exec", RiskLevel.HIGH, listOf("Ljava/lang/Runtime;->exec")),
                    SignatureRule("R2", "Both markers", RiskLevel.MEDIUM, listOf("markerA", "markerB"), allRequired = true),
                ),
            trackerPrefixes = listOf("com.adjust.sdk"),
            suspiciousPackages = emptyList(),
        )
    private val engine = RuleEngine(rules)

    @Test
    fun matchesAnyPattern() {
        val findings = engine.scan("xxx Ljava/lang/Runtime;->exec yyy")
        assertEquals(1, findings.size)
        assertEquals(FindingCode.SIGNATURE_RULE, findings.first().code)
        assertEquals(RiskLevel.HIGH, findings.first().severity)
    }

    @Test
    fun allRequiredNeedsEveryPattern() {
        assertTrue(engine.scan("markerA only").isEmpty())
        assertEquals(1, engine.scan("markerA and markerB").size)
    }

    @Test
    fun detectsTrackerPrefix() {
        val findings = engine.scan("com.adjust.sdk.Adjust")
        assertEquals(1, findings.size)
        assertEquals(FindingCode.TRACKER_SDK, findings.first().code)
        assertEquals("com.adjust.sdk", findings.first().detail)
    }

    @Test
    fun emptyCorpusHasNoFindings() {
        assertTrue(engine.scan("").isEmpty())
    }

    @Test
    fun exposesRulesVersion() {
        assertEquals(3, engine.rulesVersion())
    }
}
