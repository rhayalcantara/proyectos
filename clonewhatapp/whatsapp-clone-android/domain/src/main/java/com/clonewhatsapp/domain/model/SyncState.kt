package com.clonewhatsapp.domain.model

/**
 * Estado de la sincronizacion de mensajes pendientes.
 */
enum class EstadoSync {
    /** Sin actividad de sincronizacion */
    INACTIVO,

    /** Sincronizando mensajes pendientes */
    SINCRONIZANDO,

    /** Error durante la sincronizacion */
    ERROR
}

/**
 * Modelo de dominio que representa el estado actual del sistema de sincronizacion.
 *
 * @param estado Estado actual de la sincronizacion.
 * @param cantidadPendientes Numero de mensajes pendientes de envio.
 * @param ultimaSincronizacion Timestamp de la ultima sincronizacion exitosa, null si nunca se ha sincronizado.
 */
data class SyncState(
    val estado: EstadoSync = EstadoSync.INACTIVO,
    val cantidadPendientes: Int = 0,
    val ultimaSincronizacion: Long? = null
)
