package io.securebox.core.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import io.securebox.core.data.R
import io.securebox.core.domain.model.AppSettings
import io.securebox.core.domain.network.FirewallRepository
import io.securebox.core.domain.repository.SettingsRepository
import io.securebox.core.domain.security.ThreatFeedRepository
import io.securebox.core.domain.shield.ShieldStatsRepository
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Combined network-protection VPN.
 *
 * This is the forwarding data plane: every app is inside the tunnel, allowed TCP/UDP flows are
 * relayed through protected sockets, blocked app UIDs are dropped, and DNS is answered locally
 * through DoH. It replaces the old "only blocked apps are in the tunnel" firewall, which could not
 * filter DNS for other apps.
 *
 * Privacy change: allowed app traffic is relayed by SecureBox. It is not inspected at TLS level,
 * and no traffic is sent to a SecureBox server; forwarding happens on-device through protected
 * sockets. The UI and docs must say that plainly.
 */
@AndroidEntryPoint
class SecureBoxVpnService : VpnService() {
    @Inject
    lateinit var firewallRepository: FirewallRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var statsRepository: ShieldStatsRepository

    @Inject
    lateinit var threatFeedRepository: ThreatFeedRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunnel: ParcelFileDescriptor? = null
    private var engine: CombinedTunnelEngine? = null
    private var stateJob: Job? = null
    private var feedJob: Job? = null
    private var lastPreset: String? = null
    @Volatile private var feedDomains: Set<String> = emptySet()

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
                settingsRepository.setDnsShieldEnabled(false)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        startInForeground()
        stateJob?.cancel()
        stateJob =
            scope.launch {
                combine(firewallRepository.state, settingsRepository.settings) { firewall, settings ->
                    firewall to settings
                }.collectLatest { (firewall, settings) ->
                    if (!firewall.enabled && !settings.dnsShieldEnabled) {
                        stopSelf()
                        return@collectLatest
                    }
                    if (engine == null || lastPreset != settings.dnsPreset.name) {
                        if (!establishWithRetry(settings)) {
                            stopSelf()
                            return@collectLatest
                        }
                    }
                    engine?.update(
                        blocked = uids(firewall.effectiveBlockedPackages),
                        dnsFilter = settings.dnsShieldEnabled,
                        feedDomains = feedDomains,
                    )
                }
            }
        feedJob?.cancel()
        feedJob =
            scope.launch {
                while (true) {
                    feedDomains = runCatching { threatFeedRepository.activeFeed()?.domains?.toSet() }.getOrNull() ?: emptySet()
                    engine?.updateFeedDomains(feedDomains)
                    kotlinx.coroutines.delay(15 * 60 * 1000L)
                }
            }
        return START_STICKY
    }

    override fun onRevoke() {
        scope.launch {
            firewallRepository.setEnabled(false)
            settingsRepository.setDnsShieldEnabled(false)
        }
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        stateJob?.cancel()
        feedJob?.cancel()
        closeEngine()
        isRunning = false
        firewallRepository.setServiceRunning(false)
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun establishWithRetry(settings: AppSettings): Boolean {
        awaitNetwork()
        repeat(5) { attempt ->
            if (establish(settings)) return true
            if (attempt < 4) kotlinx.coroutines.delay(2_000)
        }
        return false
    }

    private suspend fun awaitNetwork() {
        val connectivity = getSystemService(android.net.ConnectivityManager::class.java) ?: return
        repeat(20) {
            val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
            if (capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
                return
            }
            kotlinx.coroutines.delay(1_000)
        }
    }

    private fun establish(settings: AppSettings): Boolean {
        synchronized(this) {
            closeEngine()
            val host = runCatching { URI(settings.dnsPreset.dohUrl).host }.getOrNull() ?: return false
            val addresses =
                runCatching { InetAddress.getAllByName(host).filterIsInstance<Inet4Address>() }
                    .getOrDefault(emptyList())
            if (addresses.isEmpty()) return false

            val builder =
                Builder()
                    .setSession(getString(R.string.vpn_session_name))
                    .addAddress("10.111.0.1", 32)
                    .addRoute("0.0.0.0", 0)
                    .addRoute("10.111.0.2", 32)
                    .addDnsServer("10.111.0.2")
                    .setBlocking(true)
                    .setMtu(1500)

            val descriptor = runCatching { builder.establish() }.getOrNull() ?: return false
            val doh = DohWireClient(this, settings.dnsPreset.dohUrl, host, addresses)
            val created = CombinedTunnelEngine(this, descriptor, doh, statsRepository, scope)
            tunnel = descriptor
            engine = created
            lastPreset = settings.dnsPreset.name
            created.start()
            Log.i("SecureBoxTunnel", "vpn established with ${addresses.size} DoH address(es)")
            return true
        }
    }

    private fun uids(packages: Set<String>): Set<Int> =
        packages
            .mapNotNull { packageName ->
                runCatching { packageManager.getPackageUid(packageName, 0) }.getOrNull()
            }.toSet()

    private fun closeEngine() {
        engine?.close()
        engine = null
        runCatching { tunnel?.close() }
        tunnel = null
        lastPreset = null
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
        const val ACTION_STOP: String = "io.securebox.action.STOP_NETWORK_PROTECTION"
        private const val CHANNEL_ID = "network_protection"
        private const val NOTIFICATION_ID = 4101

        @Volatile
        var isRunning: Boolean = false
            internal set
    }
}
