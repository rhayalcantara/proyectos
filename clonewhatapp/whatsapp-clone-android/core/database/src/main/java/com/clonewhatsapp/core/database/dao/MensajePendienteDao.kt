package com.clonewhatsapp.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.clonewhatsapp.core.database.entity.MensajePendienteEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones sobre la tabla de mensajes pendientes.
 * Proporciona metodos para gestionar la cola de mensajes offline.
 */
@Dao
interface MensajePendienteDao {

    /**
     * Inserta un mensaje pendiente en la cola.
     * Si ya existe uno con el mismo id, lo reemplaza.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(mensaje: MensajePendienteEntity)

    /**
     * Obtiene todos los mensajes pendientes que no han sido enviados,
     * ordenados por fecha de creacion ascendente.
     * Retorna un Flow reactivo para observar cambios.
     */
    @Query("SELECT * FROM mensajes_pendientes WHERE estado != 'enviado' ORDER BY fechaCreacion ASC")
    fun obtenerPendientes(): Flow<List<MensajePendienteEntity>>

    /**
     * Obtiene mensajes listos para enviar (pendientes o fallidos),
     * limitados por la cantidad especificada.
     * Usado por el SyncManager para procesar en lotes.
     */
    @Query(
        "SELECT * FROM mensajes_pendientes " +
            "WHERE estado = 'pendiente' OR estado = 'fallido' " +
            "ORDER BY fechaCreacion ASC " +
            "LIMIT :limite"
    )
    suspend fun obtenerParaEnviar(limite: Int = 10): List<MensajePendienteEntity>

    /**
     * Actualiza el estado de un mensaje pendiente e incrementa el contador de intentos.
     */
    @Query("UPDATE mensajes_pendientes SET estado = :estado, intentos = intentos + 1 WHERE id = :id")
    suspend fun actualizarEstado(id: String, estado: String)

    /**
     * Elimina un mensaje pendiente de la cola.
     */
    @Delete
    suspend fun eliminar(mensaje: MensajePendienteEntity)

    /**
     * Elimina un mensaje pendiente por su id.
     * Usado cuando el mensaje se envio exitosamente.
     */
    @Query("DELETE FROM mensajes_pendientes WHERE id = :id")
    suspend fun eliminarPorId(id: String)

    /**
     * Cuenta la cantidad de mensajes pendientes o fallidos.
     * Retorna un Flow reactivo para mostrar en la UI.
     */
    @Query("SELECT COUNT(*) FROM mensajes_pendientes WHERE estado = 'pendiente' OR estado = 'fallido'")
    fun contarPendientes(): Flow<Int>
}
