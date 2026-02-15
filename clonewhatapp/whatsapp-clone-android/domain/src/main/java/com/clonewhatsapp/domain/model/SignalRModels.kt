package com.clonewhatsapp.domain.model

/**
 * Modelos de dominio para eventos de SignalR.
 * Desacoplados de los DTOs de red para mantener la separacion de capas.
 */

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

data class ReceivedMessage(
    val mensajeId: String,
    val chatId: String,
    val remitenteId: String,
    val remitenteNombre: String?,
    val contenido: String?,
    val tipo: String,
    val estado: String,
    val urlArchivo: String?,
    val fechaEnvio: Long
)

data class TypingInfo(
    val chatId: String,
    val usuarioId: String,
    val nombreUsuario: String,
    val isTyping: Boolean
)

data class MessageStatusUpdate(
    val messageId: String,
    val status: String
)

data class UserStatusUpdate(
    val userId: String,
    val isOnline: Boolean,
    val lastSeen: Long?
)
