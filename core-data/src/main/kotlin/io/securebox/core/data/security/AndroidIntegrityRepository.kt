package io.securebox.core.data.security

import android.content.Context
import android.os.Build
import android.os.Debug
import dagger.hilt.android.qualifiers.ApplicationContext
import io.securebox.core.domain.security.IntegrityIssue
import io.securebox.core.domain.security.IntegrityReport
import io.securebox.core.domain.security.IntegrityRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Basic RASP: debugger, emulator, root indicators and APK signature mismatch. */
@Singleton
class AndroidIntegrityRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val signatureVerifier: PackageSignatureVerifier,
    ) : IntegrityRepository {
        override suspend fun check(): IntegrityReport =
            withContext(Dispatchers.IO) {
                val issues = mutableListOf<IntegrityIssue>()
                if (Debug.isDebuggerConnected() || Debug.waitingForDebugger()) {
                    issues += IntegrityIssue.DEBUGGER_ATTACHED
                }
                if (isEmulator()) issues += IntegrityIssue.EMULATOR
                if (isRooted()) issues += IntegrityIssue.ROOTED
                if (signatureVerifier.verify().reason == SignatureCheck.Reason.MISMATCH) {
                    issues += IntegrityIssue.SIGNATURE_MISMATCH
                }
                IntegrityReport(issues = issues, checkedAtMillis = System.currentTimeMillis())
            }

        private fun isEmulator(): Boolean {
            val fingerprint = Build.FINGERPRINT.lowercase()
            val model = Build.MODEL.lowercase()
            val hardware = Build.HARDWARE.lowercase()
            val product = Build.PRODUCT.lowercase()
            return fingerprint.startsWith("generic") ||
                fingerprint.contains("emulator") ||
                model.contains("sdk") ||
                model.contains("emulator") ||
                model.contains("android sdk built for") ||
                hardware.contains("goldfish") ||
                hardware.contains("ranchu") ||
                product.contains("sdk") ||
                product.contains("simulator")
        }

        private fun isRooted(): Boolean {
            if (Build.TAGS?.contains("test-keys") == true) return true
            val paths =
                listOf(
                    "/system/app/Superuser.apk",
                    "/sbin/su",
                    "/system/bin/su",
                    "/system/xbin/su",
                    "/data/local/xbin/su",
                    "/data/local/bin/su",
                    "/system/sd/xbin/su",
                    "/system/bin/failsafe/su",
                    "/data/local/su",
                    "/su/bin/su",
                    "/debug_ramdisk/su",
                )
            if (paths.any { File(it).exists() }) return true
            // Magisk leaves a hidden mount marker in most versions.
            return File("/sbin/magisk").exists() || File("/system/xbin/magisk").exists()
        }
    }
