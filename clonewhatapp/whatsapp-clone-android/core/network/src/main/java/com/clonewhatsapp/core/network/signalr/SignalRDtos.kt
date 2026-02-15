package com.clonewhatsapp.core.network.signalr

/**
 * DTOs que coinciden con los contratos del backend SignalR Hub.
 * Estos se serializan/deserializan automáticamente por el cliente SignalR de Microsoft.
 */

data class MensajeEnviadoDto(
    val mensajeId: String,
    val chatId: String,
    val mensaje: MensajeSignalRDto
)

data class MensajeSignalRDto(
    val id: String,
    val chatId: String,
    val remitenteId: String,
    val remitenteNombre: String?,
    val contenido: String?,
    val tipo: String,
    val estado: String,
    val urlArchivo: String?,
    val fechaEnvio: String
)

data class TypingDto(
    val chatId: String,
    val usuarioId: String,
    val nombreUsuario: String,
    val estaEscribiendo: Boolean
)

data class MessageStatusDto(
    val messageId: String,
    val status: String
)

data class UserStatusDto(
    val userId: String,
    val isOnline: Boolean,
    val lastSeen: String?
)

data class GroupUpdatedDto(
    val chatId: String,
    val nombre: String,
    val descripcion: String?,
    val imagen: String?
)
