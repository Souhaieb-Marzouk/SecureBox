package io.securebox.core.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import android.util.Log
import io.securebox.core.domain.network.DnsBlocklist
import io.securebox.core.domain.shield.ShieldStatsRepository
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * All-app forwarding data plane for the combined firewall + DNS mode.
 *
 * This is deliberately a separate engine from the old "drop blocked apps" service. It captures every
 * IPv4 packet, forwards allowed TCP/UDP flows through protected sockets, drops flows belonging to
 * blocked UIDs, and answers DNS through the DoH client. IPv6 is routed into the tunnel and dropped;
 * apps fall back to IPv4. The privacy model changes here: allowed packets are relayed by SecureBox.
 */
class CombinedTunnelEngine(
    private val vpn: VpnService,
    private val descriptor: ParcelFileDescriptor,
    private val doh: DohWireClient,
    private val stats: ShieldStatsRepository,
    private val scope: CoroutineScope,
) {
    private val output = FileOutputStream(descriptor.fileDescriptor)
    private val writeLock = ReentrantLock()
    private val running = AtomicBoolean(true)
    private val blockedUids = AtomicReference<Set<Int>>(emptySet())
    private val dnsFilterEnabled = AtomicBoolean(true)
    private val pendingDnsBlocks = AtomicLong()
    private val packetCount = AtomicLong()
    private val tcpFlows = ConcurrentHashMap<FlowKey, TcpFlow>()
    private val udpFlows = ConcurrentHashMap<FlowKey, UdpFlow>()
    private val uidResolver = UidResolver(vpn)
    private val random = Random(System.nanoTime())

    fun update(
        blocked: Set<Int>,
        dnsFilter: Boolean,
    ) {
        blockedUids.set(blocked)
        dnsFilterEnabled.set(dnsFilter)
    }

    fun start() {
        thread(name = "securebox-tunnel", isDaemon = true) {
            val buffer = ByteArray(32 * 1024)
            try {
                FileInputStream(descriptor.fileDescriptor).use { input ->
                    while (running.get()) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        handlePacket(buffer, read)
                    }
                }
            } catch (error: Exception) {
                Log.w(TAG, "tun reader stopped", error)
            }
        }
        thread(name = "securebox-tunnel-cleanup", isDaemon = true) {
            while (running.get()) {
                try {
                    Thread.sleep(30_000)
                } catch (_: InterruptedException) {
                    break
                }
                val cutoff = System.currentTimeMillis() - 180_000
                tcpFlows.entries.removeIf { (_, flow) ->
                    if (flow.lastActivity < cutoff) {
                        flow.close()
                        true
                    } else {
                        false
                    }
                }
                udpFlows.entries.removeIf { (_, flow) ->
                    if (flow.lastActivity < cutoff) {
                        flow.close()
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    fun close() {
        running.set(false)
        tcpFlows.values.forEach { it.close() }
        udpFlows.values.forEach { it.close() }
        tcpFlows.clear()
        udpFlows.clear()
        flushDnsStats()
        runCatching { output.close() }
        runCatching { descriptor.close() }
    }

    private fun handlePacket(
        buffer: ByteArray,
        length: Int,
    ) {
        if (length < IP_HEADER_MIN + 8) return
        if ((buffer[0].toInt() ushr 4) != 4) return
        val ihl = (buffer[0].toInt() and 0x0f) * 4
        if (ihl < IP_HEADER_MIN || ihl >= length) return
        val totalLength = u16(buffer, 2)
        if (totalLength > length) return
        val protocol = buffer[9].toInt() and 0xff
        val srcIp = buffer.copyOfRange(12, 16)
        val dstIp = buffer.copyOfRange(16, 20)
        if (packetCount.incrementAndGet() <= 10) {
            Log.i(TAG, "packet proto=$protocol ${ipText(srcIp)} -> ${ipText(dstIp)}")
        }
        when (protocol) {
            PROTO_TCP -> handleTcp(buffer, ihl, totalLength, srcIp, dstIp)
            PROTO_UDP -> handleUdp(buffer, ihl, totalLength, srcIp, dstIp)
        }
    }

    private fun handleTcp(
        buffer: ByteArray,
        ihl: Int,
        totalLength: Int,
        srcIp: ByteArray,
        dstIp: ByteArray,
    ) {
        if (ihl + TCP_HEADER > totalLength) return
        val srcPort = u16(buffer, ihl)
        val dstPort = u16(buffer, ihl + 2)
        if (srcPort == 0 || dstPort == 0) return
        val dataOffset = ((buffer[ihl + 12].toInt() ushr 4) and 0x0f) * 4
        if (dataOffset < TCP_HEADER || ihl + dataOffset > totalLength) return
        val seq = u32(buffer, ihl + 4)
        val ack = u32(buffer, ihl + 8)
        val flags = buffer[ihl + 13].toInt() and 0xff
        val payload = buffer.copyOfRange(ihl + dataOffset, totalLength)
        val key = FlowKey(ipText(srcIp), srcPort, ipText(dstIp), dstPort, PROTO_TCP)
        val blocked = isBlocked(key)
        if (blocked) {
            if ((flags and SYN) != 0) sendRst(srcIp, dstIp, srcPort, dstPort, ack, seq + 1)
            return
        }
        if ((flags and SYN) != 0) Log.i(TAG, "tcp syn :$dstPort")
        val flow = tcpFlows[key] ?: TcpFlow(key, srcIp, dstIp, srcPort, dstPort).also { tcpFlows[key] = it }
        if (flow.isClosed()) {
            tcpFlows.remove(key)
            tcpFlows[key] = TcpFlow(key, srcIp, dstIp, srcPort, dstPort)
        }
        tcpFlows[key]?.onPacket(seq, ack, flags, payload)
    }

    private fun handleUdp(
        buffer: ByteArray,
        ihl: Int,
        totalLength: Int,
        srcIp: ByteArray,
        dstIp: ByteArray,
    ) {
        if (ihl + UDP_HEADER > totalLength) return
        val srcPort = u16(buffer, ihl)
        val dstPort = u16(buffer, ihl + 2)
        val udpLength = u16(buffer, ihl + 4)
        if (udpLength < UDP_HEADER || ihl + udpLength > totalLength) return
        val payload = buffer.copyOfRange(ihl + UDP_HEADER, ihl + udpLength)
        val key = FlowKey(ipText(srcIp), srcPort, ipText(dstIp), dstPort, PROTO_UDP)
        if (isBlocked(key)) return
        if (dstPort == 53) {
            handleDns(srcIp, dstIp, srcPort, dstPort, payload)
            return
        }
        var flow = udpFlows[key]
        if (flow == null || flow.isClosed()) {
            udpFlows.remove(key)
            flow = runCatching { UdpFlow(key, srcIp, dstIp, srcPort, dstPort) }.getOrNull() ?: return
            udpFlows[key] = flow
        }
        flow.send(payload)
    }

    private fun handleDns(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        query: ByteArray,
    ) {
        val name = parseQuestionName(query)
        val response =
            if (dnsFilterEnabled.get() && DnsBlocklist.isBlocked(name)) {
                recordDnsBlock()
                buildDnsError(query, 3)
            } else {
                doh.forward(query) ?: fallbackDns(query) ?: buildDnsError(query, 2)
            }
        Log.i(TAG, "dns query len=${query.size} response=${response.size}")
        if (response.isNotEmpty()) {
            writeUdp(dstIp, srcIp, dstPort, srcPort, response)
        }
    }

    /**
     * Last-resort resolver when DoH is unreachable. Protected plaintext DNS prevents a broken DoH
     * path from making the whole tunnel look offline. Local blocklist filtering already ran.
     */
    private fun fallbackDns(query: ByteArray): ByteArray? {
        val target = runCatching { InetAddress.getByName("1.1.1.1") }.getOrNull() ?: return null
        val socket = DatagramSocket()
        return runCatching {
            if (!vpn.protect(socket)) {
                Log.w(TAG, "dns fallback protect returned false; binding underlying network")
                bindUnderlying(socket)
            }
            socket.soTimeout = 3_000
            socket.send(DatagramPacket(query, query.size, target, 53))
            val buffer = ByteArray(4_096)
            val response = DatagramPacket(buffer, buffer.size)
            socket.receive(response)
            buffer.copyOf(response.length)
        }.onFailure { Log.w(TAG, "plain DNS fallback failed") }
            .getOrNull()
            .also { socket.close() }
    }

    private fun isBlocked(key: FlowKey): Boolean {
        val blocked = blockedUids.get()
        if (blocked.isEmpty()) return false
        val uid = uidResolver.resolve(key)
        return uid >= 0 && uid in blocked
    }

    /** bindSocket() fallback when protect() returns false on an OEM build. */
    private fun bindUnderlying(socket: Socket): Boolean {
        val manager = vpn.getSystemService(ConnectivityManager::class.java) ?: return false
        val network =
            manager.allNetworks.firstOrNull { network ->
                val capabilities = manager.getNetworkCapabilities(network) ?: return@firstOrNull false
                capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    !capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
            } ?: return false
        return runCatching { network.bindSocket(socket) }.isSuccess
    }

    private fun bindUnderlying(socket: DatagramSocket): Boolean {
        val manager = vpn.getSystemService(ConnectivityManager::class.java) ?: return false
        val network =
            manager.allNetworks.firstOrNull { network ->
                val capabilities = manager.getNetworkCapabilities(network) ?: return@firstOrNull false
                capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    !capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)
            } ?: return false
        return runCatching { network.bindSocket(socket) }.isSuccess
    }

    private fun sendTcp(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        seq: Long,
        ack: Long,
        flags: Int,
        payload: ByteArray = ByteArray(0),
        mss: Int? = null,
    ) {
        val packet = buildTcpPacket(srcIp, dstIp, srcPort, dstPort, seq, ack, flags, payload, mss)
        write(packet)
    }

    private fun sendRst(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        ack: Long,
        seq: Long,
    ) {
        // The reply reverses the original direction: source port becomes the destination port.
        sendTcp(dstIp, srcIp, dstPort, srcPort, seq, ack, RST or ACK)
    }

    private fun writeUdp(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray,
    ) {
        val total = IP_HEADER_MIN + UDP_HEADER + payload.size
        val packet = ByteArray(total)
        writeIpHeader(packet, total, PROTO_UDP, srcIp, dstIp)
        val udp = IP_HEADER_MIN
        packet[udp] = (srcPort ushr 8).toByte()
        packet[udp + 1] = srcPort.toByte()
        packet[udp + 2] = (dstPort ushr 8).toByte()
        packet[udp + 3] = dstPort.toByte()
        val udpLength = UDP_HEADER + payload.size
        packet[udp + 4] = (udpLength ushr 8).toByte()
        packet[udp + 5] = udpLength.toByte()
        payload.copyInto(packet, udp + UDP_HEADER)
        write(packet)
    }

    private fun buildTcpPacket(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        seq: Long,
        ack: Long,
        flags: Int,
        payload: ByteArray,
        mss: Int?,
    ): ByteArray {
        val options = if (mss != null) byteArrayOf(2, 4, (mss ushr 8).toByte(), mss.toByte()) else ByteArray(0)
        val tcpHeader = TCP_HEADER + options.size
        val total = IP_HEADER_MIN + tcpHeader + payload.size
        val packet = ByteArray(total)
        writeIpHeader(packet, total, PROTO_TCP, srcIp, dstIp)
        val tcp = IP_HEADER_MIN
        packet[tcp] = (srcPort ushr 8).toByte()
        packet[tcp + 1] = srcPort.toByte()
        packet[tcp + 2] = (dstPort ushr 8).toByte()
        packet[tcp + 3] = dstPort.toByte()
        writeU32(packet, tcp + 4, seq)
        writeU32(packet, tcp + 8, ack)
        packet[tcp + 12] = ((tcpHeader / 4) shl 4).toByte()
        packet[tcp + 13] = flags.toByte()
        packet[tcp + 14] = 0xff.toByte()
        packet[tcp + 15] = 0xff.toByte()
        options.copyInto(packet, tcp + TCP_HEADER)
        payload.copyInto(packet, tcp + tcpHeader)
        val checksum = tcpChecksum(srcIp, dstIp, packet, tcp, tcpHeader + payload.size)
        packet[tcp + 16] = (checksum ushr 8).toByte()
        packet[tcp + 17] = checksum.toByte()
        return packet
    }

    private fun writeIpHeader(
        packet: ByteArray,
        totalLength: Int,
        protocol: Int,
        srcIp: ByteArray,
        dstIp: ByteArray,
    ) {
        packet[0] = 0x45
        packet[2] = (totalLength ushr 8).toByte()
        packet[3] = totalLength.toByte()
        packet[6] = 0x40
        packet[8] = 64
        packet[9] = protocol.toByte()
        srcIp.copyInto(packet, 12)
        dstIp.copyInto(packet, 16)
        val checksum = ipChecksum(packet, 0, IP_HEADER_MIN)
        packet[10] = (checksum ushr 8).toByte()
        packet[11] = checksum.toByte()
    }

    private fun tcpChecksum(
        srcIp: ByteArray,
        dstIp: ByteArray,
        packet: ByteArray,
        tcpOffset: Int,
        tcpLength: Int,
    ): Int {
        var sum = 0
        var index = 0
        while (index < 4) {
            sum += u16(srcIp, index)
            index += 2
        }
        index = 0
        while (index < 4) {
            sum += u16(dstIp, index)
            index += 2
        }
        sum += PROTO_TCP
        sum += tcpLength
        var remaining = tcpLength
        index = tcpOffset
        while (remaining > 1) {
            sum += u16(packet, index)
            index += 2
            remaining -= 2
        }
        if (remaining == 1) {
            sum += (packet[index].toInt() and 0xff) shl 8
        }
        while (sum shr 16 != 0) sum = (sum and 0xffff) + (sum shr 16)
        return sum.inv() and 0xffff
    }

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

    private fun write(packet: ByteArray) {
        writeLock.lock()
        try {
            output.write(packet)
        } catch (error: Exception) {
            Log.w(TAG, "tun write failed", error)
        } finally {
            writeLock.unlock()
        }
    }

    private fun recordDnsBlock() {
        val count = pendingDnsBlocks.incrementAndGet()
        if (count % 10L == 0L) flushDnsStats()
    }

    private fun flushDnsStats() {
        val count = pendingDnsBlocks.getAndSet(0)
        if (count > 0) scope.launch { stats.add(dns = count) }
    }

    private fun parseQuestionName(query: ByteArray): String? {
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
        return labels.takeIf { it.isNotEmpty() }?.joinToString(".")
    }

    private fun buildDnsError(
        query: ByteArray,
        rcode: Int,
    ): ByteArray {
        if (query.size < 12) return ByteArray(0)
        val response = query.copyOf()
        response[2] = 0x81.toByte()
        response[3] = (0x80 or (rcode and 0x0f)).toByte()
        response[6] = 0
        response[7] = 0
        response[8] = 0
        response[9] = 0
        response[10] = 0
        response[11] = 0
        return response
    }

    private fun u16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xff) shl 8) or (data[offset + 1].toInt() and 0xff)

    private fun u32(data: ByteArray, offset: Int): Long =
        ((data[offset].toLong() and 0xff) shl 24) or
            ((data[offset + 1].toLong() and 0xff) shl 16) or
            ((data[offset + 2].toLong() and 0xff) shl 8) or
            (data[offset + 3].toLong() and 0xff)

    private fun writeU32(data: ByteArray, offset: Int, value: Long) {
        data[offset] = (value ushr 24).toByte()
        data[offset + 1] = (value ushr 16).toByte()
        data[offset + 2] = (value ushr 8).toByte()
        data[offset + 3] = value.toByte()
    }

    private fun ipText(bytes: ByteArray): String =
        (bytes.indices).joinToString(".") { (bytes[it].toInt() and 0xff).toString() }

    private data class FlowKey(
        val srcIp: String,
        val srcPort: Int,
        val dstIp: String,
        val dstPort: Int,
        val protocol: Int,
    )

    private inner class TcpFlow(
        private val key: FlowKey,
        private val srcIp: ByteArray,
        private val dstIp: ByteArray,
        private val srcPort: Int,
        private val dstPort: Int,
    ) {
        private val lock = ReentrantLock()
        private val ackCondition = lock.newCondition()
        private var socket: Socket? = null
        private var appSeq: Long = 0
        private var serverIsn: Long = 0
        private var serverSeq: Long = 0
        private var serverAcked: Long = 0
        private var synAcked = false
        private var connecting = false
        private var closed = false
        @Volatile var lastActivity: Long = System.currentTimeMillis()

        fun onPacket(
            seq: Long,
            ack: Long,
            flags: Int,
            payload: ByteArray,
        ) {
            lastActivity = System.currentTimeMillis()
            if ((flags and RST) != 0) {
                close()
                return
            }
            if ((flags and SYN) != 0) {
                synchronized(this) {
                    if (synAcked) {
                        sendTcp(dstIp, srcIp, dstPort, srcPort, serverIsn, appSeq, SYN or ACK, mss = MSS)
                    } else if (!connecting) {
                        appSeq = (seq + 1) and 0xffffffff
                        connecting = true
                        connect()
                    }
                }
                return
            }
            if ((flags and ACK) != 0) {
                lock.lock()
                try {
                    if (ack > serverAcked) serverAcked = ack
                    ackCondition.signalAll()
                } finally {
                    lock.unlock()
                }
            }
            if ((flags and FIN) != 0) {
                appSeq = (appSeq + 1) and 0xffffffff
                runCatching { socket?.shutdownOutput() }
                sendTcp(dstIp, srcIp, dstPort, srcPort, serverSeq, appSeq, ACK)
                return
            }
            if (payload.isEmpty()) return
            if (seq != appSeq) {
                sendTcp(dstIp, srcIp, dstPort, srcPort, serverSeq, appSeq, ACK)
                return
            }
            val current = socket ?: return
            runCatching { current.getOutputStream().write(payload) }.onFailure { close() }
            appSeq = (appSeq + payload.size) and 0xffffffff
            sendTcp(dstIp, srcIp, dstPort, srcPort, serverSeq, appSeq, ACK)
        }

        private fun connect() {
            val target = runCatching { InetAddress.getByAddress(dstIp) }.getOrNull() ?: return
            thread(name = "securebox-tcp-connect", isDaemon = true) {
                val created = Socket()
                runCatching {
                    if (!vpn.protect(created)) {
                        Log.w(TAG, "protect returned false; binding underlying network")
                        if (!bindUnderlying(created)) Log.w(TAG, "underlying bind failed; trying connect")
                    }
                    created.connect(InetSocketAddress(target, dstPort), 10_000)
                    // TCP_NODELAY is only valid after the socket is connected; setting it before
                    // connect() throws immediately on Android and made every TCP SYN fail.
                    created.tcpNoDelay = true
                }.onFailure { error ->
                    Log.w(TAG, "tcp connect failed :$dstPort", error)
                    runCatching {
                        sendTcp(dstIp, srcIp, dstPort, srcPort, 0, appSeq, RST or ACK)
                    }
                    close()
                    return@thread
                }
                synchronized(this) {
                    socket = created
                    serverIsn = random.nextLong() and 0xffffffff
                    serverSeq = (serverIsn + 1) and 0xffffffff
                    serverAcked = serverSeq
                    synAcked = true
                    sendTcp(dstIp, srcIp, dstPort, srcPort, serverIsn, appSeq, SYN or ACK, mss = MSS)
                }
                Log.i(TAG, "tcp connected :$dstPort")
                readRemote(created)
            }
        }

        private fun readRemote(created: Socket) {
            val input = runCatching { created.getInputStream() }.getOrNull() ?: return
            val buffer = ByteArray(MAX_TCP_PAYLOAD)
            try {
                while (!closed) {
                    val read = input.read(buffer)
                    if (read < 0) {
                        sendTcp(dstIp, srcIp, dstPort, srcPort, serverSeq, appSeq, FIN or ACK)
                        close()
                        return
                    }
                    if (read == 0) continue
                    awaitWindow()
                    sendTcp(dstIp, srcIp, dstPort, srcPort, serverSeq, appSeq, PSH or ACK, buffer.copyOf(read))
                    serverSeq = (serverSeq + read) and 0xffffffff
                }
            } catch (_: Exception) {
                close()
            }
        }

        private fun awaitWindow() {
            lock.lock()
            try {
                var waited = 0L
                while (!closed && serverSeq - serverAcked > TCP_WINDOW) {
                    if (waited > 15_000) break
                    ackCondition.await(500, java.util.concurrent.TimeUnit.MILLISECONDS)
                    waited += 500
                }
            } finally {
                lock.unlock()
            }
        }

        fun close() {
            closed = true
            lock.lock()
            try {
                ackCondition.signalAll()
            } finally {
                lock.unlock()
            }
            runCatching { socket?.close() }
            socket = null
        }

        fun isClosed(): Boolean = closed
    }

    private inner class UdpFlow(
        private val key: FlowKey,
        private val srcIp: ByteArray,
        private val dstIp: ByteArray,
        private val srcPort: Int,
        private val dstPort: Int,
    ) {
        private val socket = DatagramSocket().also { runCatching { vpn.protect(it) } }
        @Volatile var lastActivity: Long = System.currentTimeMillis()
        private val target = runCatching { InetAddress.getByAddress(dstIp) }.getOrNull()
        @Volatile private var closed = false

        init {
            if (target != null) {
                runCatching {
                    if (!vpn.protect(socket)) {
                        Log.w(TAG, "udp protect returned false; binding underlying network")
                        bindUnderlying(socket)
                    }
                    socket.connect(InetSocketAddress(target, dstPort))
                }.onFailure { close() }
                thread(name = "securebox-udp-read", isDaemon = true) {
                    val buffer = ByteArray(64 * 1024)
                    while (running.get()) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)
                            lastActivity = System.currentTimeMillis()
                            writeUdp(dstIp, srcIp, dstPort, srcPort, buffer.copyOf(packet.length))
                        } catch (_: Exception) {
                            break
                        }
                    }
                }
            }
        }

        fun send(payload: ByteArray) {
            val remote = target ?: return
            lastActivity = System.currentTimeMillis()
            runCatching {
                socket.send(DatagramPacket(payload, payload.size, remote, dstPort))
            }.onFailure { close() }
        }

        fun close() {
            closed = true
            runCatching { socket.close() }
        }

        fun isClosed(): Boolean = closed
    }

    private class UidResolver(
        private val vpn: VpnService,
    ) {
        private val connectivity = vpn.getSystemService(ConnectivityManager::class.java)

        fun resolve(key: FlowKey): Int {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && connectivity != null) {
                val uid = runCatching {
                    val protocol = if (key.protocol == PROTO_TCP) OsConstants.IPPROTO_TCP else OsConstants.IPPROTO_UDP
                    connectivity.getConnectionOwnerUid(
                        protocol,
                        InetSocketAddress(key.srcIp, key.srcPort),
                        InetSocketAddress(key.dstIp, key.dstPort),
                    )
                }.getOrDefault(-1)
                if (uid >= 0) return uid
            }
            return procUid(key)
        }

        private fun procUid(key: FlowKey): Int {
            val file = if (key.protocol == PROTO_TCP) "/proc/net/tcp" else "/proc/net/udp"
            return runCatching {
                java.io.File(file).readLines().drop(1).firstNotNullOfOrNull { line ->
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size < 8) return@firstNotNullOfOrNull null
                    val localPort = parts[1].substringAfterLast(':').toIntOrNull(16) ?: return@firstNotNullOfOrNull null
                    val remotePort = parts[2].substringAfterLast(':').toIntOrNull(16) ?: return@firstNotNullOfOrNull null
                    if (localPort == key.srcPort && remotePort == key.dstPort) {
                        parts[7].toIntOrNull()
                    } else {
                        null
                    }
                } ?: -1
            }.getOrDefault(-1)
        }
    }

    private companion object {
        const val PROTO_TCP = 6
        const val PROTO_UDP = 17
        const val IP_HEADER_MIN = 20
        const val TCP_HEADER = 20
        const val UDP_HEADER = 8
        const val MSS = 1400
        const val MAX_TCP_PAYLOAD = 1200
        const val TCP_WINDOW = 32 * 1024L

        const val FIN = 0x01
        const val SYN = 0x02
        const val RST = 0x04
        const val PSH = 0x08
        const val ACK = 0x10
        const val TAG = "SecureBoxTunnel"
    }
}
