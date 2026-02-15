package com.clonewhatsapp.core.network.api

import com.clonewhatsapp.core.network.dto.EnviarMensajeRequest
import com.clonewhatsapp.core.network.dto.ForwardMessageRequest
import com.clonewhatsapp.core.network.dto.MensajeDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for message endpoints
 * Matches backend MessagesController routes: api/messages
 */
interface MessagesApi {

    /**
     * Get messages for a chat (paginated, returned oldest-first)
     * GET /api/messages/chat/{chatId}
     */
    @GET("api/messages/chat/{chatId}")
    suspend fun getMessages(
        @Path("chatId") chatId: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): Response<List<MensajeDto>>

    /**
     * Send a text message
     * POST /api/messages
     */
    @POST("api/messages")
    suspend fun sendMessage(
        @Body request: EnviarMensajeRequest
    ): Response<MensajeDto>

    /**
     * Send a message with a file attachment (image, audio, document)
     * POST /api/messages/with-file
     */
    @Multipart
    @POST("api/messages/with-file")
    suspend fun sendMessageWithFile(
        @Part("chatId") chatId: RequestBody,
        @Part("contenido") contenido: RequestBody?,
        @Part("mensajeRespondidoId") mensajeRespondidoId: RequestBody?,
        @Part("duracionSegundos") duracionSegundos: RequestBody?,
        @Part file: MultipartBody.Part
    ): Response<MensajeDto>

    /**
     * Mark a single message as read
     * PUT /api/messages/{id}/read
     */
    @PUT("api/messages/{id}/read")
    suspend fun markAsRead(@Path("id") id: String): Response<Unit>

    /**
     * Mark all messages in a chat as read
     * PUT /api/messages/chat/{chatId}/read-all
     */
    @PUT("api/messages/chat/{chatId}/read-all")
    suspend fun markAllAsRead(@Path("chatId") chatId: String): Response<Unit>

    /**
     * Search messages within a chat
     * GET /api/messages/chat/{chatId}/search
     */
    @GET("api/messages/chat/{chatId}/search")
    suspend fun searchMessages(
        @Path("chatId") chatId: String,
        @Query("query") query: String,
        @Query("limit") limit: Int = 20
    ): Response<List<MensajeDto>>

    /**
     * Forward a message to another chat
     * POST /api/messages/{id}/forward
     */
    @POST("api/messages/{id}/forward")
    suspend fun forwardMessage(
        @Path("id") id: String,
        @Body request: ForwardMessageRequest
    ): Response<MensajeDto>

    /**
     * Delete a message
     * DELETE /api/messages/{id}
     * @param forEveryone if true, deletes for all participants
     */
    @DELETE("api/messages/{id}")
    suspend fun deleteMessage(
        @Path("id") id: String,
        @Query("forEveryone") forEveryone: Boolean = false
    ): Response<Unit>
}
