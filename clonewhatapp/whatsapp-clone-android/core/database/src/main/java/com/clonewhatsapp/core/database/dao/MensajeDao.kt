package com.clonewhatsapp.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.clonewhatsapp.core.database.entity.MensajeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MensajeDao {

    @Query("SELECT * FROM mensajes WHERE chatId = :chatId ORDER BY fechaEnvio ASC")
    fun getMensajesByChatId(chatId: String): Flow<List<MensajeEntity>>

    @Query("SELECT * FROM mensajes WHERE chatId = :chatId ORDER BY fechaEnvio ASC")
    fun getMensajesByChatIdPaged(chatId: String): PagingSource<Int, MensajeEntity>

    @Query("SELECT * FROM mensajes WHERE id = :id")
    suspend fun getMensajeById(id: String): MensajeEntity?

    @Query("SELECT * FROM mensajes WHERE syncStatus = 'PENDING'")
    suspend fun getPendingMessages(): List<MensajeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMensaje(mensaje: MensajeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMensajes(mensajes: List<MensajeEntity>)

    @Update
    suspend fun updateMensaje(mensaje: MensajeEntity)

    @Query("UPDATE mensajes SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM mensajes WHERE chatId = :chatId")
    suspend fun deleteMensajesByChatId(chatId: String)
}
