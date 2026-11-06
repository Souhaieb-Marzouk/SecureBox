package io.securebox

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SecureBoxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createAlertChannel()
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

    companion object {
        const val CHANNEL_SECURITY_ALERTS: String = "security_alerts"
    }
}
