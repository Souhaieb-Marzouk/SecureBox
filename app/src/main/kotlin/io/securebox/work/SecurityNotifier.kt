package io.securebox.work

import android.app.PendingIntent
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.securebox.R
import io.securebox.SecureBoxApp

/** Local security notifications: scan results and available updates. */
object SecurityNotifier {
    fun notifyScanResult(
        context: Context,
        highRiskApps: Int,
        findings: Int,
    ) {
        val body = context.getString(R.string.scan_notification_body, highRiskApps, findings)
        post(
            context = context,
            id = NOTIFICATION_SCAN,
            title = context.getString(R.string.scan_notification_title, highRiskApps),
            body = body,
        )
    }

    fun notifyUpdateAvailable(
        context: Context,
        tag: String,
    ) {
        post(
            context = context,
            id = NOTIFICATION_UPDATE,
            title = context.getString(R.string.update_notification_title, tag),
            body = context.getString(R.string.update_notification_body),
        )
    }

    @SuppressLint("MissingPermission")
    private fun post(
        context: Context,
        id: Int,
        title: String,
        body: String,
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val open =
            PendingIntent.getActivity(
                context,
                id,
                context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent(),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val notification =
            NotificationCompat
                .Builder(context, SecureBoxApp.CHANNEL_SECURITY_ALERTS)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build()
        runCatching { manager.notify(id, notification) }
    }

    private const val NOTIFICATION_SCAN = 4201
    private const val NOTIFICATION_UPDATE = 4202
}
