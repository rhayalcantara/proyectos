package com.clonewhatsapp.domain.model

data class Estado(
    val id: String,
    val usuarioId: String,
    val contenido: String? = null,
    val urlArchivo: String? = null,
    val tipo: TipoEstado,
    val fechaCreacion: Long,
    val nombreUsuario: String? = null,
    val fotoUsuario: String? = null,
    val vistoPor: List<String> = emptyList(),
)

enum class TipoEstado {
    TEXTO,
    IMAGEN,
    VIDEO,
}
