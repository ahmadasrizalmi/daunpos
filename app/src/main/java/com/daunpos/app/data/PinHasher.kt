package com.daunpos.app.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    private const val ITERATIONS = 20_000
    private const val KEY_LENGTH = 256

    fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun hash(pin: String, saltBase64: String): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val bytes = factory.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun verify(pin: String, saltBase64: String, expectedHash: String): Boolean {
        if (saltBase64.isBlank() || saltBase64 == "-" || expectedHash.isBlank() || expectedHash == "NO_LOGIN") return false
        val candidate = hash(pin, saltBase64)
        // Constant-time comparison to prevent timing side-channel attacks
        return MessageDigest.isEqual(
            candidate.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }
}
