package io.securebox.core.domain.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntegrityReportTest {
    @Test
    fun rootEmulatorAndTestKeysAreAdvisory() {
        val report =
            IntegrityReport(
                issues = listOf(IntegrityIssue.ROOTED, IntegrityIssue.TEST_KEYS, IntegrityIssue.EMULATOR),
                checkedAtMillis = 0L,
            )
        assertFalse(report.compromised)
        assertTrue(report.advisory)
        assertFalse(report.signatureMismatch)
    }

    @Test
    fun signatureMismatchLocksUpdates() {
        val report = IntegrityReport(issues = listOf(IntegrityIssue.SIGNATURE_MISMATCH), checkedAtMillis = 0L)
        assertTrue(report.compromised)
        assertTrue(report.signatureMismatch)
        assertFalse(report.advisory)
    }

    @Test
    fun cleanReportIsNeitherAdvisoryNorCompromised() {
        val report = IntegrityReport(issues = emptyList(), checkedAtMillis = 0L)
        assertFalse(report.compromised)
        assertFalse(report.advisory)
    }
}
