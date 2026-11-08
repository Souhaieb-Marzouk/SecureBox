package io.securebox.core.data.shield

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import io.securebox.core.domain.shield.ShieldStats
import io.securebox.core.domain.shield.ShieldStatsRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class ShieldStatsRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : ShieldStatsRepository {
        override val stats: Flow<ShieldStats> =
            dataStore.data
                .catch { error ->
                    if (error is IOException) emit(emptyPreferences()) else throw error
                }.map { preferences ->
                    ShieldStats(
                        adsBlocked = preferences[Keys.ADS] ?: 0L,
                        trackersBlocked = preferences[Keys.TRACKERS] ?: 0L,
                        annoyancesCleaned = preferences[Keys.ANNOYANCES] ?: 0L,
                        maliciousBlocked = preferences[Keys.MALICIOUS] ?: 0L,
                        dnsBlocked = preferences[Keys.DNS] ?: 0L,
                    )
                }

        override suspend fun add(
            ads: Long,
            trackers: Long,
            annoyances: Long,
            malicious: Long,
            dns: Long,
        ) {
            if (ads == 0L && trackers == 0L && annoyances == 0L && malicious == 0L && dns == 0L) return
            dataStore.edit {
                it[Keys.ADS] = (it[Keys.ADS] ?: 0L) + ads
                it[Keys.TRACKERS] = (it[Keys.TRACKERS] ?: 0L) + trackers
                it[Keys.ANNOYANCES] = (it[Keys.ANNOYANCES] ?: 0L) + annoyances
                it[Keys.MALICIOUS] = (it[Keys.MALICIOUS] ?: 0L) + malicious
                it[Keys.DNS] = (it[Keys.DNS] ?: 0L) + dns
            }
        }

        override suspend fun reset() {
            dataStore.edit {
                it.remove(Keys.ADS)
                it.remove(Keys.TRACKERS)
                it.remove(Keys.ANNOYANCES)
                it.remove(Keys.MALICIOUS)
                it.remove(Keys.DNS)
            }
        }

        private object Keys {
            val ADS = longPreferencesKey("shield_ads_blocked")
            val TRACKERS = longPreferencesKey("shield_trackers_blocked")
            val ANNOYANCES = longPreferencesKey("shield_annoyances_cleaned")
            val MALICIOUS = longPreferencesKey("shield_malicious_blocked")
            val DNS = longPreferencesKey("shield_dns_blocked")
        }
    }
