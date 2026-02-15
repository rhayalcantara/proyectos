package com.clonewhatsapp.feature.chat.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clonewhatsapp.domain.model.Chat
import com.clonewhatsapp.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de la pantalla de lista de chats
 */
data class ChatListState(
    val chats: List<Chat> = emptyList(),
    val filteredChats: List<Chat> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val error: String? = null
)

/**
 * Eventos de la pantalla de lista de chats (MVI)
 */
sealed interface ChatListEvent {
    data class OnChatClick(val chatId: String) : ChatListEvent
    data object OnNewChatClick : ChatListEvent
    data object OnSearchClick : ChatListEvent
    data class OnSearchQueryChange(val query: String) : ChatListEvent
    data object OnCloseSearch : ChatListEvent
    data object OnRefresh : ChatListEvent
    data object OnErrorDismiss : ChatListEvent
}

/**
 * ViewModel para la lista de chats (T-027)
 * Sigue el patrón MVI con StateFlow
 */
@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    init {
        loadChats()
    }

    fun onEvent(event: ChatListEvent) {
        when (event) {
            is ChatListEvent.OnChatClick -> {
                // Navegación manejada por la pantalla
            }

            is ChatListEvent.OnNewChatClick -> {
                // Navegación manejada por la pantalla
            }

            is ChatListEvent.OnSearchClick -> {
                _state.update { it.copy(isSearching = true) }
            }

            is ChatListEvent.OnSearchQueryChange -> {
                _state.update { it.copy(searchQuery = event.query) }
                filterChats(event.query)
            }

            is ChatListEvent.OnCloseSearch -> {
                _state.update {
                    it.copy(
                        isSearching = false,
                        searchQuery = "",
                        filteredChats = it.chats
                    )
                }
            }

            is ChatListEvent.OnRefresh -> {
                refreshChats()
            }

            is ChatListEvent.OnErrorDismiss -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    /**
     * Carga los chats como Flow desde el repositorio
     */
    private fun loadChats() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            chatRepository.getChats()
                .catch { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al cargar los chats"
                        )
                    }
                }
                .collect { chats ->
                    _state.update {
                        it.copy(
                            chats = chats,
                            filteredChats = if (it.searchQuery.isNotEmpty()) {
                                chats.filter { chat ->
                                    chat.nombre.contains(it.searchQuery, ignoreCase = true)
                                }
                            } else {
                                chats
                            },
                            isLoading = false
                        )
                    }
                }
        }
    }

    /**
     * Refresca los chats desde el servidor
     */
    private fun refreshChats() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }

            chatRepository.refreshChats().fold(
                onSuccess = {
                    _state.update { it.copy(isRefreshing = false) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            error = error.message ?: "Error al refrescar los chats"
                        )
                    }
                }
            )
        }
    }

    /**
     * Filtra los chats por nombre
     */
    private fun filterChats(query: String) {
        val currentChats = _state.value.chats
        val filtered = if (query.isBlank()) {
            currentChats
        } else {
            currentChats.filter { chat ->
                chat.nombre.contains(query, ignoreCase = true) ||
                    (chat.ultimoMensaje?.contains(query, ignoreCase = true) == true)
            }
        }
        _state.update { it.copy(filteredChats = filtered) }
    }
}
