package com.clonewhatsapp.feature.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clonewhatsapp.core.common.media.AudioFocusManager
import com.clonewhatsapp.core.common.media.ProximitySensorManager
import com.clonewhatsapp.domain.model.CallInfo
import com.clonewhatsapp.domain.model.CallState
import com.clonewhatsapp.domain.model.CallType
import com.clonewhatsapp.domain.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de la UI de llamada.
 */
data class CallUiState(
    val callState: CallState = CallState.IDLE,
    val callInfo: CallInfo? = null,
    val callDuration: Long = 0L,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val error: String? = null
)

/**
 * Eventos de la UI de llamada (MVI pattern).
 */
sealed interface CallEvent {
    data class StartCall(val targetUserId: String, val callType: CallType) : CallEvent
    data object AcceptCall : CallEvent
    data object RejectCall : CallEvent
    data object EndCall : CallEvent
    data object ToggleMute : CallEvent
    data object ToggleSpeaker : CallEvent
    data object ToggleVideo : CallEvent
    data object SwitchCamera : CallEvent
}

/**
 * ViewModel que gestiona el estado de las llamadas.
 *
 * Conecta los eventos de la UI con el CallRepository y gestiona
 * los recursos auxiliares (foco de audio, sensor de proximidad).
 * Sigue el patron MVI con StateFlow.
 */
@HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val audioFocusManager: AudioFocusManager,
    private val proximitySensorManager: ProximitySensorManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private var durationJob: Job? = null
    private var callStartTime: Long = 0L

    init {
        observarEstadoLlamada()
        observarInfoLlamada()
        observarFinLlamada()
    }

    /**
     * Procesa un evento de la UI.
     */
    fun onEvent(event: CallEvent) {
        when (event) {
            is CallEvent.StartCall -> iniciarLlamada(event.targetUserId, event.callType)
            is CallEvent.AcceptCall -> aceptarLlamada()
            is CallEvent.RejectCall -> rechazarLlamada()
            is CallEvent.EndCall -> finalizarLlamada()
            is CallEvent.ToggleMute -> alternarSilencio()
            is CallEvent.ToggleSpeaker -> alternarAltavoz()
            is CallEvent.ToggleVideo -> alternarVideo()
            is CallEvent.SwitchCamera -> cambiarCamara()
        }
    }

    // -----------------------------------------------------------------------
    // Observadores del repositorio
    // -----------------------------------------------------------------------

    private fun observarEstadoLlamada() {
        viewModelScope.launch {
            callRepository.callState.collect { estado ->
                _uiState.update { it.copy(callState = estado) }

                when (estado) {
                    CallState.CALLING, CallState.RINGING -> {
                        audioFocusManager.requestCallAudioFocus()
                        iniciarSensorProximidadSiEsVoz()
                    }
                    CallState.CONNECTED -> {
                        callStartTime = System.currentTimeMillis()
                        iniciarContadorDuracion()
                    }
                    CallState.ENDED, CallState.FAILED, CallState.REJECTED -> {
                        detenerContadorDuracion()
                        liberarRecursos()
                    }
                    CallState.IDLE -> {
                        detenerContadorDuracion()
                        liberarRecursos()
                        _uiState.update { CallUiState() }
                    }
                    else -> { /* CONNECTING: mantener recursos activos */ }
                }
            }
        }
    }

    private fun observarInfoLlamada() {
        viewModelScope.launch {
            callRepository.currentCall.collect { info ->
                _uiState.update { it.copy(callInfo = info) }
            }
        }
    }

    private fun observarFinLlamada() {
        viewModelScope.launch {
            callRepository.callEnded.collect { razon ->
                _uiState.update {
                    it.copy(
                        callState = CallState.ENDED,
                        error = razon.message
                    )
                }
                detenerContadorDuracion()
                liberarRecursos()
            }
        }
    }

    // -----------------------------------------------------------------------
    // Acciones de llamada
    // -----------------------------------------------------------------------

    private fun iniciarLlamada(targetUserId: String, callType: CallType) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isVideoEnabled = callType == CallType.VIDEO,
                        error = null
                    )
                }
                callRepository.startCall(targetUserId, callType)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al iniciar llamada: ${e.message}") }
            }
        }
    }

    private fun aceptarLlamada() {
        viewModelScope.launch {
            try {
                // La respuesta SDP se genera internamente en el repositorio
                callRepository.answerCall("", "")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al aceptar llamada: ${e.message}") }
            }
        }
    }

    private fun rechazarLlamada() {
        viewModelScope.launch {
            try {
                callRepository.rejectCall("", "Rechazada por usuario")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al rechazar llamada: ${e.message}") }
            }
        }
    }

    private fun finalizarLlamada() {
        viewModelScope.launch {
            try {
                callRepository.endCall()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al finalizar llamada: ${e.message}") }
            }
        }
    }

    private fun alternarSilencio() {
        viewModelScope.launch {
            try {
                val silenciado = callRepository.toggleMute()
                _uiState.update { it.copy(isMuted = silenciado) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al cambiar silencio: ${e.message}") }
            }
        }
    }

    private fun alternarAltavoz() {
        viewModelScope.launch {
            try {
                val altavoz = callRepository.toggleSpeaker()
                audioFocusManager.setSpeakerOn(altavoz)
                _uiState.update { it.copy(isSpeakerOn = altavoz) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al cambiar altavoz: ${e.message}") }
            }
        }
    }

    private fun alternarVideo() {
        _uiState.update { it.copy(isVideoEnabled = !it.isVideoEnabled) }
        // La logica de activar/desactivar la pista de video se delega al repositorio
        // o al WebRTC manager en una implementacion futura
    }

    private fun cambiarCamara() {
        // La logica de cambio de camara se delega al WebRTC manager
        // en una implementacion futura
    }

    // -----------------------------------------------------------------------
    // Contador de duracion
    // -----------------------------------------------------------------------

    private fun iniciarContadorDuracion() {
        detenerContadorDuracion()
        durationJob = viewModelScope.launch {
            while (true) {
                val duracion = System.currentTimeMillis() - callStartTime
                _uiState.update { it.copy(callDuration = duracion) }
                delay(1_000L)
            }
        }
    }

    private fun detenerContadorDuracion() {
        durationJob?.cancel()
        durationJob = null
    }

    // -----------------------------------------------------------------------
    // Gestion de recursos
    // -----------------------------------------------------------------------

    private fun iniciarSensorProximidadSiEsVoz() {
        val info = _uiState.value.callInfo
        if (info?.callType == CallType.AUDIO) {
            proximitySensorManager.start()
        }
    }

    private fun liberarRecursos() {
        audioFocusManager.abandonCallAudioFocus()
        proximitySensorManager.stop()
    }

    override fun onCleared() {
        super.onCleared()
        detenerContadorDuracion()
        liberarRecursos()
    }
}
