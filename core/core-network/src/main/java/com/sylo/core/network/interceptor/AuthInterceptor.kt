package com.sylo.core.network.interceptor

import com.sylo.core.network.auth.AuthTokenProvider
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.net.HttpURLConnection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the bearer token to every outgoing request and performs a single
 * synchronous token refresh + retry when the server responds with 401.
 *
 * Requests can opt out of auth (e.g. login) by adding the [NO_AUTH_HEADER] header,
 * which is stripped before the request leaves the client.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenProvider: AuthTokenProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        // Public endpoints opt out of the Authorization header entirely.
        if (original.header(NO_AUTH_HEADER) != null) {
            return chain.proceed(original.newBuilder().removeHeader(NO_AUTH_HEADER).build())
        }

        val token = tokenProvider.currentAccessToken()
        val response = chain.proceed(original.authorized(token))

        if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
            response.close()
            val refreshed = tokenProvider.refreshAccessToken()
                ?: return chain.proceed(original.authorized(token)) // give up: surface 401
            return chain.proceed(original.authorized(refreshed))
        }
        return response
    }

    private fun Request.authorized(token: String?): Request =
        if (token.isNullOrBlank()) this
        else newBuilder().header("Authorization", "Bearer $token").build()

    companion object {
        /** Add this header to a request to skip authentication (e.g. login/refresh). */
        const val NO_AUTH_HEADER = "X-Sylo-No-Auth"
    }
}
