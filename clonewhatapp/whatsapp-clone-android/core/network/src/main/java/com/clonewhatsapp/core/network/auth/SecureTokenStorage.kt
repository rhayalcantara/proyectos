package com.clonewhatsapp.core.network.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for JWT tokens and user session data
 * Uses EncryptedSharedPreferences from androidx.security.crypto
 */
@Singleton
class SecureTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val sharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Save JWT authentication token
     */
    fun saveToken(token: String) {
        sharedPreferences.edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    /**
     * Get stored JWT token
     * @return Token string or null if not found
     */
    fun getToken(): String? {
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    /**
     * Save user ID (UUID from backend)
     */
    fun saveUserId(userId: String) {
        sharedPreferences.edit()
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    /**
     * Get stored user ID
     * @return User ID or null if not found
     */
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    /**
     * Save user name
     */
    fun saveUserName(userName: String) {
        sharedPreferences.edit()
            .putString(KEY_USER_NAME, userName)
            .apply()
    }

    /**
     * Get stored user name
     * @return User name or null if not found
     */
    fun getUserName(): String? {
        return sharedPreferences.getString(KEY_USER_NAME, null)
    }

    /**
     * Save user phone number
     */
    fun saveUserPhone(phone: String) {
        sharedPreferences.edit()
            .putString(KEY_USER_PHONE, phone)
            .apply()
    }

    /**
     * Get stored user phone number
     * @return Phone number or null if not found
     */
    fun getUserPhone(): String? {
        return sharedPreferences.getString(KEY_USER_PHONE, null)
    }

    /**
     * Save token expiration timestamp
     */
    fun saveTokenExpiration(expiration: Long) {
        sharedPreferences.edit()
            .putLong(KEY_TOKEN_EXPIRATION, expiration)
            .apply()
    }

    /**
     * Get token expiration timestamp
     * @return Expiration timestamp in milliseconds or 0 if not found
     */
    fun getTokenExpiration(): Long {
        return sharedPreferences.getLong(KEY_TOKEN_EXPIRATION, 0)
    }

    /**
     * Check if user is logged in
     * @return true if token exists and is not expired
     */
    fun isLoggedIn(): Boolean {
        val token = getToken()
        val expiration = getTokenExpiration()
        val currentTime = System.currentTimeMillis()

        return !token.isNullOrEmpty() && expiration > currentTime
    }

    /**
     * Clear all stored authentication data (logout)
     */
    fun clear() {
        sharedPreferences.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_PHONE)
            .remove(KEY_TOKEN_EXPIRATION)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "secure_auth_prefs"
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_TOKEN_EXPIRATION = "token_expiration"
    }
}
