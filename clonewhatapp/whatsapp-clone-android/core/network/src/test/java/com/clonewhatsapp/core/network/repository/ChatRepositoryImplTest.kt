package com.clonewhatsapp.core.network.repository

import com.clonewhatsapp.core.network.api.ChatsApi
import com.clonewhatsapp.core.network.dto.ChatDto
import com.clonewhatsapp.core.network.dto.MensajeDto
import com.clonewhatsapp.core.network.dto.ParticipanteDto
import com.clonewhatsapp.core.network.dto.UsuarioDto
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.Response

/**
 * Tests unitarios para ChatRepositoryImpl (T-122)
 *
 * Verifica:
 * - getChats exitoso y fallido
 * - getChatById exitoso y no encontrado
 * - createIndividualChat exitoso y fallido
 * - refreshChats
 * - Mapeo correcto de DTOs a modelos de dominio
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatRepositoryImplTest {

    private lateinit var chatsApi: ChatsApi
    private lateinit var gson: Gson
    private lateinit var repository: ChatRepositoryImpl

    @Before
    fun setUp() {
        chatsApi = mock()
        gson = Gson()
        repository = ChatRepositoryImpl(chatsApi, gson)
    }

    // -- Helpers --

    private fun crearChatDto(
        id: String = "chat-001",
        tipo: String = "Individual",
        nombreGrupo: String? = null,
        otroParticipanteNombre: String = "Maria Lopez"
    ): ChatDto = ChatDto(
        id = id,
        tipo = tipo,
        fechaCreacion = "2026-01-15T10:30:00",
        ultimaActividad = "2026-01-15T12:00:00",
        nombreGrupo = nombreGrupo,
        imagenGrupo = null,
        descripcionGrupo = null,
        otroParticipante = UsuarioDto(
            id = "user-002",
            numeroTelefono = "+0987654321",
            nombre = otroParticipanteNombre,
            fotoPerfil = null,
            estado = "Disponible",
            ultimaConexion = "2026-01-15T11:00:00",
            estaEnLinea = true
        ),
        ultimoMensaje = MensajeDto(
            id = "msg-001",
            chatId = id,
            remitenteId = "user-002",
            remitenteNombre = otroParticipanteNombre,
            contenido = "Hola!",
            tipo = "Texto",
            urlArchivo = null,
            nombreArchivo = null,
            tamanoArchivo = null,
            duracionSegundos = null,
            mensajeRespondidoId = null,
            mensajeRespondido = null,
            fechaEnvio = "2026-01-15T12:00:00",
            eliminado = false,
            eliminadoParaTodos = false,
            editado = false,
            estado = "Enviado"
        ),
        mensajesNoLeidos = 2,
        participantes = listOf(
            ParticipanteDto(
                usuarioId = "user-001",
                nombre = "Juan Test",
                fotoPerfil = null,
                rol = "Participante",
                estaEnLinea = false
            ),
            ParticipanteDto(
                usuarioId = "user-002",
                nombre = otroParticipanteNombre,
                fotoPerfil = null,
                rol = "Participante",
                estaEnLinea = true
            )
        ),
        silenciado = false,
        silenciadoHasta = null,
        archivado = false
    )

    // -- Tests de getChats --

    @Test
    fun `getChats exitoso retorna lista de chats mapeados`() = runTest {
        val chatDtos = listOf(crearChatDto(), crearChatDto(id = "chat-002", otroParticipanteNombre = "Pedro"))
        whenever(chatsApi.getChats()).thenReturn(Response.success(chatDtos))

        val chats = repository.getChats().first()

        assertEquals(2, chats.size)
        assertEquals("chat-001", chats[0].id)
        assertEquals("Maria Lopez", chats[0].nombre)
        assertEquals("Hola!", chats[0].ultimoMensaje)
        assertEquals(2, chats[0].mensajesNoLeidos)
    }

    @Test
    fun `getChats con error de API retorna lista vacia`() = runTest {
        val errorBody = "Error".toResponseBody("text/plain".toMediaTypeOrNull())
        whenever(chatsApi.getChats()).thenReturn(Response.error(500, errorBody))

        val chats = repository.getChats().first()

        assertTrue("Lista debe estar vacia", chats.isEmpty())
    }

    @Test
    fun `getChats con excepcion retorna lista vacia`() = runTest {
        whenever(chatsApi.getChats()).thenThrow(RuntimeException("Sin conexion"))

        val chats = repository.getChats().first()

        assertTrue("Lista debe estar vacia", chats.isEmpty())
    }

    @Test
    fun `getChats mapea chat de grupo correctamente`() = runTest {
        val grupoChatDto = crearChatDto(
            id = "grupo-001",
            tipo = "Grupo",
            nombreGrupo = "Grupo Trabajo"
        )
        whenever(chatsApi.getChats()).thenReturn(Response.success(listOf(grupoChatDto)))

        val chats = repository.getChats().first()

        assertEquals(1, chats.size)
        assertEquals("Grupo Trabajo", chats[0].nombre)
        assertTrue("Debe ser grupo", chats[0].esGrupo)
    }

    // -- Tests de getChatById --

    @Test
    fun `getChatById exitoso retorna chat mapeado`() = runTest {
        val chatDto = crearChatDto()
        whenever(chatsApi.getChatById(any())).thenReturn(Response.success(chatDto))

        val chat = repository.getChatById("chat-001")

        assertNotNull("Chat no debe ser null", chat)
        assertEquals("chat-001", chat!!.id)
        assertEquals("Maria Lopez", chat.nombre)
    }

    @Test
    fun `getChatById no encontrado retorna null`() = runTest {
        val errorBody = "Not found".toResponseBody("text/plain".toMediaTypeOrNull())
        whenever(chatsApi.getChatById(any())).thenReturn(Response.error(404, errorBody))

        val chat = repository.getChatById("chat-inexistente")

        assertNull("Chat debe ser null", chat)
    }

    @Test
    fun `getChatById con excepcion retorna null`() = runTest {
        whenever(chatsApi.getChatById(any())).thenThrow(RuntimeException("Error"))

        val chat = repository.getChatById("chat-001")

        assertNull("Chat debe ser null", chat)
    }

    // -- Tests de createIndividualChat --

    @Test
    fun `createIndividualChat exitoso retorna chat`() = runTest {
        val chatDto = crearChatDto()
        whenever(chatsApi.createIndividualChat(any())).thenReturn(Response.success(chatDto))

        val resultado = repository.createIndividualChat("user-002")

        assertTrue("Debe ser exitoso", resultado.isSuccess)
        assertEquals("chat-001", resultado.getOrNull()?.id)
    }

    @Test
    fun `createIndividualChat con error retorna failure`() = runTest {
        val errorBody = """{"message": "Usuario no encontrado"}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        whenever(chatsApi.createIndividualChat(any())).thenReturn(Response.error(404, errorBody))

        val resultado = repository.createIndividualChat("user-inexistente")

        assertTrue("Debe fallar", resultado.isFailure)
    }

    // -- Tests de refreshChats --

    @Test
    fun `refreshChats exitoso retorna success`() = runTest {
        whenever(chatsApi.getChats()).thenReturn(Response.success(emptyList()))

        val resultado = repository.refreshChats()

        assertTrue("Debe ser exitoso", resultado.isSuccess)
    }

    @Test
    fun `refreshChats con error retorna failure`() = runTest {
        val errorBody = "Error".toResponseBody("text/plain".toMediaTypeOrNull())
        whenever(chatsApi.getChats()).thenReturn(Response.error(500, errorBody))

        val resultado = repository.refreshChats()

        assertTrue("Debe fallar", resultado.isFailure)
    }

    // -- Tests de mapeo de participantes --

    @Test
    fun `getChats mapea participantes correctamente`() = runTest {
        val chatDto = crearChatDto()
        whenever(chatsApi.getChats()).thenReturn(Response.success(listOf(chatDto)))

        val chats = repository.getChats().first()

        assertEquals(2, chats[0].participantes.size)
        assertEquals("Juan Test", chats[0].participantes[0].nombre)
    }
}
