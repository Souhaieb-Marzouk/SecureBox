package io.securebox.work

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.securebox.R
import io.securebox.SecureBoxApp
import io.securebox.core.domain.network.AnomalyEvent

/** Posts one local notification per suspicious upload, with Block and Uninstall actions. */
object AnomalyNotifier {
    // POST_NOTIFICATIONS is checked in canPost() and notify() is wrapped in runCatching.
    @SuppressLint("MissingPermission")
    fun post(
        context: Context,
        event: AnomalyEvent,
    ) {
        if (!canPost(context)) return
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val openApp =
            PendingIntent.getActivity(
                context,
                1,
                context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent(),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val blockIntent =
            Intent(context, AnomalyActionReceiver::class.java)
                .setAction(AnomalyActionReceiver.ACTION_BLOCK)
                .putExtra(AnomalyActionReceiver.EXTRA_PACKAGE, event.packageName)
        val block =
            PendingIntent.getBroadcast(
                context,
                event.packageName.hashCode(),
                blockIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val uninstall =
            PendingIntent.getActivity(
                context,
                event.packageName.hashCode() + 1,
                Intent(Intent.ACTION_DELETE, Uri.parse("package:${event.packageName}")),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val size = Formatter.formatFileSize(context, event.observedBytes)
        val body = context.getString(R.string.anomaly_notification_body, event.label, size)

        val notification =
            NotificationCompat
                .Builder(context, SecureBoxApp.CHANNEL_SECURITY_ALERTS)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(context.getString(R.string.anomaly_notification_title, event.label))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .addAction(0, context.getString(R.string.anomaly_action_block), block)
                .addAction(0, context.getString(R.string.anomaly_action_uninstall), uninstall)
                .build()

        runCatching { manager.notify(event.packageName.hashCode(), notification) }
    }

    private fun canPost(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
}
