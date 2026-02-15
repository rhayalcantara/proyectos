package com.clonewhatsapp.core.common.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Permisos requeridos por la aplicacion.
 *
 * Cada permiso incluye la cadena del permiso Android correspondiente y
 * un texto de justificacion en espanol que se muestra al usuario cuando
 * necesita comprender por que se solicita el permiso.
 *
 * Los permisos de media son adaptativos segun la version de SDK:
 * - SDK 33+ (Android 13): Usa permisos granulares READ_MEDIA_*
 * - SDK < 33: Usa el permiso legacy READ_EXTERNAL_STORAGE
 *
 * @param permission Cadena del permiso Android (e.g., Manifest.permission.CAMERA)
 * @param rationaleEs Texto explicativo en espanol de por que se necesita el permiso
 */
enum class AppPermission(val permission: String, val rationaleEs: String) {
    CAMERA(
        Manifest.permission.CAMERA,
        "Se necesita acceso a la camara para tomar fotos y videos"
    ),

    RECORD_AUDIO(
        Manifest.permission.RECORD_AUDIO,
        "Se necesita acceso al microfono para grabar audio y llamadas"
    ),

    POST_NOTIFICATIONS(
        if (Build.VERSION.SDK_INT >= 33) "android.permission.POST_NOTIFICATIONS" else "",
        "Se necesitan notificaciones para mensajes y llamadas"
    ),

    READ_MEDIA_IMAGES(
        if (Build.VERSION.SDK_INT >= 33) "android.permission.READ_MEDIA_IMAGES"
        else Manifest.permission.READ_EXTERNAL_STORAGE,
        "Se necesita acceso a fotos para compartirlas"
    ),

    READ_MEDIA_VIDEO(
        if (Build.VERSION.SDK_INT >= 33) "android.permission.READ_MEDIA_VIDEO"
        else Manifest.permission.READ_EXTERNAL_STORAGE,
        "Se necesita acceso a videos para compartirlos"
    ),

    READ_MEDIA_AUDIO(
        if (Build.VERSION.SDK_INT >= 33) "android.permission.READ_MEDIA_AUDIO"
        else Manifest.permission.READ_EXTERNAL_STORAGE,
        "Se necesita acceso a archivos de audio"
    );

    /**
     * Indica si este permiso aplica para la version de SDK actual.
     * POST_NOTIFICATIONS solo aplica para SDK 33+.
     */
    fun isApplicable(): Boolean = permission.isNotBlank()
}

/**
 * Verifica si un permiso especifico esta concedido.
 *
 * @param permission Cadena del permiso a verificar
 * @return true si el permiso esta concedido, false si no
 */
fun Context.hasPermission(permission: String): Boolean {
    if (permission.isBlank()) return true
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Verifica si un [AppPermission] esta concedido.
 *
 * @param appPermission Permiso de la app a verificar
 * @return true si el permiso esta concedido o no aplica para este SDK
 */
fun Context.hasAppPermission(appPermission: AppPermission): Boolean {
    if (!appPermission.isApplicable()) return true
    return hasPermission(appPermission.permission)
}

/**
 * Abre la pantalla de configuracion de la aplicacion en el sistema.
 * Util cuando el usuario ha denegado permanentemente un permiso y necesita
 * habilitarlo manualmente desde la configuracion.
 */
fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

/**
 * Composable que crea un launcher para solicitar un permiso individual.
 *
 * Maneja automaticamente el flujo de permisos:
 * 1. Si ya esta concedido, invoca [onGranted] inmediatamente
 * 2. Si necesita rationale, muestra un dialogo explicativo
 * 3. Si fue denegado permanentemente, muestra dialogo para ir a configuracion
 *
 * @param permission Permiso de la app a solicitar
 * @param onGranted Callback invocado cuando el permiso es concedido
 * @param onDenied Callback invocado cuando el permiso es denegado
 * @return Funcion lambda que al invocarla inicia el flujo de solicitud del permiso
 */
@Composable
fun rememberPermissionLauncher(
    permission: AppPermission,
    onGranted: () -> Unit,
    onDenied: () -> Unit
): () -> Unit {
    val context = LocalContext.current

    var showRationale by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var hasRequestedOnce by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onGranted()
        } else {
            hasRequestedOnce = true
            // Si ya se solicito antes y no muestra rationale, esta denegado permanentemente
            val activity = context as? android.app.Activity
            val shouldShowRationale = activity?.shouldShowRequestPermissionRationale(
                permission.permission
            ) ?: false

            if (!shouldShowRationale && hasRequestedOnce) {
                showSettingsDialog = true
            } else {
                onDenied()
            }
        }
    }

    // Dialogo de rationale
    if (showRationale) {
        PermissionRationaleDialog(
            permission = permission,
            onConfirm = {
                showRationale = false
                permissionLauncher.launch(permission.permission)
            },
            onDismiss = {
                showRationale = false
                onDenied()
            }
        )
    }

    // Dialogo de configuracion (permiso denegado permanentemente)
    if (showSettingsDialog) {
        SettingsRedirectDialog(
            permission = permission,
            onConfirm = {
                showSettingsDialog = false
                context.openAppSettings()
            },
            onDismiss = {
                showSettingsDialog = false
                onDenied()
            }
        )
    }

    return remember(permission) {
        {
            // Si el permiso no aplica para este SDK, considerarlo concedido
            if (!permission.isApplicable()) {
                onGranted()
                return@remember
            }

            // Verificar si ya esta concedido
            if (context.hasPermission(permission.permission)) {
                onGranted()
                return@remember
            }

            // Verificar si se debe mostrar rationale
            val activity = context as? android.app.Activity
            val shouldShowRationale = activity?.shouldShowRequestPermissionRationale(
                permission.permission
            ) ?: false

            if (shouldShowRationale) {
                showRationale = true
            } else {
                permissionLauncher.launch(permission.permission)
            }
        }
    }
}

