package com.clonewhatsapp.core.network.auth

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Interceptor that adds JWT Bearer token to request headers
 * Skips authentication for login and register endpoints
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStorage: SecureTokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // Skip auth header for login and register endpoints
        if (shouldSkipAuth(path)) {
            return chain.proceed(request)
        }

        // Get token from secure storage
        val token = tokenStorage.getToken()

        // If no token, proceed without auth header
        if (token.isNullOrEmpty()) {
            return chain.proceed(request)
        }

        // Add Authorization header with Bearer token
        val authenticatedRequest = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }

    /**
     * Check if the request path should skip authentication
     */
    private fun shouldSkipAuth(path: String): Boolean {
        return AUTH_SKIP_PATHS.any { path.contains(it) }
    }

    companion object {
        private val AUTH_SKIP_PATHS = listOf(
            "/api/auth/login",
            "/api/auth/register"
        )
    }
}
