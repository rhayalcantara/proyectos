package com.clonewhatsapp.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para un estado individual del backend
 * Coincide con EstadoDTO del backend
 */
data class EstadoDto(
    @SerializedName("Id")
    val id: String,

    @SerializedName("UsuarioId")
    val usuarioId: String,

    @SerializedName("UsuarioNombre")
    val usuarioNombre: String,

    @SerializedName("UsuarioFoto")
    val usuarioFoto: String?,

    @SerializedName("Tipo")
    val tipo: String,

    @SerializedName("Contenido")
    val contenido: String?,

    @SerializedName("UrlArchivo")
    val urlArchivo: String?,

    @SerializedName("ColorFondo")
    val colorFondo: String?,

    @SerializedName("FechaCreacion")
    val fechaCreacion: String,

    @SerializedName("FechaExpiracion")
    val fechaExpiracion: String,

    @SerializedName("TotalVistas")
    val totalVistas: Int,

    @SerializedName("VioPorMi")
    val vioPorMi: Boolean
)

/**
 * DTO para crear un estado de texto
 * Coincide con CrearEstadoTextoDTO del backend
 */
data class CrearEstadoTextoRequest(
    @SerializedName("Contenido")
    val contenido: String,

    @SerializedName("ColorFondo")
    val colorFondo: String?
)

/**
 * DTO para la vista de un estado (quién lo vio)
 * Coincide con VistaEstadoDTO del backend
 */
data class VistaEstadoDto(
    @SerializedName("Id")
    val id: String,

    @SerializedName("UsuarioId")
    val usuarioId: String,

    @SerializedName("UsuarioNombre")
    val usuarioNombre: String,

    @SerializedName("UsuarioFoto")
    val usuarioFoto: String?,

    @SerializedName("FechaVista")
    val fechaVista: String
)

/**
 * DTO para los estados agrupados de un contacto
 * Coincide con EstadosContactoDTO del backend
 */
data class EstadosContactoDto(
    @SerializedName("UsuarioId")
    val usuarioId: String,

    @SerializedName("UsuarioNombre")
    val usuarioNombre: String,

    @SerializedName("UsuarioFoto")
    val usuarioFoto: String?,

    @SerializedName("Estados")
    val estados: List<EstadoDto>,

    @SerializedName("TodosVistos")
    val todosVistos: Boolean,

    @SerializedName("UltimaActualizacion")
    val ultimaActualizacion: String
)

/**
 * DTO para mis estados con total de vistas
 * Coincide con MisEstadosDTO del backend
 */
data class MisEstadosDto(
    @SerializedName("Estados")
    val estados: List<EstadoDto>,

    @SerializedName("TotalVistas")
    val totalVistas: Int
)
