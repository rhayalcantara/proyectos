package com.clonewhatsapp.domain.model

data class Llamada(
    val id: String,
    val llamanteId: String,
    val receptorId: String,
    val tipo: TipoLlamada,
    val estado: EstadoLlamada,
    val fechaInicio: Long,
    val duracionSegundos: Int? = null,
    val nombreContacto: String? = null,
    val fotoContacto: String? = null,
)

enum class TipoLlamada {
    VOZ,
    VIDEO,
}

enum class EstadoLlamada {
    EN_CURSO,
    COMPLETADA,
    PERDIDA,
    RECHAZADA,
}
