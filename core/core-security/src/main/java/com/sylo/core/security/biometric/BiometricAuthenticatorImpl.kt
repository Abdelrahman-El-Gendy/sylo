package com.sylo.core.security.biometric

import android.content.Context
import android.content.pm.PackageManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.crypto.Cipher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiometricAuthenticatorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : BiometricAuthenticator {

    // Accept weak OR strong sensors for plain unlock so weak-classified fingerprints
    // (common on older devices) are still usable.
    private val anyBiometric = BIOMETRIC_STRONG or BIOMETRIC_WEAK

    override fun canAuthenticate(): BiometricAvailability =
        when (BiometricManager.from(context).canAuthenticate(anyBiometric)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            // BIOMETRIC_STATUS_UNKNOWN (-1) / other non-decisive codes: the modern
            // BiometricManager couldn't determine status (common on OEM API 28–29).
            else -> fallbackAvailability()
        }

    /**
     * Resolves availability when the modern API is non-decisive. Tries the legacy
     * fingerprint API; if that can't see the sensor either but the device still
     * advertises one (e.g. OPPO/ColorOS Android 9, whose fingerprint sits behind a
     * proprietary HAL invisible to both APIs), returns [BiometricAvailability.UNKNOWN]
     * so biometric is offered optimistically — the prompt attempt is the real test.
     */
    @Suppress("DEPRECATION")
    private fun fallbackAvailability(): BiometricAvailability {
        val fm = FingerprintManagerCompat.from(context)
        val hasHardware = runCatching { fm.isHardwareDetected }.getOrDefault(false)
        val enrolled = runCatching { fm.hasEnrolledFingerprints() }.getOrDefault(false)
        return when {
            hasHardware && enrolled -> BiometricAvailability.AVAILABLE
            hasHardware && !enrolled -> BiometricAvailability.NOT_ENROLLED
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) ->
                BiometricAvailability.UNKNOWN
            else -> BiometricAvailability.NO_HARDWARE
        }
    }

    override fun hasStrongBiometric(): Boolean =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    override fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onResult: (BiometricResult) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onResult(BiometricResult.Success)
                }

                override fun onAuthenticationFailed() {
                    onResult(BiometricResult.Failed)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onResult(BiometricResult.Error(errorCode, errString.toString()))
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use PIN")
            // Plain unlock (no CryptoObject) can use a weak sensor too.
            .setAllowedAuthenticators(anyBiometric)
            .build()

        prompt.authenticate(promptInfo)
    }

    override fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        cipher: Cipher,
        onResult: (BiometricCipherResult) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authedCipher = result.cryptoObject?.cipher
                    if (authedCipher != null) {
                        onResult(BiometricCipherResult.Success(authedCipher))
                    } else {
                        onResult(BiometricCipherResult.Error(-1, "No authenticated cipher"))
                    }
                }

                override fun onAuthenticationFailed() {
                    onResult(BiometricCipherResult.Failed)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onResult(BiometricCipherResult.Error(errorCode, errString.toString()))
                }
            },
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }
}
