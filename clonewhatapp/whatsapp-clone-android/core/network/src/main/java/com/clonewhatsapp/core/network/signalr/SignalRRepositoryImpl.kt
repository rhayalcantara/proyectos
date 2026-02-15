package com.clonewhatsapp.core.network.signalr

import com.clonewhatsapp.core.network.auth.SecureTokenStorage
import com.clonewhatsapp.domain.model.ConnectionState
import com.clonewhatsapp.domain.model.MessageStatusUpdate
import com.clonewhatsapp.domain.model.ReceivedMessage
import com.clonewhatsapp.domain.model.TypingInfo
import com.clonewhatsapp.domain.model.UserStatusUpdate
import com.clonewhatsapp.domain.repository.SignalRRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementacion del repositorio SignalR.
 * Actua como puente entre SignalRManager (capa de red) y el dominio,
 * mapeando DTOs a modelos de dominio y exponiendo flujos tipados.
 */
@Singleton
class SignalRRepositoryImpl @Inject constructor(
    private val signalRManager: SignalRManager,
    private val secureTokenStorage: SecureTokenStorage
) : SignalRRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // -- Flujos de dominio --
    private val _messageReceived = MutableSharedFlow<ReceivedMessage>(extraBufferCapacity = 64)
    override val messageReceived: SharedFlow<ReceivedMessage> = _messageReceived.asSharedFlow()

    private val _typingReceived = MutableSharedFlow<TypingInfo>(extraBufferCapacity = 64)
    override val typingReceived: SharedFlow<TypingInfo> = _typingReceived.asSharedFlow()

    private val _messageStatusUpdated = MutableSharedFlow<MessageStatusUpdate>(extraBufferCapacity = 64)
    override val messageStatusUpdated: SharedFlow<MessageStatusUpdate> = _messageStatusUpdated.asSharedFlow()

    private val _userStatusChanged = MutableSharedFlow<UserStatusUpdate>(extraBufferCapacity = 64)
    override val userStatusChanged: SharedFlow<UserStatusUpdate> = _userStatusChanged.asSharedFlow()

    override val connectionState: StateFlow<ConnectionState>
        get() = signalRManager.connectionState

    init {
        collectManagerFlows()
    }

    /**
     * Recolecta los flujos del SignalRManager y los mapea a modelos de dominio.
     */
    private fun collectManagerFlows() {
        scope.launch {
            signalRManager.messageReceived.collect { dto ->
                val domainModel = mapMessageToDomain(dto)
                _messageReceived.tryEmit(domainModel)
            }
        }

        scope.launch {
            signalRManager.typingReceived.collect { dto ->
                _typingReceived.tryEmit(
                    TypingInfo(
                        chatId = dto.chatId,
                        usuarioId = dto.usuarioId,
                        nombreUsuario = dto.nombreUsuario,
                        isTyping = dto.estaEscribiendo
                    )
                )
            }
        }

        scope.launch {
            signalRManager.messageStatusUpdated.collect { dto ->
                _messageStatusUpdated.tryEmit(
                    MessageStatusUpdate(
                        messageId = dto.messageId,
                        status = dto.status
                    )
                )
            }
        }

        scope.launch {
            signalRManager.userStatusChanged.collect { dto ->
                _userStatusChanged.tryEmit(
                    UserStatusUpdate(
                        userId = dto.userId,
                        isOnline = dto.isOnline,
                        lastSeen = parseIso8601ToEpochMillis(dto.lastSeen)
                    )
                )
            }
        }
    }

    override suspend fun connect() {
        signalRManager.connect()
    }

    override suspend fun disconnect() {
        signalRManager.disconnect()
    }

    override fun isConnected(): Boolean {
        return signalRManager.isConnected()
    }

    override suspend fun sendMessage(chatId: String, mensajeId: String, contenido: String, tipo: String) {
        val userId = secureTokenStorage.getUserId() ?: return

        val mensajeDto = MensajeSignalRDto(
            id = mensajeId,
            chatId = chatId,
            remitenteId = userId,
            remitenteNombre = null,
            contenido = contenido,
            tipo = tipo,
            estado = "Enviado",
            urlArchivo = null,
            fechaEnvio = getCurrentIso8601()
        )

        val enviadoDto = MensajeEnviadoDto(
            mensajeId = mensajeId,
            chatId = chatId,
            mensaje = mensajeDto
        )

        signalRManager.sendMessage(enviadoDto)
    }

    override suspend fun sendTyping(chatId: String, isTyping: Boolean) {
        val userId = secureTokenStorage.getUserId() ?: return

        val typingDto = TypingDto(
            chatId = chatId,
            usuarioId = userId,
            nombreUsuario = "",
            estaEscribiendo = isTyping
        )

        signalRManager.sendTyping(typingDto)
    }

    override suspend fun markDelivered(messageId: String, chatId: String) {
        signalRManager.messageDelivered(messageId, chatId)
    }

    override suspend fun markRead(messageId: String, chatId: String) {
        signalRManager.messageRead(messageId, chatId)
    }

    override suspend fun joinChat(chatId: String) {
        signalRManager.joinChat(chatId)
    }

    override suspend fun leaveChat(chatId: String) {
        signalRManager.leaveChat(chatId)
    }

    // -- Funciones de mapeo --

    /**
     * Mapea un DTO de mensaje recibido al modelo de dominio.
     */
    private fun mapMessageToDomain(dto: MensajeEnviadoDto): ReceivedMessage {
        val msg = dto.mensaje
        return ReceivedMessage(
            mensajeId = dto.mensajeId,
            chatId = dto.chatId,
            remitenteId = msg.remitenteId,
            remitenteNombre = msg.remitenteNombre,
            contenido = msg.contenido,
            tipo = msg.tipo,
            estado = msg.estado,
            urlArchivo = msg.urlArchivo,
            fechaEnvio = parseIso8601ToEpochMillis(msg.fechaEnvio) ?: System.currentTimeMillis()
        )
    }

    /**
     * Convierte una fecha ISO 8601 a milisegundos epoch.
     * Soporta formatos con y sin milisegundos, y con 'Z' o offset de timezone.
     */
    private fun parseIso8601ToEpochMillis(dateString: String?): Long? {
        if (dateString.isNullOrBlank()) return null

        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                return sdf.parse(dateString)?.time
            } catch (_: Exception) {
                // Intentar siguiente formato
            }
        }

        return null
    }

    /**
     * Obtiene la fecha actual en formato ISO 8601 UTC.
     */
    private fun getCurrentIso8601(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date())
    }
}
