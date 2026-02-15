package com.clonewhatsapp.core.common.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor de autenticacion biometrica.
 *
 * Proporciona metodos para verificar la disponibilidad de hardware biometrico
 * (huella digital, reconocimiento facial, iris) y lanzar el flujo de autenticacion.
 *
 * Utiliza la biblioteca AndroidX Biometric para compatibilidad con multiples
 * dispositivos y versiones de Android.
 */
@Singleton
class BiometricAuthManager @Inject constructor() {

    companion object {
        private const val TAG = "BiometricAuthManager"

        /** Titulo del dialogo de autenticacion biometrica */
        private const val PROMPT_TITULO = "Verificar identidad"

        /** Subtitulo del dialogo de autenticacion biometrica */
        private const val PROMPT_SUBTITULO = "Usa tu huella o rostro para continuar"

        /** Texto del boton de cancelacion */
        private const val PROMPT_CANCELAR = "Cancelar"
    }

    /**
     * Resultado de la verificacion de disponibilidad biometrica.
     */
    enum class BiometricAvailability {
        /** Hardware biometrico disponible y configurado con al menos una credencial */
        AVAILABLE,

        /** No hay hardware biometrico en el dispositivo */
        NO_HARDWARE,

        /** Hardware presente pero no disponible actualmente */
        HARDWARE_UNAVAILABLE,

        /** Hardware disponible pero sin credenciales biometricas registradas */
        NONE_ENROLLED,

        /** Estado desconocido o error al verificar */
        UNKNOWN
    }

    /**
     * Verifica si la autenticacion biometrica esta disponible en el dispositivo.
     *
     * Comprueba hardware biometrico fuerte (huella, iris) y debil (rostro).
     * Si no hay biometria disponible, verifica si hay credenciales de dispositivo
     * (PIN, patron, contrasena) como alternativa.
     *
     * @param activity Activity necesaria para acceder al BiometricManager
     * @return [BiometricAvailability] indicando el estado del hardware biometrico
     */
    fun canAuthenticate(activity: FragmentActivity): BiometricAvailability {
        val biometricManager = BiometricManager.from(activity)

        // Intentar primero con biometria fuerte
        val strongResult = biometricManager.canAuthenticate(BIOMETRIC_STRONG)
        if (strongResult == BiometricManager.BIOMETRIC_SUCCESS) {
            Timber.tag(TAG).d("Autenticacion biometrica fuerte disponible")
            return BiometricAvailability.AVAILABLE
        }

        // Intentar con biometria debil (incluye reconocimiento facial en algunos dispositivos)
        val weakResult = biometricManager.canAuthenticate(BIOMETRIC_WEAK)
        if (weakResult == BiometricManager.BIOMETRIC_SUCCESS) {
            Timber.tag(TAG).d("Autenticacion biometrica debil disponible")
            return BiometricAvailability.AVAILABLE
        }

        // Intentar con credencial de dispositivo como alternativa
        val deviceResult = biometricManager.canAuthenticate(DEVICE_CREDENTIAL)
        if (deviceResult == BiometricManager.BIOMETRIC_SUCCESS) {
            Timber.tag(TAG).d("Credencial de dispositivo disponible como alternativa")
            return BiometricAvailability.AVAILABLE
        }

        // Mapear el resultado a nuestro enum
        return when (strongResult) {
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Timber.tag(TAG).w("No hay hardware biometrico disponible")
                BiometricAvailability.NO_HARDWARE
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Timber.tag(TAG).w("Hardware biometrico no disponible actualmente")
                BiometricAvailability.HARDWARE_UNAVAILABLE
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Timber.tag(TAG).w("No hay credenciales biometricas registradas")
                BiometricAvailability.NONE_ENROLLED
            }
            else -> {
                Timber.tag(TAG).w("Estado biometrico desconocido: %d", strongResult)
                BiometricAvailability.UNKNOWN
            }
        }
    }

    /**
     * Lanza el flujo de autenticacion biometrica.
     *
     * Muestra el dialogo del sistema para autenticacion con huella digital,
     * reconocimiento facial o iris. Si la biometria no esta disponible,
     * permite autenticacion con credencial del dispositivo (PIN/patron/contrasena).
     *
     * @param activity FragmentActivity necesaria para mostrar el BiometricPrompt
     * @param onSuccess Callback invocado cuando la autenticacion es exitosa
     * @param onError Callback invocado cuando la autenticacion falla o es cancelada.
     *               Recibe el codigo de error y el mensaje descriptivo.
     */
    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errorMessage: String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Timber.tag(TAG).d("Autenticacion biometrica exitosa")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Timber.tag(TAG).w(
                    "Error de autenticacion biometrica: codigo=%d, mensaje=%s",
                    errorCode, errString
                )
                onError(errorCode, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Timber.tag(TAG).d("Intento de autenticacion biometrica fallido (no reconocido)")
                // No llamamos onError aqui porque el usuario puede reintentar
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        // Determinar que tipo de autenticacion usar
        val promptInfo = buildPromptInfo(activity)

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error al lanzar autenticacion biometrica: %s", e.message)
            onError(-1, e.message ?: "Error desconocido al iniciar autenticacion")
        }
    }

    /**
     * Construye la informacion del dialogo de autenticacion biometrica.
     *
     * Si hay biometria disponible, muestra el boton de cancelar.
     * Si no hay biometria, permite credencial de dispositivo como alternativa.
     *
     * @param activity Activity para verificar disponibilidad biometrica
     * @return [BiometricPrompt.PromptInfo] configurado
     */
    private fun buildPromptInfo(activity: FragmentActivity): BiometricPrompt.PromptInfo {
        val biometricManager = BiometricManager.from(activity)
        val hasBiometric = biometricManager.canAuthenticate(
            BIOMETRIC_STRONG or BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS

        return if (hasBiometric) {
            // Si hay biometria, permitir tambien credencial de dispositivo
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(PROMPT_TITULO)
                .setSubtitle(PROMPT_SUBTITULO)
                .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                .build()
        } else {
            // Si no hay biometria, solo credencial de dispositivo
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(PROMPT_TITULO)
                .setSubtitle(PROMPT_SUBTITULO)
                .setAllowedAuthenticators(DEVICE_CREDENTIAL)
                .build()
        }
    }
}
