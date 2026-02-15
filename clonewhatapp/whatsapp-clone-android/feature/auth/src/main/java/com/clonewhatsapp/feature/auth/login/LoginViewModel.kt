package com.clonewhatsapp.feature.auth.login

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

data class LoginState(
    val phone: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoginSuccess: Boolean = false
)

sealed interface LoginEvent {
    data class OnPhoneChange(val phone: String) : LoginEvent
    data class OnPasswordChange(val password: String) : LoginEvent
    data object OnLoginClick : LoginEvent
    data object OnErrorDismiss : LoginEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.OnPhoneChange -> {
                _state.update { it.copy(phone = event.phone) }
            }

            is LoginEvent.OnPasswordChange -> {
                _state.update { it.copy(password = event.password) }
            }

            is LoginEvent.OnLoginClick -> {
                login()
            }

            is LoginEvent.OnErrorDismiss -> {
                _state.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun login() {
        val currentState = _state.value

        // Validation
        if (currentState.phone.isBlank()) {
            _state.update { it.copy(errorMessage = "Por favor ingresa tu número de teléfono") }
            return
        }

        if (currentState.password.isBlank()) {
            _state.update { it.copy(errorMessage = "Por favor ingresa tu contraseña") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result = authRepository.login(
                    phone = currentState.phone,
                    password = currentState.password
                )

                result.fold(
                    onSuccess = { usuario ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isLoginSuccess = true,
                                errorMessage = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.message ?: "Error al iniciar sesión"
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
