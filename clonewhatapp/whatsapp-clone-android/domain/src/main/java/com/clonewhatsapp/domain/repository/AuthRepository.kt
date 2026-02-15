package com.clonewhatsapp.domain.repository

import com.clonewhatsapp.domain.model.Usuario

/**
 * Repository interface for authentication operations
 * Part of the domain layer - defines the contract for auth operations
 */
interface AuthRepository {

    /**
     * Login with phone number and password
     * @param phone User's phone number
     * @param password User's password
     * @return Result with Usuario on success, or error message on failure
     */
    suspend fun login(phone: String, password: String): Result<Usuario>

    /**
     * Register a new user
     * @param name User's display name
     * @param phone User's phone number
     * @param password User's password
     * @return Result with Usuario on success, or error message on failure
     */
    suspend fun register(name: String, phone: String, password: String): Result<Usuario>

    /**
     * Logout the current user
     * Clears all stored authentication data
     */
    suspend fun logout()

    /**
     * Check if user is currently logged in
     * @return true if user has valid authentication token
     */
    fun isLoggedIn(): Boolean

    /**
     * Get the current logged-in user's ID
     * @return User ID or null if not logged in
     */
    fun getCurrentUserId(): String?

    /**
     * Get the current logged-in user's name
     * @return User name or null if not logged in
     */
    fun getCurrentUserName(): String?

    /**
     * Get the current logged-in user's phone
     * @return Phone number or null if not logged in
     */
    fun getCurrentUserPhone(): String?
}
