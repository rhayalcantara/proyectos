package com.clonewhatsapp.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for user registration requests
 * Matches backend RegisterDTO
 */
data class RegisterRequest(
    @SerializedName("NumeroTelefono")
    val numeroTelefono: String,

    @SerializedName("Nombre")
    val nombre: String,

    @SerializedName("Password")
    val password: String
)

/**
 * DTO for user login requests
 * Matches backend LoginDTO
 */
data class LoginRequest(
    @SerializedName("NumeroTelefono")
    val numeroTelefono: String,

    @SerializedName("Password")
    val password: String
)

/**
 * DTO for authentication responses (login/register)
 * Matches backend AuthResponseDTO
 */
data class AuthResponse(
    @SerializedName("Id")
    val id: String, // UUID

    @SerializedName("NumeroTelefono")
    val numeroTelefono: String,

    @SerializedName("Nombre")
    val nombre: String,

    @SerializedName("FotoPerfil")
    val fotoPerfil: String?,

    @SerializedName("Token")
    val token: String,

    @SerializedName("TokenExpiration")
    val tokenExpiration: String // ISO 8601 date string
)

/**
 * DTO for user profile information
 * Matches backend UsuarioDTO
 */
data class UsuarioDto(
    @SerializedName("Id")
    val id: String, // UUID

    @SerializedName("NumeroTelefono")
    val numeroTelefono: String,

    @SerializedName("Nombre")
    val nombre: String,

    @SerializedName("FotoPerfil")
    val fotoPerfil: String?,

    @SerializedName("Estado")
    val estado: String,

    @SerializedName("UltimaConexion")
    val ultimaConexion: String, // ISO 8601 date string

    @SerializedName("EstaEnLinea")
    val estaEnLinea: Boolean
)

/**
 * DTO for updating user profile
 * Matches backend UpdatePerfilDTO
 */
data class UpdatePerfilRequest(
    @SerializedName("Nombre")
    val nombre: String?,

    @SerializedName("Estado")
    val estado: String?
)

/**
 * DTO for block status
 * Matches backend BlockStatusDTO
 */
data class BlockStatusDto(
    @SerializedName("EstaBloqueado")
    val estaBloqueado: Boolean,

    @SerializedName("MeBloquearon")
    val meBloquearon: Boolean
)

/**
 * Generic error response
 */
data class ErrorResponse(
    @SerializedName("message")
    val message: String
)
