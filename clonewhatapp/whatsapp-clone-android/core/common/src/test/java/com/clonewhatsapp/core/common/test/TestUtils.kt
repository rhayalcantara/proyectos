package com.clonewhatsapp.core.common.test

import com.clonewhatsapp.domain.model.Chat
import com.clonewhatsapp.domain.model.EstadoMensaje
import com.clonewhatsapp.domain.model.Mensaje
import com.clonewhatsapp.domain.model.TipoMensaje
import com.clonewhatsapp.domain.model.Usuario
import com.clonewhatsapp.domain.repository.AuthRepository
import com.clonewhatsapp.domain.repository.ChatRepository
import com.clonewhatsapp.domain.repository.MensajeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.UUID

// ============================================================================
// MainDispatcherRule - Regla JUnit para configurar el dispatcher principal en tests
// ============================================================================

/**
 * Regla JUnit que reemplaza el dispatcher Main por un UnconfinedTestDispatcher.
 * Esto permite que las coroutines se ejecuten inmediatamente en los tests.
 *
 * Uso:
 * ```
 * @get:Rule
 * val mainDispatcherRule = MainDispatcherRule()
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

// ============================================================================
// Fakes - Implementaciones falsas de los repositorios para tests
// ============================================================================

/**
 * Implementacion falsa de AuthRepository para tests unitarios.
 * Permite configurar las respuestas de cada metodo.
 */
class FakeAuthRepository : AuthRepository {

    var loginResult: Result<Usuario> = Result.success(crearUsuarioTest())
    var registerResult: Result<Usuario> = Result.success(crearUsuarioTest())
    var loggedIn: Boolean = true
    var currentUserId: String? = "user-001"
    var currentUserName: String? = "Juan Test"
    var currentUserPhone: String? = "+1234567890"
    var logoutCalled: Boolean = false

    override suspend fun login(phone: String, password: String): Result<Usuario> = loginResult

    override suspend fun register(name: String, phone: String, password: String): Result<Usuario> = registerResult

    override suspend fun logout() {
        logoutCalled = true
    }

    override fun isLoggedIn(): Boolean = loggedIn

    override fun getCurrentUserId(): String? = currentUserId

    override fun getCurrentUserName(): String? = currentUserName

    override fun getCurrentUserPhone(): String? = currentUserPhone
}

/**
 * Implementacion falsa de ChatRepository para tests unitarios.
 * Permite configurar las respuestas de cada metodo.
 */
class FakeChatRepository : ChatRepository {

    var chatsFlow: Flow<List<Chat>> = flow { emit(crearListaChatsTest()) }
    var archivedChatsFlow: Flow<List<Chat>> = flow { emit(emptyList()) }
    var chatById: Chat? = crearChatTest()
    var createChatResult: Result<Chat> = Result.success(crearChatTest())
    var createGroupResult: Result<Chat> = Result.success(crearChatTest(esGrupo = true))
    var updateGroupResult: Result<Chat> = Result.success(crearChatTest(esGrupo = true))
    var updateGroupImageResult: Result<Chat> = Result.success(crearChatTest(esGrupo = true))
    var muteChatResult: Result<Unit> = Result.success(Unit)
    var archiveChatResult: Result<Unit> = Result.success(Unit)
    var addParticipantsResult: Result<Unit> = Result.success(Unit)
    var removeParticipantResult: Result<Unit> = Result.success(Unit)
    var changeRoleResult: Result<Unit> = Result.success(Unit)
    var leaveGroupResult: Result<Unit> = Result.success(Unit)
    var groupParticipantsResult: Result<List<Usuario>> = Result.success(crearListaUsuariosTest())
    var refreshChatsResult: Result<Unit> = Result.success(Unit)
    var searchChatsResult: List<Chat> = crearListaChatsTest()

