package io.securebox.feature.settings

import android.content.Context
import io.securebox.core.domain.security.SafeBrowsingHash

/**
 * Offline hash-prefix store. Only SHA-256 prefixes are bundled; full URLs are never stored. A
 * matching prefix blocks the navigation before the request leaves the app.
 */
object SafeBrowsingStore {
    @Volatile private var loaded = false
    @Volatile private var prefixes: Set<String> = emptySet()

    fun isBlocked(
        context: Context,
        url: String,
    ): Boolean {
        ensureLoaded(context)
        return SafeBrowsingHash.prefix(url) in prefixes
    }

    private fun ensureLoaded(context: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            prefixes =
                runCatching {
                    context.applicationContext.assets
                        .open("safe_browsing_prefixes.txt")
                        .bufferedReader()
                        .useLines { lines ->
                            lines
                                .map { it.trim() }
                                .filter { it.isNotEmpty() && !it.startsWith("#") }
                                .map { it.take(SafeBrowsingHash.DEFAULT_PREFIX_HEX).lowercase() }
                                .toSet()
                        }
                }.getOrDefault(emptySet())
            loaded = true
        }
    }
}
