package com.clonewhatsapp.domain.model

data class Usuario(
    val id: String, // UUID from backend
    val nombre: String,
    val telefono: String,
    val fotoPerfil: String? = null,
    val estado: String? = null,
    val isOnline: Boolean = false,
    val ultimaConexion: Long? = null,
)
