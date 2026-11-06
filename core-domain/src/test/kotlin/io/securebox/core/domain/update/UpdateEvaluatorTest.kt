package io.securebox.core.domain.update

import io.securebox.core.domain.model.AppVersion
import io.securebox.core.domain.model.ReleaseInfo
import io.securebox.core.domain.model.UpdateDecision
import io.securebox.core.domain.version.SemVerParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateEvaluatorTest {
    private val installed = AppVersion(versionName = "0.1.0", versionCode = 1)

    private fun release(tag: String): ReleaseInfo =
        ReleaseInfo(
            tagName = tag,
            title = tag,
            notes = "",
            publishedAt = "2026-01-01T00:00:00Z",
            version = SemVerParser.parse(tag),
            versionCode = SemVerParser.versionCodeFromTag(tag),
            prerelease = false,
            assets = emptyList(),
        )

    @Test
    fun newerPatchOffersAnUpdate() {
        val decision = UpdateEvaluator.evaluate(installed, release("v0.1.1+2"))
        assertTrue(decision is UpdateDecision.UpdateAvailable)
        assertEquals("v0.1.1+2", (decision as UpdateDecision.UpdateAvailable).release.tagName)
    }

    @Test
    fun newerMajorOffersAnUpdate() {
        assertTrue(UpdateEvaluator.evaluate(installed, release("v1.0.0+10")) is UpdateDecision.UpdateAvailable)
    }

    @Test
    fun sameVersionIsUpToDate() {
        assertEquals(UpdateDecision.UpToDate, UpdateEvaluator.evaluate(installed, release("v0.1.0+1")))
    }

    @Test
    fun olderVersionIsNotOffered() {
        assertEquals(UpdateDecision.UpToDate, UpdateEvaluator.evaluate(installed, release("v0.0.9+1")))
    }

    @Test
    fun sameNameWithHigherVersionCodeOffersAnUpdate() {
        assertTrue(UpdateEvaluator.evaluate(installed, release("v0.1.0+2")) is UpdateDecision.UpdateAvailable)
    }

    @Test
    fun sameNameWithLowerVersionCodeIsUpToDate() {
        assertEquals(UpdateDecision.UpToDate, UpdateEvaluator.evaluate(installed, release("v0.1.0+0")))
    }

    @Test
    fun nonSemanticTagIsNeverInstalled() {
        assertTrue(UpdateEvaluator.evaluate(installed, release("nightly-latest")) is UpdateDecision.Incomparable)
    }
}
