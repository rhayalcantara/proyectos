package com.clonewhatsapp.core.network.media

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado de descarga de un archivo.
 */
sealed interface FileDownloadState {
    /** Sin actividad — estado inicial */
    data object Idle : FileDownloadState

    /**
     * Descarga en progreso.
     * @param progress Progreso de la descarga de 0.0 a 1.0
     */
    data class Downloading(val progress: Float) : FileDownloadState

    /**
     * Descarga completada exitosamente.
     * @param uri URI del archivo descargado accesible via FileProvider
     */
    data class Completed(val uri: Uri) : FileDownloadState

    /**
     * Error durante la descarga.
     * @param message Descripcion del error
     */
    data class Error(val message: String) : FileDownloadState
}

/**
 * Gestiona la descarga de archivos del servidor con reporte de progreso.
 *
 * Los archivos se descargan al directorio externo de la app bajo "WhatsApp Clone/"
 * dentro de [Environment.DIRECTORY_DOWNLOADS]. Si el almacenamiento externo no esta
 * disponible, se usa el directorio interno de la app como fallback.
 *
 * Caracteristicas:
 * - Descarga con progreso en tiempo real basado en content-length
 * - Deteccion de archivos ya descargados (evita re-descarga)
 * - Creacion automatica de directorios padre
 * - URIs accesibles via FileProvider para compartir con otras apps
 * - Manejo de certificados auto-firmados (delegado al OkHttpClient inyectado)
 *
 * @param context Contexto de la aplicacion para acceder al sistema de archivos
 * @param okHttpClient Cliente HTTP con SSL configurado (inyectado por Hilt)
 */
@Singleton
class FileDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {

    companion object {
        /** Nombre de la carpeta de descargas de la app */
        private const val DOWNLOAD_FOLDER = "WhatsApp Clone"

        /** Autoridad del FileProvider (debe coincidir con AndroidManifest.xml) */
        private const val FILE_PROVIDER_AUTHORITY = "com.clonewhatsapp.fileprovider"

        /** Tamano del buffer de lectura en bytes */
        private const val BUFFER_SIZE = 8192
    }

    /**
     * Descarga un archivo desde una URL con reporte de progreso.
     *
     * Si el archivo ya existe en el directorio de descargas, emite
     * [FileDownloadState.Completed] inmediatamente sin volver a descargarlo.
     *
     * @param url URL completa del archivo a descargar
     * @param fileName Nombre con el que se guardara el archivo localmente
     * @return [Flow] que emite el estado de la descarga (Idle -> Downloading -> Completed/Error)
     */
    fun downloadFile(url: String, fileName: String): Flow<FileDownloadState> = flow {
        emit(FileDownloadState.Idle)

        try {
            val downloadDir = getDownloadDirectory()
            val targetFile = File(downloadDir, fileName)

            // Si el archivo ya existe, retornar inmediatamente
            if (targetFile.exists() && targetFile.length() > 0) {
                val uri = getFileUri(targetFile)
                emit(FileDownloadState.Completed(uri))
                return@flow
            }

            // Crear directorios padre si no existen
            downloadDir.mkdirs()

            emit(FileDownloadState.Downloading(0f))

            // Construir y ejecutar la peticion HTTP
            val request = Request.Builder()
                .url(url)
                .build()

            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                emit(FileDownloadState.Error("Error del servidor: ${response.code}"))
                response.close()
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(FileDownloadState.Error("Respuesta vacia del servidor"))
                response.close()
                return@flow
            }

            val contentLength = body.contentLength()
            val inputStream = body.byteStream()

            // Escribir en un archivo temporal para evitar archivos parciales
            val tempFile = File(downloadDir, "$fileName.tmp")

            try {
                FileOutputStream(tempFile).use { outputStream ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // Emitir progreso si conocemos el tamano total
                        if (contentLength > 0) {
                            val progress = (totalBytesRead.toFloat() / contentLength)
                                .coerceIn(0f, 1f)
                            emit(FileDownloadState.Downloading(progress))
                        } else {
                            // Si no hay content-length, emitir progreso indeterminado
                            emit(FileDownloadState.Downloading(-1f))
                        }
                    }

                    outputStream.flush()
                }

                // Renombrar el archivo temporal al nombre final
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                val renamed = tempFile.renameTo(targetFile)
                if (!renamed) {
                    // Fallback: copiar y borrar si renameTo falla
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                val uri = getFileUri(targetFile)
                emit(FileDownloadState.Completed(uri))
            } catch (e: Exception) {
                // Limpiar archivo temporal en caso de error
                tempFile.delete()
                throw e
            } finally {
                inputStream.close()
                response.close()
            }
        } catch (e: Exception) {
            emit(FileDownloadState.Error(e.message ?: "Error desconocido al descargar archivo"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Verifica si un archivo ya fue descargado.
     *
     * @param fileName Nombre del archivo a verificar
     * @return true si el archivo existe y tiene contenido
     */
    fun isFileDownloaded(fileName: String): Boolean {
        val downloadDir = getDownloadDirectory()
        val file = File(downloadDir, fileName)
        return file.exists() && file.length() > 0
    }

    /**
     * Obtiene la URI de un archivo ya descargado.
     *
     * @param fileName Nombre del archivo
     * @return URI del archivo via FileProvider, o null si no existe
     */
    fun getDownloadedFileUri(fileName: String): Uri? {
        val downloadDir = getDownloadDirectory()
        val file = File(downloadDir, fileName)
        return if (file.exists() && file.length() > 0) {
            getFileUri(file)
        } else {
            null
        }
    }

    /**
     * Elimina un archivo descargado.
     *
     * @param fileName Nombre del archivo a eliminar
     * @return true si se elimino correctamente
     */
    fun deleteDownloadedFile(fileName: String): Boolean {
        val downloadDir = getDownloadDirectory()
        val file = File(downloadDir, fileName)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    /**
     * Obtiene el directorio de descargas de la app.
     * Usa almacenamiento externo si esta disponible, sino el directorio interno.
     */
    private fun getDownloadDirectory(): File {
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return if (externalDir != null) {
            File(externalDir, DOWNLOAD_FOLDER)
        } else {
            File(context.filesDir, DOWNLOAD_FOLDER)
        }
    }

    /**
     * Genera una URI accesible para otros componentes usando FileProvider.
     */
    private fun getFileUri(file: File): Uri {
        return try {
            FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
        } catch (e: Exception) {
            // Fallback a URI de archivo directo si FileProvider falla
            Uri.fromFile(file)
        }
    }
}
