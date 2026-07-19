package com.sylo.core.security.biometric

import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

/**
 * Abstraction over the AndroidX BiometricPrompt so features depend on an interface,
 * not the framework. A concrete implementation is provided in :core-security.
 */
interface BiometricAuthenticator {

    /**
     * Whether the device has usable biometric hardware and an enrolled credential.
     * Accepts BOTH Class 3 (strong) and Class 2 (weak) sensors, so devices whose
     * fingerprint is classified weak (common on older/OPPO Android 9 phones) are
     * still recognized instead of reporting "no biometric".
     */
    fun canAuthenticate(): BiometricAvailability

    /**
     * Whether a Class 3 (STRONG) biometric is available. Only a strong biometric can
     * back a CryptoObject / Keystore-bound key, so the encrypted-DB key binding is
     * offered only when this is true; weak-only devices still get biometric unlock as
     * a convenience (the DB key stays protected by the Keystore either way).
     */
    fun hasStrongBiometric(): Boolean

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

enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    NOT_ENROLLED,
    UNAVAILABLE,

    /**
     * The platform APIs couldn't determine status, but the device advertises a
     * fingerprint sensor (common on OEM Android 9 with a proprietary HAL). Biometric
     * is offered optimistically — the prompt attempt is the real test.
     */
    UNKNOWN,
    ;

    /** Whether biometric should be OFFERED to the user (includes the optimistic UNKNOWN). */
    val usable: Boolean get() = this == AVAILABLE || this == UNKNOWN
}

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
