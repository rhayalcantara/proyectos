package com.clonewhatsapp.core.common.media

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Resultado de la seleccion de media — puede ser imagen o video.
 */
data class PickedMedia(
    val uri: Uri,
    val mimeType: String?,
    val isVideo: Boolean
)

/**
 * Crea un launcher para seleccionar una sola imagen o video desde la galeria
 * usando la API PhotoPicker de Android (SDK 33+) con fallback automatico.
 *
 * @param onImagePicked callback invocado cuando se selecciona una imagen
 * @param onVideoPicked callback invocado cuando se selecciona un video
 * @return launcher que se puede invocar con un [PickVisualMediaRequest]
 */
@Composable
fun rememberPhotoPickerLauncher(
    onImagePicked: (Uri) -> Unit,
    onVideoPicked: (Uri) -> Unit
): ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            // El tipo se determinara en el sitio de llamada usando isVideoUri()
            // Por defecto asumimos imagen; el caller puede verificar con Context.isVideoUri()
            onImagePicked(selectedUri)
        }
    }
}

/**
 * Crea un launcher para seleccionar una sola imagen o video, con deteccion
 * automatica del tipo de media. Requiere un [Context] para resolver el mime type.
 *
 * @param context contexto de la aplicacion para resolver el tipo MIME
 * @param onImagePicked callback invocado cuando se selecciona una imagen
 * @param onVideoPicked callback invocado cuando se selecciona un video
 * @return launcher que se puede invocar con un [PickVisualMediaRequest]
 */
@Composable
fun rememberSmartPhotoPickerLauncher(
    context: Context,
    onImagePicked: (Uri) -> Unit,
    onVideoPicked: (Uri) -> Unit
): ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            if (context.isVideoUri(selectedUri)) {
                onVideoPicked(selectedUri)
            } else {
                onImagePicked(selectedUri)
            }
        }
    }
}

/**
 * Crea un launcher para seleccionar multiples imagenes/videos desde la galeria.
 *
 * @param maxItems cantidad maxima de elementos que se pueden seleccionar (por defecto 5)
 * @param onMediaPicked callback invocado con la lista de URIs seleccionados
 * @return launcher que se puede invocar con un [PickVisualMediaRequest]
 */
@Composable
fun rememberMultiplePhotoPickerLauncher(
    maxItems: Int = 5,
    onMediaPicked: (List<Uri>) -> Unit
): ManagedActivityResultLauncher<PickVisualMediaRequest, List<@JvmSuppressWildcards Uri>> {
    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems)
    ) { uris ->
        if (uris.isNotEmpty()) {
            onMediaPicked(uris)
        }
    }
}

/**
 * Determina si el URI apunta a un archivo de video basandose en su tipo MIME.
 */
fun Context.isVideoUri(uri: Uri): Boolean {
    val type = contentResolver.getType(uri)
    return type?.startsWith("video/") == true
}

/**
 * Obtiene el tipo MIME de un URI usando el ContentResolver.
 */
fun Context.getMimeType(uri: Uri): String? {
    return contentResolver.getType(uri)
}

/**
 * Convierte una lista de URIs a objetos [PickedMedia] resolviendo el tipo MIME.
 */
fun Context.toPickedMediaList(uris: List<Uri>): List<PickedMedia> {
    return uris.map { uri ->
        val mimeType = getMimeType(uri)
        PickedMedia(
            uri = uri,
            mimeType = mimeType,
            isVideo = mimeType?.startsWith("video/") == true
        )
    }
}
