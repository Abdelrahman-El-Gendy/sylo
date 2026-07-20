package com.sylo.feature.auth.ui

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylo.core.security.biometric.BiometricAuthenticator
import com.sylo.core.security.biometric.BiometricPreferences
import com.sylo.core.security.crypto.BiometricCryptoManager
import com.sylo.core.security.crypto.CryptoKeyManager
import com.sylo.core.security.crypto.SessionKeyHolder
import com.sylo.core.security.pin.PinManager
import com.sylo.core.ui.R
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.Cipher
import javax.inject.Inject

data class AuthUiState(
    val enteredDigits: Int = 0,
    val biometricEnabled: Boolean = false,
    val error: String? = null,
    /** True while the PIN hash is computed/verified off the main thread. */
    val isVerifying: Boolean = false,
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
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val pin = StringBuilder()

    private val _uiState = MutableStateFlow(AuthUiState(biometricEnabled = biometricPreferences.isEnabled()))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun hasPin(): Boolean = pinManager.hasPin()

    /** True only when there's a PIN, the user opted in, and biometric is offerable. */
    fun canOfferBiometric(): Boolean =
        hasPin() && biometricPreferences.isEnabled() &&
            biometricAuthenticator.canAuthenticate().usable

    /** Whether biometric can be offered (for enabling the toggle during setup). */
    fun biometricHardwareAvailable(): Boolean =
        biometricAuthenticator.canAuthenticate().usable

    fun onDigit(digit: Char) {
        if (_uiState.value.isVerifying || pin.length >= PIN_LENGTH) return
        pin.append(digit)
        syncDots()
    }

    fun onBackspace() {
        if (_uiState.value.isVerifying || pin.isEmpty()) return
        pin.deleteCharAt(pin.length - 1)
        syncDots()
    }

    fun toggleBiometric() = _uiState.update { it.copy(biometricEnabled = !it.biometricEnabled) }

    /**
     * Persist a freshly-created PIN + the biometric preference, then invoke [onDone].
     *
     * PBKDF2 (120k iterations) runs on a background dispatcher: hashing synchronously
     * inside the 4th key's click handler used to freeze the frame before the last dot
     * could render. [MIN_VERIFY_MILLIS] keeps the verifying state on screen long
     * enough for the dot-fill animation to complete instead of hard-cutting away.
     */
    fun submitSetup(onDone: () -> Unit) {
        if (pin.length != PIN_LENGTH || _uiState.value.isVerifying) return
        val candidate = pin.toString()
        _uiState.update { it.copy(isVerifying = true, error = null) }
        viewModelScope.launch {
            val started = SystemClock.uptimeMillis()
            withContext(Dispatchers.Default) {
                pinManager.setPin(candidate)
                biometricPreferences.setEnabled(_uiState.value.biometricEnabled && biometricHardwareAvailable())
            }
            awaitMinDuration(started)
            reset()
            onDone()
        }
    }

    /** Verify an entered PIN against the stored hash (async, same rationale as [submitSetup]). */
    fun submitUnlock(onUnlocked: () -> Unit) {
        if (pin.length != PIN_LENGTH || _uiState.value.isVerifying) return
        val candidate = pin.toString()
        _uiState.update { it.copy(isVerifying = true, error = null) }
        viewModelScope.launch {
            val started = SystemClock.uptimeMillis()
            val ok = withContext(Dispatchers.Default) { pinManager.verifyPin(candidate) }
            awaitMinDuration(started)
            if (ok) {
                reset()
                onUnlocked()
            } else {
                pin.clear()
                _uiState.update {
                    it.copy(enteredDigits = 0, isVerifying = false, error = appContext.getString(R.string.unlock_incorrect_pin))
                }
            }
        }
    }

    /** Keeps the verifying state visible for at least [MIN_VERIFY_MILLIS]. */
    private suspend fun awaitMinDuration(startedUptimeMillis: Long) {
        val elapsed = SystemClock.uptimeMillis() - startedUptimeMillis
        if (elapsed < MIN_VERIFY_MILLIS) delay(MIN_VERIFY_MILLIS - elapsed)
    }

    fun showBiometricError(message: String) = _uiState.update { it.copy(error = message) }

    // ----- Biometric ⇄ DB-key binding (CryptoObject) -----

    /** Whether the DB passphrase is already wrapped by the biometric Keystore key. */
    fun isDbKeyBound(): Boolean = biometricCryptoManager.isBound()

    /**
     * If the just-created setup opted into biometrics and the DB isn't bound yet,
     * returns an ENCRYPT cipher to authenticate before wrapping the passphrase.
     *
     * Requires a STRONG (Class 3) biometric — only that can back a Keystore
     * CryptoObject. On weak-only devices this returns null, so binding is skipped
     * and biometric acts as a plain unlock gate (the DB key stays Keystore-protected).
     */
    fun encryptCipherForBinding(): Cipher? =
        if (_uiState.value.biometricEnabled && biometricAuthenticator.hasStrongBiometric() && !biometricCryptoManager.isBound()) {
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
        _uiState.update { it.copy(enteredDigits = 0, error = null, isVerifying = false) }
    }

    private fun syncDots() = _uiState.update { it.copy(enteredDigits = pin.length, error = null) }

    private companion object {
        /**
         * Floor for how long the verifying state stays visible. The dot-fill spring
         * needs ~250ms; anything shorter reads as a glitch, anything much longer
         * feels sluggish.
         */
        const val MIN_VERIFY_MILLIS = 350L
    }
}
