package com.clonewhatsapp.core.network.signalr

/**
 * DTO para la oferta de llamada recibida del servidor.
 * Evento: ReceiveCallOffer
 */
data class CallOfferDto(
    val callerId: String = "",
    val callerName: String = "",
    val callerPhoto: String? = null,
    val callType: String = "",  // "video" o "audio"
    val sdpOffer: String = ""
)

/**
 * DTO para la respuesta de llamada recibida del servidor.
 * Evento: ReceiveCallAnswer
 */
data class CallAnswerDto(
    val sdpAnswer: String = ""
)

/**
 * DTO para notificacion de llamada rechazada.
 * Evento: CallRejected
 */
data class CallRejectedDto(
    val userId: String = "",
    val reason: String = ""
)

/**
 * DTO para notificacion de llamada finalizada.
 * Evento: CallEnded
 */
data class CallEndedDto(
    val endedBy: String = ""
)

/**
 * DTO para candidato ICE recibido del servidor.
 * Evento: ReceiveICECandidate
 */
data class IceCandidateDto(
    val candidate: String = ""
)

/**
 * DTO para notificacion de llamada fallida.
 * Evento: CallFailed
 */
data class CallFailedDto(
    val reason: String = "",
    val message: String = ""
)
