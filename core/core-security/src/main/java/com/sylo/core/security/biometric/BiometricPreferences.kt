package com.sylo.core.security.biometric

import com.sylo.core.security.crypto.SecurePreferences
import javax.inject.Inject
import javax.inject.Singleton

/** Persists whether the user opted into biometric unlock. */
@Singleton
class BiometricPreferences @Inject constructor(
    private val securePreferences: SecurePreferences,
) {
    fun isEnabled(): Boolean = securePreferences.getString(KEY_ENABLED) == "1"

    fun setEnabled(enabled: Boolean) =
        securePreferences.putString(KEY_ENABLED, if (enabled) "1" else "0")

    private companion object {
        const val KEY_ENABLED = "biometric_enabled"
    }
}
