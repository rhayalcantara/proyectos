package com.clonewhatsapp.domain.repository

import com.clonewhatsapp.domain.model.ConnectionState
import com.clonewhatsapp.domain.model.MessageStatusUpdate
import com.clonewhatsapp.domain.model.ReceivedMessage
import com.clonewhatsapp.domain.model.TypingInfo
import com.clonewhatsapp.domain.model.UserStatusUpdate
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositorio de dominio para la comunicacion en tiempo real via SignalR.
 * Define el contrato que la capa de datos debe implementar.
 */
interface SignalRRepository {

    /** Estado actual de la conexion SignalR */
    val connectionState: StateFlow<ConnectionState>

    /** Flujo de mensajes recibidos en tiempo real */
    val messageReceived: SharedFlow<ReceivedMessage>

    /** Flujo de notificaciones de escritura */
    val typingReceived: SharedFlow<TypingInfo>

    /** Flujo de actualizaciones de estado de mensajes (Entregado, Leido) */
    val messageStatusUpdated: SharedFlow<MessageStatusUpdate>

    /** Flujo de cambios de estado de usuarios (online/offline) */
    val userStatusChanged: SharedFlow<UserStatusUpdate>

    /** Conectar al hub de SignalR */
    suspend fun connect()

    /** Desconectar del hub de SignalR */
    suspend fun disconnect()

    /** Verificar si la conexion esta activa */
    fun isConnected(): Boolean

    /** Enviar un mensaje a traves de SignalR */
    suspend fun sendMessage(chatId: String, mensajeId: String, contenido: String, tipo: String)

    /** Notificar que el usuario esta escribiendo */
    suspend fun sendTyping(chatId: String, isTyping: Boolean)

    /** Marcar un mensaje como entregado */
    suspend fun markDelivered(messageId: String, chatId: String)

    /** Marcar un mensaje como leido */
    suspend fun markRead(messageId: String, chatId: String)

    /** Unirse a un chat para recibir mensajes en tiempo real */
    suspend fun joinChat(chatId: String)

    /** Salir de un chat */
    suspend fun leaveChat(chatId: String)
}
