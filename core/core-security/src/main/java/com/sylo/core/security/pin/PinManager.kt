package com.sylo.core.security.pin

import android.util.Base64
import com.sylo.core.security.crypto.SecurePreferences
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores and verifies the user's PIN as a salted PBKDF2 hash inside
 * [SecurePreferences] (itself Keystore-encrypted). The raw PIN is never persisted.
 */
@Singleton
class PinManager @Inject constructor(
    private val securePreferences: SecurePreferences,
) {
    fun hasPin(): Boolean = securePreferences.getString(KEY_HASH) != null

    fun setPin(pin: String) {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(pin.toCharArray(), salt)
        securePreferences.putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
        securePreferences.putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
    }

    fun verifyPin(pin: String): Boolean {
        val salt = securePreferences.getString(KEY_SALT)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val stored = securePreferences.getString(KEY_HASH)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val candidate = pbkdf2(pin.toCharArray(), salt)
        return MessageDigest.isEqual(candidate, stored) // constant-time
    }

    fun clearPin() {
        securePreferences.remove(KEY_SALT)
        securePreferences.remove(KEY_HASH)
    }

    private fun pbkdf2(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private companion object {
        const val KEY_SALT = "pin_salt"
        const val KEY_HASH = "pin_hash"
        const val SALT_BYTES = 16
        const val ITERATIONS = 120_000
        const val KEY_LENGTH_BITS = 256
    }
}
