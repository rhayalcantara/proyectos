package com.clonewhatsapp.feature.status.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clonewhatsapp.domain.model.EstadoCompleto
import com.clonewhatsapp.domain.model.EstadosContacto
import com.clonewhatsapp.domain.model.MisEstados
import com.clonewhatsapp.domain.repository.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de la pantalla de lista de estados (T-088)
 */
data class StatusListState(
    val misEstados: MisEstados = MisEstados(estados = emptyList(), totalVistas = 0),
    val estadosContactos: List<EstadosContacto> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

/**
 * Eventos de la pantalla de lista de estados (MVI)
 */
sealed interface StatusListEvent {
    /** Navegar a ver mis estados */
    data object OnMyStatusClick : StatusListEvent
    /** Navegar a ver estados de un contacto */
    data class OnContactStatusClick(val usuarioId: String) : StatusListEvent
    /** Crear nuevo estado de texto */
    data object OnCreateTextStatus : StatusListEvent
    /** Crear nuevo estado con imagen */
    data object OnCreateImageStatus : StatusListEvent
    /** Refrescar lista */
    data object OnRefresh : StatusListEvent
    /** Descartar error */
    data object OnErrorDismiss : StatusListEvent
}

/**
 * ViewModel para la lista de estados (T-088)
 * Sigue el patrón MVI con StateFlow
 */
@HiltViewModel
class StatusListViewModel @Inject constructor(
    private val statusRepository: StatusRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatusListState())
    val state: StateFlow<StatusListState> = _state.asStateFlow()

    init {
        cargarEstados()
    }

    fun onEvent(event: StatusListEvent) {
        when (event) {
            is StatusListEvent.OnMyStatusClick -> {
                // Navegación manejada por la pantalla
            }
            is StatusListEvent.OnContactStatusClick -> {
                // Navegación manejada por la pantalla
            }
            is StatusListEvent.OnCreateTextStatus -> {
                // Navegación manejada por la pantalla
            }
            is StatusListEvent.OnCreateImageStatus -> {
                // Navegación manejada por la pantalla
            }
            is StatusListEvent.OnRefresh -> {
                refrescarEstados()
            }
            is StatusListEvent.OnErrorDismiss -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    /**
     * Carga mis estados y los estados de contactos
     */
    private fun cargarEstados() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            // Cargar mis estados
            statusRepository.getMyStatuses().fold(
                onSuccess = { misEstados ->
                    _state.update { it.copy(misEstados = misEstados) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(error = error.message ?: "Error al cargar mis estados")
                    }
                }
            )

            // Cargar estados de contactos
            statusRepository.getContactsStatuses().fold(
                onSuccess = { contactos ->
                    _state.update {
                        it.copy(
                            estadosContactos = contactos,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al cargar estados de contactos"
                        )
                    }
                }
            )
        }
    }

    /**
     * Refresca los estados desde el servidor
     */
    private fun refrescarEstados() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            statusRepository.getMyStatuses().fold(
                onSuccess = { misEstados ->
                    _state.update { it.copy(misEstados = misEstados) }
                },
                onFailure = { /* ignorar en refresh */ }
            )

            statusRepository.getContactsStatuses().fold(
                onSuccess = { contactos ->
                    _state.update {
                        it.copy(
                            estadosContactos = contactos,
                            isRefreshing = false
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            error = error.message ?: "Error al refrescar estados"
                        )
                    }
                }
            )
        }
    }
}
