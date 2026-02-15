package com.clonewhatsapp.core.network.api

import com.clonewhatsapp.core.network.dto.AuthResponse
import com.clonewhatsapp.core.network.dto.LoginRequest
import com.clonewhatsapp.core.network.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API interface for authentication endpoints
 * Matches backend AuthController routes
 */
interface AuthApi {

    /**
     * Register a new user
     * POST /api/auth/register
     * @param request Registration details (phone, name, password)
     * @return AuthResponse with user info and JWT token
     */
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    /**
     * Login an existing user
     * POST /api/auth/login
     * @param request Login credentials (phone, password)
     * @return AuthResponse with user info and JWT token
     */
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
}
