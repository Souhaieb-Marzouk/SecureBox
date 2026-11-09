package io.securebox.core.data.network

import io.securebox.core.domain.network.ListeningPort
import io.securebox.core.domain.network.PortRepository
import io.securebox.core.domain.network.PortScanResult
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads /proc/net/{tcp,tcp6,udp,udp6} and attributes listeners to apps by UID.
 *
 * Recent Android releases restrict /proc/net for third-party apps; when the files cannot be read
 * the result reports `available = false` and the UI explains the limitation instead of pretending
 * the device is clean.
 */
@Singleton
class AndroidPortRepository
    @Inject
    constructor(
        private val registry: AppRegistry,
    ) : PortRepository {
        override suspend fun listeningPorts(): PortScanResult =
            withContext(Dispatchers.IO) {
                val apps = registry.byUid()
                val found = LinkedHashMap<Pair<Int, ListeningPort.Protocol>, ListeningPort>()
                var readable = false

                val sources =
                    listOf(
                        "/proc/net/tcp" to ListeningPort.Protocol.TCP,
                        "/proc/net/tcp6" to ListeningPort.Protocol.TCP,
                        "/proc/net/udp" to ListeningPort.Protocol.UDP,
                        "/proc/net/udp6" to ListeningPort.Protocol.UDP,
                    )

                for ((path, protocol) in sources) {
                    val lines = runCatching { File(path).readLines() }.getOrNull() ?: continue
                    readable = true
                    for (line in lines.drop(1)) {
                        parseLine(line, protocol, apps)?.let { port ->
                            found[port.port to port.protocol] = port
                        }
                    }
                }

                PortScanResult(
                    available = readable,
                    ports = found.values.sortedBy { it.port },
                )
            }

        private fun parseLine(
            line: String,
            protocol: ListeningPort.Protocol,
            apps: Map<Int, AppRegistry.Entry>,
        ): ListeningPort? {
            val parts = line.trim().split(WHITESPACE)
            if (parts.size < 8) return null

            val local = parts[1]
            val state = parts[3]
            val remote = parts[2]

            if (protocol == ListeningPort.Protocol.TCP && state != TCP_LISTEN) return null
            if (protocol == ListeningPort.Protocol.UDP && !remote.endsWith(":0000")) return null

            val port = local.substringAfterLast(':').toIntOrNull(16) ?: return null
            if (port <= 0) return null
            // Port 68 is the DHCP client, not a listening service. Listing it as internet-facing
            // alarms users without giving them anything actionable.
            if (protocol == ListeningPort.Protocol.UDP && port == 68) return null

            val address = local.substringBeforeLast(':')
            val loopback = address == IPV4_LOOPBACK || address == IPV6_LOOPBACK
            val uid = parts[7].toIntOrNull() ?: -1
            val entry = apps[uid]

            return ListeningPort(
                packageName = entry?.packageName,
                label = entry?.label ?: "UID $uid",
                uid = uid,
                port = port,
                protocol = protocol,
                exposed = !loopback,
            )
        }

        private companion object {
            val WHITESPACE: Regex = Regex("\\s+")
            const val TCP_LISTEN: String = "0A"
            const val IPV4_LOOPBACK: String = "0100007F"
            const val IPV6_LOOPBACK: String = "00000000000000000000000001000000"
        }
    }
