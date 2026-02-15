package com.clonewhatsapp.core.network.repository

import com.clonewhatsapp.core.network.api.MessagesApi
import com.clonewhatsapp.core.network.dto.EnviarMensajeRequest
import com.clonewhatsapp.core.network.dto.ForwardMessageRequest
import com.clonewhatsapp.core.network.dto.MensajeDto
import com.clonewhatsapp.domain.model.EstadoMensaje
import com.clonewhatsapp.domain.model.Mensaje
import com.clonewhatsapp.domain.model.TipoMensaje
import com.clonewhatsapp.domain.repository.MensajeRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MensajeRepository
 * Handles message API calls and maps DTOs to domain models
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messagesApi: MessagesApi,
    private val gson: Gson
) : MensajeRepository {

    /**
     * Get messages for a chat (paginated)
     */
    override fun getMessages(chatId: String, page: Int, pageSize: Int): Flow<List<Mensaje>> = flow {
        try {
            val response = messagesApi.getMessages(chatId, page, pageSize)
            if (response.isSuccessful) {
                val mensajes = response.body()?.map { it.toDomain() } ?: emptyList()
                emit(mensajes)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    /**
     * Send a text message
     */
    override suspend fun sendMessage(
        chatId: String,
        contenido: String,
        tipo: TipoMensaje,
        mensajeRespondidoId: String?
    ): Result<Mensaje> {
        return try {
            val request = EnviarMensajeRequest(
                chatId = chatId,
                contenido = contenido,
                tipo = tipo.toBackendString(),
                mensajeRespondidoId = mensajeRespondidoId
            )
            val response = messagesApi.sendMessage(request)

            if (response.isSuccessful) {
                val mensajeDto = response.body()
                if (mensajeDto != null) {
                    Result.success(mensajeDto.toDomain())
                } else {
                    Result.failure(Exception("Respuesta vacia del servidor"))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Send a message with a file attachment
     */
    override suspend fun sendMessageWithFile(
        chatId: String,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        contenido: String?,
        mensajeRespondidoId: String?,
        duracionSegundos: Int?
    ): Result<Mensaje> {
        return try {
            val chatIdBody = chatId.toRequestBody("text/plain".toMediaTypeOrNull())
            val contenidoBody = contenido?.toRequestBody("text/plain".toMediaTypeOrNull())
            val respondidoIdBody = mensajeRespondidoId?.toRequestBody("text/plain".toMediaTypeOrNull())
            val duracionBody = duracionSegundos?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

            val fileRequestBody = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", fileName, fileRequestBody)

            val response = messagesApi.sendMessageWithFile(
                chatId = chatIdBody,
                contenido = contenidoBody,
                mensajeRespondidoId = respondidoIdBody,
                duracionSegundos = duracionBody,
                file = filePart
            )

            if (response.isSuccessful) {
                val mensajeDto = response.body()
                if (mensajeDto != null) {
                    Result.success(mensajeDto.toDomain())
                } else {
                    Result.failure(Exception("Respuesta vacia del servidor"))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark a single message as read
     */
    override suspend fun markAsRead(messageId: String): Result<Unit> {
        return try {
            val response = messagesApi.markAsRead(messageId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark all messages in a chat as read
     */
    override suspend fun markAllAsRead(chatId: String): Result<Unit> {
        return try {
            val response = messagesApi.markAllAsRead(chatId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Search messages within a chat
     */
    override suspend fun searchMessages(chatId: String, query: String, limit: Int): Result<List<Mensaje>> {
        return try {
            val response = messagesApi.searchMessages(chatId, query, limit)
            if (response.isSuccessful) {
                val mensajes = response.body()?.map { it.toDomain() } ?: emptyList()
                Result.success(mensajes)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Forward a message to another chat
     */
    override suspend fun forwardMessage(messageId: String, targetChatId: String): Result<Mensaje> {
        return try {
            val request = ForwardMessageRequest(targetChatId = targetChatId)
            val response = messagesApi.forwardMessage(messageId, request)

            if (response.isSuccessful) {
                val mensajeDto = response.body()
                if (mensajeDto != null) {
                    Result.success(mensajeDto.toDomain())
                } else {
                    Result.failure(Exception("Respuesta vacia del servidor"))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a message
     */
    override suspend fun deleteMessage(messageId: String, forEveryone: Boolean): Result<Unit> {
        return try {
            val response = messagesApi.deleteMessage(messageId, forEveryone)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh messages from the server
     */
    override suspend fun refreshMessages(chatId: String): Result<Unit> {
        return try {
            val response = messagesApi.getMessages(chatId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- Mapping extension functions ----

    /**
     * Map MensajeDto to domain Mensaje model
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

    /**
     * Parse backend tipo string to TipoMensaje enum
     */
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

    /**
     * Parse backend estado string to EstadoMensaje enum
     */
    private fun parseEstadoMensaje(estado: String): EstadoMensaje {
        return when (estado.lowercase()) {
            "enviado" -> EstadoMensaje.ENVIADO
            "entregado" -> EstadoMensaje.ENTREGADO
            "leido" -> EstadoMensaje.LEIDO
            else -> EstadoMensaje.ENVIADO
        }
    }

    /**
     * Convert TipoMensaje enum to backend string format
     */
    private fun TipoMensaje.toBackendString(): String {
        return when (this) {
            TipoMensaje.TEXTO -> "Texto"
            TipoMensaje.IMAGEN -> "Imagen"
            TipoMensaje.VIDEO -> "Video"
            TipoMensaje.AUDIO -> "Audio"
            TipoMensaje.DOCUMENTO -> "Documento"
            TipoMensaje.UBICACION -> "Ubicacion"
            TipoMensaje.CONTACTO -> "Contacto"
            TipoMensaje.SISTEMA -> "Sistema"
        }
    }

    /**
     * Parse error message from HTTP response
     */
    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val errorResponse = gson.fromJson(errorBody, Map::class.java)
                errorResponse["message"]?.toString() ?: "Error desconocido"
            } else {
                when (response.code()) {
                    400 -> "Solicitud invalida"
                    401 -> "No autorizado"
                    403 -> "Acceso denegado"
                    404 -> "No encontrado"
                    500 -> "Error del servidor"
                    else -> "Error de conexion (${response.code()})"
                }
            }
        } catch (e: Exception) {
            "Error de conexion"
        }
    }

    /**
     * Parse ISO 8601 date string to milliseconds
     */
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
