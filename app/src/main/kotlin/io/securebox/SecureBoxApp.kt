package io.securebox

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import io.securebox.work.AnomalyCheckWorker
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class SecureBoxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createAlertChannel()
        scheduleAnomalyChecks()
    }

    /**
     * Alerts are local notifications only. The channel is created up-front so the first alert can
     * never be silently dropped, and its importance stays at DEFAULT (no sound spam on TV boxes).
     */
    private fun createAlertChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
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

    companion object {
        const val CHANNEL_SECURITY_ALERTS: String = "security_alerts"
    }
}
