package com.clonewhatsapp.core.network.repository

import com.clonewhatsapp.core.network.api.MessagesApi
import com.clonewhatsapp.core.network.dto.MensajeDto
import com.clonewhatsapp.domain.model.EstadoMensaje
import com.clonewhatsapp.domain.model.TipoMensaje
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.Response

/**
 * Tests unitarios para MessageRepositoryImpl (T-122)
 *
 * Verifica:
 * - getMessages retorna Flow con mensajes mapeados
 * - sendMessage exitoso y fallido
 * - markAllAsRead exitoso y fallido
 * - Mapeo correcto de DTOs: tipos, estados, fechas
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MessageRepositoryImplTest {

    private lateinit var messagesApi: MessagesApi
    private lateinit var gson: Gson
    private lateinit var repository: MessageRepositoryImpl

    @Before
    fun setUp() {
        messagesApi = mock()
        gson = Gson()
        repository = MessageRepositoryImpl(messagesApi, gson)
    }

    // -- Helpers --

    private fun crearMensajeDto(
        id: String = "msg-001",
        chatId: String = "chat-001",
        remitenteId: String = "user-001",
        contenido: String? = "Hola mundo",
        tipo: String = "Texto",
        estado: String = "Enviado",
        fechaEnvio: String = "2026-01-15T12:30:00"
    ): MensajeDto = MensajeDto(
        id = id,
        chatId = chatId,
        remitenteId = remitenteId,
        remitenteNombre = "Juan Test",
        contenido = contenido,
        tipo = tipo,
        urlArchivo = null,
        nombreArchivo = null,
        tamanoArchivo = null,
        duracionSegundos = null,
        mensajeRespondidoId = null,
        mensajeRespondido = null,
        fechaEnvio = fechaEnvio,
        eliminado = false,
        eliminadoParaTodos = false,
        editado = false,
        estado = estado
    )

    // -- Tests de getMessages --

    @Test
    fun `getMessages exitoso retorna Flow con mensajes mapeados`() = runTest {
        val mensajeDtos = listOf(
            crearMensajeDto(id = "msg-1", contenido = "Hola"),
            crearMensajeDto(id = "msg-2", contenido = "Como estas?")
        )
        whenever(messagesApi.getMessages(any(), any(), any()))
            .thenReturn(Response.success(mensajeDtos))

        val mensajes = repository.getMessages("chat-001").first()

        assertEquals(2, mensajes.size)
        assertEquals("msg-1", mensajes[0].id)
        assertEquals("Hola", mensajes[0].contenido)
        assertEquals("chat-001", mensajes[0].chatId)
        assertEquals(TipoMensaje.TEXTO, mensajes[0].tipo)
    }

    @Test
    fun `getMessages con error de API retorna lista vacia`() = runTest {
        val errorBody = "Error".toResponseBody("text/plain".toMediaTypeOrNull())
        whenever(messagesApi.getMessages(any(), any(), any()))
            .thenReturn(Response.error(500, errorBody))

        val mensajes = repository.getMessages("chat-001").first()

        assertTrue("Lista debe estar vacia", mensajes.isEmpty())
    }

    @Test
    fun `getMessages con excepcion retorna lista vacia`() = runTest {
        whenever(messagesApi.getMessages(any(), any(), any()))
            .thenThrow(RuntimeException("Sin conexion"))

        val mensajes = repository.getMessages("chat-001").first()

        assertTrue("Lista debe estar vacia", mensajes.isEmpty())
    }

    // -- Tests de sendMessage --

    @Test
    fun `sendMessage exitoso retorna mensaje mapeado`() = runTest {
        val mensajeDto = crearMensajeDto()
        whenever(messagesApi.sendMessage(any())).thenReturn(Response.success(mensajeDto))

        val resultado = repository.sendMessage(
            chatId = "chat-001",
            contenido = "Hola mundo"
        )

        assertTrue("Envio debe ser exitoso", resultado.isSuccess)
        val mensaje = resultado.getOrNull()
        assertEquals("msg-001", mensaje?.id)
        assertEquals("Hola mundo", mensaje?.contenido)
        assertEquals("Juan Test", mensaje?.nombreRemitente)
    }

    @Test
    fun `sendMessage con error retorna failure`() = runTest {
        val errorBody = """{"message": "Chat no encontrado"}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        whenever(messagesApi.sendMessage(any()))
            .thenReturn(Response.error(404, errorBody))

        val resultado = repository.sendMessage(
            chatId = "chat-001",
            contenido = "Hola"
        )

        assertTrue("Envio debe fallar", resultado.isFailure)
    }

    @Test
    fun `sendMessage con respuesta vacia retorna failure`() = runTest {
        whenever(messagesApi.sendMessage(any()))
            .thenReturn(Response.success(null))

        val resultado = repository.sendMessage(
            chatId = "chat-001",
            contenido = "Hola"
        )

        assertTrue("Envio debe fallar con respuesta vacia", resultado.isFailure)
    }

    @Test
    fun `sendMessage con excepcion de red retorna failure`() = runTest {
        whenever(messagesApi.sendMessage(any())).thenThrow(RuntimeException("Timeout"))

        val resultado = repository.sendMessage(
            chatId = "chat-001",
            contenido = "Hola"
        )

        assertTrue("Envio debe fallar", resultado.isFailure)
    }

    // -- Tests de markAllAsRead --

    @Test
    fun `markAllAsRead exitoso retorna success`() = runTest {
        whenever(messagesApi.markAllAsRead(any()))
            .thenReturn(Response.success(Unit))

        val resultado = repository.markAllAsRead("chat-001")

        assertTrue("Debe ser exitoso", resultado.isSuccess)
    }

    @Test
    fun `markAllAsRead con error retorna failure`() = runTest {
        val errorBody = "Error".toResponseBody("text/plain".toMediaTypeOrNull())
        whenever(messagesApi.markAllAsRead(any()))
            .thenReturn(Response.error(500, errorBody))

        val resultado = repository.markAllAsRead("chat-001")

        assertTrue("Debe fallar", resultado.isFailure)
    }

    // -- Tests de mapeo de tipos --

    @Test
    fun `mapeo de tipo Imagen es correcto`() = runTest {
        val dto = crearMensajeDto(tipo = "Imagen")
        whenever(messagesApi.getMessages(any(), any(), any()))
            .thenReturn(Response.success(listOf(dto)))

        val mensajes = repository.getMessages("chat-001").first()

        assertEquals(TipoMensaje.IMAGEN, mensajes[0].tipo)
    }

    @Test
    fun `mapeo de tipo Audio es correcto`() = runTest {
        val dto = crearMensajeDto(tipo = "Audio")
        whenever(messagesApi.getMessages(any(), any(), any()))
            .thenReturn(Response.success(listOf(dto)))

        val mensajes = repository.getMessages("chat-001").first()

        assertEquals(TipoMensaje.AUDIO, mensajes[0].tipo)
    }

    @Test
    fun `mapeo de tipo Video es correcto`() = runTest {
        val dto = crearMensajeDto(tipo = "Video")
        whenever(messagesApi.getMessages(any(), any(), any()))
            .thenReturn(Response.success(listOf(dto)))

        val mensajes = repository.getMessages("chat-001").first()

        assertEquals(TipoMensaje.VIDEO, mensajes[0].tipo)
    }

    @Test
    fun `mapeo de tipo Documento es correcto`() = runTest {
        val dto = crearMensajeDto(tipo = "Documento")
        whenever(messagesApi.getMessages(any(), any(), any()))
            .thenReturn(Response.success(listOf(dto)))

        val mensajes = repository.getMessages("chat-001").first()

        assertEquals(TipoMensaje.DOCUMENTO, mensajes[0].tipo)
    }

    @Test
    fun `mapeo de tipo desconocido usa TEXTO por defecto`() = runTest {
        val dto = crearMensajeDto(tipo = "TipoInventado")
        whenever(messagesApi.getMessages(any(), any(), any()))
            .thenReturn(Response.success(listOf(dto)))

        val mensajes = repository.getMessages("chat-001").first()

        assertEquals(TipoMensaje.TEXTO, mensajes[0].tipo)
    }

    // -- Tests de mapeo de estados --

    @Test
    fun `mapeo de estado Entregado es correcto`() = runTest {
        val dto = crearMensajeDto(estado = "Entregado")
        whenever(messagesApi.getMessages(any(), any(), any()))
            .thenReturn(Response.success(listOf(dto)))

        val mensajes = repository.getMessages("chat-001").first()

        assertEquals(EstadoMensaje.ENTREGADO, mensajes[0].estado)
    }

    @Test
    fun `mapeo de estado Leido es correcto`() = runTest {
        val dto = crearMensajeDto(estado = "Leido")
        whenever(messagesApi.getMessages(any(), any(), any()))
            .thenReturn(Response.success(listOf(dto)))

        val mensajes = repository.getMessages("chat-001").first()

        assertEquals(EstadoMensaje.LEIDO, mensajes[0].estado)
    }

    // -- Tests de mapeo de fechas --

    @Test
    fun `mapeo de fecha ISO 8601 a milisegundos es correcto`() = runTest {
        val dto = crearMensajeDto(fechaEnvio = "2026-01-15T12:30:00")
        whenever(messagesApi.getMessages(any(), any(), any()))
            .thenReturn(Response.success(listOf(dto)))

        val mensajes = repository.getMessages("chat-001").first()

        // La fecha debe ser un timestamp positivo
        assertTrue("Fecha debe ser mayor que 0", mensajes[0].fechaEnvio > 0)
    }

    // -- Tests de deleteMessage --

    @Test
    fun `deleteMessage exitoso retorna success`() = runTest {
        whenever(messagesApi.deleteMessage(any(), any()))
            .thenReturn(Response.success(Unit))

        val resultado = repository.deleteMessage("msg-001", forEveryone = false)

        assertTrue("Debe ser exitoso", resultado.isSuccess)
    }

    @Test
    fun `deleteMessage con error retorna failure`() = runTest {
        val errorBody = "Error".toResponseBody("text/plain".toMediaTypeOrNull())
        whenever(messagesApi.deleteMessage(any(), any()))
            .thenReturn(Response.error(500, errorBody))

        val resultado = repository.deleteMessage("msg-001")

        assertTrue("Debe fallar", resultado.isFailure)
    }
}
