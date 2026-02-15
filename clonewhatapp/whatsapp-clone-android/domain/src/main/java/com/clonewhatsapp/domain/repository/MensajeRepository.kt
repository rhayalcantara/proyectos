package com.clonewhatsapp.domain.repository

import com.clonewhatsapp.domain.model.Mensaje
import com.clonewhatsapp.domain.model.TipoMensaje
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for message operations
 * Part of the domain layer - defines the contract for message operations
 */
interface MensajeRepository {

    /**
     * Get messages for a chat as a Flow (paginated)
     * @param chatId Chat UUID
     * @param page Page number (1-based)
     * @param pageSize Number of messages per page
     * @return Flow emitting list of messages ordered by date
     */
    fun getMessages(chatId: String, page: Int = 1, pageSize: Int = 50): Flow<List<Mensaje>>

    /**
     * Send a text message
     * @param chatId Chat UUID
     * @param contenido Message content
     * @param tipo Message type (defaults to TEXTO)
     * @param mensajeRespondidoId Optional UUID of the message being replied to
     * @return Result with the sent Mensaje
     */
    suspend fun sendMessage(
        chatId: String,
        contenido: String,
        tipo: TipoMensaje = TipoMensaje.TEXTO,
        mensajeRespondidoId: String? = null
    ): Result<Mensaje>

    /**
     * Send a message with a file attachment
     * @param chatId Chat UUID
     * @param fileBytes File content bytes
     * @param fileName Original file name
     * @param mimeType MIME type of the file
     * @param contenido Optional text content
     * @param mensajeRespondidoId Optional UUID of the message being replied to
     * @param duracionSegundos Optional audio duration in seconds
     * @return Result with the sent Mensaje
     */
    suspend fun sendMessageWithFile(
        chatId: String,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        contenido: String? = null,
        mensajeRespondidoId: String? = null,
        duracionSegundos: Int? = null
    ): Result<Mensaje>

    /**
     * Mark a single message as read
     * @param messageId Message UUID
     */
    suspend fun markAsRead(messageId: String): Result<Unit>

    /**
     * Mark all messages in a chat as read
     * @param chatId Chat UUID
     */
    suspend fun markAllAsRead(chatId: String): Result<Unit>

    /**
     * Search messages within a chat
     * @param chatId Chat UUID
     * @param query Search text
     * @param limit Maximum number of results
     * @return Result with list of matching messages
     */
    suspend fun searchMessages(chatId: String, query: String, limit: Int = 20): Result<List<Mensaje>>

    /**
     * Forward a message to another chat
     * @param messageId UUID of the message to forward
     * @param targetChatId UUID of the destination chat
     * @return Result with the new forwarded Mensaje
     */
    suspend fun forwardMessage(messageId: String, targetChatId: String): Result<Mensaje>

    /**
     * Delete a message
     * @param messageId Message UUID
     * @param forEveryone If true, deletes for all participants
     * @return Result indicating success or failure
     */
    suspend fun deleteMessage(messageId: String, forEveryone: Boolean = false): Result<Unit>

    /**
     * Refresh messages from the server
     * @param chatId Chat UUID
     */
    suspend fun refreshMessages(chatId: String): Result<Unit>
}
