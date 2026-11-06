package io.securebox.core.data.permission

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.securebox.core.domain.permission.PlatformPermissions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPlatformPermissions
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PlatformPermissions {
        /** Usage access is granted through a special settings screen, not a runtime dialog. */
        override fun hasUsageAccess(): Boolean {
            val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
            val mode =
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            return mode == AppOpsManager.MODE_ALLOWED
        }

        override fun hasNotificationPermission(): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

        override fun canInstallPackages(): Boolean =
            context.packageManager.canRequestPackageInstalls()
    }