    override fun getChats(): Flow<List<Chat>> = chatsFlow
    override fun getArchivedChats(): Flow<List<Chat>> = archivedChatsFlow
    override suspend fun getChatById(chatId: String): Chat? = chatById
    override suspend fun createIndividualChat(contactId: String): Result<Chat> = createChatResult
    override suspend fun createGroup(nombre: String, descripcion: String?, participantesIds: List<String>): Result<Chat> = createGroupResult
    override suspend fun updateGroup(id: String, nombre: String?, descripcion: String?): Result<Chat> = updateGroupResult
    override suspend fun updateGroupImage(id: String, imageBytes: ByteArray, fileName: String): Result<Chat> = updateGroupImageResult
    override suspend fun muteChat(chatId: String, silenciar: Boolean, duracion: String?): Result<Unit> = muteChatResult
    override suspend fun archiveChat(chatId: String, archivar: Boolean): Result<Unit> = archiveChatResult
    override suspend fun addParticipants(groupId: String, participantesIds: List<String>): Result<Unit> = addParticipantsResult
    override suspend fun removeParticipant(groupId: String, usuarioId: String): Result<Unit> = removeParticipantResult
    override suspend fun changeParticipantRole(groupId: String, usuarioId: String, rol: String): Result<Unit> = changeRoleResult
    override suspend fun leaveGroup(groupId: String): Result<Unit> = leaveGroupResult
    override suspend fun getGroupParticipants(groupId: String): Result<List<Usuario>> = groupParticipantsResult
    override suspend fun refreshChats(): Result<Unit> = refreshChatsResult
    override suspend fun searchChats(query: String): List<Chat> = searchChatsResult
}

/**
 * Implementacion falsa de MensajeRepository para tests unitarios.
 * Permite configurar las respuestas de cada metodo.
 */
class FakeMensajeRepository : MensajeRepository {

    var messagesFlow: Flow<List<Mensaje>> = flow { emit(crearListaMensajesTest()) }
    var sendMessageResult: Result<Mensaje> = Result.success(crearMensajeTest())
    var sendMessageWithFileResult: Result<Mensaje> = Result.success(crearMensajeTest())
    var markAsReadResult: Result<Unit> = Result.success(Unit)
    var markAllAsReadResult: Result<Unit> = Result.success(Unit)
    var searchMessagesResult: Result<List<Mensaje>> = Result.success(crearListaMensajesTest())
    var forwardMessageResult: Result<Mensaje> = Result.success(crearMensajeTest())
    var deleteMessageResult: Result<Unit> = Result.success(Unit)
    var refreshMessagesResult: Result<Unit> = Result.success(Unit)

    override fun getMessages(chatId: String, page: Int, pageSize: Int): Flow<List<Mensaje>> = messagesFlow

    override suspend fun sendMessage(
        chatId: String,
        contenido: String,
        tipo: TipoMensaje,
        mensajeRespondidoId: String?
    ): Result<Mensaje> = sendMessageResult

    override suspend fun sendMessageWithFile(
        chatId: String,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        contenido: String?,
        mensajeRespondidoId: String?,
        duracionSegundos: Int?
    ): Result<Mensaje> = sendMessageWithFileResult

    override suspend fun markAsRead(messageId: String): Result<Unit> = markAsReadResult
    override suspend fun markAllAsRead(chatId: String): Result<Unit> = markAllAsReadResult
    override suspend fun searchMessages(chatId: String, query: String, limit: Int): Result<List<Mensaje>> = searchMessagesResult
    override suspend fun forwardMessage(messageId: String, targetChatId: String): Result<Mensaje> = forwardMessageResult
    override suspend fun deleteMessage(messageId: String, forEveryone: Boolean): Result<Unit> = deleteMessageResult
    override suspend fun refreshMessages(chatId: String): Result<Unit> = refreshMessagesResult
}

// ============================================================================
// Funciones auxiliares para crear datos de prueba
// ============================================================================

/**
 * Crea un Usuario de prueba con valores por defecto configurables.
 */
