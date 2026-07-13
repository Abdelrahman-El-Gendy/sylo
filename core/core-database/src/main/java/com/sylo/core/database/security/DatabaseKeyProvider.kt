package com.sylo.core.database.security

/**
 * Supplies the SQLCipher passphrase used to open the encrypted DB.
 *
 * The real implementation lives in :core-security (backed by a Keystore-wrapped
 * key stored in EncryptedSharedPreferences). :core-database only declares the
 * contract so it never depends on the security module's internals.
 */
interface DatabaseKeyProvider {
    /** Returns the raw passphrase bytes for SQLCipher. Never log or copy these. */
    fun getDatabasePassphrase(): ByteArray
}
