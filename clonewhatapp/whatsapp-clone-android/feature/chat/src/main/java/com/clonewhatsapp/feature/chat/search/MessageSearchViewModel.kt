package com.clonewhatsapp.feature.chat.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clonewhatsapp.core.database.dao.SearchDao
import com.clonewhatsapp.core.database.dao.SearchResultEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Modelo de resultado de busqueda para la capa de presentacion.
 *
 * Contiene toda la informacion necesaria para mostrar un resultado
 * de busqueda en la interfaz de usuario.
 */
data class SearchResult(
    val chatId: String,
    val chatName: String,
    val messageId: String,
    val messagePreview: String,
    val timestamp: Long,
    val senderName: String
)

/**
 * Estado de la pantalla de busqueda de mensajes (MVI).
 */
data class MessageSearchState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null
)

/**
 * Eventos de la pantalla de busqueda de mensajes (MVI).
 */
sealed interface MessageSearchEvent {
    data class OnQueryChange(val query: String) : MessageSearchEvent
    data class OnResultClick(val chatId: String, val messageId: String) : MessageSearchEvent
    data object OnBackClick : MessageSearchEvent
    data object OnClearQuery : MessageSearchEvent
    data object OnErrorDismiss : MessageSearchEvent
}

/**
 * ViewModel para la busqueda global de mensajes (T-131).
 *
 * Implementa busqueda con debounce de 300ms para evitar consultas
 * innecesarias mientras el usuario escribe. Busca en la base de datos
 * local Room usando LIKE para coincidencias parciales.
 *
 * Sigue el patron MVI con StateFlow.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class MessageSearchViewModel @Inject constructor(
    private val searchDao: SearchDao
) : ViewModel() {

    private val _state = MutableStateFlow(MessageSearchState())
    val state: StateFlow<MessageSearchState> = _state.asStateFlow()

    // Flow interno para manejar el debounce de la busqueda
    private val _queryFlow = MutableStateFlow("")

    init {
        configurarBusquedaConDebounce()
    }

    /**
     * Procesa los eventos de la pantalla de busqueda.
     */
    fun onEvent(event: MessageSearchEvent) {
        when (event) {
            is MessageSearchEvent.OnQueryChange -> {
                _state.update { it.copy(query = event.query) }
                _queryFlow.value = event.query
            }

            is MessageSearchEvent.OnResultClick -> {
                // La navegacion se maneja en la pantalla
            }

            is MessageSearchEvent.OnBackClick -> {
                // La navegacion se maneja en la pantalla
            }

            is MessageSearchEvent.OnClearQuery -> {
                _state.update {
                    it.copy(
                        query = "",
                        results = emptyList(),
                        hasSearched = false
                    )
                }
                _queryFlow.value = ""
            }

            is MessageSearchEvent.OnErrorDismiss -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    /**
     * Configura la busqueda con debounce de 300ms.
     *
     * Solo ejecuta la busqueda cuando:
     * - Han pasado 300ms desde la ultima pulsacion de tecla
     * - La consulta tiene al menos 2 caracteres
     * - La consulta es diferente a la anterior
     */
    private fun configurarBusquedaConDebounce() {
        viewModelScope.launch {
            _queryFlow
                .debounce(300L)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.length >= 2) {
                        ejecutarBusqueda(query)
                    } else if (query.isEmpty()) {
                        _state.update {
                            it.copy(
                                results = emptyList(),
                                hasSearched = false,
                                isSearching = false
                            )
                        }
                    }
                }
        }
    }

    /**
     * Ejecuta la busqueda en la base de datos local.
     */
    private suspend fun ejecutarBusqueda(query: String) {
        _state.update { it.copy(isSearching = true) }

        try {
            val resultadosEntidad = searchDao.buscarMensajes(query)
            val resultados = resultadosEntidad.map { entidad ->
                entidad.toSearchResult()
            }

            _state.update {
                it.copy(
                    results = resultados,
                    isSearching = false,
                    hasSearched = true
                )
            }
        } catch (excepcion: Exception) {
            _state.update {
                it.copy(
                    isSearching = false,
                    hasSearched = true,
                    error = excepcion.message ?: "Error al buscar mensajes"
                )
            }
        }
    }
}

/**
 * Extension para convertir SearchResultEntity a SearchResult de presentacion.
 */
private fun SearchResultEntity.toSearchResult(): SearchResult {
    return SearchResult(
        chatId = chatId,
        chatName = chatName,
        messageId = messageId,
        messagePreview = contenido ?: "",
        timestamp = fechaEnvio,
        senderName = remitenteNombre ?: ""
    )
}
