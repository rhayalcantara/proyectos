package com.clonewhatsapp.domain.repository

import android.net.Uri
import com.clonewhatsapp.domain.model.EstadoCompleto
import com.clonewhatsapp.domain.model.EstadosContacto
import com.clonewhatsapp.domain.model.MisEstados
import com.clonewhatsapp.domain.model.VistaEstado

/**
 * Interfaz del repositorio de estados (T-086)
 * Define el contrato para las operaciones de estados/stories
 */
interface StatusRepository {

    /**
     * Obtiene mis estados publicados
     * @return Result con MisEstados en caso de éxito
     */
    suspend fun getMyStatuses(): Result<MisEstados>

    /**
     * Obtiene los estados de los contactos
     * @return Result con lista de EstadosContacto en caso de éxito
     */
    suspend fun getContactsStatuses(): Result<List<EstadosContacto>>

    /**
     * Crea un estado de texto
     * @param contenido Texto del estado
     * @param colorFondo Color de fondo opcional (formato hex)
     * @return Result con el estado creado
     */
    suspend fun createTextStatus(contenido: String, colorFondo: String?): Result<EstadoCompleto>

    /**
     * Crea un estado con imagen
     * @param imageUri URI de la imagen seleccionada
     * @param caption Texto descriptivo opcional
     * @return Result con el estado creado
     */
    suspend fun createImageStatus(imageUri: Uri, caption: String?): Result<EstadoCompleto>

    /**
     * Marca un estado como visto
     * @param statusId ID del estado
     * @return Result indicando éxito o fallo
     */
    suspend fun markAsViewed(statusId: String): Result<Unit>

    /**
     * Obtiene la lista de usuarios que vieron un estado
     * @param statusId ID del estado
     * @return Result con lista de VistaEstado
     */
    suspend fun getViewers(statusId: String): Result<List<VistaEstado>>

    /**
     * Elimina un estado propio
     * @param statusId ID del estado a eliminar
     * @return Result indicando éxito o fallo
     */
    suspend fun deleteStatus(statusId: String): Result<Unit>
}
