package com.clonewhatsapp.feature.auth.biometric

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.clonewhatsapp.core.common.security.BiometricAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Estado de la pantalla de configuracion biometrica.
 *
 * @param isBiometricEnabled Indica si el bloqueo biometrico esta activado
 * @param isBiometricAvailable Indica si el dispositivo soporta biometria
 * @param availabilityMessage Mensaje descriptivo sobre la disponibilidad biometrica
 * @param isLoading Indica si se esta cargando la configuracion
 * @param errorMessage Mensaje de error para mostrar al usuario
 */
data class BiometricSettingsState(
    val isBiometricEnabled: Boolean = false,
    val isBiometricAvailable: Boolean = false,
    val availabilityMessage: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Eventos de la pantalla de configuracion biometrica (MVI).
 */
sealed interface BiometricSettingsEvent {
    /** Toggle para activar/desactivar el bloqueo biometrico */
    data class OnToggleBiometric(val enabled: Boolean) : BiometricSettingsEvent

    /** Descartar mensaje de error */
    data object OnErrorDismiss : BiometricSettingsEvent

    /** Verificar disponibilidad biometrica (llamado al iniciar) */
    data object OnCheckAvailability : BiometricSettingsEvent
}

/**
 * ViewModel MVI para la configuracion de autenticacion biometrica.
 *
 * Gestiona la preferencia de bloqueo biometrico almacenandola de forma
 * segura en EncryptedSharedPreferences. Verifica la disponibilidad del
 * hardware biometrico y permite al usuario activar/desactivar el bloqueo.
 *
 * @param biometricAuthManager Gestor de autenticacion biometrica
 * @param context Contexto de aplicacion para acceso a SharedPreferences cifradas
 */
@HiltViewModel
class BiometricSettingsViewModel @Inject constructor(
    private val biometricAuthManager: BiometricAuthManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "BiometricSettingsVM"
        private const val PREFS_NAME = "biometric_settings_prefs"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_lock_enabled"
    }

    private val _state = MutableStateFlow(BiometricSettingsState())
    val state: StateFlow<BiometricSettingsState> = _state.asStateFlow()

    /**
     * SharedPreferences cifradas para almacenar la preferencia de bloqueo biometrico.
     * Se inicializa de forma perezosa para manejar posibles errores de cifrado.
     */
    private val securePrefs: SharedPreferences? by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error al crear EncryptedSharedPreferences: %s", e.message)
            null
        }
    }

    init {
        loadBiometricPreference()
    }

    /**
     * Procesa los eventos de la interfaz de usuario.
     *
     * @param event Evento de la UI a procesar
     */
    fun onEvent(event: BiometricSettingsEvent) {
        when (event) {
            is BiometricSettingsEvent.OnToggleBiometric -> {
                toggleBiometric(event.enabled)
            }
            is BiometricSettingsEvent.OnErrorDismiss -> {
                _state.update { it.copy(errorMessage = null) }
            }
            is BiometricSettingsEvent.OnCheckAvailability -> {
                // La disponibilidad se verifica desde la Activity que tiene acceso
                // al FragmentActivity necesario para BiometricManager
            }
        }
    }

    /**
     * Actualiza el estado de disponibilidad biometrica.
     *
     * Debe ser llamado desde la Activity/Fragment con acceso al FragmentActivity,
     * ya que BiometricManager requiere una Activity para verificar el hardware.
     *
     * @param availability Estado de disponibilidad del hardware biometrico
     */
    fun updateBiometricAvailability(availability: BiometricAuthManager.BiometricAvailability) {
        val (isAvailable, message) = when (availability) {
            BiometricAuthManager.BiometricAvailability.AVAILABLE ->
                true to "Autenticacion biometrica disponible"

            BiometricAuthManager.BiometricAvailability.NO_HARDWARE ->
                false to "Este dispositivo no tiene hardware biometrico"

            BiometricAuthManager.BiometricAvailability.HARDWARE_UNAVAILABLE ->
                false to "El hardware biometrico no esta disponible actualmente"

            BiometricAuthManager.BiometricAvailability.NONE_ENROLLED ->
                false to "No hay huellas o rostros registrados. Configuralos en Ajustes del sistema"

            BiometricAuthManager.BiometricAvailability.UNKNOWN ->
                false to "No se pudo verificar la disponibilidad biometrica"
        }

        _state.update {
            it.copy(
                isBiometricAvailable = isAvailable,
                availabilityMessage = message
            )
        }

        // Si no hay biometria disponible, desactivar el bloqueo
        if (!isAvailable && _state.value.isBiometricEnabled) {
            toggleBiometric(false)
        }
    }

    /**
     * Carga la preferencia de bloqueo biometrico desde el almacenamiento seguro.
     */
    private fun loadBiometricPreference() {
        viewModelScope.launch {
            try {
                val isEnabled = securePrefs?.getBoolean(KEY_BIOMETRIC_ENABLED, false) ?: false
                _state.update {
                    it.copy(
                        isBiometricEnabled = isEnabled,
                        isLoading = false
                    )
                }
                Timber.tag(TAG).d("Preferencia biometrica cargada: habilitado=%s", isEnabled)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error al cargar preferencia biometrica: %s", e.message)
                _state.update {
                    it.copy(
                        isBiometricEnabled = false,
                        isLoading = false,
                        errorMessage = "Error al cargar configuracion de seguridad"
                    )
                }
            }
        }
    }

    /**
     * Activa o desactiva el bloqueo biometrico.
     *
     * Almacena la preferencia en EncryptedSharedPreferences de forma segura.
     *
     * @param enabled true para activar el bloqueo, false para desactivarlo
     */
    private fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            try {
                securePrefs?.edit()
                    ?.putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
                    ?.apply()

                _state.update { it.copy(isBiometricEnabled = enabled) }

                Timber.tag(TAG).d(
                    "Bloqueo biometrico %s",
                    if (enabled) "activado" else "desactivado"
                )
            } catch (e: Exception) {
                Timber.tag(TAG).e(
                    e, "Error al guardar preferencia biometrica: %s", e.message
                )
                _state.update {
                    it.copy(
                        errorMessage = "Error al guardar configuracion de seguridad"
                    )
                }
            }
        }
    }

    /**
     * Verifica si el bloqueo biometrico esta habilitado.
     *
     * Metodo de conveniencia para consultar el estado sin suscribirse al StateFlow.
     * Util para verificacion rapida al reanudar la app.
     *
     * @return true si el bloqueo biometrico esta activado
     */
    fun isBiometricLockEnabled(): Boolean {
        return try {
            securePrefs?.getBoolean(KEY_BIOMETRIC_ENABLED, false) ?: false
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error al verificar estado biometrico: %s", e.message)
            false
        }
    }
}
