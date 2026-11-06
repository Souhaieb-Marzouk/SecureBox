package io.securebox.core.domain.permission

/**
 * The runtime permissions SecureBox needs, expressed as plain booleans so the presentation layer
 * never talks to Android APIs directly and can be unit-tested with fakes.
 */
public interface PlatformPermissions {
    public fun hasUsageAccess(): Boolean

    public fun hasNotificationPermission(): Boolean

    public fun canInstallPackages(): Boolean
}
