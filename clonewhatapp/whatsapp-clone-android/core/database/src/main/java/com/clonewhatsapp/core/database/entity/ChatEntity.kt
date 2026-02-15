package com.clonewhatsapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clonewhatsapp.domain.model.Chat

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey
    val id: String,
    val nombre: String,
    val esGrupo: Boolean = false,
    val fotoUrl: String? = null,
    val ultimoMensaje: String? = null,
    val ultimoMensajeTiempo: Long? = null,
    val mensajesNoLeidos: Int = 0,
)

fun ChatEntity.toDomain(): Chat {
    return Chat(
        id = id,
        nombre = nombre,
        esGrupo = esGrupo,
        fotoUrl = fotoUrl,
        ultimoMensaje = ultimoMensaje,
        ultimoMensajeTiempo = ultimoMensajeTiempo,
        mensajesNoLeidos = mensajesNoLeidos,
    )
}

fun Chat.toEntity(): ChatEntity {
    return ChatEntity(
        id = id,
        nombre = nombre,
        esGrupo = esGrupo,
        fotoUrl = fotoUrl,
        ultimoMensaje = ultimoMensaje,
        ultimoMensajeTiempo = ultimoMensajeTiempo,
        mensajesNoLeidos = mensajesNoLeidos,
    )
}
