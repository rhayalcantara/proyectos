package com.clonewhatsapp.domain.model

data class Chat(
    val id: String,
    val nombre: String,
    val esGrupo: Boolean = false,
    val fotoUrl: String? = null,
    val ultimoMensaje: String? = null,
    val ultimoMensajeTiempo: Long? = null,
    val mensajesNoLeidos: Int = 0,
    val participantes: List<Usuario> = emptyList(),
)
