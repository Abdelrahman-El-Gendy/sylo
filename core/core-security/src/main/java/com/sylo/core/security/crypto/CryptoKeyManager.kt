package com.sylo.core.security.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the SQLCipher database passphrase. The passphrase is generated once with a
 * CSPRNG and persisted (encrypted) via [SecurePreferences]; on later launches the
 * same key is returned so the encrypted DB stays readable.
 */
@Singleton
class CryptoKeyManager @Inject constructor(
    private val securePreferences: SecurePreferences,
) {
    /** Returns the raw DB passphrase bytes, generating & storing one on first use. */
    fun getOrCreateDatabasePassphrase(): ByteArray {
        securePreferences.getString(KEY_DB_PASSPHRASE)?.let { stored ->
            return Base64.decode(stored, Base64.NO_WRAP)
        }
        val fresh = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        securePreferences.putString(KEY_DB_PASSPHRASE, Base64.encodeToString(fresh, Base64.NO_WRAP))
        return fresh
    }

    private companion object {
        const val KEY_DB_PASSPHRASE = "db_passphrase"
        const val PASSPHRASE_BYTES = 32 // 256-bit
    }
}
