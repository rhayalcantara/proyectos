package com.clonewhatsapp.core.common.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Informacion de un permiso de la aplicacion.
 *
 * @param permission Cadena del permiso Android (ej. Manifest.permission.CAMERA)
 * @param description Descripcion del proposito del permiso en espanol
 * @param isRequired Indica si el permiso es obligatorio para el funcionamiento basico
 * @param minSdkVersion Version minima de SDK donde aplica este permiso (0 = todas)
 * @param maxSdkVersion Version maxima de SDK donde aplica este permiso (Int.MAX_VALUE = todas)
 */
data class PermissionInfo(
    val permission: String,
    val description: String,
    val isRequired: Boolean,
    val minSdkVersion: Int = 0,
    val maxSdkVersion: Int = Int.MAX_VALUE
) {
    /**
     * Indica si este permiso aplica para la version de SDK actual del dispositivo.
     */
    fun isApplicable(): Boolean {
        return Build.VERSION.SDK_INT in minSdkVersion..maxSdkVersion
    }
}

/**
 * Estado de un permiso verificado en tiempo de ejecucion.
 *
 * @param info Informacion del permiso
 * @param isGranted true si el permiso esta concedido
 */
data class PermissionStatus(
    val info: PermissionInfo,
    val isGranted: Boolean
)

/**
 * Auditor de permisos de la aplicacion.
 *
 * Proporciona metodos para documentar, auditar y verificar todos los permisos
 * utilizados por la aplicacion. Genera reportes de auditoria en builds de debug
 * para facilitar la revision de seguridad.
 *
 * @param context Contexto de la aplicacion inyectado por Hilt
 */
