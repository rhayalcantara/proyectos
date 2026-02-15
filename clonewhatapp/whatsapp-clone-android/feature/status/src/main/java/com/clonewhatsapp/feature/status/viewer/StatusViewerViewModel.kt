package com.clonewhatsapp.feature.status.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clonewhatsapp.domain.model.EstadoCompleto
import com.clonewhatsapp.domain.model.EstadosContacto
import com.clonewhatsapp.domain.model.TipoEstado
import com.clonewhatsapp.domain.model.VistaEstado
import com.clonewhatsapp.domain.repository.StatusRepository
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
 * Estado del visor de estados (T-089)
 */
data class StatusViewerState(
    val estados: List<EstadoCompleto> = emptyList(),
    val indiceActual: Int = 0,
    val progreso: Float = 0f,
    val usuarioNombre: String = "",
    val usuarioFoto: String? = null,
    val esMiEstado: Boolean = false,
    val vistas: List<VistaEstado> = emptyList(),
    val mostrarVistas: Boolean = false,
    val isLoading: Boolean = false,
    val cerrar: Boolean = false
)

/**
 * Eventos del visor de estados (MVI)
 */
sealed interface StatusViewerEvent {
    /** Ir al estado anterior */
    data object OnAnterior : StatusViewerEvent
    /** Ir al siguiente estado */
    data object OnSiguiente : StatusViewerEvent
    /** Pausar el temporizador */
    data object OnPausar : StatusViewerEvent
    /** Reanudar el temporizador */
    data object OnReanudar : StatusViewerEvent
    /** Cerrar el visor */
    data object OnCerrar : StatusViewerEvent
    /** Mostrar/ocultar lista de vistas */
    data object OnToggleVistas : StatusViewerEvent
    /** Eliminar el estado actual (solo mis estados) */
    data object OnEliminar : StatusViewerEvent
}

/**
 * ViewModel para el visor de estados (T-089)
 * Maneja auto-avance con temporizador y marcado como visto
 */
