package com.clonewhatsapp.core.network.repository

import com.clonewhatsapp.core.network.api.ChatsApi
import com.clonewhatsapp.core.network.dto.ActualizarGrupoRequest
import com.clonewhatsapp.core.network.dto.AgregarParticipantesRequest
import com.clonewhatsapp.core.network.dto.ArchivarChatRequest
import com.clonewhatsapp.core.network.dto.CambiarRolRequest
import com.clonewhatsapp.core.network.dto.ChatDto
import com.clonewhatsapp.core.network.dto.CrearChatIndividualRequest
import com.clonewhatsapp.core.network.dto.CrearGrupoRequest
import com.clonewhatsapp.core.network.dto.ParticipanteDto
import com.clonewhatsapp.core.network.dto.SilenciarChatRequest
import com.clonewhatsapp.domain.model.Chat
import com.clonewhatsapp.domain.model.Usuario
import com.clonewhatsapp.domain.repository.ChatRepository
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
 * Implementation of ChatRepository
 * Handles chat API calls and maps DTOs to domain models
 */
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatsApi: ChatsApi,
    private val gson: Gson
) : ChatRepository {

    /**
     * Get all non-archived chats
     */
    override fun getChats(): Flow<List<Chat>> = flow {
        try {
            val response = chatsApi.getChats()
            if (response.isSuccessful) {
                val chats = response.body()?.map { it.toDomain() } ?: emptyList()
                emit(chats)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    /**
     * Get all archived chats
     */
    override fun getArchivedChats(): Flow<List<Chat>> = flow {
        try {
            val response = chatsApi.getArchivedChats()
            if (response.isSuccessful) {
                val chats = response.body()?.map { it.toDomain() } ?: emptyList()
                emit(chats)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    /**
     * Get a specific chat by ID
     */
    override suspend fun getChatById(chatId: String): Chat? {
        return try {
            val response = chatsApi.getChatById(chatId)
            if (response.isSuccessful) {
                response.body()?.toDomain()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Create an individual chat
     */
    override suspend fun createIndividualChat(contactId: String): Result<Chat> {
        return try {
            val request = CrearChatIndividualRequest(contactoId = contactId)
            val response = chatsApi.createIndividualChat(request)

            if (response.isSuccessful) {
                val chatDto = response.body()
                if (chatDto != null) {
                    Result.success(chatDto.toDomain())
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
     * Create a group chat
     */
    override suspend fun createGroup(
        nombre: String,
        descripcion: String?,
        participantesIds: List<String>
    ): Result<Chat> {
        return try {
            val request = CrearGrupoRequest(
                nombre = nombre,
                descripcion = descripcion,
                participantesIds = participantesIds
            )
            val response = chatsApi.createGroup(request)

            if (response.isSuccessful) {
                val chatDto = response.body()
                if (chatDto != null) {
                    Result.success(chatDto.toDomain())
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
     * Update group info
     */
    override suspend fun updateGroup(
        id: String,
        nombre: String?,
        descripcion: String?
    ): Result<Chat> {
        return try {
            val request = ActualizarGrupoRequest(nombre = nombre, descripcion = descripcion)
            val response = chatsApi.updateGroup(id, request)

            if (response.isSuccessful) {
                val chatDto = response.body()
                if (chatDto != null) {
                    Result.success(chatDto.toDomain())
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
     * Update group image
     */
    override suspend fun updateGroupImage(
        id: String,
        imageBytes: ByteArray,
        fileName: String
    ): Result<Chat> {
        return try {
            val requestBody = imageBytes.toRequestBody("image/*".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("imagen", fileName, requestBody)
            val response = chatsApi.updateGroupImage(id, part)

            if (response.isSuccessful) {
                val chatDto = response.body()
                if (chatDto != null) {
                    Result.success(chatDto.toDomain())
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
     * Mute or unmute a chat
     */
    override suspend fun muteChat(chatId: String, silenciar: Boolean, duracion: String?): Result<Unit> {
        return try {
            val request = SilenciarChatRequest(silenciar = silenciar, duracion = duracion)
            val response = chatsApi.muteChat(chatId, request)

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
     * Archive or unarchive a chat
     */
    override suspend fun archiveChat(chatId: String, archivar: Boolean): Result<Unit> {
        return try {
            val request = ArchivarChatRequest(archivar = archivar)
            val response = chatsApi.archiveChat(chatId, request)

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
     * Add participants to a group
     */
    override suspend fun addParticipants(groupId: String, participantesIds: List<String>): Result<Unit> {
        return try {
            val request = AgregarParticipantesRequest(participantesIds = participantesIds)
            val response = chatsApi.addParticipants(groupId, request)

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
     * Remove a participant from a group
     */
    override suspend fun removeParticipant(groupId: String, usuarioId: String): Result<Unit> {
        return try {
            val response = chatsApi.removeParticipant(groupId, usuarioId)

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
     * Change a participant's role
     */
    override suspend fun changeParticipantRole(groupId: String, usuarioId: String, rol: String): Result<Unit> {
        return try {
            val request = CambiarRolRequest(usuarioId = usuarioId, rol = rol)
            val response = chatsApi.changeParticipantRole(groupId, usuarioId, request)

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
     * Leave a group
     */
    override suspend fun leaveGroup(groupId: String): Result<Unit> {
        return try {
            val response = chatsApi.leaveGroup(groupId)

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
     * Get group participants
     */
    override suspend fun getGroupParticipants(groupId: String): Result<List<Usuario>> {
        return try {
            val response = chatsApi.getGroupParticipants(groupId)

            if (response.isSuccessful) {
                val participantes = response.body()?.map { it.toDomain() } ?: emptyList()
                Result.success(participantes)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh chats from the server (triggers a new fetch)
     */
    override suspend fun refreshChats(): Result<Unit> {
        return try {
            val response = chatsApi.getChats()
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
     * Search chats locally by name (filters from all chats)
     */
    override suspend fun searchChats(query: String): List<Chat> {
        return try {
            val response = chatsApi.getChats()
            if (response.isSuccessful) {
                val allChats = response.body()?.map { it.toDomain() } ?: emptyList()
                allChats.filter { chat ->
                    chat.nombre.contains(query, ignoreCase = true)
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ---- Mapping extension functions ----

    /**
     * Map ChatDto to domain Chat model
     */
    private fun ChatDto.toDomain(): Chat {
        val nombre = when {
            nombreGrupo != null -> nombreGrupo
            otroParticipante != null -> otroParticipante.nombre
            else -> "Chat"
        }

        val fotoUrl = when {
            imagenGrupo != null -> imagenGrupo
            otroParticipante?.fotoPerfil != null -> otroParticipante.fotoPerfil
            else -> null
        }

        return Chat(
            id = id,
            nombre = nombre,
            esGrupo = tipo == "Grupo",
            fotoUrl = fotoUrl,
            ultimoMensaje = ultimoMensaje?.contenido,
            ultimoMensajeTiempo = ultimoMensaje?.fechaEnvio?.let { parseIso8601ToMillis(it) },
            mensajesNoLeidos = mensajesNoLeidos,
            participantes = participantes.map { it.toDomain() }
        )
    }

    /**
     * Map ParticipanteDto to domain Usuario model
     */
    private fun ParticipanteDto.toDomain(): Usuario {
        return Usuario(
            id = usuarioId,
            nombre = nombre,
            telefono = "", // Not available in ParticipanteDTO
            fotoPerfil = fotoPerfil,
            isOnline = estaEnLinea
        )
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
