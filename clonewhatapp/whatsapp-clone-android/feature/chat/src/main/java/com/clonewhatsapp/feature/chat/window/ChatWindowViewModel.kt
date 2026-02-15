package com.clonewhatsapp.feature.chat.window

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clonewhatsapp.domain.model.Mensaje
import com.clonewhatsapp.domain.repository.AuthRepository
import com.clonewhatsapp.domain.repository.ChatRepository
import com.clonewhatsapp.domain.repository.MensajeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de la ventana de chat
 */
data class ChatWindowState(
    val chatId: String = "",
    val chatName: String = "",
    val chatPhotoUrl: String? = null,
    val isOnline: Boolean = false,
    val messages: List<Mensaje> = emptyList(),
    val messageText: String = "",
    val currentUserId: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

/**
 * Eventos de la ventana de chat (MVI)
 */
sealed interface ChatWindowEvent {
    data class OnTextChange(val text: String) : ChatWindowEvent
    data object OnSendClick : ChatWindowEvent
    data object OnBackClick : ChatWindowEvent
    data object OnErrorDismiss : ChatWindowEvent
}

/**
 * ViewModel para la ventana de chat (T-029)
 * Sigue el patrón MVI con StateFlow
 * Recibe chatId via SavedStateHandle de navegación
 */
@HiltViewModel
class ChatWindowViewModel @Inject constructor(
    private val mensajeRepository: MensajeRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(ChatWindowState())
    val state: StateFlow<ChatWindowState> = _state.asStateFlow()

    init {
        val chatId = savedStateHandle.get<String>("chatId") ?: ""
        val currentUserId = authRepository.getCurrentUserId() ?: ""

        _state.update {
            it.copy(
                chatId = chatId,
                currentUserId = currentUserId
            )
        }

        if (chatId.isNotEmpty()) {
            loadChatInfo(chatId)
            loadMessages(chatId)
        }
    }

    fun onEvent(event: ChatWindowEvent) {
        when (event) {
            is ChatWindowEvent.OnTextChange -> {
                _state.update { it.copy(messageText = event.text) }
            }

            is ChatWindowEvent.OnSendClick -> {
                sendMessage()
            }

            is ChatWindowEvent.OnBackClick -> {
                // Navegación manejada por la pantalla
            }

            is ChatWindowEvent.OnErrorDismiss -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    /**
     * Carga la información del chat (nombre, foto, etc.)
     */
    private fun loadChatInfo(chatId: String) {
        viewModelScope.launch {
            try {
                val chat = chatRepository.getChatById(chatId)
                if (chat != null) {
                    _state.update {
                        it.copy(
                            chatName = chat.nombre,
                            chatPhotoUrl = chat.fotoUrl
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = e.message ?: "Error al cargar información del chat")
                }
            }
        }
    }

    /**
     * Carga los mensajes del chat como Flow
     */
    private fun loadMessages(chatId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            mensajeRepository.getMessages(chatId)
                .catch { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Error al cargar los mensajes"
                        )
                    }
                }
                .collect { messages ->
                    _state.update {
                        it.copy(
                            messages = messages.sortedByDescending { msg -> msg.fechaEnvio },
                            isLoading = false
                        )
                    }
                }
        }

        // Marcar mensajes como leídos
        viewModelScope.launch {
            try {
                mensajeRepository.markAllAsRead(chatId)
            } catch (_: Exception) {
                // Silenciar errores al marcar como leído
            }
        }
    }

    /**
     * Envía un mensaje de texto
     */
    private fun sendMessage() {
        val currentState = _state.value
        val text = currentState.messageText.trim()

        if (text.isBlank() || currentState.chatId.isEmpty()) return

        viewModelScope.launch {
            _state.update { it.copy(isSending = true, messageText = "") }

            mensajeRepository.sendMessage(
                chatId = currentState.chatId,
                contenido = text
            ).fold(
                onSuccess = {
                    _state.update { it.copy(isSending = false) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            messageText = text, // Restaurar texto si falla
                            error = error.message ?: "Error al enviar el mensaje"
                        )
                    }
                }
            )
        }
    }
}
