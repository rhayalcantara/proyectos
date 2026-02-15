package com.clonewhatsapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.clonewhatsapp.domain.model.Usuario

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey
    val id: String,
    val nombre: String,
    val telefono: String,
    val fotoPerfil: String? = null,
    val estado: String? = null,
    val isOnline: Boolean = false,
    val ultimaConexion: Long? = null,
)

fun UsuarioEntity.toDomain(): Usuario {
    return Usuario(
        id = id,
        nombre = nombre,
        telefono = telefono,
        fotoPerfil = fotoPerfil,
        estado = estado,
        isOnline = isOnline,
        ultimaConexion = ultimaConexion,
    )
}

fun Usuario.toEntity(): UsuarioEntity {
    return UsuarioEntity(
        id = id,
        nombre = nombre,
        telefono = telefono,
        fotoPerfil = fotoPerfil,
        estado = estado,
        isOnline = isOnline,
        ultimaConexion = ultimaConexion,
    )
}
