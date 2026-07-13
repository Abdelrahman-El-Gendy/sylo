package com.sylo.core.security.biometric

import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

/**
 * Abstraction over the AndroidX BiometricPrompt so features depend on an interface,
 * not the framework. A concrete implementation is provided in :core-security.
 */
interface BiometricAuthenticator {

    /** Whether the device has usable biometric hardware and an enrolled credential. */
    fun canAuthenticate(): BiometricAvailability

    /**
     * Shows the system biometric prompt. [onResult] reports the outcome so callers
     * can unlock the session or fall back to PIN.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (BiometricResult) -> Unit,
    )

    /**
     * Shows the biometric prompt bound to a [Cipher] via a CryptoObject. On success
     * the returned cipher is authenticated and can wrap/unwrap the DB key.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        cipher: Cipher,
        onResult: (BiometricCipherResult) -> Unit,
    )
}

enum class BiometricAvailability { AVAILABLE, NO_HARDWARE, NOT_ENROLLED, UNAVAILABLE }

sealed interface BiometricResult {
    data object Success : BiometricResult
    data object Failed : BiometricResult
    data class Error(val code: Int, val message: String) : BiometricResult
}

/** Result of a CryptoObject-bound authentication. */
sealed interface BiometricCipherResult {
    data class Success(val cipher: Cipher) : BiometricCipherResult
    data object Failed : BiometricCipherResult
    data class Error(val code: Int, val message: String) : BiometricCipherResult
}
