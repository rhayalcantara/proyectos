package com.clonewhatsapp.feature.chat.list

import com.clonewhatsapp.domain.model.Chat
import com.clonewhatsapp.domain.repository.ChatRepository
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests unitarios para ChatListViewModel (T-121)
 *
 * Verifica:
 * - Carga exitosa de chats
 * - Carga con error
 * - Refresh de chats
 * - Filtrado por busqueda
 * - Cierre de busqueda
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var chatRepository: ChatRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        chatRepository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -- Helpers --

    private fun crearChatsTest(): List<Chat> = listOf(
        Chat(id = "1", nombre = "Maria Lopez", ultimoMensaje = "Hola!", mensajesNoLeidos = 2),
        Chat(id = "2", nombre = "Pedro Garcia", ultimoMensaje = "Nos vemos manana"),
        Chat(id = "3", nombre = "Grupo Trabajo", esGrupo = true, ultimoMensaje = "Reunion a las 3")
    )

    private fun crearViewModel(): ChatListViewModel {
        return ChatListViewModel(chatRepository)
    }

    // -- Tests de carga de chats --

    @Test
    fun `cargar chats exitosamente actualiza el estado con la lista`() = runTest {
        // Configurar repositorio para emitir chats
        val chats = crearChatsTest()
        whenever(chatRepository.getChats()).thenReturn(flowOf(chats))

        val viewModel = crearViewModel()
        advanceUntilIdle()

        // Verificar estado
        val estado = viewModel.state.value
        assertEquals(3, estado.chats.size)
        assertEquals(3, estado.filteredChats.size)
        assertFalse("No debe estar cargando", estado.isLoading)
        assertNull("No debe haber error", estado.error)
    }

    @Test
    fun `cargar chats con lista vacia muestra estado vacio`() = runTest {
        whenever(chatRepository.getChats()).thenReturn(flowOf(emptyList()))

        val viewModel = crearViewModel()
        advanceUntilIdle()

        val estado = viewModel.state.value
        assertTrue("Lista de chats debe estar vacia", estado.chats.isEmpty())
        assertTrue("Lista filtrada debe estar vacia", estado.filteredChats.isEmpty())
        assertFalse("No debe estar cargando", estado.isLoading)
    }

    @Test
    fun `cargar chats con error emite mensaje de error`() = runTest {
        whenever(chatRepository.getChats()).thenReturn(
            flow { throw Exception("Error de conexion") }
        )

        val viewModel = crearViewModel()
        advanceUntilIdle()

        val estado = viewModel.state.value
        assertFalse("No debe estar cargando", estado.isLoading)
        assertTrue("Debe tener error", estado.error != null)
    }

    // -- Tests de refresh --

    @Test
    fun `refresh exitoso no muestra error`() = runTest {
        val chats = crearChatsTest()
        whenever(chatRepository.getChats()).thenReturn(flowOf(chats))
        whenever(chatRepository.refreshChats()).thenReturn(Result.success(Unit))

        val viewModel = crearViewModel()
        advanceUntilIdle()

        // Ejecutar refresh
        viewModel.onEvent(ChatListEvent.OnRefresh)
        advanceUntilIdle()

        val estado = viewModel.state.value
        assertFalse("No debe estar refrescando", estado.isRefreshing)
        assertNull("No debe haber error", estado.error)
    }

    @Test
    fun `refresh con error muestra mensaje de error`() = runTest {
        val chats = crearChatsTest()
        whenever(chatRepository.getChats()).thenReturn(flowOf(chats))
        whenever(chatRepository.refreshChats()).thenReturn(
            Result.failure(Exception("Sin conexion"))
        )

        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ChatListEvent.OnRefresh)
        advanceUntilIdle()

        val estado = viewModel.state.value
        assertFalse("No debe estar refrescando", estado.isRefreshing)
        assertTrue("Debe tener error", estado.error != null)
    }

    // -- Tests de busqueda --

    @Test
    fun `buscar por nombre filtra los chats correctamente`() = runTest {
        val chats = crearChatsTest()
        whenever(chatRepository.getChats()).thenReturn(flowOf(chats))

        val viewModel = crearViewModel()
        advanceUntilIdle()

        // Buscar "Maria"
        viewModel.onEvent(ChatListEvent.OnSearchQueryChange("Maria"))

        val estado = viewModel.state.value
        assertEquals("Maria", estado.searchQuery)
        assertEquals(1, estado.filteredChats.size)
        assertEquals("Maria Lopez", estado.filteredChats.first().nombre)
    }

    @Test
    fun `buscar por contenido de ultimo mensaje filtra correctamente`() = runTest {
        val chats = crearChatsTest()
        whenever(chatRepository.getChats()).thenReturn(flowOf(chats))

        val viewModel = crearViewModel()
        advanceUntilIdle()

        // Buscar por contenido de ultimo mensaje
        viewModel.onEvent(ChatListEvent.OnSearchQueryChange("Reunion"))

        val estado = viewModel.state.value
        assertEquals(1, estado.filteredChats.size)
        assertEquals("Grupo Trabajo", estado.filteredChats.first().nombre)
    }

    @Test
    fun `busqueda vacia muestra todos los chats`() = runTest {
        val chats = crearChatsTest()
        whenever(chatRepository.getChats()).thenReturn(flowOf(chats))

        val viewModel = crearViewModel()
        advanceUntilIdle()

        // Buscar algo y luego limpiar
        viewModel.onEvent(ChatListEvent.OnSearchQueryChange("Maria"))
        viewModel.onEvent(ChatListEvent.OnSearchQueryChange(""))

        val estado = viewModel.state.value
        assertEquals(3, estado.filteredChats.size)
    }

    @Test
    fun `busqueda sin resultados muestra lista vacia`() = runTest {
        val chats = crearChatsTest()
        whenever(chatRepository.getChats()).thenReturn(flowOf(chats))

        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ChatListEvent.OnSearchQueryChange("NoExiste"))

        val estado = viewModel.state.value
        assertTrue("Lista filtrada debe estar vacia", estado.filteredChats.isEmpty())
    }

    // -- Tests de eventos de UI --

    @Test
    fun `activar busqueda actualiza el estado`() = runTest {
        whenever(chatRepository.getChats()).thenReturn(flowOf(emptyList()))

        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.onEvent(ChatListEvent.OnSearchClick)

        assertTrue("Debe estar en modo busqueda", viewModel.state.value.isSearching)
    }

    @Test
    fun `cerrar busqueda restablece los chats y limpia la consulta`() = runTest {
        val chats = crearChatsTest()
        whenever(chatRepository.getChats()).thenReturn(flowOf(chats))

        val viewModel = crearViewModel()
        advanceUntilIdle()

        // Activar busqueda y buscar
        viewModel.onEvent(ChatListEvent.OnSearchClick)
        viewModel.onEvent(ChatListEvent.OnSearchQueryChange("Maria"))

        // Cerrar busqueda
        viewModel.onEvent(ChatListEvent.OnCloseSearch)

        val estado = viewModel.state.value
        assertFalse("No debe estar en modo busqueda", estado.isSearching)
        assertEquals("", estado.searchQuery)
        assertEquals(3, estado.filteredChats.size)
    }

    @Test
    fun `descartar error limpia el mensaje de error`() = runTest {
        val chats = crearChatsTest()
        whenever(chatRepository.getChats()).thenReturn(flowOf(chats))
        whenever(chatRepository.refreshChats()).thenReturn(
            Result.failure(Exception("Error"))
        )

        val viewModel = crearViewModel()
        advanceUntilIdle()

        // Provocar error
        viewModel.onEvent(ChatListEvent.OnRefresh)
        advanceUntilIdle()

        // Descartar error
        viewModel.onEvent(ChatListEvent.OnErrorDismiss)

        assertNull("Error debe ser null", viewModel.state.value.error)
    }
}
