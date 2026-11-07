package io.securebox.core.data.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.securebox.core.domain.network.AnomalyEvent
import io.securebox.core.domain.network.AnomalyKind
import io.securebox.core.domain.network.AnomalyRepository
import io.securebox.core.domain.network.AnomalySnapshot
import io.securebox.core.domain.network.DetectAnomaliesUseCase
import io.securebox.core.domain.network.TrafficRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class AnomalyRepositoryImpl
    @Inject
    constructor(
        private val traffic: TrafficRepository,
        private val dataStore: DataStore<Preferences>,
        private val detector: DetectAnomaliesUseCase,
    ) : AnomalyRepository {
        override suspend fun runCheck(): List<AnomalyEvent> {
            val now = System.currentTimeMillis()
            val observed = traffic.hourlyUploads(now)
            val baseline = traffic.baselineHourlyUploads(now)
            val labels = traffic.appLabels()
            val events = detector(now, observed, baseline, labels)
            record(events, now)
            return events
        }

        override suspend fun record(
            events: List<AnomalyEvent>,
            checkedAtMillis: Long,
        ) {
            dataStore.edit { preferences ->
                preferences[Keys.LAST_CHECK] = checkedAtMillis
                preferences[Keys.EVENTS] = encode(events.take(MAX_STORED_EVENTS))
            }
        }

        override suspend fun snapshot(): AnomalySnapshot {
            val preferences = dataStore.data.first()
            return AnomalySnapshot(
                lastCheckedAtMillis = preferences[Keys.LAST_CHECK],
                events = decode(preferences[Keys.EVENTS]),
            )
        }

        private fun encode(events: List<AnomalyEvent>): String =
            JSONArray().apply {
                events.forEach { event ->
                    put(
                        JSONObject().apply {
                            put("packageName", event.packageName)
                            put("label", event.label)
                            put("kind", event.kind.name)
                            put("observedBytes", event.observedBytes)
                            put("baselineBytesPerHour", event.baselineBytesPerHour)
                            put("atMillis", event.atMillis)
                        },
                    )
                }
            }.toString()

        private fun decode(raw: String?): List<AnomalyEvent> {
            if (raw.isNullOrBlank()) return emptyList()
            return runCatching {
                val array = JSONArray(raw)
                buildList {
                    for (index in 0 until array.length()) {
                        val item = array.getJSONObject(index)
                        add(
                            AnomalyEvent(
                                packageName = item.getString("packageName"),
                                label = item.optString("label", item.getString("packageName")),
                                kind =
                                    runCatching {
                                        AnomalyKind.valueOf(item.optString("kind", AnomalyKind.UPLOAD_SPIKE.name))
                                    }.getOrDefault(AnomalyKind.UPLOAD_SPIKE),
                                observedBytes = item.optLong("observedBytes", 0L),
                                baselineBytesPerHour = item.optLong("baselineBytesPerHour", 0L),
                                atMillis = item.optLong("atMillis", 0L),
                            ),
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }

        private object Keys {
            val LAST_CHECK = longPreferencesKey("anomaly_last_check")
            val EVENTS = stringPreferencesKey("anomaly_recent_events")
        }

        private companion object {
            const val MAX_STORED_EVENTS: Int = 10
        }
    }
