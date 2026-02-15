package com.clonewhatsapp.feature.auth.register

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

data class RegisterState(
    val name: String = "",
    val phone: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegisterSuccess: Boolean = false
)

sealed interface RegisterEvent {
    data class OnNameChange(val name: String) : RegisterEvent
    data class OnPhoneChange(val phone: String) : RegisterEvent
    data class OnPasswordChange(val password: String) : RegisterEvent
    data class OnConfirmPasswordChange(val confirmPassword: String) : RegisterEvent
    data object OnRegisterClick : RegisterEvent
    data object OnErrorDismiss : RegisterEvent
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.OnNameChange -> {
                _state.update { it.copy(name = event.name) }
            }

            is RegisterEvent.OnPhoneChange -> {
                _state.update { it.copy(phone = event.phone) }
            }

            is RegisterEvent.OnPasswordChange -> {
                _state.update { it.copy(password = event.password) }
            }

            is RegisterEvent.OnConfirmPasswordChange -> {
                _state.update { it.copy(confirmPassword = event.confirmPassword) }
            }

            is RegisterEvent.OnRegisterClick -> {
                register()
            }

            is RegisterEvent.OnErrorDismiss -> {
                _state.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun register() {
        val currentState = _state.value

        // Validation
        if (currentState.name.isBlank()) {
            _state.update { it.copy(errorMessage = "Por favor ingresa tu nombre") }
            return
        }

        if (currentState.phone.isBlank()) {
            _state.update { it.copy(errorMessage = "Por favor ingresa tu número de teléfono") }
            return
        }

        if (currentState.password.isBlank()) {
            _state.update { it.copy(errorMessage = "Por favor ingresa una contraseña") }
            return
        }

        if (currentState.confirmPassword.isBlank()) {
            _state.update { it.copy(errorMessage = "Por favor confirma tu contraseña") }
            return
        }

        if (currentState.password != currentState.confirmPassword) {
            _state.update { it.copy(errorMessage = "Las contraseñas no coinciden") }
            return
        }

        if (currentState.password.length < 6) {
            _state.update { it.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = authRepository.register(
                    name = currentState.name,
                    phone = currentState.phone,
                    password = currentState.password
                )

                result.fold(
                    onSuccess = { usuario ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isRegisterSuccess = true,
                                errorMessage = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Error al registrarse"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error desconocido"
                    )
                }
            }
        }
    }
}