fun crearUsuarioTest(
    id: String = "user-001",
    nombre: String = "Juan Test",
    telefono: String = "+1234567890",
    fotoPerfil: String? = null,
    estado: String? = "Disponible",
    isOnline: Boolean = false,
    ultimaConexion: Long? = null
): Usuario = Usuario(
    id = id,
    nombre = nombre,
    telefono = telefono,
    fotoPerfil = fotoPerfil,
    estado = estado,
    isOnline = isOnline,
    ultimaConexion = ultimaConexion
)

/**
 * Crea un Chat de prueba con valores por defecto configurables.
 */
fun crearChatTest(
    id: String = "chat-001",
    nombre: String = "Chat de Prueba",
    esGrupo: Boolean = false,
    fotoUrl: String? = null,
    ultimoMensaje: String? = "Hola, este es un mensaje de prueba",
    ultimoMensajeTiempo: Long? = System.currentTimeMillis(),
    mensajesNoLeidos: Int = 0,
    participantes: List<Usuario> = emptyList()
): Chat = Chat(
    id = id,
    nombre = nombre,
    esGrupo = esGrupo,
    fotoUrl = fotoUrl,
    ultimoMensaje = ultimoMensaje,
    ultimoMensajeTiempo = ultimoMensajeTiempo,
    mensajesNoLeidos = mensajesNoLeidos,
    participantes = participantes
)

/**
 * Crea una lista de Chats de prueba.
 */
fun crearListaChatsTest(cantidad: Int = 3): List<Chat> {
    return (1..cantidad).map { index ->
        crearChatTest(
            id = "chat-$index",
            nombre = "Chat $index",
            ultimoMensaje = "Ultimo mensaje del chat $index",
            mensajesNoLeidos = index % 3
        )
    }
}

/**
 * Crea un Mensaje de prueba con valores por defecto configurables.
 */
fun crearMensajeTest(
    id: String = UUID.randomUUID().toString(),
    chatId: String = "chat-001",
    remitenteId: String = "user-001",
    contenido: String? = "Mensaje de prueba",
    urlArchivo: String? = null,
    tipo: TipoMensaje = TipoMensaje.TEXTO,
    estado: EstadoMensaje = EstadoMensaje.ENVIADO,
    fechaEnvio: Long = System.currentTimeMillis(),
    editado: Boolean = false,
    eliminado: Boolean = false,
    eliminadoParaTodos: Boolean = false,
    mensajeRespondidoId: String? = null,
    duracionSegundos: Int? = null,
    nombreRemitente: String? = "Juan Test"
): Mensaje = Mensaje(
    id = id,
    chatId = chatId,
    remitenteId = remitenteId,
    contenido = contenido,
    urlArchivo = urlArchivo,
    tipo = tipo,
    estado = estado,
    fechaEnvio = fechaEnvio,
    editado = editado,
    eliminado = eliminado,
    eliminadoParaTodos = eliminadoParaTodos,
    mensajeRespondidoId = mensajeRespondidoId,
    duracionSegundos = duracionSegundos,
    nombreRemitente = nombreRemitente
)

/**
 * Crea una lista de Mensajes de prueba.
 */
fun crearListaMensajesTest(cantidad: Int = 5, chatId: String = "chat-001"): List<Mensaje> {
    return (1..cantidad).map { index ->
        crearMensajeTest(
            id = "msg-$index",
            chatId = chatId,
            contenido = "Mensaje de prueba #$index",
            fechaEnvio = System.currentTimeMillis() - (cantidad - index) * 60_000L,
            remitenteId = if (index % 2 == 0) "user-001" else "user-002"
        )
    }
}

/**
 * Crea una lista de Usuarios de prueba.
 */
fun crearListaUsuariosTest(cantidad: Int = 3): List<Usuario> {
    return (1..cantidad).map { index ->
        crearUsuarioTest(
            id = "user-$index",
            nombre = "Usuario $index",
            telefono = "+123456789$index"
        )
    }
}
