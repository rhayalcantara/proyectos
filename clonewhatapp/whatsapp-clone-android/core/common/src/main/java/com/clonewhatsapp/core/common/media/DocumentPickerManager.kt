package com.clonewhatsapp.core.common.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Informacion de un documento seleccionado desde el dispositivo.
 *
 * @param uri URI del documento en el ContentProvider
 * @param name Nombre del archivo con extension
 * @param mimeType Tipo MIME del archivo (puede ser null si no se puede determinar)
 * @param sizeBytes Tamano del archivo en bytes
 */
data class PickedDocument(
    val uri: Uri,
    val name: String,
    val mimeType: String?,
    val sizeBytes: Long
)

/**
 * Crea un launcher para seleccionar documentos del dispositivo usando
 * la API [ActivityResultContracts.OpenDocument].
 *
 * Acepta todos los tipos de archivo (*\/*) y extrae la informacion del
 * documento seleccionado (nombre, tamano, tipo MIME) usando el ContentResolver.
 *
 * @param onDocumentPicked callback invocado con la informacion del documento seleccionado
 * @return launcher que se puede invocar con un array de tipos MIME permitidos
 */
@Composable
fun rememberDocumentPickerLauncher(
    onDocumentPicked: (PickedDocument) -> Unit
): ManagedActivityResultLauncher<Array<String>, Uri?> {
    val context = androidx.compose.ui.platform.LocalContext.current

    return rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            context.getDocumentInfo(selectedUri)?.let { document ->
                onDocumentPicked(document)
            }
        }
    }
}

/**
 * Obtiene la informacion de un documento a partir de su URI usando el ContentResolver.
 *
 * Consulta las columnas [OpenableColumns.DISPLAY_NAME] y [OpenableColumns.SIZE]
 * para extraer el nombre y tamano del archivo.
 *
 * @param uri URI del documento a consultar
 * @return [PickedDocument] con la informacion del archivo, o null si no se puede leer
 */
fun Context.getDocumentInfo(uri: Uri): PickedDocument? {
    var name: String? = null
    var size: Long = 0L

    if (uri.scheme == "content") {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }

                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
    }

    // Fallback: usar el ultimo segmento del URI como nombre
    if (name == null) {
        name = uri.lastPathSegment ?: return null
    }

    val mimeType = contentResolver.getType(uri)

    return PickedDocument(
        uri = uri,
        name = name!!,
        mimeType = mimeType,
        sizeBytes = size
    )
}

/**
 * Obtiene la extension del archivo a partir de su nombre.
 *
 * @return Extension en minusculas sin el punto, o cadena vacia si no tiene extension
 */
fun PickedDocument.getExtension(): String {
    val dotIndex = name.lastIndexOf('.')
    return if (dotIndex >= 0 && dotIndex < name.length - 1) {
        name.substring(dotIndex + 1).lowercase()
    } else {
        ""
    }
}

/**
 * Formatea el tamano del archivo en una cadena legible.
 *
 * Ejemplos: "1.5 MB", "256 KB", "3.2 GB"
 *
 * @return Cadena con el tamano formateado
 */
fun PickedDocument.formattedSize(): String {
    return formatFileSize(sizeBytes)
}

/**
 * Formatea un tamano en bytes a una cadena legible.
 *
 * @param bytes Tamano en bytes
 * @return Cadena formateada con la unidad apropiada (B, KB, MB, GB)
 */
fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"

    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    return if (size == size.toLong().toDouble()) {
        "${size.toLong()} ${units[unitIndex]}"
    } else {
        "%.1f ${units[unitIndex]}".format(size)
    }
}
