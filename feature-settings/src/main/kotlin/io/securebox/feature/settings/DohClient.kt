package io.securebox.feature.settings

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class DohResult(
    val endpoint: String,
    val addresses: List<String>,
    val latencyMs: Long,
)

/**
 * Minimal DNS-over-HTTPS probe. It sends a wire-format A query to the selected endpoint and reads
 * the A records from the wire response. This verifies that the DoH endpoint is reachable from the
 * device; Android's system-wide Private DNS still uses DoT, which the DNS screen explains.
 */
object DohClient {
    private val client =
        OkHttpClient
            .Builder()
            .callTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(6, TimeUnit.SECONDS)
            .build()

    suspend fun test(
        endpoint: String,
        host: String = "example.com",
    ): Result<DohResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val query = buildQuery(host)
                val request =
                    Request
                        .Builder()
                        .url(endpoint)
                        .header("Accept", "application/dns-message")
                        .header("User-Agent", "SecureBox-DNS/1.0")
                        .post(query.toRequestBody("application/dns-message".toMediaType()))
                        .build()
                val started = System.nanoTime()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    val bytes = response.body?.bytes() ?: error("Empty DNS response")
                    val latency = (System.nanoTime() - started) / 1_000_000
                    val addresses = parseAddresses(bytes)
                    if (addresses.isEmpty()) error("No A records in the response")
                    DohResult(endpoint = endpoint, addresses = addresses, latencyMs = latency)
                }
            }
        }

    private fun buildQuery(host: String): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.writeShort(0) // ID
            data.writeShort(0x0100) // recursion desired
            data.writeShort(1) // QDCOUNT
            data.writeShort(0) // ANCOUNT
            data.writeShort(0) // NSCOUNT
            data.writeShort(0) // ARCOUNT
            for (label in host.split('.')) {
                val bytes = label.toByteArray(Charsets.UTF_8)
                data.writeByte(bytes.size)
                data.write(bytes)
            }
            data.writeByte(0)
            data.writeShort(1) // A
            data.writeShort(1) // IN
        }
        return output.toByteArray()
    }

    private fun parseAddresses(data: ByteArray): List<String> {
        if (data.size < 12) return emptyList()

        fun u16(offset: Int): Int =
            ((data[offset].toInt() and 0xff) shl 8) or (data[offset + 1].toInt() and 0xff)

        val questions = u16(4)
        val answers = u16(6)
        var offset = 12

        repeat(questions) {
            offset = skipName(data, offset) + 4
        }

        val addresses = mutableListOf<String>()
        repeat(answers) {
            if (offset >= data.size) return@repeat
            offset = skipName(data, offset)
            if (offset + 10 > data.size) return@repeat
            val type = u16(offset)
            offset += 2
            offset += 2 // class
            offset += 4 // TTL
            val length = u16(offset)
            offset += 2
            if (type == 1 && length == 4 && offset + 4 <= data.size) {
                addresses +=
                    listOf(
                        data[offset].toInt() and 0xff,
                        data[offset + 1].toInt() and 0xff,
                        data[offset + 2].toInt() and 0xff,
                        data[offset + 3].toInt() and 0xff,
                    ).joinToString(".")
            }
            offset += length
        }
        return addresses
    }

    private fun skipName(
        data: ByteArray,
        start: Int,
    ): Int {
        var offset = start
        while (offset < data.size) {
            val length = data[offset].toInt() and 0xff
            if (length == 0) return offset + 1
            if (length and 0xc0 == 0xc0) return (offset + 2).coerceAtMost(data.size)
            offset += length + 1
        }
        return offset
    }
}
