package com.clonewhatsapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entidad Room para mensajes pendientes de envio.
 * Almacena los mensajes que no se pudieron enviar por falta de conexion
 * y se enviaran automaticamente cuando haya red disponible.
 */
@Entity(tableName = "mensajes_pendientes")
data class MensajePendienteEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val chatId: String,
    val contenido: String,
    val tipoMensaje: String = "Texto", // Texto, Imagen, Audio, Video, Documento
    val archivoLocalUri: String? = null,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val intentos: Int = 0,
    val maxIntentos: Int = 5,
    val estado: String = "pendiente" // pendiente, enviando, fallido
)
