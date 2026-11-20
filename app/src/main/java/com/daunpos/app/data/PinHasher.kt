package com.daunpos.app.data

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    private const val LEGACY_ITERATIONS = 20_000
    private const val CURRENT_ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val CURRENT_PREFIX = "v2:"
    private const val BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return encodeBase64(salt)
    }

    fun hash(pin: String, saltBase64: String): String =
        CURRENT_PREFIX + derive(pin, saltBase64, CURRENT_ITERATIONS)

    fun verify(pin: String, saltBase64: String, expectedHash: String): Boolean {
        if (saltBase64.isBlank() || saltBase64 == "-" ||
            expectedHash.isBlank() || expectedHash == "NO_LOGIN"
        ) return false

        val isCurrent = expectedHash.startsWith(CURRENT_PREFIX)
        val storedValue = if (isCurrent) expectedHash.removePrefix(CURRENT_PREFIX) else expectedHash
        val iterations = if (isCurrent) CURRENT_ITERATIONS else LEGACY_ITERATIONS
        val candidate = runCatching { derive(pin, saltBase64, iterations) }.getOrNull() ?: return false

        return MessageDigest.isEqual(
            candidate.toByteArray(Charsets.UTF_8),
            storedValue.toByteArray(Charsets.UTF_8)
        )
    }

    private fun derive(pin: String, saltBase64: String, iterations: Int): String {
        val salt = decodeBase64(saltBase64)
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            encodeBase64(factory.generateSecret(spec).encoded)
        } finally {
            spec.clearPassword()
        }
    }

    private fun encodeBase64(bytes: ByteArray): String {
        val result = StringBuilder(((bytes.size + 2) / 3) * 4)
        var index = 0
        while (index < bytes.size) {
            val first = bytes[index].toInt() and 0xFF
            val hasSecond = index + 1 < bytes.size
            val hasThird = index + 2 < bytes.size
            val second = if (hasSecond) bytes[index + 1].toInt() and 0xFF else 0
            val third = if (hasThird) bytes[index + 2].toInt() and 0xFF else 0

            result.append(BASE64_ALPHABET[first ushr 2])
            result.append(BASE64_ALPHABET[((first and 0x03) shl 4) or (second ushr 4)])
            result.append(if (hasSecond) BASE64_ALPHABET[((second and 0x0F) shl 2) or (third ushr 6)] else '=')
            result.append(if (hasThird) BASE64_ALPHABET[third and 0x3F] else '=')
            index += 3
        }
        return result.toString()
    }

    private fun decodeBase64(value: String): ByteArray {
        val input = value.filterNot(Char::isWhitespace)
        require(input.isNotEmpty() && input.length % 4 == 0) { "Invalid Base64 value." }
        val padding = when {
            input.endsWith("==") -> 2
            input.endsWith("=") -> 1
            else -> 0
        }
        val output = ByteArray(input.length / 4 * 3 - padding)
        var outputIndex = 0

        fun digit(char: Char): Int {
            val index = BASE64_ALPHABET.indexOf(char)
            require(index >= 0) { "Invalid Base64 character." }
            return index
        }

        var index = 0
        while (index < input.length) {
            val c0 = digit(input[index])
            val c1 = digit(input[index + 1])
            val c2 = if (input[index + 2] == '=') 0 else digit(input[index + 2])
            val c3 = if (input[index + 3] == '=') 0 else digit(input[index + 3])
            val block = (c0 shl 18) or (c1 shl 12) or (c2 shl 6) or c3

            if (outputIndex < output.size) output[outputIndex++] = (block ushr 16).toByte()
            if (outputIndex < output.size) output[outputIndex++] = (block ushr 8).toByte()
            if (outputIndex < output.size) output[outputIndex++] = block.toByte()
            index += 4
        }
        return output
    }
}
