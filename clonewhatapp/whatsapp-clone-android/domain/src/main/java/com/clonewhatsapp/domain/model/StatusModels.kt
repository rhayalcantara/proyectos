package com.clonewhatsapp.domain.model

/**
 * Modelos de dominio para la funcionalidad de Estados/Stories (T-086)
 */

/**
 * Representa un estado individual publicado por un usuario
 */
data class EstadoCompleto(
    val id: String,
    val usuarioId: String,
    val usuarioNombre: String,
    val usuarioFoto: String? = null,
    val tipo: TipoEstado,
    val contenido: String? = null,
    val urlArchivo: String? = null,
    val colorFondo: String? = null,
    val fechaCreacion: Long,
    val fechaExpiracion: Long,
    val totalVistas: Int = 0,
    val vistoPorMi: Boolean = false
)

/**
 * Representa una vista de un estado (quién lo vio)
 */
data class VistaEstado(
    val id: String,
    val usuarioId: String,
    val usuarioNombre: String,
    val usuarioFoto: String? = null,
    val fechaVista: Long
)

/**
 * Agrupa los estados de un contacto
 */
data class EstadosContacto(
    val usuarioId: String,
    val usuarioNombre: String,
    val usuarioFoto: String? = null,
    val estados: List<EstadoCompleto>,
    val todosVistos: Boolean,
    val ultimaActualizacion: Long
)

/**
 * Mis propios estados con total de vistas
 */
data class MisEstados(
    val estados: List<EstadoCompleto>,
    val totalVistas: Int
)
