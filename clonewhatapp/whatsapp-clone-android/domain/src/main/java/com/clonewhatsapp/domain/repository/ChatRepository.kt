package com.clonewhatsapp.domain.repository

import com.clonewhatsapp.domain.model.Chat
import com.clonewhatsapp.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for chat operations
 * Part of the domain layer - defines the contract for chat operations
 */
interface ChatRepository {

    /**
     * Get all non-archived chats for the current user
     * @return Flow emitting list of chats ordered by last activity
     */
    fun getChats(): Flow<List<Chat>>

    /**
     * Get all archived chats for the current user
     * @return Flow emitting list of archived chats
     */
    fun getArchivedChats(): Flow<List<Chat>>

    /**
     * Get a specific chat by ID
     * @param chatId Chat UUID
     * @return The chat or null if not found
     */
    suspend fun getChatById(chatId: String): Chat?

    /**
     * Create an individual (1-on-1) chat with a contact
     * If a chat already exists with the contact, returns the existing one
     * @param contactId UUID of the other participant
     * @return Result with Chat on success
     */
    suspend fun createIndividualChat(contactId: String): Result<Chat>

    /**
     * Create a group chat
     * @param nombre Group name
     * @param descripcion Optional group description
     * @param participantesIds List of participant UUIDs
     * @return Result with Chat on success
     */
    suspend fun createGroup(
        nombre: String,
        descripcion: String?,
        participantesIds: List<String>
    ): Result<Chat>

    /**
     * Update group info (name and/or description)
     * @param id Group chat UUID
     * @param nombre New name (null to keep current)
     * @param descripcion New description (null to keep current)
     * @return Result with updated Chat on success
     */
    suspend fun updateGroup(
        id: String,
        nombre: String?,
        descripcion: String?
    ): Result<Chat>

    /**
     * Update group image
     * @param id Group chat UUID
     * @param imageBytes Image file bytes
     * @param fileName Original file name
     * @return Result with updated Chat on success
     */
    suspend fun updateGroupImage(
        id: String,
        imageBytes: ByteArray,
        fileName: String
    ): Result<Chat>

    /**
     * Mute or unmute a chat
     * @param chatId Chat UUID
     * @param silenciar true to mute, false to unmute
     * @param duracion Duration string: "8h", "1w", "always", or null
     * @return Result indicating success or failure
     */
    suspend fun muteChat(chatId: String, silenciar: Boolean, duracion: String?): Result<Unit>

    /**
     * Archive or unarchive a chat
     * @param chatId Chat UUID
     * @param archivar true to archive, false to unarchive
     * @return Result indicating success or failure
     */
    suspend fun archiveChat(chatId: String, archivar: Boolean): Result<Unit>

    /**
     * Add participants to a group
     * @param groupId Group chat UUID
     * @param participantesIds List of user UUIDs to add
     * @return Result indicating success or failure
     */
    suspend fun addParticipants(groupId: String, participantesIds: List<String>): Result<Unit>

    /**
     * Remove a participant from a group
     * @param groupId Group chat UUID
     * @param usuarioId UUID of the user to remove
     * @return Result indicating success or failure
     */
    suspend fun removeParticipant(groupId: String, usuarioId: String): Result<Unit>

    /**
     * Change a participant's role in a group
     * @param groupId Group chat UUID
     * @param usuarioId UUID of the user
     * @param rol New role: "Admin" or "Participante"
     * @return Result indicating success or failure
     */
    suspend fun changeParticipantRole(groupId: String, usuarioId: String, rol: String): Result<Unit>

    /**
     * Leave a group chat
     * @param groupId Group chat UUID
     * @return Result indicating success or failure
     */
    suspend fun leaveGroup(groupId: String): Result<Unit>

    /**
     * Get all participants of a group
     * @param groupId Group chat UUID
     * @return Result with list of participants on success
     */
    suspend fun getGroupParticipants(groupId: String): Result<List<Usuario>>

    /**
     * Refresh the list of chats from the server
     */
    suspend fun refreshChats(): Result<Unit>

    /**
     * Search chats by name
     * @param query Search text
     * @return List of matching chats
     */
    suspend fun searchChats(query: String): List<Chat>
}
