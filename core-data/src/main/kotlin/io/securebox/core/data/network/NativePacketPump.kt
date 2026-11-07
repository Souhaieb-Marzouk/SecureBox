package io.securebox.core.data.network

/**
 * Loads the native TUN pump. If the library is missing (unsupported ABI or a build without the
 * NDK), [available] is false and the service falls back to the Kotlin reader.
 */
internal object NativePacketPump {
    val available: Boolean = runCatching { System.loadLibrary("securebox_pump") }.isSuccess

    external fun drain(fd: Int)
}
