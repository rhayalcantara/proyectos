package com.clonewhatsapp.core.network.webrtc

import android.util.Log
import com.clonewhatsapp.core.network.signalr.SignalRManager
import com.clonewhatsapp.domain.model.CallEndReason
import com.clonewhatsapp.domain.model.CallInfo
import com.clonewhatsapp.domain.model.CallState
import com.clonewhatsapp.domain.model.CallType
import com.clonewhatsapp.domain.model.IncomingCallOffer
import com.clonewhatsapp.domain.repository.AuthRepository
import com.clonewhatsapp.domain.repository.CallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.PeerConnection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacion del repositorio de llamadas.
 * Coordina SignalRManager (senalizacion) y WebRTCManager (media).
 */
@Singleton
class CallRepositoryImpl @Inject constructor(
    private val signalRManager: SignalRManager,
    private val webRTCManager: WebRTCManager,
    private val authRepository: AuthRepository
) : CallRepository {

    companion object {
        private const val TAG = "CallRepositoryImpl"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Estado de la llamada
    private val _callState = MutableStateFlow(CallState.IDLE)
    override val callState: StateFlow<CallState> = _callState.asStateFlow()

    // Informacion de la llamada actual
    private val _currentCall = MutableStateFlow<CallInfo?>(null)
    override val currentCall: StateFlow<CallInfo?> = _currentCall.asStateFlow()

    // Llamadas entrantes
    private val _incomingCall = MutableSharedFlow<IncomingCallOffer>(extraBufferCapacity = 8)
    override val incomingCall: SharedFlow<IncomingCallOffer> = _incomingCall.asSharedFlow()

    // Finalizacion de llamada
    private val _callEnded = MutableSharedFlow<CallEndReason>(extraBufferCapacity = 8)
    override val callEnded: SharedFlow<CallEndReason> = _callEnded.asSharedFlow()

    // ID del usuario remoto en la llamada actual
    private var remoteUserId: String? = null

    // Estado de mute y altavoz
    private var muted: Boolean = false
    private var speakerOn: Boolean = false

    // Pendiente de oferta SDP (para llamadas entrantes)
    private var pendingSdpOffer: String? = null

    init {
        observeSignalREvents()
        observeWebRTCEvents()
    }

    /**
     * Inicia una llamada saliente.
     * Crea la oferta SDP via WebRTC y la envia via SignalR.
     */
    override suspend fun startCall(targetUserId: String, callType: CallType) {
        if (_callState.value != CallState.IDLE) {
            Log.w(TAG, "No se puede iniciar llamada: estado actual = ${_callState.value}")
            return
        }

        try {
            _callState.value = CallState.CALLING
            remoteUserId = targetUserId

            val currentUserName = authRepository.getCurrentUserName() ?: "Usuario"

            _currentCall.value = CallInfo(
                callerId = authRepository.getCurrentUserId() ?: "",
                callerName = currentUserName,
                callerPhoto = null,
                callType = callType,
                isOutgoing = true
            )

            // Inicializar WebRTC y crear oferta
            webRTCManager.initialize()

            if (callType == CallType.VIDEO) {
                webRTCManager.startLocalVideo()
            }

            val sdpOffer = webRTCManager.createOffer()

            // Enviar oferta via SignalR
            val callTypeStr = if (callType == CallType.VIDEO) "video" else "audio"
            signalRManager.callUser(targetUserId, callTypeStr, sdpOffer)

            Log.i(TAG, "Llamada iniciada a $targetUserId, tipo: $callTypeStr")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar llamada", e)
            _callState.value = CallState.FAILED
            cleanup()
        }
    }

    /**
     * Responde a una llamada entrante.
     * Crea la respuesta SDP via WebRTC y la envia via SignalR.
     */
    override suspend fun answerCall(callerId: String, sdpAnswer: String) {
        // Si se pasa un sdpAnswer vacio, generar la respuesta desde WebRTC
        try {
            _callState.value = CallState.CONNECTING

            val offer = pendingSdpOffer
            if (offer == null) {
                Log.e(TAG, "No hay oferta SDP pendiente para responder")
                _callState.value = CallState.FAILED
                return
            }

            // Inicializar WebRTC y crear respuesta
            webRTCManager.initialize()

            val currentCallInfo = _currentCall.value
            if (currentCallInfo?.callType == CallType.VIDEO) {
                webRTCManager.startLocalVideo()
            }

            val answer = webRTCManager.createAnswer(offer)

            // Enviar respuesta via SignalR
            signalRManager.answerCall(callerId, answer)
            pendingSdpOffer = null

            Log.i(TAG, "Llamada respondida a $callerId")
        } catch (e: Exception) {
            Log.e(TAG, "Error al responder llamada", e)
            _callState.value = CallState.FAILED
            cleanup()
        }
    }

    /**
     * Rechaza una llamada entrante.
     */
    override suspend fun rejectCall(callerId: String, reason: String) {
        signalRManager.rejectCall(callerId, reason)
        pendingSdpOffer = null
        _callState.value = CallState.REJECTED
        _currentCall.value = null
        remoteUserId = null
        Log.i(TAG, "Llamada rechazada de $callerId: $reason")

        // Volver a IDLE despues de un momento
        _callState.value = CallState.IDLE
    }

    /**
     * Finaliza la llamada actual.
     */
    override suspend fun endCall() {
        val otherUserId = remoteUserId
        if (otherUserId != null) {
            signalRManager.endCall(otherUserId)
        }
        _callState.value = CallState.ENDED
        _callEnded.tryEmit(CallEndReason(endedBy = authRepository.getCurrentUserId()))
        cleanup()
        Log.i(TAG, "Llamada finalizada")
    }

    /**
     * Envia un candidato ICE al usuario remoto via SignalR.
     */
    override suspend fun sendIceCandidate(targetUserId: String, candidate: String) {
        signalRManager.sendIceCandidate(targetUserId, candidate)
    }

    /**
     * Alterna el estado de silencio del microfono.
     */
    override suspend fun toggleMute(): Boolean {
        webRTCManager.toggleAudio()
        muted = !muted
        return muted
    }

    /**
     * Alterna el altavoz.
     * Nota: La implementacion real del altavoz requiere AudioManager del sistema.
     */
    override suspend fun toggleSpeaker(): Boolean {
        speakerOn = !speakerOn
        return speakerOn
    }

    override fun isMuted(): Boolean = muted

    override fun isSpeakerOn(): Boolean = speakerOn

    // -- Observadores de eventos --

    /**
     * Observa eventos de SignalR relacionados con llamadas.
     */
    private fun observeSignalREvents() {
        // Oferta de llamada entrante
        scope.launch {
            signalRManager.callOfferReceived.collect { dto ->
                Log.d(TAG, "Oferta de llamada recibida de: ${dto.callerId}")

                val callType = if (dto.callType.equals("video", ignoreCase = true)) {
                    CallType.VIDEO
                } else {
                    CallType.AUDIO
                }

                pendingSdpOffer = dto.sdpOffer
                remoteUserId = dto.callerId

                _currentCall.value = CallInfo(
                    callerId = dto.callerId,
                    callerName = dto.callerName,
                    callerPhoto = dto.callerPhoto,
                    callType = callType,
                    isOutgoing = false
                )
                _callState.value = CallState.RINGING

                _incomingCall.tryEmit(
                    IncomingCallOffer(
                        callerId = dto.callerId,
                        callerName = dto.callerName,
                        callerPhoto = dto.callerPhoto,
                        callType = callType,
                        sdpOffer = dto.sdpOffer
                    )
                )
            }
        }

        // Respuesta de llamada recibida (el receptor acepto)
        scope.launch {
            signalRManager.callAnswerReceived.collect { dto ->
                Log.d(TAG, "Respuesta de llamada recibida")
                try {
                    _callState.value = CallState.CONNECTING
                    webRTCManager.setRemoteAnswer(dto.sdpAnswer)
                } catch (e: Exception) {
                    Log.e(TAG, "Error al establecer respuesta remota", e)
                    _callState.value = CallState.FAILED
                }
            }
        }

        // Llamada rechazada
        scope.launch {
            signalRManager.callRejected.collect { dto ->
                Log.d(TAG, "Llamada rechazada por: ${dto.userId}, razon: ${dto.reason}")
                _callState.value = CallState.REJECTED
                _callEnded.tryEmit(CallEndReason(reason = dto.reason))
                cleanup()
            }
        }

        // Llamada finalizada por el otro usuario
        scope.launch {
            signalRManager.callEnded.collect { dto ->
                Log.d(TAG, "Llamada finalizada por: ${dto.endedBy}")
                _callState.value = CallState.ENDED
                _callEnded.tryEmit(CallEndReason(endedBy = dto.endedBy))
                cleanup()
            }
        }

        // Candidato ICE recibido
        scope.launch {
            signalRManager.iceCandidateReceived.collect { dto ->
                Log.d(TAG, "Candidato ICE recibido via SignalR")
                webRTCManager.addIceCandidate(dto.candidate)
            }
        }

        // Llamada fallida
        scope.launch {
            signalRManager.callFailed.collect { dto ->
                Log.e(TAG, "Llamada fallida: ${dto.reason} - ${dto.message}")
                _callState.value = CallState.FAILED
                _callEnded.tryEmit(CallEndReason(reason = dto.reason, message = dto.message))
                cleanup()
            }
        }
    }

    /**
     * Observa eventos de WebRTC.
     */
    private fun observeWebRTCEvents() {
        // Enviar candidatos ICE generados localmente al peer remoto via SignalR
        scope.launch {
            webRTCManager.iceCandidates.collect { candidateJson ->
                val targetId = remoteUserId
                if (targetId != null) {
                    signalRManager.sendIceCandidate(targetId, candidateJson)
                    Log.d(TAG, "Candidato ICE enviado via SignalR a $targetId")
                }
            }
        }

        // Observar estado de conexion ICE para actualizar estado de llamada
        scope.launch {
            webRTCManager.iceConnectionState.collect { state ->
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> {
                        if (_callState.value == CallState.CONNECTING ||
                            _callState.value == CallState.CALLING
                        ) {
                            _callState.value = CallState.CONNECTED
                            Log.i(TAG, "Llamada conectada")
                        }
                    }

                    PeerConnection.IceConnectionState.FAILED -> {
                        _callState.value = CallState.FAILED
                        _callEnded.tryEmit(CallEndReason(reason = "ICE connection failed"))
                        cleanup()
                        Log.e(TAG, "Conexion ICE fallida")
                    }

                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        Log.w(TAG, "Conexion ICE desconectada")
                        // Puede reconectarse, no terminar inmediatamente
                    }

                    else -> {
                        // Otros estados: NEW, CHECKING, CLOSED
                    }
                }
            }
        }
    }

    /**
     * Limpia los recursos despues de finalizar una llamada.
     */
    private fun cleanup() {
        webRTCManager.dispose()
        remoteUserId = null
        _currentCall.value = null
        pendingSdpOffer = null
        muted = false
        speakerOn = false

        // Resetear a IDLE despues de limpiar
        _callState.value = CallState.IDLE
    }
}
