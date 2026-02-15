package com.clonewhatsapp.feature.calls.history

import androidx.lifecycle.ViewModel
import com.clonewhatsapp.domain.model.CallHistoryItem
import com.clonewhatsapp.domain.model.CallType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Estado de la UI del historial de llamadas.
 *
 * @param calls Lista de elementos del historial de llamadas.
 * @param isLoading Indica si se estan cargando los datos.
 * @param errorMessage Mensaje de error si ocurrio alguno, null si no hay error.
 */
data class CallHistoryUiState(
    val calls: List<CallHistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel para la pantalla de historial de llamadas.
 *
 * Implementacion MVI simple. Por ahora utiliza datos placeholder que seran
 * reemplazados cuando la API de llamadas este disponible.
 *
 * Inyectado con Hilt.
 */
@HiltViewModel
class CallHistoryViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CallHistoryUiState())

    /** Estado observable de la UI del historial de llamadas. */
    val uiState: StateFlow<CallHistoryUiState> = _uiState.asStateFlow()

    init {
        loadCallHistory()
    }

    /**
     * Carga el historial de llamadas.
     * Por ahora utiliza datos placeholder de ejemplo.
     */
    private fun loadCallHistory() {
        _uiState.value = CallHistoryUiState(
            calls = generatePlaceholderCalls(),
            isLoading = false
        )
    }

    /**
     * Refresca el historial de llamadas.
     */
    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadCallHistory()
    }

    /**
     * Genera datos placeholder de ejemplo para el historial.
     * Estos datos seran reemplazados por datos reales del repositorio.
     */
    private fun generatePlaceholderCalls(): List<CallHistoryItem> {
        val now = System.currentTimeMillis()
        val oneHour = 3_600_000L
        val oneDay = 86_400_000L

        return listOf(
            CallHistoryItem(
                id = "1",
                contactId = "c1",
                contactName = "Maria Garcia",
                contactPhoto = null,
                callType = CallType.VIDEO,
                isOutgoing = true,
                wasAnswered = true,
                timestamp = now - oneHour,
                durationSeconds = 320
            ),
            CallHistoryItem(
                id = "2",
                contactId = "c2",
                contactName = "Carlos Lopez",
                contactPhoto = null,
                callType = CallType.AUDIO,
                isOutgoing = false,
                wasAnswered = false,
                timestamp = now - (2 * oneHour),
                durationSeconds = null
            ),
            CallHistoryItem(
                id = "3",
                contactId = "c2",
                contactName = "Carlos Lopez",
                contactPhoto = null,
                callType = CallType.AUDIO,
                isOutgoing = false,
                wasAnswered = false,
                timestamp = now - (3 * oneHour),
                durationSeconds = null
            ),
            CallHistoryItem(
                id = "4",
                contactId = "c3",
                contactName = "Ana Martinez",
                contactPhoto = null,
                callType = CallType.VIDEO,
                isOutgoing = false,
                wasAnswered = true,
                timestamp = now - oneDay,
                durationSeconds = 180
            ),
            CallHistoryItem(
                id = "5",
                contactId = "c4",
                contactName = "Pedro Sanchez",
                contactPhoto = null,
                callType = CallType.AUDIO,
                isOutgoing = true,
                wasAnswered = true,
                timestamp = now - (2 * oneDay),
                durationSeconds = 45
            ),
            CallHistoryItem(
                id = "6",
                contactId = "c5",
                contactName = "Laura Fernandez",
                contactPhoto = null,
                callType = CallType.VIDEO,
                isOutgoing = true,
                wasAnswered = false,
                timestamp = now - (5 * oneDay),
                durationSeconds = null
            )
        )
    }
}
