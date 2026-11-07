package io.securebox.core.data.network

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import dagger.hilt.android.qualifiers.ApplicationContext
import io.securebox.core.domain.network.AppTraffic
import io.securebox.core.domain.network.TrafficRange
import io.securebox.core.domain.network.TrafficRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * TrafficStats for live counters and NetworkStatsManager for history. Both are read-only, local
 * APIs; nothing leaves the device. Without usage access NetworkStatsManager throws, so every query
 * degrades to an empty result instead of crashing.
 */
@Singleton
class AndroidTrafficRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val registry: AppRegistry,
    ) : TrafficRepository {
        private val statsManager: NetworkStatsManager? = context.getSystemService(NetworkStatsManager::class.java)

        override suspend fun liveTotals(): List<AppTraffic> =
            withContext(Dispatchers.IO) {
                registry
                    .byUid()
                    .map { (uid, entry) ->
                        AppTraffic(
                            packageName = entry.packageName,
                            label = entry.label,
                            uid = uid,
                            rxBytes = TrafficStats.getUidRxBytes(uid).coerceAtLeast(0L),
                            txBytes = TrafficStats.getUidTxBytes(uid).coerceAtLeast(0L),
                            isSystem = entry.isSystem,
                        )
                    }.filter { it.totalBytes > 0 }
            }

        override suspend fun historicalUsage(range: TrafficRange): List<AppTraffic> =
            withContext(Dispatchers.IO) {
                val (start, end) = window(range)
                val totals = aggregate(start, end)
                val apps = registry.byUid()
                totals
                    .mapNotNull { (uid, value) ->
                        val entry = apps[uid] ?: return@mapNotNull null
                        AppTraffic(
                            packageName = entry.packageName,
                            label = entry.label,
                            uid = uid,
                            rxBytes = value.rx,
                            txBytes = value.tx,
                            isSystem = entry.isSystem,
                        )
                    }.filter { it.totalBytes > 0 }
            }

        override suspend fun hourlyUploads(nowMillis: Long): Map<String, Long> =
            withContext(Dispatchers.IO) {
                val totals = aggregate(nowMillis - HOUR_MILLIS, nowMillis)
                toPackageMap(totals) { it.tx }
            }

        override suspend fun baselineHourlyUploads(nowMillis: Long): Map<String, Long> =
            withContext(Dispatchers.IO) {
                val totals = aggregate(nowMillis - SEVEN_DAYS_MILLIS, nowMillis)
                toPackageMap(totals) { it.tx / (7L * 24L) }
            }

        override suspend fun appLabels(): Map<String, String> = registry.labels()

        private fun toPackageMap(
            totals: Map<Int, Totals>,
            selector: (Totals) -> Long,
        ): Map<String, Long> {
            val apps = registry.byUid()
            val result = LinkedHashMap<String, Long>()
            totals.forEach { (uid, value) ->
                val entry = apps[uid] ?: return@forEach
                result[entry.packageName] = (result[entry.packageName] ?: 0L) + selector(value)
            }
            return result
        }

        private fun window(range: TrafficRange): Pair<Long, Long> {
            val end = System.currentTimeMillis()
            val start =
                when (range) {
                    TrafficRange.DAY, TrafficRange.LIVE -> end - 24L * HOUR_MILLIS
                    TrafficRange.WEEK -> end - SEVEN_DAYS_MILLIS
                    TrafficRange.MONTH -> end - 30L * 24L * HOUR_MILLIS
                }
            return start to end
        }

        @Suppress("DEPRECATION")
        private fun aggregate(
            startMillis: Long,
            endMillis: Long,
        ): Map<Int, Totals> {
            val manager = statsManager ?: return emptyMap()
            val result = LinkedHashMap<Int, Totals>()
            for (networkType in intArrayOf(ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE)) {
                try {
                    manager.querySummary(networkType, null, startMillis, endMillis).use { stats ->
                        val bucket = NetworkStats.Bucket()
                        while (stats.hasNextBucket()) {
                            stats.getNextBucket(bucket)
                            if (bucket.uid < 0) continue
                            val totals = result.getOrPut(bucket.uid) { Totals() }
                            totals.rx += bucket.rxBytes
                            totals.tx += bucket.txBytes
                        }
                    }
                } catch (_: SecurityException) {
                    // Usage access revoked: the UI shows a permission banner.
                    return emptyMap()
                } catch (_: Exception) {
                    // NetworkStatsManager is flaky on some OEM builds; skip this transport.
                }
            }
            return result
        }

        private class Totals {
            var rx: Long = 0L
            var tx: Long = 0L
        }

        private companion object {
            const val HOUR_MILLIS: Long = 60L * 60L * 1000L
            const val SEVEN_DAYS_MILLIS: Long = 7L * 24L * HOUR_MILLIS
        }
    }
