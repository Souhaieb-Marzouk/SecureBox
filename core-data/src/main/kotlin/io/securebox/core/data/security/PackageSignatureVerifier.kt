package io.securebox.core.data.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Verifies at runtime that the installed APK is signed with the certificate SecureBox was built
 * with. A re-signed (tampered) build fails this check and the UI degrades into a warning state.
 *
 * The expected digest is injected so the data layer stays independent of the application module's
 * generated BuildConfig.
 */
@Singleton
class PackageSignatureVerifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @Named(EXPECTED_SIGNING_CERT_SHA256) private val expectedCertSha256: String,
    ) {
        public fun verify(): SignatureCheck {
            if (expectedCertSha256.isBlank()) {
                return SignatureCheck(ok = false, actual = "", reason = SignatureCheck.Reason.NOT_CONFIGURED)
            }
            val actual = currentSigningCertificateSha256()
                ?: return SignatureCheck(ok = false, actual = "", reason = SignatureCheck.Reason.UNREADABLE)

            val matches = MessageDigest.isEqual(
                expectedCertSha256.lowercase().toByteArray(Charsets.US_ASCII),
                actual.lowercase().toByteArray(Charsets.US_ASCII),
            )
            return SignatureCheck(
                ok = matches,
                actual = actual,
                reason = if (matches) SignatureCheck.Reason.OK else SignatureCheck.Reason.MISMATCH,
            )
        }

        private fun currentSigningCertificateSha256(): String? =
            try {
                val packageManager = context.packageManager
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES,
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                }

                val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val info = packageInfo.signingInfo
                    when {
                        info == null -> null
                        info.hasMultipleSigners() -> info.apkContentsSigners
                        else -> info.signingCertificateHistory
                    }
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.signatures
                }

                val certBytes = signatures?.firstOrNull()?.toByteArray() ?: return null
                MessageDigest.getInstance("SHA-256")
                    .digest(certBytes)
                    .joinToString(separator = "") { byte -> "%02x".format(byte) }
            } catch (error: PackageManager.NameNotFoundException) {
                null
            }

        public companion object {
            public const val EXPECTED_SIGNING_CERT_SHA256: String = "securebox.expected_signing_cert_sha256"
        }
    }

public data class SignatureCheck(
    val ok: Boolean,
    val actual: String,
    val reason: Reason,
) {
    public enum class Reason {
        OK,
        MISMATCH,
        NOT_CONFIGURED,
        UNREADABLE,
    }
}
