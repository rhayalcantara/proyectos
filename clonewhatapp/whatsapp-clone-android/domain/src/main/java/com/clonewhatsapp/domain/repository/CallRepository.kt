package com.clonewhatsapp.domain.repository

import com.clonewhatsapp.domain.model.CallEndReason
import com.clonewhatsapp.domain.model.CallInfo
import com.clonewhatsapp.domain.model.CallState
import com.clonewhatsapp.domain.model.CallType
import com.clonewhatsapp.domain.model.IncomingCallOffer
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repositorio para gestionar llamadas de audio y video.
 * Define el contrato entre la capa de dominio y la implementacion de red.
 */
interface CallRepository {

    /** Estado actual de la llamada. */
    val callState: StateFlow<CallState>

    /** Informacion de la llamada en curso, null si no hay llamada activa. */
    val currentCall: StateFlow<CallInfo?>

    /** Flujo de ofertas de llamadas entrantes. */
    val incomingCall: SharedFlow<IncomingCallOffer>

    /** Flujo de eventos de finalizacion de llamada. */
    val callEnded: SharedFlow<CallEndReason>

    /**
     * Inicia una llamada al usuario objetivo.
     * Crea la oferta SDP y la envia via SignalR.
     */
    suspend fun startCall(targetUserId: String, callType: CallType)

    /**
     * Responde a una llamada entrante con la respuesta SDP.
     */
    suspend fun answerCall(callerId: String, sdpAnswer: String)

    /**
     * Rechaza una llamada entrante.
     */
    suspend fun rejectCall(callerId: String, reason: String = "Rejected")

    /**
     * Finaliza la llamada actual.
     */
    suspend fun endCall()

    /**
     * Envia un candidato ICE al usuario objetivo.
     */
    suspend fun sendIceCandidate(targetUserId: String, candidate: String)

    /**
     * Alterna el estado de silencio del microfono.
     * @return true si el microfono queda silenciado.
     */
    suspend fun toggleMute(): Boolean

    /**
     * Alterna el altavoz.
     * @return true si el altavoz queda activado.
     */
    suspend fun toggleSpeaker(): Boolean

    /** Indica si el microfono esta silenciado. */
    fun isMuted(): Boolean

    /** Indica si el altavoz esta activado. */
    fun isSpeakerOn(): Boolean
}
