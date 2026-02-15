package com.clonewhatsapp.core.network.api

import com.clonewhatsapp.core.network.dto.CrearEstadoTextoRequest
import com.clonewhatsapp.core.network.dto.EstadoDto
import com.clonewhatsapp.core.network.dto.EstadosContactoDto
import com.clonewhatsapp.core.network.dto.MisEstadosDto
import com.clonewhatsapp.core.network.dto.VistaEstadoDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Interfaz Retrofit para los endpoints de estados (T-087)
 * Coincide con las rutas del backend: api/estados
 */
interface StatusApi {

    /**
     * Obtiene mis estados
     * GET /api/estados
     */
    @GET("api/estados")
    suspend fun getMyStatuses(): Response<MisEstadosDto>

    /**
     * Obtiene los estados de los contactos
     * GET /api/estados/contactos
     */
    @GET("api/estados/contactos")
    suspend fun getContactsStatuses(): Response<List<EstadosContactoDto>>

    /**
     * Crea un estado de texto
     * POST /api/estados
     */
    @POST("api/estados")
    suspend fun createTextStatus(
        @Body request: CrearEstadoTextoRequest
    ): Response<EstadoDto>

    /**
     * Crea un estado con imagen (multipart)
     * POST /api/estados/imagen
     */
    @Multipart
    @POST("api/estados/imagen")
    suspend fun createImageStatus(
        @Part imagen: MultipartBody.Part,
        @Part("caption") caption: RequestBody?
    ): Response<EstadoDto>

    /**
     * Marca un estado como visto
     * POST /api/estados/{id}/vista
     */
    @POST("api/estados/{id}/vista")
    suspend fun markAsViewed(
        @Path("id") statusId: String
    ): Response<Unit>

    /**
     * Obtiene quiénes vieron un estado
     * GET /api/estados/{id}/vistas
     */
    @GET("api/estados/{id}/vistas")
    suspend fun getViewers(
        @Path("id") statusId: String
    ): Response<List<VistaEstadoDto>>

    /**
     * Elimina un estado propio
     * DELETE /api/estados/{id}
     */
    @DELETE("api/estados/{id}")
    suspend fun deleteStatus(
        @Path("id") statusId: String
    ): Response<Unit>
}
