package io.securebox.core.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import io.securebox.core.data.R
import io.securebox.core.domain.network.FirewallPreset
import io.securebox.core.domain.network.FirewallRepository
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Local-only firewall.
 *
 * The blocked apps are the only ones allowed into the tunnel; every packet they send is discarded
 * before it can reach the network. All other apps bypass the VPN and work normally. Nothing is
 * forwarded anywhere, so no traffic ever leaves the device through SecureBox.
 */
@AndroidEntryPoint
class SecureBoxVpnService : VpnService() {
    @Inject
    lateinit var firewallRepository: FirewallRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunnel: ParcelFileDescriptor? = null
    private var pump: Thread? = null
    private var lastBlocked: Set<String>? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        ensureChannel()
        firewallRepository.setServiceRunning(true)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP) {
            scope.launch {
                firewallRepository.setEnabled(false)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        startInForeground()

        scope.launch {
            firewallRepository.state.collectLatest { state ->
                if (!state.enabled) {
                    stopSelf()
                    return@collectLatest
                }
                establish(state.effectiveBlockedPackages)
            }
        }
        return START_STICKY
    }

    override fun onRevoke() {
        scope.launch { firewallRepository.setEnabled(false) }
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        closeTunnel()
        isRunning = false
        firewallRepository.setServiceRunning(false)
        scope.cancel()
        super.onDestroy()
    }

    private fun establish(blocked: Set<String>) {
        if (blocked.isEmpty()) {
            // A VPN with no allowed applications would capture the whole device; never do that.
            stopSelf()
            return
        }
        if (blocked == lastBlocked && tunnel != null) return

        closeTunnel()

        val builder =
            Builder()
                .setSession(getString(R.string.vpn_session_name))
                .addAddress("10.111.0.1", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("10.111.0.2")
                .setBlocking(true)

        var added = 0
        for (packageName in blocked) {
            val result = runCatching { builder.addAllowedApplication(packageName) }
            if (result.isSuccess) added++
        }

        // If every package was uninstalled, establishing would route all traffic and block it.
        if (added == 0) {
            stopSelf()
            return
        }

        val descriptor = runCatching { builder.establish() }.getOrNull()
        if (descriptor == null) {
            stopSelf()
            return
        }

        tunnel = descriptor
        lastBlocked = blocked
        startPump(descriptor)
    }

    private fun startPump(descriptor: ParcelFileDescriptor) {
        pump =
            Thread {
                val buffer = ByteArray(32 * 1024)
                try {
                    FileInputStream(descriptor.fileDescriptor).use { input ->
                        while (!Thread.currentThread().isInterrupted) {
                            if (input.read(buffer) < 0) break
                        }
                    }
                } catch (_: IOException) {
                    // Tunnel closed.
                }
            }.apply {
                name = "securebox-vpn-pump"
                isDaemon = true
                start()
            }
    }

    private fun closeTunnel() {
        pump?.interrupt()
        pump = null
        runCatching { tunnel?.close() }
        tunnel = null
        lastBlocked = null
    }

    private fun startInForeground() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val openApp =
            launchIntent?.let {
                PendingIntent.getActivity(this, 1, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            }
        val stopIntent =
            PendingIntent.getService(
                this,
                2,
                Intent(this, SecureBoxVpnService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle(getString(R.string.vpn_notification_title))
                .setContentText(getString(R.string.vpn_notification_text))
                .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.vpn_notification_text)))
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .apply { if (openApp != null) setContentIntent(openApp) }
                .addAction(0, getString(R.string.vpn_notification_stop), stopIntent)
                .build()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.vpn_channel_description)
                setShowBadge(false)
            }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_STOP: String = "io.securebox.action.STOP_FIREWALL"
        private const val CHANNEL_ID = "firewall_status"
        private const val NOTIFICATION_ID = 4101

        @Volatile
        var isRunning: Boolean = false
            internal set
    }
}
