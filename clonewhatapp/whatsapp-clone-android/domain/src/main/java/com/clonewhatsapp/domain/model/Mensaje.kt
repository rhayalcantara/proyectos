package com.clonewhatsapp.domain.model

data class Mensaje(
    val id: String,
    val chatId: String,
    val remitenteId: String,
    val contenido: String? = null,
    val urlArchivo: String? = null,
    val tipo: TipoMensaje = TipoMensaje.TEXTO,
    val estado: EstadoMensaje = EstadoMensaje.ENVIADO,
    val fechaEnvio: Long,
    val editado: Boolean = false,
    val eliminado: Boolean = false,
    val eliminadoParaTodos: Boolean = false,
    val mensajeRespondidoId: String? = null,
    val duracionSegundos: Int? = null,
    val nombreRemitente: String? = null,
)

enum class TipoMensaje {
    TEXTO,
    IMAGEN,
    VIDEO,
    AUDIO,
    DOCUMENTO,
    UBICACION,
    CONTACTO,
    SISTEMA,
}

enum class EstadoMensaje {
    ENVIADO,
    ENTREGADO,
    LEIDO,
}

enum class SyncStatus {
    PENDING,
    SYNCED,
    FAILED,
}
