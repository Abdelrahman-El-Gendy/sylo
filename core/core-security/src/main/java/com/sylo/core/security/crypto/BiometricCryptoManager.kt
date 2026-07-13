package com.sylo.core.security.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Binds the SQLCipher database passphrase to a biometric-authenticated Android
 * Keystore key. The key requires user (biometric) authentication for every use, so
 * the wrapped passphrase can only be decrypted inside a [Cipher] that has been
 * unlocked via BiometricPrompt's CryptoObject.
 *
 * Flow:
 *  - setup:  encryptCipher() -> BiometricPrompt(CryptoObject) -> wrapAndStore(cipher, passphrase)
 *  - unlock: decryptCipher()  -> BiometricPrompt(CryptoObject) -> unwrap(cipher) -> passphrase
 */

@Singleton
class BiometricCryptoManager @Inject constructor(
    private val securePreferences: SecurePreferences,
) {
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    /** True once the passphrase has been wrapped by the biometric key. */
    fun isBound(): Boolean = securePreferences.getString(KEY_WRAPPED) != null

    /** ENCRYPT cipher for wrapping the passphrase; using it requires biometric auth. */
    fun encryptCipher(): Cipher {
        val key = getOrCreateKey()
        return Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    /**
     * DECRYPT cipher initialised with the stored IV. Returns null if the key was
     * invalidated (e.g. biometrics re-enrolled) or no binding exists — caller should
     * then fall back to re-binding.
     */
    fun decryptCipher(): Cipher? {
        val ivB64 = securePreferences.getString(KEY_IV) ?: return null
        val key = existingKey() ?: return null
        val iv = Base64.decode(ivB64, Base64.NO_WRAP)
        return try {
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            clearBinding()
            null
        }
    }

    /** After biometric success on the ENCRYPT cipher: wrap + persist the passphrase. */
    fun wrapAndStore(cipher: Cipher, passphrase: ByteArray) {
        val ciphertext = cipher.doFinal(passphrase)
        securePreferences.putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
        securePreferences.putString(KEY_WRAPPED, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
    }

    /** After biometric success on the DECRYPT cipher: recover the passphrase. */
    fun unwrap(cipher: Cipher): ByteArray {
        val wrapped = Base64.decode(securePreferences.getString(KEY_WRAPPED)!!, Base64.NO_WRAP)
        return cipher.doFinal(wrapped)
    }

    fun clearBinding() {
        securePreferences.remove(KEY_IV)
        securePreferences.remove(KEY_WRAPPED)
        runCatching { keyStore.deleteEntry(KEY_ALIAS) }
    }

    private fun existingKey(): SecretKey? =
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey

    private fun getOrCreateKey(): SecretKey {
        existingKey()?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1) // require auth every use
        }
        generator.init(builder.build())
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "sylo_db_biometric_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val KEY_IV = "db_key_iv"
        const val KEY_WRAPPED = "db_key_wrapped"
    }
}
