package com.sylo.core.security.crypto

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the decrypted DB passphrase in memory for the current session only (never
 * persisted). Populated after a biometric CryptoObject unlock, then read by the
 * database layer. Cleared on lock/logout.
 */
@Singleton
class SessionKeyHolder @Inject constructor() {
    @Volatile
    var databasePassphrase: ByteArray? = null
        private set

    fun set(passphrase: ByteArray) {
        databasePassphrase = passphrase
    }

    fun clear() {
        databasePassphrase?.fill(0)
        databasePassphrase = null
    }
}
