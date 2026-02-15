package com.clonewhatsapp.core.network.push

import android.content.Context
import android.content.SharedPreferences
import com.clonewhatsapp.core.network.api.EliminarTokenRequest
import com.clonewhatsapp.core.network.api.PushApi
import com.clonewhatsapp.core.network.api.PushKeysDto
import com.clonewhatsapp.core.network.api.RegistrarTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor de tokens FCM (Firebase Cloud Messaging).
 *
 * Se encarga de:
 * - Obtener y registrar el token FCM actual con el backend
 * - Detectar cambios de token y actualizar el registro
 * - Eliminar el token del backend al cerrar sesion
 * - Almacenar el token localmente para detectar cambios
 *
 * Debe invocarse en:
 * - Inicio de la aplicacion
 * - Login del usuario
 * - Refresco de token (onNewToken)
 */
@Singleton
class FcmTokenManager @Inject constructor(
    private val pushApi: PushApi,
    @ApplicationContext private val contexto: Context
) {

    private val preferencias: SharedPreferences by lazy {
        contexto.getSharedPreferences(PREFS_FCM, Context.MODE_PRIVATE)
    }

    /**
     * Obtiene el token FCM actual y lo registra en el backend.
     * Si el token no ha cambiado respecto al almacenado, no hace nada.
     *
     * @param forzar Si es true, registra el token aunque no haya cambiado
     */
    suspend fun registrarToken(forzar: Boolean = false) {
        try {
            val tokenActual = FirebaseMessaging.getInstance().token.await()
            val tokenAlmacenado = obtenerTokenLocal()

            if (!forzar && tokenActual == tokenAlmacenado) {
                Timber.d("Token FCM sin cambios, omitiendo registro")
                return
            }

            Timber.d("Registrando token FCM en el backend")

            // Adaptar al formato del backend (endpoint + keys)
            // Para FCM nativo, las keys son placeholders
            val request = RegistrarTokenRequest(
                endpoint = tokenActual,
                keys = PushKeysDto(
                    p256dh = "fcm-android",
                    auth = "fcm-android"
                )
            )

            val respuesta = pushApi.registrarToken(request)

            if (respuesta.isSuccessful) {
                guardarTokenLocal(tokenActual)
                Timber.i("Token FCM registrado exitosamente")
            } else {
                Timber.e("Error al registrar token FCM: ${respuesta.code()} - ${respuesta.message()}")
            }
        } catch (excepcion: Exception) {
            Timber.e(excepcion, "Error al registrar token FCM")
        }
    }

    /**
     * Registra un token FCM especifico (usado desde onNewToken).
     *
     * @param token Token FCM nuevo proporcionado por Firebase
     */
    suspend fun registrarTokenEspecifico(token: String) {
        try {
            Timber.d("Registrando nuevo token FCM: ${token.take(10)}...")

            val request = RegistrarTokenRequest(
                endpoint = token,
                keys = PushKeysDto(
                    p256dh = "fcm-android",
                    auth = "fcm-android"
                )
            )

            val respuesta = pushApi.registrarToken(request)

            if (respuesta.isSuccessful) {
                guardarTokenLocal(token)
                Timber.i("Nuevo token FCM registrado exitosamente")
            } else {
                Timber.e("Error al registrar nuevo token FCM: ${respuesta.code()}")
            }
        } catch (excepcion: Exception) {
            Timber.e(excepcion, "Error al registrar nuevo token FCM")
        }
    }

    /**
     * Elimina el token FCM del backend (se usa al cerrar sesion).
     * Tambien limpia el token almacenado localmente.
     */
    suspend fun eliminarToken() {
        try {
            val tokenAlmacenado = obtenerTokenLocal() ?: return

            Timber.d("Eliminando token FCM del backend")

            val request = EliminarTokenRequest(endpoint = tokenAlmacenado)
            val respuesta = pushApi.eliminarToken(request)

            if (respuesta.isSuccessful) {
                limpiarTokenLocal()
                Timber.i("Token FCM eliminado exitosamente")
            } else {
                Timber.e("Error al eliminar token FCM: ${respuesta.code()}")
            }
        } catch (excepcion: Exception) {
            Timber.e(excepcion, "Error al eliminar token FCM")
        }
    }

    // -----------------------------------------------------------------------
    // Almacenamiento local del token
    // -----------------------------------------------------------------------

    /**
     * Guarda el token FCM en preferencias locales.
     */
    private fun guardarTokenLocal(token: String) {
        preferencias.edit()
            .putString(KEY_TOKEN_FCM, token)
            .apply()
    }

    /**
     * Obtiene el token FCM almacenado localmente.
     * @return Token almacenado o null si no existe
     */
    private fun obtenerTokenLocal(): String? {
        return preferencias.getString(KEY_TOKEN_FCM, null)
    }

    /**
     * Limpia el token FCM almacenado localmente.
     */
    private fun limpiarTokenLocal() {
        preferencias.edit()
            .remove(KEY_TOKEN_FCM)
            .apply()
    }

    companion object {
        private const val PREFS_FCM = "fcm_prefs"
        private const val KEY_TOKEN_FCM = "token_fcm"
    }
}
