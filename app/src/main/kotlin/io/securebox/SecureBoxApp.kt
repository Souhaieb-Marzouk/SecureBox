package io.securebox

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import io.securebox.work.AnomalyCheckWorker
import io.securebox.work.ScanWorker
import io.securebox.work.ThreatFeedWorker
import io.securebox.work.UpdateCheckWorker
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class SecureBoxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createAlertChannel()
        scheduleAnomalyChecks()
        scheduleWeeklyScan()
        scheduleUpdateChecks()
        scheduleThreatFeed()
    }

    /**
     * Alerts are local notifications only. The channel is created up-front so the first alert can
     * never be silently dropped, and its importance stays at DEFAULT (no sound spam on TV boxes).
     */
    private fun createAlertChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_SECURITY_ALERTS,
                getString(R.string.notification_channel_alerts),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.notification_channel_alerts_description)
                setShowBadge(true)
            }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Hourly upload-anomaly check. WorkManager persists across reboots and Doze. */
    private fun scheduleAnomalyChecks() {
        val request =
            PeriodicWorkRequestBuilder<AnomalyCheckWorker>(1, TimeUnit.HOURS)
                .build()
        WorkManager
            .getInstance(this)
            .enqueueUniquePeriodicWork(
                AnomalyCheckWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }

    /** Weekly deep scan. The worker re-checks the user's preference before doing anything. */
    private fun scheduleWeeklyScan() {
        val request = PeriodicWorkRequestBuilder<ScanWorker>(7, TimeUnit.DAYS).build()
        WorkManager
            .getInstance(this)
            .enqueueUniquePeriodicWork(
                ScanWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }

    /** Daily update check; silent when offline or already current. */
    private fun scheduleUpdateChecks() {
        val request =
            PeriodicWorkRequestBuilder<UpdateCheckWorker>(24, TimeUnit.HOURS)
                .setConstraints(NETWORK_CONSTRAINTS)
                .build()
        WorkManager
            .getInstance(this)
            .enqueueUniquePeriodicWork(
                UpdateCheckWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }

    /** Daily signed-feed refresh; the worker re-checks both feed preferences. */
    private fun scheduleThreatFeed() {
        val request =
            PeriodicWorkRequestBuilder<ThreatFeedWorker>(24, TimeUnit.HOURS)
                .setConstraints(NETWORK_CONSTRAINTS)
                .build()
        WorkManager
            .getInstance(this)
            .enqueueUniquePeriodicWork(
                ThreatFeedWorker.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }

    companion object {
        const val CHANNEL_SECURITY_ALERTS: String = "security_alerts"

        private val NETWORK_CONSTRAINTS: Constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    }
}
