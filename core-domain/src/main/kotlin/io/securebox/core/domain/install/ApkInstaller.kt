package io.securebox.core.domain.install

import java.io.File

/**
 * Hands a verified APK to the platform package installer. Silent install is intentionally out of
 * scope: both phone and TV use the standard installer dialog so the user always confirms.
 */
public interface ApkInstaller {
    public fun canInstallPackages(): Boolean

    public fun openInstallPermissionSettings()

    public fun install(file: File): InstallOutcome
}

public sealed interface InstallOutcome {
    public data object Started : InstallOutcome

    /** The user has not granted REQUEST_INSTALL_PACKAGES yet. */
    public data object PermissionRequired : InstallOutcome

    public data class Failed(
        val reason: String,
    ) : InstallOutcome
}
