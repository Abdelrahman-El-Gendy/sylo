package com.sylo.core.security.session

import com.sylo.core.security.crypto.SecurePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the authenticated session (access/refresh tokens) in encrypted storage and
 * exposes login state as a [StateFlow] the UI can observe.
 */
@Singleton
class SessionManager @Inject constructor(
    private val securePreferences: SecurePreferences,
) {
    private val _isLoggedIn = MutableStateFlow(accessToken() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun accessToken(): String? = securePreferences.getString(KEY_ACCESS_TOKEN)

    fun refreshToken(): String? = securePreferences.getString(KEY_REFRESH_TOKEN)

    fun updateTokens(accessToken: String, refreshToken: String?) {
        securePreferences.putString(KEY_ACCESS_TOKEN, accessToken)
        refreshToken?.let { securePreferences.putString(KEY_REFRESH_TOKEN, it) }
        _isLoggedIn.value = true
    }

    fun clear() {
        securePreferences.remove(KEY_ACCESS_TOKEN)
        securePreferences.remove(KEY_REFRESH_TOKEN)
        _isLoggedIn.value = false
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
