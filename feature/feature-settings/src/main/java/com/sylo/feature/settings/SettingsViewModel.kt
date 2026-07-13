package com.sylo.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sylo.core.database.TransactionRepository
import com.sylo.core.database.UserPreferencesRepository
import com.sylo.core.security.biometric.BiometricAuthenticator
import com.sylo.core.security.biometric.BiometricAvailability
import com.sylo.core.security.biometric.BiometricPreferences
import com.sylo.core.security.crypto.BiometricCryptoManager
import com.sylo.core.security.identity.VerifiedEmailManager
import com.sylo.core.security.identity.VerifiedEmailResult
import com.sylo.core.ui.component.formatMoney
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import javax.inject.Inject

data class ProfileStats(
    val totalBalance: String = "$0.00",
    val daysTracking: String = "0",
    val transactions: String = "0",
    val thisMonth: String = "$0.00",
)

data class SettingsUiState(
    val biometricEnabled: Boolean = false,
    val dbBiometricBound: Boolean = false,
    val stats: ProfileStats = ProfileStats(),
    val displayName: String = "Sylo User",
    val profilePhotoPath: String? = null,
    val smsImportEnabled: Boolean = false,
    val verifiedEmail: String? = null,
    val verifyingEmail: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val biometricPreferences: BiometricPreferences,
    private val biometricAuthenticator: BiometricAuthenticator,
    private val biometricCryptoManager: BiometricCryptoManager,
    private val transactionRepository: TransactionRepository,
    private val userPreferences: UserPreferencesRepository,
    private val verifiedEmailManager: VerifiedEmailManager,
) : ViewModel() {

    val biometricAvailable: Boolean
        get() = biometricAuthenticator.canAuthenticate() == BiometricAvailability.AVAILABLE

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            biometricEnabled = biometricPreferences.isEnabled(),
            dbBiometricBound = biometricCryptoManager.isBound(),
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            transactionRepository.observeAll().collect { txns ->
                _uiState.update { it.copy(stats = computeStats(txns)) }
            }
        }
        viewModelScope.launch {
            userPreferences.displayName.collect { name ->
                _uiState.update { it.copy(displayName = name) }
            }
        }
        viewModelScope.launch {
            userPreferences.profilePhotoPath.collect { path ->
                _uiState.update { it.copy(profilePhotoPath = path) }
            }
        }
        viewModelScope.launch {
            userPreferences.smsAutoCaptureEnabled.collect { enabled ->
                _uiState.update { it.copy(smsImportEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            userPreferences.verifiedEmail.collect { email ->
                _uiState.update { it.copy(verifiedEmail = email) }
            }
        }
    }

    /**
     * Runs the OTP-less verified-email flow (Credential Manager Digital Credentials).
     * On success, persists the email and adopts the credential's name as the display
     * name. [activity] must be an Activity context; [onMessage] surfaces a user-facing
     * result string for a snackbar.
     */
    fun verifyEmail(activity: Context, onMessage: (String) -> Unit) {
        if (_uiState.value.verifyingEmail) return
        _uiState.update { it.copy(verifyingEmail = true) }
        viewModelScope.launch {
            when (val result = verifiedEmailManager.requestVerifiedEmail(activity)) {
                is VerifiedEmailResult.Success -> {
                    val info = result.info
                    userPreferences.setVerifiedEmail(info.email)
                    if (info.displayName.isNotBlank() && info.displayName != info.email) {
                        userPreferences.setDisplayName(info.displayName)
                    }
                    // TODO(server): forward the raw credential response + nonce to the Sylo
                    //  backend for full cryptographic validation before trusting this identity.
                    onMessage("Verified ${info.email}")
                }
                VerifiedEmailResult.Cancelled -> Unit // user dismissed — stay quiet
                VerifiedEmailResult.NoCredential ->
                    onMessage("No verifiable email found on this device")
                is VerifiedEmailResult.Error ->
                    onMessage("Couldn't verify email. Please try again.")
            }
            _uiState.update { it.copy(verifyingEmail = false) }
        }
    }

    /**
     * Toggles interval-based bank-SMS capture. Only the preference is written here;
     * :app observes it and (de)schedules the WorkManager scan. Caller must have already
     * obtained the READ_SMS grant before enabling.
     */
    fun setSmsImportEnabled(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setSmsAutoCaptureEnabled(enabled) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        val effective = enabled && biometricAvailable
        biometricPreferences.setEnabled(effective)
        if (!effective) biometricCryptoManager.clearBinding()
        _uiState.update { it.copy(biometricEnabled = effective, dbBiometricBound = biometricCryptoManager.isBound()) }
    }

    fun saveDisplayName(name: String) {
        val trimmed = name.trim().ifBlank { "Sylo User" }
        viewModelScope.launch { userPreferences.setDisplayName(trimmed) }
    }

    fun saveProfilePhoto(path: String?) {
        viewModelScope.launch { userPreferences.setProfilePhotoPath(path) }
    }

    private fun computeStats(txns: List<com.sylo.core.database.entity.TransactionEntity>): ProfileStats {
        val currency = txns.firstOrNull()?.currency ?: "USD"
        val balance = txns.sumOf { it.amountMinor }

        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val monthSpend = txns.filter { it.amountMinor < 0 && it.timestampEpochMillis >= monthStart }
            .sumOf { abs(it.amountMinor) }

        val days = txns.minByOrNull { it.timestampEpochMillis }?.let {
            ((System.currentTimeMillis() - it.timestampEpochMillis) / 86_400_000L + 1).toInt()
        } ?: 0

        return ProfileStats(
            totalBalance = formatMoney(balance, currency),
            daysTracking = days.toString(),
            transactions = txns.size.toString(),
            thisMonth = formatMoney(monthSpend, currency),
        )
    }
}
