package io.securebox.core.data.network

import android.net.VpnService
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.SocketFactory
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OkHttp socket factory that keeps the DNS shield's own DoH traffic out of its VPN tunnel. Without
 * protect(), the resolver request would be routed back into the tunnel and loop.
 */
class ProtectedSocketFactory(
    private val vpn: VpnService,
) : SocketFactory() {
    override fun createSocket(): Socket = Socket().also { vpn.protect(it) }

    override fun createSocket(
        host: String?,
        port: Int,
    ): Socket = createSocket().apply { connect(InetSocketAddress(host, port)) }

    override fun createSocket(
        host: String?,
        port: Int,
        localHost: InetAddress?,
        localPort: Int,
    ): Socket =
        createSocket().apply {
            bind(InetSocketAddress(localHost, localPort))
            connect(InetSocketAddress(host, port))
        }

    override fun createSocket(
        host: InetAddress?,
        port: Int,
    ): Socket = createSocket().apply { connect(InetSocketAddress(host, port)) }

    override fun createSocket(
        host: InetAddress?,
        port: Int,
        localAddress: InetAddress?,
        localPort: Int,
    ): Socket =
        createSocket().apply {
            bind(InetSocketAddress(localAddress, localPort))
            connect(InetSocketAddress(host, port))
        }
}

/**
 * Forwards a raw DNS wire query to a DoH endpoint. The endpoint host is resolved before the VPN is
 * established and supplied here, so the resolver never needs to look up its own hostname through
 * the tunnel.
 */
class DohWireClient(
    vpn: VpnService,
    private val endpoint: String,
    private val endpointHost: String,
    private val endpointAddresses: List<InetAddress>,
) {
    private val client =
        OkHttpClient
            .Builder()
            .socketFactory(ProtectedSocketFactory(vpn))
            .dns(
                object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> =
                        if (hostname.equals(endpointHost, ignoreCase = true)) {
                            endpointAddresses
                        } else {
                            Dns.SYSTEM.lookup(hostname)
                        }
                },
            ).callTimeout(6, TimeUnit.SECONDS)
            .connectTimeout(4, TimeUnit.SECONDS)
            .build()

    fun forward(query: ByteArray): ByteArray? =
        runCatching {
            val request =
                Request
                    .Builder()
                    .url(endpoint)
                    .header("Accept", "application/dns-message")
                    .post(query.toRequestBody("application/dns-message".toMediaType()))
                    .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.bytes()
            }
        }.getOrNull()
}
