package com.healthmine.testforge.utility

import java.math.BigInteger
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Produces password hashes compatible with the existing member schema:
 *   member_phi.password        = hashHex (32 bytes)
 *   member_password_salt.pwd_salt = saltHex (32 bytes)
 *
 * Algorithm matches QAUtilities' SaltedPasswordUtil: PBKDF2WithHmacSHA1,
 * 901 iterations, 32-byte output.
 */
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val ITERATIONS = 901
    private const val SALT_BYTES = 32
    private const val HASH_BYTES = 32

    data class Hashed(val hashHex: String, val saltHex: String)

    fun hash(password: String): Hashed {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(password.toCharArray(), salt)
        return Hashed(hashHex = toHex(hash), saltHex = toHex(salt))
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, HASH_BYTES * 8)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    }

    private fun toHex(bytes: ByteArray): String {
        val hex = BigInteger(1, bytes).toString(16)
        val pad = bytes.size * 2 - hex.length
        return if (pad > 0) "0".repeat(pad) + hex else hex
    }
}
