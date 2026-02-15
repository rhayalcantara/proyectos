package com.clonewhatsapp.domain.model

/**
 * Representa un elemento del historial de llamadas.
 *
 * @param id Identificador unico del registro de llamada.
 * @param contactId Identificador del contacto involucrado en la llamada.
 * @param contactName Nombre del contacto.
 * @param contactPhoto URL de la foto del contacto, null si no tiene.
 * @param callType Tipo de llamada (audio o video).
 * @param isOutgoing true si la llamada fue saliente, false si fue entrante.
 * @param wasAnswered true si la llamada fue contestada, false si fue perdida o rechazada.
 * @param timestamp Marca de tiempo en milisegundos de cuando se realizo la llamada.
 * @param durationSeconds Duracion en segundos de la llamada, null si no fue contestada.
 */
data class CallHistoryItem(
    val id: String,
    val contactId: String,
    val contactName: String,
    val contactPhoto: String?,
    val callType: CallType,
    val isOutgoing: Boolean,
    val wasAnswered: Boolean,
    val timestamp: Long,
    val durationSeconds: Int?
)
