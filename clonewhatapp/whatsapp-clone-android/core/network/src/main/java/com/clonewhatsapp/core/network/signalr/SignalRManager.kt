package com.clonewhatsapp.core.network.signalr

import android.util.Log
import com.clonewhatsapp.core.network.auth.SecureTokenStorage
import com.clonewhatsapp.domain.model.ConnectionState
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import com.microsoft.signalr.TransportEnum
import io.reactivex.rxjava3.core.Single
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Gestor centralizado de la conexion SignalR.
 * Maneja el ciclo de vida de la conexion, registro de listeners,
 * y exposicion de eventos como flujos de Kotlin.
 */
@Singleton
class SignalRManager @Inject constructor(
    private val secureTokenStorage: SecureTokenStorage,
    @Named("hubUrl") private val hubUrl: String
) {
    companion object {
        private const val TAG = "SignalRManager"
    }

    private var hubConnection: HubConnection? = null

    // -- Estado de conexion --
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // -- Flujos de eventos del servidor --
    private val _messageReceived = MutableSharedFlow<MensajeEnviadoDto>(extraBufferCapacity = 64)
    val messageReceived: SharedFlow<MensajeEnviadoDto> = _messageReceived.asSharedFlow()

    private val _typingReceived = MutableSharedFlow<TypingDto>(extraBufferCapacity = 64)
    val typingReceived: SharedFlow<TypingDto> = _typingReceived.asSharedFlow()

    private val _messageStatusUpdated = MutableSharedFlow<MessageStatusDto>(extraBufferCapacity = 64)
    val messageStatusUpdated: SharedFlow<MessageStatusDto> = _messageStatusUpdated.asSharedFlow()

    private val _userStatusChanged = MutableSharedFlow<UserStatusDto>(extraBufferCapacity = 64)
    val userStatusChanged: SharedFlow<UserStatusDto> = _userStatusChanged.asSharedFlow()

    private val _groupUpdated = MutableSharedFlow<GroupUpdatedDto>(extraBufferCapacity = 64)
    val groupUpdated: SharedFlow<GroupUpdatedDto> = _groupUpdated.asSharedFlow()

    // -- Flujos de eventos de llamadas --
    private val _callOfferReceived = MutableSharedFlow<CallOfferDto>(extraBufferCapacity = 8)
    val callOfferReceived: SharedFlow<CallOfferDto> = _callOfferReceived.asSharedFlow()

    private val _callAnswerReceived = MutableSharedFlow<CallAnswerDto>(extraBufferCapacity = 8)
    val callAnswerReceived: SharedFlow<CallAnswerDto> = _callAnswerReceived.asSharedFlow()

    private val _callRejected = MutableSharedFlow<CallRejectedDto>(extraBufferCapacity = 8)
    val callRejected: SharedFlow<CallRejectedDto> = _callRejected.asSharedFlow()

    private val _callEnded = MutableSharedFlow<CallEndedDto>(extraBufferCapacity = 8)
    val callEnded: SharedFlow<CallEndedDto> = _callEnded.asSharedFlow()

    private val _iceCandidateReceived = MutableSharedFlow<IceCandidateDto>(extraBufferCapacity = 64)
    val iceCandidateReceived: SharedFlow<IceCandidateDto> = _iceCandidateReceived.asSharedFlow()

    private val _callFailed = MutableSharedFlow<CallFailedDto>(extraBufferCapacity = 8)
    val callFailed: SharedFlow<CallFailedDto> = _callFailed.asSharedFlow()

    /**
     * Establece la conexion con el hub de SignalR.
     * Configura autenticacion JWT, listeners y manejo de reconexion.
     */
    suspend fun connect() {
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING
        ) {
            Log.d(TAG, "Ya conectado o conectando, ignorando solicitud de conexion")
            return
        }

        _connectionState.value = ConnectionState.CONNECTING

        withContext(Dispatchers.IO) {
            try {
                // Cerrar conexion anterior si existe
                hubConnection?.close()

                val connection = buildHubConnection()
                hubConnection = connection

                registerServerEventListeners(connection)
                registerConnectionCallbacks(connection)

                Log.d(TAG, "Iniciando conexion a: $hubUrl")
                connection.start().blockingAwait()

                _connectionState.value = ConnectionState.CONNECTED
                Log.i(TAG, "Conexion SignalR establecida exitosamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al conectar con SignalR", e)
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    /**
     * Desconecta del hub de SignalR de forma limpia.
     */
    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                hubConnection?.close()
                hubConnection = null
                _connectionState.value = ConnectionState.DISCONNECTED
                Log.i(TAG, "Desconectado de SignalR")
            } catch (e: Exception) {
                Log.e(TAG, "Error al desconectar de SignalR", e)
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    /**
     * Verifica si la conexion esta activa.
     */
    fun isConnected(): Boolean {
        return hubConnection?.connectionState == HubConnectionState.CONNECTED
    }

    // -- Metodos para invocar acciones en el servidor --

    /**
     * Envia un mensaje a traves del hub.
     */
    suspend fun sendMessage(mensajeEnviadoDto: MensajeEnviadoDto) {
        invokeHub("SendMessage", mensajeEnviadoDto)
    }

    /**
     * Envia notificacion de escritura.
     */
    suspend fun sendTyping(typingDto: TypingDto) {
        invokeHub("SendTyping", typingDto)
    }

    /**
     * Notifica que un mensaje fue entregado.
     */
    suspend fun messageDelivered(messageId: String, chatId: String) {
        invokeHub("MessageDelivered", messageId, chatId)
    }

    /**
     * Notifica que un mensaje fue leido.
     */
    suspend fun messageRead(messageId: String, chatId: String) {
        invokeHub("MessageRead", messageId, chatId)
    }

    /**
     * Se une a un chat para recibir sus mensajes.
     */
    suspend fun joinChat(chatId: String) {
        invokeHub("JoinChat", chatId)
    }

    /**
     * Sale de un chat.
     */
    suspend fun leaveChat(chatId: String) {
        invokeHub("LeaveChat", chatId)
    }

    // -- Metodos de llamadas --

    /**
     * Inicia una llamada al usuario objetivo.
     */
    suspend fun callUser(targetUserId: String, callType: String, sdpOffer: String) {
        invokeHub("CallUser", targetUserId, callType, sdpOffer)
    }

    /**
     * Responde a una llamada entrante.
     */
    suspend fun answerCall(callerId: String, sdpAnswer: String) {
        invokeHub("AnswerCall", callerId, sdpAnswer)
    }

    /**
     * Rechaza una llamada entrante.
     */
    suspend fun rejectCall(callerId: String, reason: String) {
        invokeHub("RejectCall", callerId, reason)
    }

    /**
     * Finaliza la llamada actual.
     */
    suspend fun endCall(otherUserId: String) {
        invokeHub("EndCall", otherUserId)
    }

    /**
     * Envia un candidato ICE al usuario objetivo.
     */
    suspend fun sendIceCandidate(targetUserId: String, candidate: String) {
        invokeHub("SendICECandidate", targetUserId, candidate)
    }

    // -- Metodos privados --

    /**
     * Construye la conexion al hub con autenticacion JWT y configuracion SSL.
     */
    private fun buildHubConnection(): HubConnection {
        return HubConnectionBuilder.create(hubUrl)
            .withAccessTokenProvider(
                Single.defer {
                    val token = secureTokenStorage.getToken() ?: ""
                    Single.just(token)
                }
            )
            .withTransport(TransportEnum.WEBSOCKETS)
            .shouldSkipNegotiate(true)
            .build()
    }

    /**
     * Registra todos los listeners de eventos del servidor.
     */
    private fun registerServerEventListeners(connection: HubConnection) {
        // Recepcion de mensajes
        connection.on("ReceiveMessage", { dto ->
            Log.d(TAG, "Mensaje recibido: chatId=${dto.chatId}, mensajeId=${dto.mensajeId}")
            _messageReceived.tryEmit(dto)
        }, MensajeEnviadoDto::class.java)

        // Notificaciones de escritura
        connection.on("UserTyping", { dto ->
            Log.d(TAG, "Typing: usuario=${dto.nombreUsuario}, escribiendo=${dto.estaEscribiendo}")
            _typingReceived.tryEmit(dto)
        }, TypingDto::class.java)

        // Actualizacion de estado de mensajes
        connection.on("MessageStatusUpdated", { dto ->
            Log.d(TAG, "Estado de mensaje actualizado: ${dto.messageId} -> ${dto.status}")
            _messageStatusUpdated.tryEmit(dto)
        }, MessageStatusDto::class.java)

        // Cambio de estado de usuarios (online/offline)
        connection.on("UserStatusChanged", { dto ->
            Log.d(TAG, "Estado de usuario: ${dto.userId}, online=${dto.isOnline}")
            _userStatusChanged.tryEmit(dto)
        }, UserStatusDto::class.java)

        // Actualizacion de grupo
        connection.on("GroupUpdated", { dto ->
            Log.d(TAG, "Grupo actualizado: ${dto.chatId}, nombre=${dto.nombre}")
            _groupUpdated.tryEmit(dto)
        }, GroupUpdatedDto::class.java)

        // -- Eventos de llamadas --

        // Oferta de llamada entrante
        connection.on("ReceiveCallOffer", { dto ->
            Log.d(TAG, "Oferta de llamada recibida de: ${dto.callerId}, tipo: ${dto.callType}")
            _callOfferReceived.tryEmit(dto)
        }, CallOfferDto::class.java)

        // Respuesta de llamada
        connection.on("ReceiveCallAnswer", { dto ->
            Log.d(TAG, "Respuesta de llamada recibida")
            _callAnswerReceived.tryEmit(dto)
        }, CallAnswerDto::class.java)

        // Llamada rechazada
        connection.on("CallRejected", { dto ->
            Log.d(TAG, "Llamada rechazada por: ${dto.userId}, razon: ${dto.reason}")
            _callRejected.tryEmit(dto)
        }, CallRejectedDto::class.java)

        // Llamada finalizada
        connection.on("CallEnded", { dto ->
            Log.d(TAG, "Llamada finalizada por: ${dto.endedBy}")
            _callEnded.tryEmit(dto)
        }, CallEndedDto::class.java)

        // Candidato ICE recibido
        connection.on("ReceiveICECandidate", { dto ->
            Log.d(TAG, "Candidato ICE recibido")
            _iceCandidateReceived.tryEmit(dto)
        }, IceCandidateDto::class.java)

        // Llamada fallida
        connection.on("CallFailed", { dto ->
            Log.e(TAG, "Llamada fallida: ${dto.reason} - ${dto.message}")
            _callFailed.tryEmit(dto)
        }, CallFailedDto::class.java)
    }

    /**
     * Registra callbacks de ciclo de vida de la conexion.
     * La reconexion es manejada por ReconnectionManager que observa connectionState.
     */
    private fun registerConnectionCallbacks(connection: HubConnection) {
        connection.onClosed { exception ->
            if (exception != null) {
                Log.w(TAG, "Conexion SignalR cerrada con error", exception)
            } else {
                Log.i(TAG, "Conexion SignalR cerrada")
            }
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    /**
     * Invoca un metodo en el hub de forma segura.
     */
    private suspend fun invokeHub(method: String, vararg args: Any) {
        withContext(Dispatchers.IO) {
            val connection = hubConnection
            if (connection == null || connection.connectionState != HubConnectionState.CONNECTED) {
                Log.w(TAG, "No se puede invocar '$method': no hay conexion activa")
                return@withContext
            }

            try {
                when (args.size) {
                    0 -> connection.send(method)
                    1 -> connection.send(method, args[0])
                    2 -> connection.send(method, args[0], args[1])
                    3 -> connection.send(method, args[0], args[1], args[2])
                    else -> Log.e(TAG, "invokeHub no soporta ${args.size} argumentos")
                }
                Log.d(TAG, "Invocado '$method' exitosamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al invocar '$method'", e)
            }
        }
    }
}
