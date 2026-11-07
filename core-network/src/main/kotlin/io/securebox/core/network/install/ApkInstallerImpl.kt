package io.securebox.core.network.install

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import io.securebox.core.domain.install.ApkInstaller
import io.securebox.core.domain.install.InstallOutcome
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hands a verified APK to the system installer. The APK lives in the app cache and is shared
 * through a FileProvider scoped to `cacheDir/updates` only.
 */
@Singleton
class ApkInstallerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ApkInstaller {
        override fun canInstallPackages(): Boolean =
            context.packageManager.canRequestPackageInstalls()

        override fun openInstallPermissionSettings() {
            val intent =
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }

        override fun install(file: File): InstallOutcome {
            if (!canInstallPackages()) return InstallOutcome.PermissionRequired
            if (!file.exists()) return InstallOutcome.Failed("The downloaded package is missing.")

            return try {
                val uri =
                    FileProvider.getUriForFile(
                        context,
                        // Must match the manifest's ${applicationId}.fileprovider, including any
                        // debug applicationIdSuffix.
                        "${context.packageName}.fileprovider",
                        file,
                    )
                val intent =
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, APK_MIME_TYPE)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(intent)
                InstallOutcome.Started
            } catch (error: RuntimeException) {
                InstallOutcome.Failed(error.message ?: "No package installer available.")
            }
        }

        private companion object {
            const val APK_MIME_TYPE: String = "application/vnd.android.package-archive"
        }
    }
