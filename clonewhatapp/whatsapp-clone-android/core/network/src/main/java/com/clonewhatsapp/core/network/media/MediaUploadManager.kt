package com.clonewhatsapp.core.network.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.clonewhatsapp.core.common.media.ImageCompressor
import com.clonewhatsapp.core.network.api.MessagesApi
import com.clonewhatsapp.core.network.dto.MensajeDto
import com.clonewhatsapp.domain.model.EstadoMensaje
import com.clonewhatsapp.domain.model.Mensaje
import com.clonewhatsapp.domain.model.TipoMensaje
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado del progreso de una subida
 */
sealed interface UploadState {
    /** Sin actividad */
    data object Idle : UploadState

    /** Subiendo archivo — progress de 0.0 a 1.0 */
    data class Uploading(val progress: Float) : UploadState

    /** Subida exitosa con el mensaje resultante */
    data class Success(val mensaje: Mensaje) : UploadState

    /** Error en la subida */
    data class Error(val message: String) : UploadState
}

/**
 * Gestiona la subida de archivos multimedia al servidor con progreso.
 * Usa un ProgressRequestBody para reportar el avance de la subida en tiempo real.
 */
@Singleton
class MediaUploadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messagesApi: MessagesApi,
    private val imageCompressor: ImageCompressor
) {

    /**
     * Sube un archivo multimedia como mensaje.
     *
     * @param chatId ID del chat destino
     * @param fileUri URI del archivo a subir
     * @param caption Texto opcional del mensaje
     * @param tipo Tipo de mensaje (IMAGEN, VIDEO, AUDIO, DOCUMENTO)
     * @param duracionSegundos Duracion en segundos (para audio/video)
     * @return Flow que emite el estado del upload (Idle -> Uploading(progress) -> Success/Error)
     */
    fun uploadMedia(
        chatId: String,
        fileUri: Uri,
        caption: String? = null,
        tipo: TipoMensaje = TipoMensaje.IMAGEN,
        duracionSegundos: Int? = null
    ): Flow<UploadState> = callbackFlow {
        trySend(UploadState.Uploading(0f))

        try {
            // Comprimir imagen si corresponde
            val processedUri = withContext(Dispatchers.IO) {
                if (tipo == TipoMensaje.IMAGEN) {
                    imageCompressor.compressIfNeeded(fileUri)
                } else {
                    fileUri
                }
            }

            // Obtener informacion del archivo
            val mimeType = context.contentResolver.getType(processedUri) ?: "application/octet-stream"
            val fileName = getFileName(processedUri) ?: "archivo_${System.currentTimeMillis()}"
            val fileBytes = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(processedUri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("No se pudo leer el archivo")
            }

            // Crear ProgressRequestBody para reportar progreso
            val progressRequestBody = ProgressRequestBody(
                contentType = mimeType.toMediaTypeOrNull(),
                content = fileBytes,
                onProgress = { progress ->
                    trySend(UploadState.Uploading(progress))
                }
            )

            // Crear la parte multipart del archivo
            val filePart = MultipartBody.Part.createFormData("file", fileName, progressRequestBody)

            // Crear las otras partes del request
            val chatIdBody = chatId.toRequestBody("text/plain".toMediaTypeOrNull())
            val contenidoBody = caption?.toRequestBody("text/plain".toMediaTypeOrNull())
            val respondidoIdBody: RequestBody? = null
            val duracionBody = duracionSegundos?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

            // Ejecutar la subida
            val response = withContext(Dispatchers.IO) {
                messagesApi.sendMessageWithFile(
                    chatId = chatIdBody,
                    contenido = contenidoBody,
                    mensajeRespondidoId = respondidoIdBody,
                    duracionSegundos = duracionBody,
                    file = filePart
                )
            }

            if (response.isSuccessful) {
                val mensajeDto = response.body()
                if (mensajeDto != null) {
                    val mensaje = mensajeDto.toDomain()
                    trySend(UploadState.Success(mensaje))
                } else {
                    trySend(UploadState.Error("Respuesta vacia del servidor"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error del servidor (${response.code()})"
                trySend(UploadState.Error(errorMsg))
            }
        } catch (e: Exception) {
            trySend(UploadState.Error(e.message ?: "Error desconocido al subir archivo"))
        }

        channel.close()
        awaitClose()
    }

    /**
     * Obtiene el nombre del archivo desde el ContentResolver.
     */
    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
        if (name == null) {
            name = uri.lastPathSegment
        }
        return name
    }

    /**
     * Mapea MensajeDto a modelo de dominio Mensaje.
     */
    private fun MensajeDto.toDomain(): Mensaje {
        return Mensaje(
            id = id,
            chatId = chatId,
            remitenteId = remitenteId,
            contenido = contenido,
            urlArchivo = urlArchivo,
            tipo = parseTipoMensaje(tipo),
            estado = parseEstadoMensaje(estado),
            fechaEnvio = parseIso8601ToMillis(fechaEnvio),
            editado = editado,
            eliminado = eliminado,
            eliminadoParaTodos = eliminadoParaTodos,
            mensajeRespondidoId = mensajeRespondidoId,
            duracionSegundos = duracionSegundos,
            nombreRemitente = remitenteNombre
        )
    }

    private fun parseTipoMensaje(tipo: String): TipoMensaje {
        return when (tipo.lowercase()) {
            "texto" -> TipoMensaje.TEXTO
            "imagen" -> TipoMensaje.IMAGEN
            "video" -> TipoMensaje.VIDEO
            "audio" -> TipoMensaje.AUDIO
            "documento" -> TipoMensaje.DOCUMENTO
            "ubicacion" -> TipoMensaje.UBICACION
            "contacto" -> TipoMensaje.CONTACTO
            "sistema" -> TipoMensaje.SISTEMA
            else -> TipoMensaje.TEXTO
        }
    }

    private fun parseEstadoMensaje(estado: String): EstadoMensaje {
        return when (estado.lowercase()) {
            "enviado" -> EstadoMensaje.ENVIADO
            "entregado" -> EstadoMensaje.ENTREGADO
            "leido" -> EstadoMensaje.LEIDO
            else -> EstadoMensaje.ENVIADO
        }
    }

    private fun parseIso8601ToMillis(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}

/**
 * RequestBody personalizado que reporta el progreso de escritura.
 * Se usa para monitorear el avance de subidas multipart con OkHttp.
 *
 * @param contentType tipo MIME del contenido
 * @param content bytes del archivo a subir
 * @param onProgress callback invocado con el progreso (0.0 a 1.0)
 */
class ProgressRequestBody(
    private val contentType: MediaType?,
    private val content: ByteArray,
    private val onProgress: (Float) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = contentType

    override fun contentLength(): Long = content.size.toLong()

    override fun writeTo(sink: BufferedSink) {
        val totalBytes = content.size.toLong()
        val bufferSize = 8192
        var uploaded = 0L

        val inputStream: InputStream = content.inputStream()
        val buffer = ByteArray(bufferSize)

        inputStream.use { stream ->
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                sink.write(buffer, 0, bytesRead)
                uploaded += bytesRead
                onProgress(uploaded.toFloat() / totalBytes)
            }
        }
    }
}
