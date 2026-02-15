package com.clonewhatsapp.core.common.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utilidad para verificar que EncryptedSharedPreferences funciona correctamente.
 *
 * Verifica que el MasterKey esta disponible y que las operaciones de cifrado
 * funcionan sin errores. Si la verificacion falla, intenta recrear el almacenamiento
 * seguro eliminando los datos corruptos.
 *
 * @param context Contexto de la aplicacion inyectado por Hilt
 */
@Singleton
class SecureStorageVerifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "SecureStorageVerifier"
        private const val TEST_PREFS_NAME = "secure_storage_verification"
        private const val TEST_KEY = "verification_test_key"
        private const val TEST_VALUE = "verification_test_value"
        private const val SECURE_AUTH_PREFS_NAME = "secure_auth_prefs"
    }

    /**
     * Resultado de la verificacion del almacenamiento seguro.
     *
     * @param isSecure Indica si el almacenamiento cifrado funciona correctamente
     * @param masterKeyAvailable Indica si el MasterKey esta disponible
     * @param encryptionWorking Indica si las operaciones de cifrado/descifrado funcionan
     * @param wasRecreated Indica si se tuvo que recrear el almacenamiento por fallo
     * @param errorMessage Mensaje de error si algo fallo, null si todo esta correcto
     */
    data class VerificationResult(
        val isSecure: Boolean,
        val masterKeyAvailable: Boolean,
        val encryptionWorking: Boolean,
        val wasRecreated: Boolean = false,
        val errorMessage: String? = null
    )

    /**
     * Verifica que el almacenamiento seguro funciona correctamente.
     *
     * Realiza las siguientes comprobaciones:
     * 1. Verifica que el MasterKey se puede crear/obtener
     * 2. Verifica que EncryptedSharedPreferences puede escribir y leer datos
     * 3. Si falla, intenta recrear el almacenamiento como mecanismo de recuperacion
     *
     * @return [VerificationResult] con el estado de la verificacion
     */
    fun verify(): VerificationResult {
        // Paso 1: Verificar disponibilidad del MasterKey
        val masterKey = try {
            createMasterKey()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "No se pudo crear el MasterKey: %s", e.message)
            return VerificationResult(
                isSecure = false,
                masterKeyAvailable = false,
                encryptionWorking = false,
                errorMessage = "MasterKey no disponible: ${e.message}"
            )
        }

        // Paso 2: Verificar operaciones de cifrado con SharedPreferences de prueba
        try {
            val testPrefs = createEncryptedPrefs(TEST_PREFS_NAME, masterKey)
            return verifyEncryptionOperations(testPrefs)
        } catch (e: Exception) {
            Timber.tag(TAG).w(
                e,
                "EncryptedSharedPreferences fallo, intentando recuperar: %s",
                e.message
            )
            // Paso 3: Intentar mecanismo de recuperacion
            return attemptRecovery(masterKey, e.message)
        }
    }

    /**
     * Verifica que las operaciones de escritura y lectura cifrada funcionan.
     *
     * Escribe un valor de prueba, lo lee y compara para confirmar integridad.
     *
     * @param prefs SharedPreferences cifradas para verificar
     * @return [VerificationResult] indicando el estado de las operaciones
     */
    private fun verifyEncryptionOperations(prefs: SharedPreferences): VerificationResult {
        return try {
            // Escribir valor de prueba
            prefs.edit()
                .putString(TEST_KEY, TEST_VALUE)
                .commit()

            // Leer y verificar
            val readValue = prefs.getString(TEST_KEY, null)
            val isWorking = readValue == TEST_VALUE

            if (!isWorking) {
                Timber.tag(TAG).w(
                    "Lectura/escritura cifrada inconsistente: esperado=%s, obtenido=%s",
                    TEST_VALUE, readValue
                )
            }

            // Limpiar valor de prueba
            prefs.edit().remove(TEST_KEY).commit()

            Timber.tag(TAG).d("Verificacion del almacenamiento seguro exitosa")

            VerificationResult(
                isSecure = isWorking,
                masterKeyAvailable = true,
                encryptionWorking = isWorking,
                errorMessage = if (!isWorking) "Inconsistencia en lectura/escritura cifrada" else null
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error en operaciones de cifrado: %s", e.message)
            VerificationResult(
                isSecure = false,
                masterKeyAvailable = true,
                encryptionWorking = false,
                errorMessage = "Error en operaciones de cifrado: ${e.message}"
            )
        }
    }

    /**
     * Intenta recuperar el almacenamiento seguro cuando la verificacion falla.
     *
     * Elimina los archivos de SharedPreferences corruptos y los recrea.
     * ADVERTENCIA: Esto eliminara los datos almacenados previamente (tokens, sesion).
     *
     * @param masterKey MasterKey para crear nuevas preferencias cifradas
     * @param originalError Mensaje del error original que provoco la recuperacion
     * @return [VerificationResult] indicando si la recuperacion fue exitosa
     */
    private fun attemptRecovery(masterKey: MasterKey, originalError: String?): VerificationResult {
        return try {
            Timber.tag(TAG).w(
                "Nivel de seguridad degradado: eliminando almacenamiento corrupto y recreando"
            )

            // Eliminar archivos de preferencias corruptos
            clearPreferencesFile(TEST_PREFS_NAME)
            clearPreferencesFile(SECURE_AUTH_PREFS_NAME)

            // Intentar recrear
            val newPrefs = createEncryptedPrefs(TEST_PREFS_NAME, masterKey)
            val result = verifyEncryptionOperations(newPrefs)

            if (result.isSecure) {
                Timber.tag(TAG).w(
                    "Recuperacion exitosa: almacenamiento seguro recreado. " +
                        "Los datos de sesion anteriores fueron eliminados."
                )
            }

            result.copy(wasRecreated = true)
        } catch (e: Exception) {
            Timber.tag(TAG).e(
                e,
                "Recuperacion fallida. El almacenamiento seguro no esta disponible: %s",
                e.message
            )
            VerificationResult(
                isSecure = false,
                masterKeyAvailable = true,
                encryptionWorking = false,
                wasRecreated = true,
                errorMessage = "Recuperacion fallida. Error original: $originalError, " +
                    "Error de recuperacion: ${e.message}"
            )
        }
    }

    /**
     * Crea un MasterKey usando AES256_GCM para cifrado.
     *
     * @return [MasterKey] valido para operaciones de cifrado
     * @throws Exception si el KeyStore no esta disponible o es incompatible
     */
    private fun createMasterKey(): MasterKey {
        return MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    /**
     * Crea EncryptedSharedPreferences con los esquemas de cifrado estandar.
     *
     * @param name Nombre del archivo de preferencias
     * @param masterKey Clave maestra para cifrado
     * @return [SharedPreferences] cifradas
     */
    private fun createEncryptedPrefs(name: String, masterKey: MasterKey): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Elimina el archivo de SharedPreferences del almacenamiento interno.
     *
     * @param prefsName Nombre del archivo de preferencias a eliminar
     */
    private fun clearPreferencesFile(prefsName: String) {
        try {
            val prefsFile = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefsFile.edit().clear().commit()

            // Tambien intentar eliminar el archivo fisico
            val prefsDir = context.filesDir.parentFile?.resolve("shared_prefs")
            val file = prefsDir?.resolve("$prefsName.xml")
            if (file?.exists() == true) {
                file.delete()
                Timber.tag(TAG).d("Archivo de preferencias eliminado: %s", prefsName)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error al eliminar archivo de preferencias: %s", prefsName)
        }
    }
}
