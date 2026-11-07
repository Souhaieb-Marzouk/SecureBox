package io.securebox.core.data.security

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import io.securebox.core.domain.security.DangerousSetting
import io.securebox.core.domain.security.DeviceSettingsRepository
import io.securebox.core.domain.security.SettingId
import io.securebox.core.domain.security.SettingSeverity
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AndroidDeviceSettingsRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DeviceSettingsRepository {
        override suspend fun audit(): List<DangerousSetting> =
            withContext(Dispatchers.IO) {
                val resolver = context.contentResolver
                val developerOptions = Settings.Global.getInt(resolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1
                val adbEnabled = Settings.Global.getInt(resolver, Settings.Global.ADB_ENABLED, 0) == 1
                val accessibilityEnabled = Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
                val admins = context.getSystemService(DevicePolicyManager::class.java)?.activeAdmins.orEmpty()
                val userCertificates = countUserCertificates()
                val installUnknown = context.packageManager.canRequestPackageInstalls()

                listOf(
                    DangerousSetting(SettingId.USB_DEBUGGING, SettingSeverity.HIGH, adbEnabled),
                    DangerousSetting(SettingId.DEVELOPER_OPTIONS, SettingSeverity.MEDIUM, developerOptions),
                    DangerousSetting(SettingId.ACCESSIBILITY, SettingSeverity.HIGH, accessibilityEnabled),
                    DangerousSetting(SettingId.DEVICE_ADMIN, SettingSeverity.HIGH, admins.isNotEmpty(), admins.size),
                    DangerousSetting(SettingId.USER_CA_CERTIFICATES, SettingSeverity.HIGH, userCertificates > 0, userCertificates),
                    DangerousSetting(SettingId.INSTALL_UNKNOWN_APPS, SettingSeverity.MEDIUM, installUnknown),
                )
            }

        private fun countUserCertificates(): Int =
            runCatching {
                val keyStore = KeyStore.getInstance("AndroidCAStore")
                keyStore.load(null, null)
                var user = 0
                var system = 0
                val aliases = keyStore.aliases()
                while (aliases.hasMoreElements()) {
                    val alias = aliases.nextElement()
                    if (alias.startsWith("user:")) user++ else system++
                }
                user
            }.getOrDefault(0)
    }
