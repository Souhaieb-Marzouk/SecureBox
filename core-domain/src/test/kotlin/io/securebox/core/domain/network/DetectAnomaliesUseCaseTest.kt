package io.securebox.core.domain.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectAnomaliesUseCaseTest {
    private val detector = DetectAnomaliesUseCase()

    @Test
    fun hardThresholdAlertsEvenWithoutBaseline() {
        val events =
            detector(
                nowMillis = 1_000L,
                observedUploadPerPackage = mapOf("com.bad" to 150L * 1024 * 1024),
                baselinePerHour = emptyMap(),
                labels = mapOf("com.bad" to "Bad App"),
            )
        assertEquals(1, events.size)
        assertEquals(AnomalyKind.HARD_THRESHOLD, events.first().kind)
        assertEquals("Bad App", events.first().label)
    }

    @Test
    fun spikeOverBaselineAlerts() {
        val events =
            detector(
                nowMillis = 1_000L,
                observedUploadPerPackage = mapOf("com.app" to 12L * 1024 * 1024),
                baselinePerHour = mapOf("com.app" to 3L * 1024 * 1024),
                labels = emptyMap(),
            )
        assertEquals(1, events.size)
        assertEquals(AnomalyKind.UPLOAD_SPIKE, events.first().kind)
    }

    @Test
    fun normalUsageIsIgnored() {
        val events =
            detector(
                nowMillis = 1_000L,
                observedUploadPerPackage = mapOf("com.app" to 4L * 1024 * 1024),
                baselinePerHour = mapOf("com.app" to 3L * 1024 * 1024),
                labels = emptyMap(),
            )
        assertTrue(events.isEmpty())
    }

    @Test
    fun tinyBaselineDoesNotProduceNoise() {
        val events =
            detector(
                nowMillis = 1_000L,
                observedUploadPerPackage = mapOf("com.tiny" to 8L * 1024),
                baselinePerHour = mapOf("com.tiny" to 2L * 1024),
                labels = emptyMap(),
            )
        assertTrue(events.isEmpty())
    }

    @Test
    fun zeroOrNegativeUploadsAreIgnored() {
        val events =
            detector(
                nowMillis = 1_000L,
                observedUploadPerPackage = mapOf("a" to 0L, "b" to -5L),
                baselinePerHour = emptyMap(),
                labels = emptyMap(),
            )
        assertTrue(events.isEmpty())
    }

    @Test
    fun resultsAreSortedByObservedBytes() {
        val events =
            detector(
                nowMillis = 1_000L,
                observedUploadPerPackage =
                    mapOf(
                        "small" to 101L * 1024 * 1024,
                        "large" to 400L * 1024 * 1024,
                    ),
                baselinePerHour = emptyMap(),
                labels = emptyMap(),
            )
        assertEquals(listOf("large", "small"), events.map { it.packageName })
    }
}
