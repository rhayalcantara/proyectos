package com.clonewhatsapp.core.network.repository

import com.clonewhatsapp.core.network.api.AuthApi
import com.clonewhatsapp.core.network.auth.SecureTokenStorage
import com.clonewhatsapp.core.network.dto.AuthResponse
import com.clonewhatsapp.core.network.dto.LoginRequest
import com.clonewhatsapp.core.network.dto.RegisterRequest
import com.clonewhatsapp.domain.model.Usuario
import com.clonewhatsapp.domain.repository.AuthRepository
import com.google.gson.Gson
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AuthRepository
 * Handles authentication API calls and maps DTOs to domain models
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStorage: SecureTokenStorage,
    private val gson: Gson
) : AuthRepository {

    /**
     * Login with phone number and password
     */
    override suspend fun login(phone: String, password: String): Result<Usuario> {
        return try {
            val request = LoginRequest(
                numeroTelefono = phone,
                password = password
            )

            val response = authApi.login(request)

            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse != null) {
                    // Save authentication data
                    saveAuthData(authResponse)

                    // Map to domain model
                    val usuario = mapAuthResponseToUsuario(authResponse)
                    Result.success(usuario)
                } else {
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                val errorMessage = parseErrorMessage(response)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register a new user
     */
    override suspend fun register(name: String, phone: String, password: String): Result<Usuario> {
        return try {
            val request = RegisterRequest(
                numeroTelefono = phone,
                nombre = name,
                password = password
            )

            val response = authApi.register(request)

            if (response.isSuccessful) {
                val authResponse = response.body()
                if (authResponse != null) {
                    // Save authentication data
                    saveAuthData(authResponse)

                    // Map to domain model
                    val usuario = mapAuthResponseToUsuario(authResponse)
                    Result.success(usuario)
                } else {
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                val errorMessage = parseErrorMessage(response)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Logout the current user
     */
    override suspend fun logout() {
        tokenStorage.clear()
    }

    /**
     * Check if user is logged in
     */
    override fun isLoggedIn(): Boolean {
        return tokenStorage.isLoggedIn()
    }

    /**
     * Get current user ID
     */
    override fun getCurrentUserId(): String? {
        return tokenStorage.getUserId()
    }

    /**
     * Get current user name
     */
    override fun getCurrentUserName(): String? {
        return tokenStorage.getUserName()
    }

    /**
     * Get current user phone
     */
    override fun getCurrentUserPhone(): String? {
        return tokenStorage.getUserPhone()
    }

    /**
     * Save authentication data to secure storage
     */
    private fun saveAuthData(authResponse: AuthResponse) {
        tokenStorage.saveToken(authResponse.token)
        tokenStorage.saveUserId(authResponse.id)
        tokenStorage.saveUserName(authResponse.nombre)
        tokenStorage.saveUserPhone(authResponse.numeroTelefono)

        // Parse and save token expiration
        try {
            val expirationMillis = parseIso8601ToMillis(authResponse.tokenExpiration)
            tokenStorage.saveTokenExpiration(expirationMillis)
        } catch (e: Exception) {
            // If parsing fails, set expiration to 7 days from now (default backend behavior)
            val defaultExpiration = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)
            tokenStorage.saveTokenExpiration(defaultExpiration)
        }
    }

    /**
     * Map AuthResponse DTO to Usuario domain model
     */
    private fun mapAuthResponseToUsuario(authResponse: AuthResponse): Usuario {
        return Usuario(
            id = authResponse.id,
            nombre = authResponse.nombre,
            telefono = authResponse.numeroTelefono,
            fotoPerfil = authResponse.fotoPerfil,
            estado = null, // Not included in AuthResponse
            isOnline = false, // Not included in AuthResponse
            ultimaConexion = null // Not included in AuthResponse
        )
    }

    /**
     * Parse error message from HTTP response
     */
    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                // Try to parse JSON error response
                val errorResponse = gson.fromJson(errorBody, Map::class.java)
                errorResponse["message"]?.toString() ?: "Error desconocido"
            } else {
                when (response.code()) {
                    400 -> "Datos inválidos"
                    401 -> "Credenciales inválidas"
                    404 -> "Usuario no encontrado"
                    500 -> "Error del servidor"
                    else -> "Error de conexión (${response.code()})"
                }
            }
        } catch (e: Exception) {
            "Error de conexión"
        }
    }

    /**
     * Parse ISO 8601 date string to milliseconds
     */
    private fun parseIso8601ToMillis(dateString: String): Long {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.parse(dateString)?.time ?: 0L
    }
}
