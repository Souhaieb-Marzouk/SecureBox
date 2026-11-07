package io.securebox.core.domain.network

/**
 * Pure anomaly rules for upload spikes.
 *
 * An app is flagged when, during the last hour:
 * - it uploaded at least [hardThresholdBytes] (default 100 MB), or
 * - its upload exceeded [multiplier] times its 7-day hourly baseline, provided that baseline is
 *   meaningful (at least [minimumBaselineBytesPerHour]).
 *
 * The baseline guard prevents tiny apps from producing noise: 2 KB to 8 KB is technically 4x but
 * is not worth waking the user for.
 */
public class DetectAnomaliesUseCase(
    private val multiplier: Double = 3.0,
    private val hardThresholdBytes: Long = 100L * 1024 * 1024,
    private val minimumBaselineBytesPerHour: Long = 512L * 1024,
) {
    public operator fun invoke(
        nowMillis: Long,
        observedUploadPerPackage: Map<String, Long>,
        baselinePerHour: Map<String, Long>,
        labels: Map<String, String>,
    ): List<AnomalyEvent> =
        observedUploadPerPackage
            .mapNotNull { (packageName, observedBytes) ->
                if (observedBytes <= 0L) return@mapNotNull null
                val baseline = baselinePerHour[packageName] ?: 0L
                val kind =
                    when {
                        observedBytes >= hardThresholdBytes -> AnomalyKind.HARD_THRESHOLD
                        baseline >= minimumBaselineBytesPerHour &&
                            observedBytes >= (baseline * multiplier).toLong() -> AnomalyKind.UPLOAD_SPIKE

                        else -> return@mapNotNull null
                    }
                AnomalyEvent(
                    packageName = packageName,
                    label = labels[packageName] ?: packageName,
                    kind = kind,
                    observedBytes = observedBytes,
                    baselineBytesPerHour = baseline,
                    atMillis = nowMillis,
                )
            }.sortedByDescending { it.observedBytes }

    public companion object {
        public const val DEFAULT_MULTIPLIER: Double = 3.0
        public const val DEFAULT_HARD_THRESHOLD_BYTES: Long = 100L * 1024 * 1024
    }
}
