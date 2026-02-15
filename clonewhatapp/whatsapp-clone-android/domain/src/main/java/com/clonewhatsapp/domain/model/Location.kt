package com.clonewhatsapp.domain.model

/**
 * Modelo de ubicación compartida (T-134)
 *
 * Representa una ubicación geográfica compartida en un chat,
 * incluyendo coordenadas, dirección legible y momento del envío.
 *
 * @param latitude Latitud de la ubicación
 * @param longitude Longitud de la ubicación
 * @param address Dirección legible obtenida por geocodificación inversa (puede ser null)
 * @param timestamp Momento en que se compartió la ubicación (milisegundos)
 */
data class SharedLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
    val timestamp: Long
)
