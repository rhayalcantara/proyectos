package com.clonewhatsapp.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for message data from backend
 * Matches backend MensajeDTO
 */
data class MensajeDto(
    @SerializedName("Id")
    val id: String,

    @SerializedName("ChatId")
    val chatId: String,

    @SerializedName("RemitenteId")
    val remitenteId: String,

    @SerializedName("RemitenteNombre")
    val remitenteNombre: String?,

    @SerializedName("Contenido")
    val contenido: String?,

    @SerializedName("Tipo")
    val tipo: String,

    @SerializedName("UrlArchivo")
    val urlArchivo: String?,

    @SerializedName("NombreArchivo")
    val nombreArchivo: String?,

    @SerializedName("TamanoArchivo")
    val tamanoArchivo: Long?,

    @SerializedName("DuracionSegundos")
    val duracionSegundos: Int?,

    @SerializedName("MensajeRespondidoId")
    val mensajeRespondidoId: String?,

    @SerializedName("MensajeRespondido")
    val mensajeRespondido: MensajeDto?,

    @SerializedName("FechaEnvio")
    val fechaEnvio: String,

    @SerializedName("Eliminado")
    val eliminado: Boolean,

    @SerializedName("EliminadoParaTodos")
    val eliminadoParaTodos: Boolean,

    @SerializedName("Editado")
    val editado: Boolean,

    @SerializedName("Estado")
    val estado: String // Enviado, Entregado, Leido
)

/**
 * DTO for sending a text message
 * Matches backend EnviarMensajeDTO
 */
data class EnviarMensajeRequest(
    @SerializedName("ChatId")
    val chatId: String,

    @SerializedName("Contenido")
    val contenido: String?,

    @SerializedName("Tipo")
    val tipo: String = "Texto",

    @SerializedName("MensajeRespondidoId")
    val mensajeRespondidoId: String?
)

/**
 * DTO for forwarding a message
 * Matches backend ForwardMessageDTO
 */
data class ForwardMessageRequest(
    @SerializedName("TargetChatId")
    val targetChatId: String
)
