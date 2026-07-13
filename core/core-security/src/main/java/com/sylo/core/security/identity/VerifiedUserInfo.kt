package com.sylo.core.security.identity

/**
 * The user attributes extracted (client-side) from a verified-email Digital Credential.
 *
 * NOTE: these values are parsed on-device for display only. They are NOT proof of
 * verification — full cryptographic validation of the SD-JWT (issuer, signature,
 * key-binding, nonce) must happen server-side before they can be trusted.
 */
data class VerifiedUserInfo(
    val email: String,
    val displayName: String,
    val emailVerified: Boolean = false,
)

/** Outcome of a verified-email request, keeping androidx.credentials types out of callers. */
sealed interface VerifiedEmailResult {
    data class Success(val info: VerifiedUserInfo) : VerifiedEmailResult

    /** User dismissed the Credential Manager sheet. */
    data object Cancelled : VerifiedEmailResult

    /** No eligible verifiable-email credential on this device. */
    data object NoCredential : VerifiedEmailResult

    data class Error(val message: String) : VerifiedEmailResult
}