@HiltViewModel
class StatusViewerViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        /** Duración para estados de texto (5 segundos) */
        private const val DURACION_TEXTO_MS = 5000L
        /** Duración para estados de imagen (7 segundos) */
        private const val DURACION_IMAGEN_MS = 7000L
        /** Intervalo de actualización del progreso */
        private const val INTERVALO_PROGRESO_MS = 50L
    }

    private val _state = MutableStateFlow(StatusViewerState())
    val state: StateFlow<StatusViewerState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var pausado = false

    /**
     * Inicializa el visor con los estados de un contacto
     * @param estados Lista de estados a mostrar
     * @param usuarioNombre Nombre del usuario
     * @param usuarioFoto Foto del usuario
     * @param esMiEstado Si son mis propios estados
     */
    fun inicializar(
        estados: List<EstadoCompleto>,
        usuarioNombre: String,
        usuarioFoto: String?,
        esMiEstado: Boolean
    ) {
        _state.update {
            it.copy(
                estados = estados,
                indiceActual = 0,
                usuarioNombre = usuarioNombre,
                usuarioFoto = usuarioFoto,
                esMiEstado = esMiEstado,
                progreso = 0f
            )
        }

        // Encontrar el primer estado no visto
        val primerNoVisto = estados.indexOfFirst { !it.vistoPorMi }
        if (primerNoVisto > 0 && !esMiEstado) {
            _state.update { it.copy(indiceActual = primerNoVisto) }
        }

        marcarComoVisto()
        iniciarTemporizador()
    }

    fun onEvent(event: StatusViewerEvent) {
        when (event) {
            is StatusViewerEvent.OnAnterior -> irAnterior()
            is StatusViewerEvent.OnSiguiente -> irSiguiente()
            is StatusViewerEvent.OnPausar -> pausar()
            is StatusViewerEvent.OnReanudar -> reanudar()
            is StatusViewerEvent.OnCerrar -> {
                timerJob?.cancel()
                _state.update { it.copy(cerrar = true) }
            }
            is StatusViewerEvent.OnToggleVistas -> toggleVistas()
            is StatusViewerEvent.OnEliminar -> eliminarEstadoActual()
        }
    }

    /**
     * Ir al estado anterior
     */
    private fun irAnterior() {
        val estadoActual = _state.value
        if (estadoActual.indiceActual > 0) {
            _state.update {
                it.copy(
                    indiceActual = it.indiceActual - 1,
                    progreso = 0f,
                    mostrarVistas = false
                )
            }
            marcarComoVisto()
            iniciarTemporizador()
        }
    }

    /**
     * Ir al siguiente estado
     */
    private fun irSiguiente() {
        val estadoActual = _state.value
        if (estadoActual.indiceActual < estadoActual.estados.size - 1) {
            _state.update {
                it.copy(
                    indiceActual = it.indiceActual + 1,
                    progreso = 0f,
                    mostrarVistas = false
                )
            }
            marcarComoVisto()
            iniciarTemporizador()
        } else {
            // Último estado, cerrar visor
            timerJob?.cancel()
            _state.update { it.copy(cerrar = true) }
        }
    }

    /**
     * Pausar el temporizador
     */
    private fun pausar() {
        pausado = true
        timerJob?.cancel()
    }

    /**
     * Reanudar el temporizador
     */
    private fun reanudar() {
        pausado = false
        iniciarTemporizador()
    }

    /**
     * Mostrar/ocultar lista de vistas (solo mis estados)
     */
    private fun toggleVistas() {
        val estado = _state.value
        if (estado.esMiEstado) {
            if (!estado.mostrarVistas) {
                // Cargar vistas
                pausar()
                cargarVistas()
            } else {
                _state.update { it.copy(mostrarVistas = false) }
                reanudar()
            }
        }
    }

    /**
     * Carga la lista de quiénes vieron el estado actual
     */
    private fun cargarVistas() {
        val estadoActual = obtenerEstadoActual() ?: return

        viewModelScope.launch {
            statusRepository.getViewers(estadoActual.id).fold(
                onSuccess = { vistas ->
                    _state.update {
                        it.copy(
                            vistas = vistas,
                            mostrarVistas = true
                        )
                    }
                },
                onFailure = { /* Ignorar error silenciosamente */ }
            )
        }
    }

    /**
     * Elimina el estado actual (solo mis estados)
     */
    private fun eliminarEstadoActual() {
        val estadoActual = obtenerEstadoActual() ?: return

        viewModelScope.launch {
            statusRepository.deleteStatus(estadoActual.id).fold(
                onSuccess = {
                    val estadosRestantes = _state.value.estados.filterNot { it.id == estadoActual.id }
                    if (estadosRestantes.isEmpty()) {
                        _state.update { it.copy(cerrar = true) }
                    } else {
                        val nuevoIndice = _state.value.indiceActual.coerceAtMost(estadosRestantes.size - 1)
                        _state.update {
                            it.copy(
                                estados = estadosRestantes,
                                indiceActual = nuevoIndice,
                                progreso = 0f
                            )
                        }
                        iniciarTemporizador()
                    }
                },
                onFailure = { /* Ignorar error */ }
            )
        }
    }

    /**
     * Marca el estado actual como visto
     */
    private fun marcarComoVisto() {
        val estado = obtenerEstadoActual() ?: return
        if (_state.value.esMiEstado) return // No marcar mis propios estados

        viewModelScope.launch {
            statusRepository.markAsViewed(estado.id)
        }
    }

    /**
     * Inicia el temporizador de auto-avance
     */
    private fun iniciarTemporizador() {
        timerJob?.cancel()

        val estadoActual = obtenerEstadoActual() ?: return
        val duracionTotal = when (estadoActual.tipo) {
            TipoEstado.TEXTO -> DURACION_TEXTO_MS
            TipoEstado.IMAGEN -> DURACION_IMAGEN_MS
            else -> DURACION_TEXTO_MS
        }

        // Comenzar desde el progreso actual
        val progresoInicial = _state.value.progreso

        timerJob = viewModelScope.launch {
            var tiempoTranscurrido = (progresoInicial * duracionTotal).toLong()

            while (tiempoTranscurrido < duracionTotal) {
                delay(INTERVALO_PROGRESO_MS)
                tiempoTranscurrido += INTERVALO_PROGRESO_MS
                val nuevoProgreso = (tiempoTranscurrido.toFloat() / duracionTotal).coerceAtMost(1f)
                _state.update { it.copy(progreso = nuevoProgreso) }
            }

            // Avanzar al siguiente
            irSiguiente()
        }
    }

    /**
     * Obtiene el estado actualmente visible
     */
    private fun obtenerEstadoActual(): EstadoCompleto? {
        val estado = _state.value
        return estado.estados.getOrNull(estado.indiceActual)
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
