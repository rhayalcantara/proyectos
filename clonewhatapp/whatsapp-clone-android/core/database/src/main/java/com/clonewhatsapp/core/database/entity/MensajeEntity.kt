package com.clonewhatsapp.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.clonewhatsapp.domain.model.EstadoMensaje
import com.clonewhatsapp.domain.model.Mensaje
import com.clonewhatsapp.domain.model.SyncStatus
import com.clonewhatsapp.domain.model.TipoMensaje

@Entity(
    tableName = "mensajes",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["chatId"]),
    ],
)
data class MensajeEntity(
    @PrimaryKey
    val id: String,
    val chatId: String,
    val remitenteId: String,
    val contenido: String? = null,
    val urlArchivo: String? = null,
    val tipo: String = TipoMensaje.TEXTO.name,
    val estado: String = EstadoMensaje.ENVIADO.name,
    val fechaEnvio: Long,
    val editado: Boolean = false,
    val eliminado: Boolean = false,
    val eliminadoParaTodos: Boolean = false,
    val mensajeRespondidoId: String? = null,
    val duracionSegundos: Int? = null,
    val nombreRemitente: String? = null,
    @ColumnInfo(defaultValue = "PENDING")
    val syncStatus: String = SyncStatus.PENDING.name,
)

fun MensajeEntity.toDomain(): Mensaje {
    return Mensaje(
        id = id,
        chatId = chatId,
        remitenteId = remitenteId,
        contenido = contenido,
        urlArchivo = urlArchivo,
        tipo = TipoMensaje.valueOf(tipo),
        estado = EstadoMensaje.valueOf(estado),
        fechaEnvio = fechaEnvio,
        editado = editado,
        eliminado = eliminado,
        eliminadoParaTodos = eliminadoParaTodos,
        mensajeRespondidoId = mensajeRespondidoId,
        duracionSegundos = duracionSegundos,
        nombreRemitente = nombreRemitente,
    )
}

fun Mensaje.toEntity(syncStatus: SyncStatus = SyncStatus.PENDING): MensajeEntity {
    return MensajeEntity(
        id = id,
        chatId = chatId,
        remitenteId = remitenteId,
        contenido = contenido,
        urlArchivo = urlArchivo,
        tipo = tipo.name,
        estado = estado.name,
        fechaEnvio = fechaEnvio,
        editado = editado,
        eliminado = eliminado,
        eliminadoParaTodos = eliminadoParaTodos,
        mensajeRespondidoId = mensajeRespondidoId,
        duracionSegundos = duracionSegundos,
        nombreRemitente = nombreRemitente,
        syncStatus = syncStatus.name,
    )
}
