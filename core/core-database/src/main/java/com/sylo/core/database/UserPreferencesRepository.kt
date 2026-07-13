package com.sylo.core.database

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesStore by preferencesDataStore("sylo_user_prefs")

/**
 * User-set account preferences: an opening balance and the display currency. The
 * dashboard balance = opening balance + net of all transactions.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val openingBalanceKey = longPreferencesKey("opening_balance_minor")
    private val currencyKey = stringPreferencesKey("currency")
    private val displayNameKey = stringPreferencesKey("display_name")
    private val profilePhotoPathKey = stringPreferencesKey("profile_photo_path")
    private val verifiedEmailKey = stringPreferencesKey("verified_email")
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val balanceConfiguredKey = booleanPreferencesKey("balance_configured")
    private val smsAutoCaptureEnabledKey = booleanPreferencesKey("sms_auto_capture_enabled")
    private val smsLastProcessedMillisKey = longPreferencesKey("sms_last_processed_millis")

    private val prefs: Flow<androidx.datastore.preferences.core.Preferences> =
        context.userPreferencesStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    val openingBalanceMinor: Flow<Long> = prefs.map { it[openingBalanceKey] ?: 0L }

    val currency: Flow<String> = prefs.map { it[currencyKey] ?: DEFAULT_CURRENCY }

    /** The user's display name shown on the Profile screen. */
    val displayName: Flow<String> = prefs.map { it[displayNameKey] ?: DEFAULT_NAME }

    /** Absolute path to the user's profile photo in app-internal storage, if set. */
    val profilePhotoPath: Flow<String?> = prefs.map { it[profilePhotoPathKey] }

    /** The user's cryptographically verified email (via Credential Manager), if set. */
    val verifiedEmail: Flow<String?> = prefs.map { it[verifiedEmailKey] }

    /** Whether the first-run onboarding has been completed. */
    val onboardingCompleted: Flow<Boolean> = prefs.map { it[onboardingCompletedKey] ?: false }

    /**
     * Whether the user has completed the first-run opening-balance setup. Gates the
     * mandatory balance step shown once, right after registration.
     */
    val balanceConfigured: Flow<Boolean> = prefs.map { it[balanceConfiguredKey] ?: false }

    /** Whether the interval-based bank-SMS auto-capture is switched on. */
    val smsAutoCaptureEnabled: Flow<Boolean> = prefs.map { it[smsAutoCaptureEnabledKey] ?: false }

    /**
     * High-water mark of the newest SMS (by received time) already imported, so each
     * rescan only processes messages that arrived since the last run — no duplicates.
     */
    val smsLastProcessedMillis: Flow<Long> = prefs.map { it[smsLastProcessedMillisKey] ?: 0L }

    suspend fun setOpeningBalance(amountMinor: Long) {
        context.userPreferencesStore.edit { it[openingBalanceKey] = amountMinor }
    }

    suspend fun setCurrency(code: String) {
        context.userPreferencesStore.edit { it[currencyKey] = code }
    }

    suspend fun setDisplayName(name: String) {
        context.userPreferencesStore.edit { it[displayNameKey] = name }
    }

    suspend fun setProfilePhotoPath(path: String?) {
        context.userPreferencesStore.edit {
            if (path == null) it.remove(profilePhotoPathKey) else it[profilePhotoPathKey] = path
        }
    }

    suspend fun setVerifiedEmail(email: String?) {
        context.userPreferencesStore.edit {
            if (email == null) it.remove(verifiedEmailKey) else it[verifiedEmailKey] = email
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.userPreferencesStore.edit { it[onboardingCompletedKey] = completed }
    }

    suspend fun setBalanceConfigured(configured: Boolean) {
        context.userPreferencesStore.edit { it[balanceConfiguredKey] = configured }
    }

    suspend fun setSmsAutoCaptureEnabled(enabled: Boolean) {
        context.userPreferencesStore.edit { it[smsAutoCaptureEnabledKey] = enabled }
    }

    suspend fun setSmsLastProcessedMillis(millis: Long) {
        context.userPreferencesStore.edit { it[smsLastProcessedMillisKey] = millis }
    }

    companion object {
        const val DEFAULT_CURRENCY = "USD"
        const val DEFAULT_NAME = "Sylo User"
        val CURRENCIES = listOf("USD", "EUR", "GBP", "EGP", "AED", "SAR", "INR", "JPY")
    }
}
