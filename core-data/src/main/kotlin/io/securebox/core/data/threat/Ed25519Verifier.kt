package io.securebox.core.data.threat

import java.security.MessageDigest
import java.util.Base64
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec

/** Detached Ed25519 verification shared by the feed client and its tests. */
internal object Ed25519Verifier {
    fun verify(
        payload: ByteArray,
        signatureBase64: String,
        publicKeyHex: String,
    ): Boolean =
        runCatching {
            val signature = Base64.getDecoder().decode(signatureBase64.trim())
            val publicKeyBytes = publicKeyHex.hexToBytes()
            if (publicKeyBytes.size != 32) return false
            val spec =
                EdDSAPublicKeySpec(
                    publicKeyBytes,
                    EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519),
                )
            val engine = EdDSAEngine(MessageDigest.getInstance("SHA-512"))
            engine.initVerify(EdDSAPublicKey(spec))
            engine.update(payload)
            engine.verify(signature)
        }.getOrDefault(false)

    fun ByteArray.toHex(): String = joinToString(separator = "") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private fun String.hexToBytes(): ByteArray =
        runCatching { chunked(2).map { it.toInt(16).toByte() }.toByteArray() }.getOrDefault(ByteArray(0))
}
