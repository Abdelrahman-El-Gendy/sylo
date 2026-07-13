package com.sylo.feature.auth.ui

import androidx.lifecycle.ViewModel
import com.sylo.core.security.biometric.BiometricAuthenticator
import com.sylo.core.security.biometric.BiometricAvailability
import com.sylo.core.security.biometric.BiometricPreferences
import com.sylo.core.security.crypto.BiometricCryptoManager
import com.sylo.core.security.crypto.CryptoKeyManager
import com.sylo.core.security.crypto.SessionKeyHolder
import com.sylo.core.security.pin.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.crypto.Cipher
import javax.inject.Inject

data class AuthUiState(
    val enteredDigits: Int = 0,
    val biometricEnabled: Boolean = false,
    val error: String? = null,
) {
    val isComplete: Boolean get() = enteredDigits == PIN_LENGTH
}

const val PIN_LENGTH = 4

/**
 * Drives both PIN screens (setup + unlock). Holds the transient PIN buffer, exposes
 * dot progress, and delegates to [PinManager] for real salted-hash storage/verification
 * and to [BiometricAuthenticator]/[BiometricPreferences] for biometric unlock.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val pinManager: PinManager,
    private val biometricPreferences: BiometricPreferences,
    private val cryptoKeyManager: CryptoKeyManager,
    private val biometricCryptoManager: BiometricCryptoManager,
    private val sessionKeyHolder: SessionKeyHolder,
    val biometricAuthenticator: BiometricAuthenticator,
) : ViewModel() {

    private val pin = StringBuilder()

    private val _uiState = MutableStateFlow(AuthUiState(biometricEnabled = biometricPreferences.isEnabled()))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun hasPin(): Boolean = pinManager.hasPin()

    /** True only when there's a PIN, the user opted in, and the device can actually authenticate. */
    fun canOfferBiometric(): Boolean =
        hasPin() && biometricPreferences.isEnabled() &&
            biometricAuthenticator.canAuthenticate() == BiometricAvailability.AVAILABLE

    /** Whether biometric hardware is usable (for enabling the toggle during setup). */
    fun biometricHardwareAvailable(): Boolean =
        biometricAuthenticator.canAuthenticate() == BiometricAvailability.AVAILABLE

    fun onDigit(digit: Char) {
        if (pin.length >= PIN_LENGTH) return
        pin.append(digit)
        syncDots()
    }

    fun onBackspace() {
        if (pin.isEmpty()) return
        pin.deleteCharAt(pin.length - 1)
        syncDots()
    }

    fun toggleBiometric() = _uiState.update { it.copy(biometricEnabled = !it.biometricEnabled) }

    /** Persist a freshly-created PIN + the biometric preference. Returns false if incomplete. */
    fun confirmSetup(): Boolean {
        if (pin.length != PIN_LENGTH) return false
        pinManager.setPin(pin.toString())
        biometricPreferences.setEnabled(_uiState.value.biometricEnabled && biometricHardwareAvailable())
        reset()
        return true
    }

    /** Verify an entered PIN against the stored hash. */
    fun verifyUnlock(): Boolean {
        if (pin.length != PIN_LENGTH) return false
        val ok = pinManager.verifyPin(pin.toString())
        if (ok) {
            reset()
        } else {
            pin.clear()
            _uiState.update { it.copy(enteredDigits = 0, error = "Incorrect PIN. Try again.") }
        }
        return ok
    }

    fun showBiometricError(message: String) = _uiState.update { it.copy(error = message) }

    // ----- Biometric ⇄ DB-key binding (CryptoObject) -----

    /** Whether the DB passphrase is already wrapped by the biometric Keystore key. */
    fun isDbKeyBound(): Boolean = biometricCryptoManager.isBound()

    /**
     * If the just-created setup opted into biometrics and the DB isn't bound yet,
     * returns an ENCRYPT cipher to authenticate before wrapping the passphrase.
     */
    fun encryptCipherForBinding(): Cipher? =
        if (_uiState.value.biometricEnabled && biometricHardwareAvailable() && !biometricCryptoManager.isBound()) {
            biometricCryptoManager.encryptCipher()
        } else {
            null
        }

    /** After biometric success on the ENCRYPT cipher: wrap the DB passphrase to the key. */
    fun completeBinding(cipher: Cipher) {
        val passphrase = cryptoKeyManager.getOrCreateDatabasePassphrase()
        biometricCryptoManager.wrapAndStore(cipher, passphrase)
        sessionKeyHolder.set(passphrase)
    }

    /** DECRYPT cipher for a biometric-bound unlock, or null if not bound / key invalidated. */
    fun decryptCipherForUnlock(): Cipher? =
        if (biometricCryptoManager.isBound()) biometricCryptoManager.decryptCipher() else null

    /** After biometric success on the DECRYPT cipher: release the DB passphrase for this session. */
    fun completeCryptoUnlock(cipher: Cipher) {
        sessionKeyHolder.set(biometricCryptoManager.unwrap(cipher))
    }

    private fun reset() {
        pin.clear()
        _uiState.update { it.copy(enteredDigits = 0, error = null) }
    }

    private fun syncDots() = _uiState.update { it.copy(enteredDigits = pin.length, error = null) }
}
