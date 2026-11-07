package io.securebox.core.data.threat

import java.security.MessageDigest
import java.util.Base64
import net.i2p.crypto.eddsa.EdDSAEngine
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Ed25519VerifierTest {
    private val curve = EdDSANamedCurveTable.getByName(EdDSANamedCurveTable.ED_25519)
    private val privateKey = EdDSAPrivateKey(EdDSAPrivateKeySpec(ByteArray(32) { (it + 1).toByte() }, curve))
    private val publicHex = privateKey.abyte.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private fun sign(payload: ByteArray): String {
        val engine = EdDSAEngine(MessageDigest.getInstance("SHA-512"))
        engine.initSign(privateKey)
        engine.update(payload)
        return Base64.getEncoder().encodeToString(engine.sign())
    }

    @Test
    fun acceptsAValidSignature() {
        val payload = "securebox-feed-v1".toByteArray()
        assertTrue(Ed25519Verifier.verify(payload, sign(payload), publicHex))
    }

    @Test
    fun rejectsATamperedPayload() {
        val payload = "securebox-feed-v1".toByteArray()
        val signature = sign(payload)
        assertFalse(Ed25519Verifier.verify("securebox-feed-v2".toByteArray(), signature, publicHex))
    }

    @Test
    fun rejectsAWrongKeyAndMalformedSignature() {
        val payload = "securebox-feed-v1".toByteArray()
        val otherKey = EdDSAPrivateKey(EdDSAPrivateKeySpec(ByteArray(32) { (it + 9).toByte() }, curve))
        val otherHex = otherKey.abyte.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
        assertFalse(Ed25519Verifier.verify(payload, sign(payload), otherHex))
        assertFalse(Ed25519Verifier.verify(payload, "not-base64!!", publicHex))
        assertFalse(Ed25519Verifier.verify(payload, sign(payload), "00"))
    }
}
