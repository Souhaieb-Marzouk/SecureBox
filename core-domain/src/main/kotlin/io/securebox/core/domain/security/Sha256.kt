package io.securebox.core.domain.security

import java.io.File
import java.security.MessageDigest

/**
 * SHA-256 helpers used to verify downloaded update packages.
 *
 * Comparison is constant-time and case-insensitive so a malicious server cannot learn the expected
 * digest through timing, and so assets published by different tools still verify.
 */
public object Sha256 {
    private const val BUFFER_SIZE: Int = 64 * 1024

    public fun of(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    public fun of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    /** Normalises a hash as published: trims whitespace, drops a leading `sha256:` label, lowercases. */
    public fun normalize(hash: String): String =
        hash.trim()
            .removePrefix("sha256:")
            .removePrefix("SHA256:")
            .replace(" ", "")
            .lowercase()

    public fun matches(
        expected: String,
        actual: String,
    ): Boolean {
        val left = normalize(expected).toByteArray(Charsets.US_ASCII)
        val right = normalize(actual).toByteArray(Charsets.US_ASCII)
        return MessageDigest.isEqual(left, right)
    }

    /**
     * Extracts the checksum for [fileName] from a `SHA256SUMS` document.
     * Supports both `<hash>  <name>` and `<hash> *<name>` formats, and ignores comments.
     */
    public fun parseChecksumFile(
        content: String,
        fileName: String,
    ): String? =
        content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s+"), limit = 2)
                if (parts.size != 2) return@mapNotNull null
                val name = parts[1].removePrefix("*").trim()
                if (name == fileName) parts[0] else null
            }.firstOrNull()
            ?.takeIf { normalize(it).length == 64 }

    private fun ByteArray.toHex(): String {
        val builder = StringBuilder(size * 2)
        for (byte in this) {
            val value = byte.toInt() and 0xFF
            builder.append(HEX[value ushr 4])
            builder.append(HEX[value and 0x0F])
        }
        return builder.toString()
    }

    private const val HEX: String = "0123456789abcdef"
}
