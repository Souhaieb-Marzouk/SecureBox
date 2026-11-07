package io.securebox.core.data.network

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Cached package metadata shared by traffic, port and firewall code. */
@Singleton
class AppRegistry
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        data class Entry(
            val packageName: String,
            val label: String,
            val isSystem: Boolean,
        )

        @Volatile
        private var cache: Map<Int, Entry>? = null

        fun byUid(): Map<Int, Entry> {
            cache?.let { return it }
            return build().also { cache = it }
        }

        fun invalidate() {
            cache = null
        }

        fun labels(): Map<String, String> = byUid().values.associate { it.packageName to it.label }

        fun labelFor(packageName: String): String =
            byUid().values.firstOrNull { it.packageName == packageName }?.label ?: packageName

        /**
         * Apps installed outside the Play Store. Used by the "Block unknown apps" preset, which is
         * the most useful default on cheap TV boxes where most risky APKs are sideloaded.
         */
        fun unknownPackages(): Set<String> {
            val packageManager = context.packageManager
            return byUid()
                .values
                .asSequence()
                .filter { !it.isSystem && it.packageName != context.packageName }
                .filter { installerOf(packageManager, it.packageName) != PLAY_STORE }
                .map { it.packageName }
                .toSet()
        }

        @Suppress("DEPRECATION")
        private fun installerOf(
            packageManager: PackageManager,
            packageName: String,
        ): String? =
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    packageManager.getInstallSourceInfo(packageName).installingPackageName
                } else {
                    packageManager.getInstallerPackageName(packageName)
                }
            }.getOrNull()

        @Suppress("DEPRECATION")
        private fun build(): Map<Int, Entry> {
            val packageManager = context.packageManager
            val result = LinkedHashMap<Int, Entry>()
            val apps = runCatching { packageManager.getInstalledApplications(0) }.getOrDefault(emptyList())
            for (app in apps) {
                val label = runCatching { packageManager.getApplicationLabel(app).toString() }.getOrDefault(app.packageName)
                result[app.uid] =
                    Entry(
                        packageName = app.packageName,
                        label = label,
                        isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    )
            }
            return result
        }

        private companion object {
            const val PLAY_STORE: String = "com.android.vending"
        }
    }
