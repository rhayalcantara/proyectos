package com.clonewhatsapp.domain.model

/**
 * Tipo de llamada: audio o video.
 */
enum class CallType { AUDIO, VIDEO }

/**
 * Estados posibles de una llamada.
 */
enum class CallState {
    IDLE,
    CALLING,       // Llamada saliente sonando
    RINGING,       // Llamada entrante sonando
    CONNECTING,    // Llamada aceptada, estableciendo WebRTC
    CONNECTED,     // Llamada activa
    ENDED,         // Llamada finalizada
    FAILED,        // Llamada fallida
    REJECTED       // Llamada rechazada
}

/**
 * Informacion de una llamada en curso.
 */
data class CallInfo(
    val callerId: String,
    val callerName: String,
    val callerPhoto: String?,
    val callType: CallType,
    val isOutgoing: Boolean
)

/**
 * Oferta de llamada entrante recibida via SignalR.
 */
data class IncomingCallOffer(
    val callerId: String,
    val callerName: String,
    val callerPhoto: String?,
    val callType: CallType,
    val sdpOffer: String
)

/**
 * Razon de finalizacion de una llamada.
 */
data class CallEndReason(
    val endedBy: String? = null,
    val reason: String? = null,
    val message: String? = null
)
