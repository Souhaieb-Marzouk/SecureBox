package io.securebox.core.domain.device

/** Detects whether the app is running on a television form factor. */
public interface DeviceModeDetector {
    public fun isTelevision(): Boolean
}
