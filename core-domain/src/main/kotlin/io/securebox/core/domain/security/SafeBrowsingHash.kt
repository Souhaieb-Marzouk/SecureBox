package io.securebox.core.domain.security

import java.security.MessageDigest
import java.util.Locale

/**
 * Local hash-prefix lookup used by the private browser. Only the first bytes of the SHA-256 digest
 * are stored, never the URL itself. This follows the same privacy principle as Safe Browsing: the
 * app checks a short prefix locally and does not send the visited URL to a server.
 */
public object SafeBrowsingHash {
    public const val DEFAULT_PREFIX_HEX: Int = 8

    public fun prefix(
        url: String,
        hexChars: Int = DEFAULT_PREFIX_HEX,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.trim().toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { byte -> "%02x".format(Locale.US, byte.toInt() and 0xff) }
        return hex.take(hexChars.coerceIn(2, 64))
    }
}