/**
 * Composable que crea un launcher para solicitar multiples permisos a la vez.
 *
 * Filtra los permisos que no aplican para el SDK actual y los que ya estan
 * concedidos antes de solicitar los pendientes.
 *
 * @param permissions Lista de permisos de la app a solicitar
 * @param onAllGranted Callback invocado cuando todos los permisos son concedidos
 * @param onDenied Callback invocado con la lista de permisos denegados
 * @return Funcion lambda que al invocarla inicia el flujo de solicitud de permisos
 */
@Composable
fun rememberMultiplePermissionsLauncher(
    permissions: List<AppPermission>,
    onAllGranted: () -> Unit,
    onDenied: (List<AppPermission>) -> Unit
): () -> Unit {
    val context = LocalContext.current

    var showSettingsDialog by remember { mutableStateOf(false) }
    var deniedPermissions by remember { mutableStateOf<List<AppPermission>>(emptyList()) }

    // Filtrar permisos aplicables
    val applicablePermissions = remember(permissions) {
        permissions.filter { it.isApplicable() }
    }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = applicablePermissions.filter { appPermission ->
            results[appPermission.permission] != true
        }

        if (denied.isEmpty()) {
            onAllGranted()
        } else {
            // Verificar si algun permiso fue denegado permanentemente
            val activity = context as? android.app.Activity
            val permanentlyDenied = denied.any { appPermission ->
                val shouldShow = activity?.shouldShowRequestPermissionRationale(
                    appPermission.permission
                ) ?: true
                !shouldShow
            }

            if (permanentlyDenied) {
                deniedPermissions = denied
                showSettingsDialog = true
            } else {
                onDenied(denied)
            }
        }
    }

    // Dialogo de configuracion para permisos denegados permanentemente
    if (showSettingsDialog && deniedPermissions.isNotEmpty()) {
        SettingsRedirectDialog(
            permission = deniedPermissions.first(),
            onConfirm = {
                showSettingsDialog = false
                deniedPermissions = emptyList()
                context.openAppSettings()
            },
            onDismiss = {
                showSettingsDialog = false
                val denied = deniedPermissions
                deniedPermissions = emptyList()
                onDenied(denied)
            }
        )
    }

    return remember(applicablePermissions) {
        {
            // Verificar cuales ya estan concedidos
            val pendingPermissions = applicablePermissions.filter { appPermission ->
                !context.hasPermission(appPermission.permission)
            }

            if (pendingPermissions.isEmpty()) {
                onAllGranted()
            } else {
                val permissionStrings = pendingPermissions
                    .map { it.permission }
                    .toTypedArray()
                multiplePermissionLauncher.launch(permissionStrings)
            }
        }
    }
}

/**
 * Dialogo Material 3 que muestra la justificacion de un permiso.
 *
 * Se muestra antes de solicitar el permiso cuando Android indica que
 * se debe explicar al usuario por que se necesita el permiso.
 *
 * @param permission Permiso cuya justificacion se mostrara
 * @param onConfirm Callback al presionar "Permitir" — debe lanzar la solicitud del permiso
 * @param onDismiss Callback al presionar "Cancelar" o dismiss
 */
@Composable
fun PermissionRationaleDialog(
    permission: AppPermission,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Permiso necesario")
        },
        text = {
            Text(text = permission.rationaleEs)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Permitir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar")
            }
        }
    )
}

/**
 * Dialogo que indica al usuario que debe habilitar el permiso manualmente
 * desde la configuracion de la aplicacion.
 *
 * Se muestra cuando el usuario ha denegado permanentemente un permiso
 * (selecciono "No volver a preguntar").
 *
 * @param permission Permiso denegado permanentemente
 * @param onConfirm Callback al presionar "Ir a configuracion"
 * @param onDismiss Callback al presionar "Cancelar" o dismiss
 */
@Composable
private fun SettingsRedirectDialog(
    permission: AppPermission,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Permiso requerido")
        },
        text = {
            Text(
                text = "${permission.rationaleEs}. " +
                    "Este permiso fue denegado anteriormente. " +
                    "Por favor, habilitalo manualmente en la configuracion de la aplicacion."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Ir a configuracion")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancelar")
            }
        }
    )
}
