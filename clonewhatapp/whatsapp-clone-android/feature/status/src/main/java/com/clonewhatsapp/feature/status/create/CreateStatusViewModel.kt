package com.clonewhatsapp.feature.status.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clonewhatsapp.domain.model.EstadoCompleto
import com.clonewhatsapp.domain.repository.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Colores disponibles para estados de texto
 */
val COLORES_ESTADO = listOf(
    "#075E54", // Verde oscuro WhatsApp
    "#128C7E", // Verde WhatsApp
    "#25D366", // Verde claro WhatsApp
    "#1DA1F2", // Azul
    "#6C5CE7", // Morado
    "#E17055", // Naranja
    "#D63031", // Rojo
    "#2D3436", // Negro
    "#0984E3", // Azul cielo
    "#E84393"  // Rosa
)

/**
 * Modo de creación del estado
 */
enum class ModoCreacion {
    TEXTO,
    IMAGEN
}

/**
 * Estado de la pantalla de creación de estados (T-090)
 */
data class CreateStatusState(
    val modo: ModoCreacion = ModoCreacion.TEXTO,
    val textoContenido: String = "",
    val colorFondoIndice: Int = 0,
    val imagenUri: Uri? = null,
    val caption: String = "",
    val isCreating: Boolean = false,
    val estadoCreado: EstadoCompleto? = null,
    val error: String? = null
) {
    /** Color de fondo actual basado en el índice */
    val colorFondoActual: String get() = COLORES_ESTADO[colorFondoIndice % COLORES_ESTADO.size]
}

/**
 * Eventos de la pantalla de creación de estados (MVI)
 */
sealed interface CreateStatusEvent {
    /** Cambiar el texto del estado */
    data class OnTextoChange(val texto: String) : CreateStatusEvent
    /** Cambiar el caption de la imagen */
    data class OnCaptionChange(val caption: String) : CreateStatusEvent
    /** Ciclar al siguiente color de fondo */
    data object OnCambiarColor : CreateStatusEvent
    /** Seleccionar imagen */
    data class OnImagenSeleccionada(val uri: Uri) : CreateStatusEvent
    /** Cambiar a modo texto */
    data object OnModoTexto : CreateStatusEvent
    /** Cambiar a modo imagen */
    data object OnModoImagen : CreateStatusEvent
    /** Enviar/publicar el estado */
    data object OnPublicar : CreateStatusEvent
    /** Descartar error */
    data object OnErrorDismiss : CreateStatusEvent
}

/**
 * ViewModel para la creación de estados (T-090)
 * Sigue el patrón MVI con StateFlow
 */
@HiltViewModel
class CreateStatusViewModel @Inject constructor(
    private val statusRepository: StatusRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreateStatusState())
    val state: StateFlow<CreateStatusState> = _state.asStateFlow()

    fun onEvent(event: CreateStatusEvent) {
        when (event) {
            is CreateStatusEvent.OnTextoChange -> {
                _state.update { it.copy(textoContenido = event.texto) }
            }
            is CreateStatusEvent.OnCaptionChange -> {
                _state.update { it.copy(caption = event.caption) }
            }
            is CreateStatusEvent.OnCambiarColor -> {
                _state.update {
                    it.copy(colorFondoIndice = (it.colorFondoIndice + 1) % COLORES_ESTADO.size)
                }
            }
            is CreateStatusEvent.OnImagenSeleccionada -> {
                _state.update {
                    it.copy(
                        modo = ModoCreacion.IMAGEN,
                        imagenUri = event.uri
                    )
                }
            }
            is CreateStatusEvent.OnModoTexto -> {
                _state.update { it.copy(modo = ModoCreacion.TEXTO, imagenUri = null) }
            }
            is CreateStatusEvent.OnModoImagen -> {
                _state.update { it.copy(modo = ModoCreacion.IMAGEN) }
            }
            is CreateStatusEvent.OnPublicar -> {
                publicarEstado()
            }
            is CreateStatusEvent.OnErrorDismiss -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    /**
     * Publica el estado según el modo actual
     */
    private fun publicarEstado() {
        val estadoActual = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isCreating = true) }

            val resultado = when (estadoActual.modo) {
                ModoCreacion.TEXTO -> {
                    if (estadoActual.textoContenido.isBlank()) {
                        _state.update {
                            it.copy(
                                isCreating = false,
                                error = "Escribe algo para publicar tu estado"
                            )
                        }
                        return@launch
                    }
                    statusRepository.createTextStatus(
                        contenido = estadoActual.textoContenido,
                        colorFondo = estadoActual.colorFondoActual
                    )
                }
                ModoCreacion.IMAGEN -> {
                    val uri = estadoActual.imagenUri
                    if (uri == null) {
                        _state.update {
                            it.copy(
                                isCreating = false,
                                error = "Selecciona una imagen para publicar"
                            )
                        }
                        return@launch
                    }
                    statusRepository.createImageStatus(
                        imageUri = uri,
                        caption = estadoActual.caption.ifBlank { null }
                    )
                }
            }

            resultado.fold(
                onSuccess = { estado ->
                    _state.update {
                        it.copy(
                            isCreating = false,
                            estadoCreado = estado
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isCreating = false,
                            error = error.message ?: "Error al publicar el estado"
                        )
                    }
                }
            )
        }
    }
}
