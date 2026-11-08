package io.securebox.core.data.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import io.securebox.core.data.R
import io.securebox.core.domain.model.DnsPreset
import io.securebox.core.domain.network.DnsBlocklist
import io.securebox.core.domain.repository.SettingsRepository
import io.securebox.core.domain.shield.ShieldStatsRepository
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * DNS-only VPN.
 *
 * It adds a private DNS server address and a route for that address only. Android sends DNS queries
 * into this tunnel, while all other app traffic keeps using the normal network. The service resolves
 * queries through the selected DoH provider and blocks the local ad/tracker list first.
 *
 * This is the alternative to Android Private DNS, which is unavailable on some TV boxes. It needs
 * the one-time Android VPN permission, but not root and not a Private DNS hostname.
 */
@AndroidEntryPoint
class DnsShieldVpnService : VpnService() {
    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var statsRepository: ShieldStatsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pendingDnsBlocks = AtomicLong()
    private var tunnel: ParcelFileDescriptor? = null
    private var pump: Thread? = null
    private var startJob: Job? = null
    private var doh: DohWireClient? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        ensureChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground()
        startJob?.cancel()
        startJob =
            scope.launch {
                val preset = runCatching { settingsRepository.settings.first().dnsPreset }.getOrDefault(DnsPreset.ADGUARD)
                val host = runCatching { URI(preset.dohUrl).host }.getOrNull()
                if (host.isNullOrBlank()) {
                    stopSelf()
                    return@launch
                }
                // Resolve the DoH hostname before the tunnel exists; this avoids a DNS loop.
                val addresses =
                    runCatching {
                        InetAddress.getAllByName(host).filterIsInstance<Inet4Address>()
                    }.getOrDefault(emptyList())
                if (addresses.isEmpty()) {
                    stopSelf()
                    return@launch
                }
                establish(preset, host, addresses)
            }
        return START_STICKY
    }

    override fun onRevoke() {
        stopSelf()
        super.onRevoke()
    }

    override fun onDestroy() {
        startJob?.cancel()
        closeTunnel()
        val pending = pendingDnsBlocks.getAndSet(0)
        if (pending > 0) {
            runBlocking { statsRepository.add(dns = pending) }
        }
        isRunning = false
        scope.cancel()
        super.onDestroy()
    }

    private fun establish(
        preset: DnsPreset,
        host: String,
        addresses: List<InetAddress>,
    ) {
        synchronized(this) {
            closeTunnel()
            val builder =
                Builder()
                    .setSession(getString(R.string.dns_shield_session_name))
                    .addAddress(DNS_CLIENT_ADDRESS, 24)
                    .addDnsServer(DNS_SERVER_ADDRESS)
                    .addRoute(DNS_SERVER_ADDRESS, 32)
                    .setBlocking(true)

            // Some Android builds keep using the underlying network's resolver even when a VPN is
            // up. Route those resolver IPs into the tunnel too, so DNS is captured without routing
            // general app traffic (which would require a full TCP/UDP forwarder).
            runCatching {
                val manager = getSystemService(ConnectivityManager::class.java)
                val properties = manager?.getLinkProperties(manager.activeNetwork)
                properties?.dnsServers
                    ?.filterIsInstance<Inet4Address>()
                    ?.forEach { address ->
                        runCatching { builder.addRoute(address.hostAddress ?: return@forEach, 32) }
                    }
            }

            val descriptor = runCatching { builder.establish() }.getOrNull()
            if (descriptor == null) {
                stopSelf()
                return
            }
            tunnel = descriptor
            doh = DohWireClient(this, preset.dohUrl, host, addresses)
            startPump(descriptor)
        }
    }

    private fun startPump(descriptor: ParcelFileDescriptor) {
        pump =
            Thread {
                val buffer = ByteArray(32 * 1024)
                try {
                    FileInputStream(descriptor.fileDescriptor).use { input ->
                        FileOutputStream(descriptor.fileDescriptor).use { output ->
                            while (!Thread.currentThread().isInterrupted) {
                                val read = input.read(buffer)
                                if (read <= 0) break
                                handlePacket(buffer, read, output)
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Tunnel was closed, or the packet was malformed.
                }
            }.apply {
                name = "securebox-dns-shield"
                isDaemon = true
                start()
            }
    }

    private fun handlePacket(
        buffer: ByteArray,
        length: Int,
        output: FileOutputStream,
    ) {
        if (length < IP_HEADER_MIN + UDP_HEADER) return
        if ((buffer[0].toInt() ushr 4) != 4) return
        val ihl = (buffer[0].toInt() and 0x0f) * 4
        if (ihl < IP_HEADER_MIN || ihl + UDP_HEADER > length) return
        if ((buffer[9].toInt() and 0xff) != 17) return

        val sourceIp = buffer.copyOfRange(12, 16)
        val destinationIp = buffer.copyOfRange(16, 20)
        val sourcePort = u16(buffer, ihl)
        val destinationPort = u16(buffer, ihl + 2)
        if (destinationPort != 53) return
        val udpLength = u16(buffer, ihl + 4)
        if (udpLength < UDP_HEADER) return
        val dnsOffset = ihl + UDP_HEADER
        val dnsLength = udpLength - UDP_HEADER
        if (dnsOffset + dnsLength > length) return
        val query = buffer.copyOfRange(dnsOffset, dnsOffset + dnsLength)
        val question = parseQuestion(query) ?: return

        val response =
            if (DnsBlocklist.isBlocked(question.name)) {
                recordDnsBlock()
                buildDnsError(query, rcode = 3)
            } else {
                doh?.forward(query) ?: buildDnsError(query, rcode = 2)
            }
        if (response.isEmpty()) return

        val packet =
            buildResponsePacket(
                dns = response,
                sourceIp = destinationIp,
                destinationIp = sourceIp,
                sourcePort = destinationPort,
                destinationPort = sourcePort,
            )
        output.write(packet)
    }

    private fun buildResponsePacket(
        dns: ByteArray,
        sourceIp: ByteArray,
        destinationIp: ByteArray,
        sourcePort: Int,
        destinationPort: Int,
    ): ByteArray {
        val totalLength = IP_HEADER_MIN + UDP_HEADER + dns.size
        val packet = ByteArray(totalLength)
        packet[0] = 0x45
        packet[2] = (totalLength ushr 8).toByte()
        packet[3] = totalLength.toByte()
        packet[6] = 0x40 // Don't fragment
        packet[8] = 64
        packet[9] = 17
        sourceIp.copyInto(packet, 12)
        destinationIp.copyInto(packet, 16)
        val checksum = ipChecksum(packet, 0, IP_HEADER_MIN)
        packet[10] = (checksum ushr 8).toByte()
        packet[11] = checksum.toByte()

        val udp = IP_HEADER_MIN
        packet[udp] = (sourcePort ushr 8).toByte()
        packet[udp + 1] = sourcePort.toByte()
        packet[udp + 2] = (destinationPort ushr 8).toByte()
        packet[udp + 3] = destinationPort.toByte()
        val udpLength = UDP_HEADER + dns.size
        packet[udp + 4] = (udpLength ushr 8).toByte()
        packet[udp + 5] = udpLength.toByte()
        // UDP checksum is optional in IPv4; 0 means "not computed".
        packet[udp + 6] = 0
        packet[udp + 7] = 0
        dns.copyInto(packet, udp + UDP_HEADER)
        return packet
    }

    private fun buildDnsError(
        query: ByteArray,
        rcode: Int,
    ): ByteArray {
        if (query.size < 12) return ByteArray(0)
        val response = query.copyOf()
        response[2] = 0x81.toByte() // QR + recursion desired
        response[3] = (0x80 or (rcode and 0x0f)).toByte() // recursion available + rcode
        response[6] = 0
        response[7] = 0
        response[8] = 0
        response[9] = 0
        response[10] = 0
        response[11] = 0
        return response
    }

    private data class Question(
        val name: String,
    )

    private fun parseQuestion(query: ByteArray): Question? {
        if (query.size < 13) return null
        var offset = 12
        val labels = mutableListOf<String>()
        while (offset < query.size) {
            val length = query[offset].toInt() and 0xff
            offset++
            if (length == 0) break
            if (length and 0xc0 != 0 || offset + length > query.size) return null
            labels += String(query, offset, length, Charsets.US_ASCII)
            offset += length
        }
        if (labels.isEmpty()) return null
        return Question(labels.joinToString("."))
    }

    private fun recordDnsBlock() {
        val count = pendingDnsBlocks.incrementAndGet()
        if (count % 10L == 0L) flushDnsStats()
    }

    private fun flushDnsStats() {
        val count = pendingDnsBlocks.getAndSet(0)
        if (count <= 0) return
        scope.launch { statsRepository.add(dns = count) }
    }

    private fun closeTunnel() {
        pump?.interrupt()
        pump = null
        doh = null
        runCatching { tunnel?.close() }
        tunnel = null
    }

    private fun startInForeground() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val openApp =
            launchIntent?.let {
                PendingIntent.getActivity(this, 11, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            }
        val stopIntent =
            PendingIntent.getService(
                this,
                12,
                Intent(this, DnsShieldVpnService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle(getString(R.string.dns_shield_notification_title))
                .setContentText(getString(R.string.dns_shield_notification_text))
                .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.dns_shield_notification_text)))
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .apply { if (openApp != null) setContentIntent(openApp) }
                .addAction(0, getString(R.string.dns_shield_notification_stop), stopIntent)
                .build()
        val type =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.dns_shield_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.dns_shield_channel_description)
                setShowBadge(false)
            }
        manager.createNotificationChannel(channel)
    }

    private fun u16(
        data: ByteArray,
        offset: Int,
    ): Int = ((data[offset].toInt() and 0xff) shl 8) or (data[offset + 1].toInt() and 0xff)

    private fun ipChecksum(
        data: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        var sum = 0
        var index = offset
        while (index < offset + length) {
            sum += u16(data, index)
            index += 2
        }
        while (sum shr 16 != 0) sum = (sum and 0xffff) + (sum shr 16)
        return sum.inv() and 0xffff
    }

    companion object {
        const val ACTION_STOP: String = "io.securebox.action.STOP_DNS_SHIELD"

        @Volatile
        var isRunning: Boolean = false
            internal set

        private const val DNS_CLIENT_ADDRESS = "10.111.222.1"
        private const val DNS_SERVER_ADDRESS = "10.111.222.2"
        private const val IP_HEADER_MIN = 20
        private const val UDP_HEADER = 8
        private const val CHANNEL_ID = "dns_shield_status"
        private const val NOTIFICATION_ID = 4102
    }
}
