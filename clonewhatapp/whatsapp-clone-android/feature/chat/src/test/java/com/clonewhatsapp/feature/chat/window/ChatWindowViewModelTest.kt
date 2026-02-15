package com.clonewhatsapp.feature.chat.window

import androidx.lifecycle.SavedStateHandle
import com.clonewhatsapp.domain.model.Chat
import com.clonewhatsapp.domain.model.Mensaje
import com.clonewhatsapp.domain.model.TipoMensaje
import com.clonewhatsapp.domain.repository.AuthRepository
import com.clonewhatsapp.domain.repository.ChatRepository
import com.clonewhatsapp.domain.repository.MensajeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests unitarios para ChatWindowViewModel (T-121)
 *
 * Verifica:
 * - Carga de mensajes
 * - Envio de mensaje exitoso
 * - Envio de mensaje fallido
 * - Cambio de texto
 * - Descarte de errores
 * - Carga de informacion del chat
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatWindowViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var mensajeRepository: MensajeRepository
    private lateinit var chatRepository: ChatRepository
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mensajeRepository = mock()
        chatRepository = mock()
        authRepository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -- Helpers --

    private fun crearMensajesTest(): List<Mensaje> = listOf(
        Mensaje(id = "msg-1", chatId = "chat-001", remitenteId = "user-001", contenido = "Hola", fechaEnvio = 1000L),
        Mensaje(id = "msg-2", chatId = "chat-001", remitenteId = "user-002", contenido = "Que tal", fechaEnvio = 2000L),
        Mensaje(id = "msg-3", chatId = "chat-001", remitenteId = "user-001", contenido = "Bien", fechaEnvio = 3000L)
    )

    private fun crearChatTest(): Chat = Chat(
        id = "chat-001",
        nombre = "Pedro Garcia",
        fotoUrl = "https://ejemplo.com/foto.jpg"
    )

    private fun crearSavedStateHandle(chatId: String = "chat-001"): SavedStateHandle {
        return SavedStateHandle(mapOf("chatId" to chatId))
    }

    private suspend fun configurarRepositoriosPorDefecto() {
        whenever(authRepository.getCurrentUserId()).thenReturn("user-001")
        whenever(chatRepository.getChatById(any())).thenReturn(crearChatTest())
        whenever(mensajeRepository.getMessages(any(), any(), any()))
            .thenReturn(flowOf(crearMensajesTest()))
        whenever(mensajeRepository.markAllAsRead(any())).thenReturn(Result.success(Unit))
    }

    private suspend fun crearViewModel(chatId: String = "chat-001"): ChatWindowViewModel {
        configurarRepositoriosPorDefecto()
        return ChatWindowViewModel(
            mensajeRepository = mensajeRepository,
            chatRepository = chatRepository,
            authRepository = authRepository,
            savedStateHandle = crearSavedStateHandle(chatId)
        )
    }

    // -- Tests de carga de mensajes --

    @Test
    fun `cargar mensajes exitosamente actualiza el estado`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        val estado = viewModel.state.value
        assertEquals(3, estado.messages.size)
        assertFalse("No debe estar cargando", estado.isLoading)
        assertEquals("chat-001", estado.chatId)
        assertEquals("user-001", estado.currentUserId)
    }

    @Test
    fun `cargar mensajes ordena por fecha descendente`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        val estado = viewModel.state.value
        // Los mensajes deben estar ordenados por fechaEnvio descendente
        assertTrue(
            "El primer mensaje debe tener la fecha mas reciente",
            estado.messages.first().fechaEnvio >= estado.messages.last().fechaEnvio
        )
    }

    @Test
    fun `cargar informacion del chat actualiza nombre y foto`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        val estado = viewModel.state.value
        assertEquals("Pedro Garcia", estado.chatName)
        assertEquals("https://ejemplo.com/foto.jpg", estado.chatPhotoUrl)
    }

    @Test
    fun `cargar mensajes con error emite mensaje de error`() = runTest {
        whenever(authRepository.getCurrentUserId()).thenReturn("user-001")
        whenever(chatRepository.getChatById(any())).thenReturn(crearChatTest())
        whenever(mensajeRepository.getMessages(any(), any(), any()))
            .thenReturn(flow { throw Exception("Error de red") })
        whenever(mensajeRepository.markAllAsRead(any())).thenReturn(Result.success(Unit))

        val viewModel = ChatWindowViewModel(
            mensajeRepository = mensajeRepository,
            chatRepository = chatRepository,
            authRepository = authRepository,
            savedStateHandle = crearSavedStateHandle()
        )
        advanceUntilIdle()

        val estado = viewModel.state.value
        assertTrue("Debe tener error", estado.error != null)
    }

    // -- Tests de envio de mensaje --

    @Test
    fun `enviar mensaje exitosamente limpia el texto`() = runTest {
        val mensajeEnviado = Mensaje(
            id = "msg-new",
            chatId = "chat-001",
            remitenteId = "user-001",
            contenido = "Nuevo mensaje",
            fechaEnvio = System.currentTimeMillis()
        )
        whenever(mensajeRepository.sendMessage(any(), any(), any(), any()))
            .thenReturn(Result.success(mensajeEnviado))

        val viewModel = crearViewModel()
        advanceUntilIdle()

        // Escribir mensaje
        viewModel.onEvent(ChatWindowEvent.OnTextChange("Nuevo mensaje"))
        assertEquals("Nuevo mensaje", viewModel.state.value.messageText)

        // Enviar mensaje
        viewModel.onEvent(ChatWindowEvent.OnSendClick)
        advanceUntilIdle()

        val estado = viewModel.state.value
        assertEquals("", estado.messageText)
        assertFalse("No debe estar enviando", estado.isSending)
    }

    @Test
    fun `enviar mensaje con error restaura el texto`() = runTest {
        whenever(mensajeRepository.sendMessage(any(), any(), any(), any()))
            .thenReturn(Result.failure(Exception("Error al enviar")))

        val viewModel = crearViewModel()
        advanceUntilIdle()

        // Escribir mensaje
        viewModel.onEvent(ChatWindowEvent.OnTextChange("Mensaje importante"))

        // Enviar mensaje
        viewModel.onEvent(ChatWindowEvent.OnSendClick)
        advanceUntilIdle()

        val estado = viewModel.state.value
        assertEquals(
            "El texto debe restaurarse tras un fallo",
            "Mensaje importante",
            estado.messageText
        )
        assertFalse("No debe estar enviando", estado.isSending)
        assertTrue("Debe tener error", estado.error != null)
    }

    @Test
    fun `enviar mensaje vacio no hace nada`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        // No escribir nada e intentar enviar
        viewModel.onEvent(ChatWindowEvent.OnSendClick)
        advanceUntilIdle()

        // No debe haber llamado al repositorio
        val estado = viewModel.state.value
        assertFalse("No debe estar enviando", estado.isSending)
    }

    @Test
    fun `enviar mensaje con solo espacios no hace nada`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ChatWindowEvent.OnTextChange("   "))
        viewModel.onEvent(ChatWindowEvent.OnSendClick)
        advanceUntilIdle()

        assertFalse("No debe estar enviando", viewModel.state.value.isSending)
    }

    // -- Tests de cambio de texto --

    @Test
    fun `cambiar texto actualiza el estado`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ChatWindowEvent.OnTextChange("Hola mundo"))

        assertEquals("Hola mundo", viewModel.state.value.messageText)
    }

    // -- Tests de descarte de error --

    @Test
    fun `descartar error limpia el mensaje de error`() = runTest {
        whenever(mensajeRepository.sendMessage(any(), any(), any(), any()))
            .thenReturn(Result.failure(Exception("Error")))

        val viewModel = crearViewModel()
        advanceUntilIdle()

        // Provocar error
        viewModel.onEvent(ChatWindowEvent.OnTextChange("test"))
        viewModel.onEvent(ChatWindowEvent.OnSendClick)
        advanceUntilIdle()

        // Descartar error
        viewModel.onEvent(ChatWindowEvent.OnErrorDismiss)

        assertNull("Error debe ser null", viewModel.state.value.error)
    }

    // -- Tests con chatId vacio --

    @Test
    fun `chatId vacio no carga mensajes ni informacion del chat`() = runTest {
        whenever(authRepository.getCurrentUserId()).thenReturn("user-001")

        val viewModel = ChatWindowViewModel(
            mensajeRepository = mensajeRepository,
            chatRepository = chatRepository,
            authRepository = authRepository,
            savedStateHandle = SavedStateHandle(mapOf("chatId" to ""))
        )
        advanceUntilIdle()

        val estado = viewModel.state.value
        assertTrue("No debe tener mensajes", estado.messages.isEmpty())
        assertEquals("", estado.chatId)
    }

    // -- Tests de marcado como leido --

    @Test
    fun `al cargar mensajes se marcan como leidos`() = runTest {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        // Verificar que se llamo markAllAsRead
        verify(mensajeRepository).markAllAsRead("chat-001")
    }
}
