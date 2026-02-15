package com.clonewhatsapp.core.network.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Interfaz Retrofit para endpoints de notificaciones push.
 * Gestiona el registro y eliminacion de tokens FCM en el backend.
 */
interface PushApi {

    /**
     * Registra el token FCM del dispositivo en el backend.
     * POST /api/push/subscribe
     * @param request Datos del token FCM y claves de autenticacion
     */
    @POST("api/push/subscribe")
    suspend fun registrarToken(@Body request: RegistrarTokenRequest): Response<Unit>

    /**
     * Elimina el token FCM del dispositivo en el backend (logout).
     * POST /api/push/unsubscribe
     * @param request Token FCM a eliminar
     */
    @POST("api/push/unsubscribe")
    suspend fun eliminarToken(@Body request: EliminarTokenRequest): Response<Unit>
}

// -----------------------------------------------------------------------
// DTOs para la API de Push
// -----------------------------------------------------------------------

/**
 * Request para registrar un token FCM en el backend.
 * Se adapta al formato esperado por el backend (endpoint + keys).
 *
 * @param endpoint Token FCM del dispositivo
 * @param keys Claves de autenticacion push (p256dh y auth)
 */
data class RegistrarTokenRequest(
    @SerializedName("endpoint")
    val endpoint: String,

    @SerializedName("keys")
    val keys: PushKeysDto
)

/**
 * Claves de autenticacion para push notifications.
 * En el caso de FCM nativo, se envian valores placeholder
 * ya que Firebase maneja la encriptacion internamente.
 *
 * @param p256dh Clave publica ECDH P-256 (placeholder para FCM)
 * @param auth Secreto de autenticacion (placeholder para FCM)
 */
data class PushKeysDto(
    @SerializedName("p256dh")
    val p256dh: String,

    @SerializedName("auth")
    val auth: String
)

/**
 * Request para eliminar un token FCM del backend.
 *
 * @param endpoint Token FCM a eliminar
 */
data class EliminarTokenRequest(
    @SerializedName("endpoint")
    val endpoint: String
)
