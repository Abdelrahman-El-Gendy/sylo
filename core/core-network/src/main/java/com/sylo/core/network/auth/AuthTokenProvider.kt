package com.sylo.core.network.auth

/**
 * Supplies the current access token (and a way to refresh) to the networking layer
 * WITHOUT :core-network depending on :core-security or :feature-auth.
 *
 * The real implementation (backed by the Android Keystore / EncryptedSharedPreferences)
 * is provided by the security/auth layer and bound into the graph there. This keeps
 * the dependency direction one-way: features/security implement, network consumes.
 */
interface AuthTokenProvider {

    /** The current bearer token, or null when the user is not authenticated. */
    fun currentAccessToken(): String?

    /**
     * Called when the server rejects the current token (HTTP 401). Should attempt a
     * synchronous refresh and return the new token, or null if refresh failed.
     */
    fun refreshAccessToken(): String?
}
