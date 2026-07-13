package com.sylo.core.security.identity

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrieves a cryptographically **verified email** with the Credential Manager
 * Digital Credentials API (OpenID4VP) — an OTP-less way to attach a trusted
 * identity to the local Sylo profile.
 *
 * This is the client-side integration only. The returned claims are parsed on-device
 * for display; production use must forward the raw response + nonce to a backend for
 * full cryptographic validation (issuer, SD-JWT signature, key-binding, nonce replay).
 *
 * Requires Android 9 (API 28)+ and Google Play services 25.49+.
 */
@Singleton
class VerifiedEmailManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val credentialManager = CredentialManager.create(context)

    /**
     * Presents the Credential Manager UI and returns the verified email on success.
     * [activity] should be an Activity context so the system sheet can attach.
     */
    @OptIn(ExperimentalDigitalCredentialApi::class)
    suspend fun requestVerifiedEmail(activity: Context): VerifiedEmailResult {
        val nonce = generateSecureRandomNonce()
        val option = GetDigitalCredentialOption(requestJson = buildOpenId4vpRequest(nonce))
        val request = GetCredentialRequest(listOf(option))

        return try {
            val response = credentialManager.getCredential(activity, request)
            when (val credential = response.credential) {
                is DigitalCredential -> parseResponse(credential.credentialJson, nonce)
                else -> VerifiedEmailResult.Error("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            VerifiedEmailResult.Cancelled
        } catch (e: NoCredentialException) {
            VerifiedEmailResult.NoCredential
        } catch (e: GetCredentialException) {
            Log.w(TAG, "Verified-email request failed", e)
            VerifiedEmailResult.Error(e.message ?: "Verification failed")
        }
    }

    /**
     * Preliminary client-side parse of the OpenID4VP response for display. The raw
     * [responseJsonString] and [nonce] must still be validated server-side.
     */
    private fun parseResponse(responseJsonString: String, nonce: String): VerifiedEmailResult =
        runCatching {
            val vpToken = JSONObject(responseJsonString).getJSONObject("vp_token")
            val credentialId = vpToken.keys().next()
            val rawSdJwt = vpToken.getJSONArray(credentialId).getString(0)

            val claims = SdJwtParser.parse(rawSdJwt)

            // TODO(server): POST responseJsonString + nonce to the Sylo backend for full
            //  cryptographic validation before trusting this identity:
            //   - iss == https://verifiablecredentials-pa.googleapis.com
            //   - SD-JWT signature against .well-known/vc-public-jwks
            //   - cnf / key-binding proves same-device presentation
            //   - nonce matches and has not been replayed
            val info = VerifiedUserInfo(
                email = claims.getString("email"),
                displayName = claims.optString("name", claims.optString("email")),
                emailVerified = claims.optBoolean("email_verified", false),
            )
            VerifiedEmailResult.Success(info)
        }.getOrElse { e ->
            Log.w(TAG, "Could not parse verified-email response", e)
            VerifiedEmailResult.Error("Couldn't read the verified email")
        }

    private fun generateSecureRandomNonce(): String {
        val bytes = ByteArray(NONCE_BYTES).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /** OpenID4VP request for a UserInfoCredential (verified email + profile claims). */
    private fun buildOpenId4vpRequest(nonce: String): String = """
        {
          "requests": [
            {
              "protocol": "openid4vp-v1-unsigned",
              "data": {
                "response_type": "vp_token",
                "response_mode": "dc_api",
                "nonce": "$nonce",
                "dcql_query": {
                  "credentials": [
                    {
                      "id": "user_info_query",
                      "format": "dc+sd-jwt",
                      "meta": { "vct_values": ["UserInfoCredential"] },
                      "claims": [
                        {"path": ["email"]},
                        {"path": ["name"]},
                        {"path": ["given_name"]},
                        {"path": ["family_name"]},
                        {"path": ["picture"]},
                        {"path": ["hd"]},
                        {"path": ["email_verified"]}
                      ]
                    }
                  ]
                }
              }
            }
          ]
        }
    """.trimIndent()

    private companion object {
        const val TAG = "VerifiedEmail"
        const val NONCE_BYTES = 32
    }
}
