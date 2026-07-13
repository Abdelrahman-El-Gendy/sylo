package com.sylo.core.security.identity

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/**
 * Minimal, client-side SD-JWT reader for **display purposes only**.
 *
 * An SD-JWT is `<issuer-jwt>~<disclosure>~<disclosure>~…~<optional-kb-jwt>`. This
 * decodes the issuer JWT payload and folds in each selective-disclosure value
 * (`[salt, name, value]`), returning the merged claims as a [JSONObject].
 *
 * It performs **no signature or key-binding verification** — that is the server's
 * job (see [VerifiedEmailManager]). Do not use these claims for security decisions.
 */
object SdJwtParser {

    fun parse(rawSdJwt: String): JSONObject {
        val segments = rawSdJwt.split("~")
        val issuerJwt = segments.firstOrNull().orEmpty()

        // Base claims from the issuer JWT payload (the middle segment of header.payload.sig).
        val claims = decodeJwtPayload(issuerJwt)

        // Fold in disclosed claims. Disclosures are single base64url tokens (no dots);
        // a trailing key-binding JWT (has dots) is skipped.
        segments.drop(1)
            .filter { it.isNotBlank() && !it.contains(".") }
            .forEach { disclosure ->
                runCatching {
                    val arr = JSONArray(String(base64UrlDecode(disclosure)))
                    // [salt, claimName, claimValue]
                    if (arr.length() >= 3) {
                        claims.put(arr.getString(1), arr.get(2))
                    }
                }
            }

        return claims
    }

    private fun decodeJwtPayload(jwt: String): JSONObject {
        val parts = jwt.split(".")
        if (parts.size < 2) return JSONObject()
        return runCatching { JSONObject(String(base64UrlDecode(parts[1]))) }.getOrElse { JSONObject() }
    }

    private fun base64UrlDecode(value: String): ByteArray =
        Base64.decode(value, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}
