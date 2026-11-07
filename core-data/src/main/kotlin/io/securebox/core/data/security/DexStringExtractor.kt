package io.securebox.core.data.security

import java.io.File
import java.util.zip.ZipFile

/** Extracts printable ASCII strings from an APK's DEX files for rule matching. */
object DexStringExtractor {
    private const val MIN_STRING = 4
    private const val DEFAULT_MAX_CHARS = 6_000_000
    private const val MAX_DEX_BYTES = 32L * 1024 * 1024

    fun extract(
        apk: File,
        maxChars: Int = DEFAULT_MAX_CHARS,
    ): String {
        val output = StringBuilder()
        runCatching {
            ZipFile(apk).use { zip ->
                for (entry in zip.entries()) {
                    if (!entry.name.endsWith(".dex")) continue
                    if (entry.size <= 0 || entry.size > MAX_DEX_BYTES) continue

                    zip.getInputStream(entry).buffered(64 * 1024).use { input ->
                        val buffer = ByteArray(64 * 1024)
                        val current = StringBuilder()
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            for (index in 0 until read) {
                                val value = buffer[index].toInt() and 0xFF
                                if (value in 32..126) {
                                    current.append(value.toChar())
                                } else {
                                    if (current.length >= MIN_STRING) output.append(current).append('\n')
                                    current.setLength(0)
                                }
                            }
                            if (output.length >= maxChars) return output.toString()
                        }
                        if (current.length >= MIN_STRING) output.append(current)
                    }
                    if (output.length >= maxChars) return output.toString()
                }
            }
        }
        return output.toString()
    }
}
