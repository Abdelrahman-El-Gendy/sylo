package com.sylo.core.security.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over [EncryptedSharedPreferences], whose contents are encrypted with
 * a Keystore-backed master key. Use for small secrets (tokens, the DB passphrase),
 * never for large data.
 */
@Singleton
class SecurePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getString(key: String): String? = prefs.getString(key, null)

    fun putString(key: String, value: String) = prefs.edit().putString(key, value).apply()

    fun remove(key: String) = prefs.edit().remove(key).apply()

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val FILE_NAME = "sylo_secure_prefs"
    }
}