@Singleton
class PermissionAuditor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "PermissionAuditor"
    }

    /**
     * Retorna la lista de permisos requeridos por la aplicacion.
     *
     * Estos permisos son necesarios para el funcionamiento basico de la app
     * (red, notificaciones, servicios en primer plano).
     *
     * @return Lista de [PermissionInfo] con los permisos obligatorios
     */
    fun getRequiredPermissions(): List<PermissionInfo> {
        return listOf(
            PermissionInfo(
                permission = Manifest.permission.INTERNET,
                description = "Conexion a internet para enviar y recibir mensajes, " +
                    "realizar llamadas y sincronizar datos con el servidor",
                isRequired = true
            ),
            PermissionInfo(
                permission = Manifest.permission.ACCESS_NETWORK_STATE,
                description = "Monitorear el estado de la red para mostrar indicador " +
                    "de conexion y manejar reconexiones automaticas",
                isRequired = true
            ),
            PermissionInfo(
                permission = "android.permission.POST_NOTIFICATIONS",
                description = "Mostrar notificaciones de mensajes nuevos, llamadas entrantes " +
                    "y actualizaciones de estado",
                isRequired = true,
                minSdkVersion = 33
            ),
            PermissionInfo(
                permission = Manifest.permission.VIBRATE,
                description = "Vibracion para notificaciones de mensajes y llamadas entrantes",
                isRequired = true
            ),
            PermissionInfo(
                permission = Manifest.permission.FOREGROUND_SERVICE,
                description = "Ejecutar servicios en primer plano para llamadas de voz/video " +
                    "activas y sincronizacion de mensajes",
                isRequired = true
            ),
            PermissionInfo(
                permission = "android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK",
                description = "Servicio en primer plano para reproduccion de audio " +
                    "en notas de voz y multimedia",
                isRequired = true
            ),
            PermissionInfo(
                permission = "android.permission.FOREGROUND_SERVICE_PHONE_CALL",
                description = "Servicio en primer plano para mantener llamadas de voz " +
                    "y video activas en segundo plano",
                isRequired = true
            ),
            PermissionInfo(
                permission = "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
                description = "Servicio en primer plano para sincronizacion de mensajes " +
                    "y archivos multimedia en segundo plano",
                isRequired = true
            ),
            PermissionInfo(
                permission = Manifest.permission.WAKE_LOCK,
                description = "Mantener el dispositivo activo durante llamadas de voz/video " +
                    "para evitar que se suspenda la conexion",
                isRequired = true
            )
        )
    }

    /**
     * Retorna la lista de permisos opcionales de la aplicacion.
     *
     * Estos permisos solo se necesitan para funcionalidades especificas como
     * camara, microfono y almacenamiento. Se solicitan en tiempo de ejecucion
     * cuando el usuario accede a la funcionalidad correspondiente.
     *
     * @return Lista de [PermissionInfo] con los permisos opcionales
     */
    fun getOptionalPermissions(): List<PermissionInfo> {
        return listOf(
            PermissionInfo(
                permission = Manifest.permission.CAMERA,
                description = "Camara para tomar fotos de perfil, enviar fotos/videos en chats " +
                    "y activar video en llamadas",
                isRequired = false
            ),
            PermissionInfo(
                permission = Manifest.permission.RECORD_AUDIO,
                description = "Microfono para grabar notas de voz, realizar llamadas de voz " +
                    "y activar audio en videollamadas",
                isRequired = false
            ),
            PermissionInfo(
                permission = "android.permission.READ_MEDIA_IMAGES",
                description = "Acceso a galeria de imagenes para compartir fotos en chats " +
                    "y establecer foto de perfil",
                isRequired = false,
                minSdkVersion = 33
            ),
            PermissionInfo(
                permission = "android.permission.READ_MEDIA_VIDEO",
                description = "Acceso a galeria de videos para compartir videos en chats " +
                    "y estados",
                isRequired = false,
                minSdkVersion = 33
            ),
            PermissionInfo(
                permission = "android.permission.READ_MEDIA_AUDIO",
                description = "Acceso a archivos de audio para compartir musica y audios " +
                    "en conversaciones",
                isRequired = false,
                minSdkVersion = 33
            ),
            PermissionInfo(
                permission = Manifest.permission.READ_EXTERNAL_STORAGE,
                description = "Acceso al almacenamiento para compartir fotos, videos y " +
                    "documentos en versiones anteriores a Android 13",
                isRequired = false,
                maxSdkVersion = 32
            ),
            PermissionInfo(
                permission = Manifest.permission.MODIFY_AUDIO_SETTINGS,
                description = "Modificar configuracion de audio para cambiar entre altavoz, " +
                    "auriculares y bluetooth durante llamadas",
                isRequired = false
            ),
            PermissionInfo(
                permission = Manifest.permission.BLUETOOTH_CONNECT,
                description = "Conexion Bluetooth para usar auriculares inalambricos " +
                    "durante llamadas de voz y video",
                isRequired = false
            )
        )
    }

    /**
     * Verifica el estado de todos los permisos de la aplicacion.
     *
     * Evalua cada permiso (requerido y opcional) y retorna su estado actual
     * (concedido o denegado). Solo incluye permisos aplicables al SDK actual.
     *
     * @return Lista de [PermissionStatus] con el estado de cada permiso
     */
    fun checkPermissionStatus(): List<PermissionStatus> {
        val allPermissions = getRequiredPermissions() + getOptionalPermissions()

        return allPermissions
            .filter { it.isApplicable() }
            .map { permissionInfo ->
                val isGranted = try {
                    ContextCompat.checkSelfPermission(
                        context,
                        permissionInfo.permission
                    ) == PackageManager.PERMISSION_GRANTED
                } catch (e: Exception) {
                    // Algunos permisos normales estan siempre concedidos
                    // y checkSelfPermission puede fallar con permisos no estandar
                    false
                }

                PermissionStatus(
                    info = permissionInfo,
                    isGranted = isGranted
                )
            }
    }

    /**
     * Genera y registra un reporte de auditoria de permisos.
     *
     * Solo genera el reporte en builds de debug para no afectar rendimiento
     * ni exponer informacion de seguridad en produccion.
     *
     * @param isDebug Indica si la app esta en modo debug (tipicamente BuildConfig.DEBUG)
     */
    fun logAuditReport(isDebug: Boolean) {
        if (!isDebug) return

        val statuses = checkPermissionStatus()

        val report = buildString {
            appendLine("=== REPORTE DE AUDITORIA DE PERMISOS ===")
            appendLine("Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("SDK: ${Build.VERSION.SDK_INT} (Android ${Build.VERSION.RELEASE})")
            appendLine()

            // Permisos requeridos
            appendLine("--- PERMISOS REQUERIDOS ---")
            statuses.filter { it.info.isRequired }.forEach { status ->
                val estado = if (status.isGranted) "CONCEDIDO" else "DENEGADO"
                appendLine("  [$estado] ${status.info.permission}")
                appendLine("    Proposito: ${status.info.description}")
            }
            appendLine()

            // Permisos opcionales
            appendLine("--- PERMISOS OPCIONALES ---")
            statuses.filter { !it.info.isRequired }.forEach { status ->
                val estado = if (status.isGranted) "CONCEDIDO" else "DENEGADO"
                appendLine("  [$estado] ${status.info.permission}")
                appendLine("    Proposito: ${status.info.description}")
            }
            appendLine()

            // Resumen
            val concedidos = statuses.count { it.isGranted }
            val total = statuses.size
            appendLine("--- RESUMEN ---")
            appendLine("  Total de permisos aplicables: $total")
            appendLine("  Concedidos: $concedidos")
            appendLine("  Denegados: ${total - concedidos}")
            appendLine("==========================================")
        }

        Timber.tag(TAG).d(report)
    }
}
