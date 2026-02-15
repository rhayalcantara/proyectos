package com.clonewhatsapp.feature.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clonewhatsapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Estado de la pantalla de perfil de usuario
 */
data class ProfileState(
    val name: String = "",
    val phone: String = "",
    val info: String = "Disponible",
    val avatarUrl: String? = null,
    val selectedAvatarUri: Uri? = null,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val isLoading: Boolean = false,
    val hasChanges: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
)

/**
 * Eventos de la pantalla de perfil (MVI)
 */
sealed interface ProfileEvent {
    data class OnNameChange(val name: String) : ProfileEvent
    data class OnInfoChange(val info: String) : ProfileEvent
    data class OnAvatarSelected(val uri: Uri) : ProfileEvent
    data object OnSave : ProfileEvent
    data object OnStartEditing : ProfileEvent
    data object OnCancelEditing : ProfileEvent
    data object OnDismissError : ProfileEvent
    data object OnDismissSuccess : ProfileEvent
}

/**
 * ViewModel para la pantalla de perfil de usuario (T-135)
 *
 * Gestiona la carga, edición y guardado del perfil del usuario.
 * Incluye actualización de nombre, info/estado y avatar.
 * Sigue el patrón MVI con StateFlow.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    // Valores originales para detectar cambios
    private var originalName = ""
    private var originalInfo = ""

    init {
        cargarPerfil()
    }

    /**
     * Procesa un evento de la UI.
     */
    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.OnNameChange -> {
                _state.update {
                    it.copy(
                        name = event.name,
                        hasChanges = event.name != originalName || it.info != originalInfo || it.selectedAvatarUri != null
                    )
                }
            }

            is ProfileEvent.OnInfoChange -> {
                _state.update {
                    it.copy(
                        info = event.info,
                        hasChanges = it.name != originalName || event.info != originalInfo || it.selectedAvatarUri != null
                    )
                }
            }

            is ProfileEvent.OnAvatarSelected -> {
                _state.update {
                    it.copy(
                        selectedAvatarUri = event.uri,
                        hasChanges = true
                    )
                }
            }

            is ProfileEvent.OnSave -> guardarPerfil()

            is ProfileEvent.OnStartEditing -> {
                _state.update { it.copy(isEditing = true) }
            }

            is ProfileEvent.OnCancelEditing -> {
                _state.update {
                    it.copy(
                        isEditing = false,
                        name = originalName,
                        info = originalInfo,
                        selectedAvatarUri = null,
                        hasChanges = false
                    )
                }
            }

            is ProfileEvent.OnDismissError -> {
                _state.update { it.copy(error = null) }
            }

            is ProfileEvent.OnDismissSuccess -> {
                _state.update { it.copy(saveSuccess = false) }
            }
        }
    }

    /**
     * Carga la información del perfil actual desde el repositorio de autenticación.
     */
    private fun cargarPerfil() {
        _state.update { it.copy(isLoading = true) }

        val nombre = authRepository.getCurrentUserName() ?: ""
        val telefono = authRepository.getCurrentUserPhone() ?: ""

        originalName = nombre
        originalInfo = "Disponible" // Valor por defecto

        _state.update {
            it.copy(
                name = nombre,
                phone = telefono,
                info = originalInfo,
                isLoading = false
            )
        }
    }

    /**
     * Guarda los cambios del perfil.
     * Actualiza nombre, info y avatar si fueron modificados.
     */
    private fun guardarPerfil() {
        val currentState = _state.value
        if (!currentState.hasChanges) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            try {
                // Simular guardado del perfil
                // En implementación real:
                // - Subir avatar si selectedAvatarUri != null
                // - Actualizar nombre e info en el backend

                // Actualizar valores originales
                originalName = currentState.name
                originalInfo = currentState.info

                _state.update {
                    it.copy(
                        isSaving = false,
                        isEditing = false,
                        hasChanges = false,
                        selectedAvatarUri = null,
                        saveSuccess = true
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Error al guardar el perfil"
                    )
                }
            }
        }
    }
}
