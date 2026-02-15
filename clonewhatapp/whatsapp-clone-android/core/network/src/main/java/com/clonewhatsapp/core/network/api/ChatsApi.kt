package com.clonewhatsapp.core.network.api

import com.clonewhatsapp.core.network.dto.ActualizarGrupoRequest
import com.clonewhatsapp.core.network.dto.AgregarParticipantesRequest
import com.clonewhatsapp.core.network.dto.ArchivarChatRequest
import com.clonewhatsapp.core.network.dto.CambiarRolRequest
import com.clonewhatsapp.core.network.dto.ChatDto
import com.clonewhatsapp.core.network.dto.CrearChatIndividualRequest
import com.clonewhatsapp.core.network.dto.CrearGrupoRequest
import com.clonewhatsapp.core.network.dto.ParticipanteDto
import com.clonewhatsapp.core.network.dto.SilenciarChatRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Retrofit API interface for chat endpoints
 * Matches backend ChatsController routes: api/chats
 */
interface ChatsApi {

    /**
     * Get all non-archived chats for the current user
     * GET /api/chats
     */
    @GET("api/chats")
    suspend fun getChats(): Response<List<ChatDto>>

    /**
     * Get all archived chats for the current user
     * GET /api/chats/archivados
     */
    @GET("api/chats/archivados")
    suspend fun getArchivedChats(): Response<List<ChatDto>>

    /**
     * Get a specific chat by ID
     * GET /api/chats/{id}
     */
    @GET("api/chats/{id}")
    suspend fun getChatById(@Path("id") id: String): Response<ChatDto>

    /**
     * Create an individual (1-on-1) chat
     * POST /api/chats/individual
     */
    @POST("api/chats/individual")
    suspend fun createIndividualChat(
        @Body request: CrearChatIndividualRequest
    ): Response<ChatDto>

    /**
     * Create a group chat
     * POST /api/chats/grupo
     */
    @POST("api/chats/grupo")
    suspend fun createGroup(
        @Body request: CrearGrupoRequest
    ): Response<ChatDto>

    /**
     * Update group info (name, description)
     * PUT /api/chats/grupo/{id}
     */
    @PUT("api/chats/grupo/{id}")
    suspend fun updateGroup(
        @Path("id") id: String,
        @Body request: ActualizarGrupoRequest
    ): Response<ChatDto>

    /**
     * Update group image
     * POST /api/chats/grupo/{id}/imagen
     */
    @Multipart
    @POST("api/chats/grupo/{id}/imagen")
    suspend fun updateGroupImage(
        @Path("id") id: String,
        @Part imagen: MultipartBody.Part
    ): Response<ChatDto>

    /**
     * Mute or unmute a chat
     * PUT /api/chats/{id}/silenciar
     */
    @PUT("api/chats/{id}/silenciar")
    suspend fun muteChat(
        @Path("id") id: String,
        @Body request: SilenciarChatRequest
    ): Response<Unit>

    /**
     * Archive or unarchive a chat
     * PUT /api/chats/{id}/archivar
     */
    @PUT("api/chats/{id}/archivar")
    suspend fun archiveChat(
        @Path("id") id: String,
        @Body request: ArchivarChatRequest
    ): Response<Unit>

    /**
     * Add participants to a group
     * POST /api/chats/grupo/{id}/participantes
     */
    @POST("api/chats/grupo/{id}/participantes")
    suspend fun addParticipants(
        @Path("id") id: String,
        @Body request: AgregarParticipantesRequest
    ): Response<Unit>

    /**
     * Remove a participant from a group
     * DELETE /api/chats/grupo/{id}/participantes/{usuarioId}
     */
    @DELETE("api/chats/grupo/{id}/participantes/{usuarioId}")
    suspend fun removeParticipant(
        @Path("id") id: String,
        @Path("usuarioId") usuarioId: String
    ): Response<Unit>

    /**
     * Change a participant's role in a group
     * PUT /api/chats/grupo/{id}/participantes/{usuarioId}/rol
     */
    @PUT("api/chats/grupo/{id}/participantes/{usuarioId}/rol")
    suspend fun changeParticipantRole(
        @Path("id") id: String,
        @Path("usuarioId") usuarioId: String,
        @Body request: CambiarRolRequest
    ): Response<Unit>

    /**
     * Leave a group
     * POST /api/chats/grupo/{id}/salir
     */
    @POST("api/chats/grupo/{id}/salir")
    suspend fun leaveGroup(@Path("id") id: String): Response<Unit>

    /**
     * Get all participants of a group
     * GET /api/chats/grupo/{id}/participantes
     */
    @GET("api/chats/grupo/{id}/participantes")
    suspend fun getGroupParticipants(
        @Path("id") id: String
    ): Response<List<ParticipanteDto>>
}
