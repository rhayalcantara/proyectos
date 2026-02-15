package com.clonewhatsapp.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clonewhatsapp.core.database.WhatsAppDatabase
import com.clonewhatsapp.core.database.entity.ChatEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests de integracion para ChatDao con Room (T-125)
 *
 * Usa Room.inMemoryDatabaseBuilder para crear una base de datos en memoria.
 * Verifica las operaciones CRUD sobre la tabla de chats.
 */
@RunWith(AndroidJUnit4::class)
class ChatDaoTest {

    private lateinit var database: WhatsAppDatabase
    private lateinit var chatDao: ChatDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            WhatsAppDatabase::class.java
        ).allowMainThreadQueries().build()

        chatDao = database.chatDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // -- Helpers --

    private fun crearChatEntity(
        id: String = "chat-001",
        nombre: String = "Chat de Prueba",
        esGrupo: Boolean = false,
        fotoUrl: String? = null,
        ultimoMensaje: String? = "Hola",
        ultimoMensajeTiempo: Long? = System.currentTimeMillis(),
        mensajesNoLeidos: Int = 0
    ): ChatEntity = ChatEntity(
        id = id,
        nombre = nombre,
        esGrupo = esGrupo,
        fotoUrl = fotoUrl,
        ultimoMensaje = ultimoMensaje,
        ultimoMensajeTiempo = ultimoMensajeTiempo,
        mensajesNoLeidos = mensajesNoLeidos
    )

    // -- Tests de insercion y consulta --

    @Test
    fun insertarYConsultarChat() = runTest {
        val chat = crearChatEntity()
        chatDao.insertChat(chat)

        val chats = chatDao.getAllChats().first()

        assertEquals(1, chats.size)
        assertEquals("chat-001", chats[0].id)
        assertEquals("Chat de Prueba", chats[0].nombre)
    }

    @Test
    fun insertarMultiplesChatsYConsultar() = runTest {
        val chats = listOf(
            crearChatEntity(id = "chat-1", nombre = "Chat 1", ultimoMensajeTiempo = 3000L),
            crearChatEntity(id = "chat-2", nombre = "Chat 2", ultimoMensajeTiempo = 1000L),
            crearChatEntity(id = "chat-3", nombre = "Chat 3", ultimoMensajeTiempo = 2000L)
        )
        chatDao.insertChats(chats)

        val resultado = chatDao.getAllChats().first()

        assertEquals(3, resultado.size)
    }

    @Test
    fun getAllChatsOrdenadosPorUltimoMensajeTiempoDescendente() = runTest {
        val chats = listOf(
            crearChatEntity(id = "chat-1", nombre = "Antiguo", ultimoMensajeTiempo = 1000L),
            crearChatEntity(id = "chat-2", nombre = "Reciente", ultimoMensajeTiempo = 3000L),
            crearChatEntity(id = "chat-3", nombre = "Medio", ultimoMensajeTiempo = 2000L)
        )
        chatDao.insertChats(chats)

        val resultado = chatDao.getAllChats().first()

        assertEquals("Reciente", resultado[0].nombre)
        assertEquals("Medio", resultado[1].nombre)
        assertEquals("Antiguo", resultado[2].nombre)
    }

    // -- Tests de getChatById --

    @Test
    fun getChatByIdRetornaElChatCorrecto() = runTest {
        chatDao.insertChat(crearChatEntity(id = "chat-test", nombre = "Mi Chat"))

        val chat = chatDao.getChatById("chat-test").first()

        assertNotNull("Chat no debe ser null", chat)
        assertEquals("Mi Chat", chat!!.nombre)
    }

    @Test
    fun getChatByIdRetornaNullSiNoExiste() = runTest {
        val chat = chatDao.getChatById("chat-inexistente").first()

        assertNull("Chat debe ser null", chat)
    }

    // -- Tests de actualizacion --

    @Test
    fun actualizarChatModificaLosDatos() = runTest {
        val chatOriginal = crearChatEntity(id = "chat-001", nombre = "Nombre Original")
        chatDao.insertChat(chatOriginal)

        val chatActualizado = chatOriginal.copy(nombre = "Nombre Actualizado", mensajesNoLeidos = 5)
        chatDao.updateChat(chatActualizado)

        val chat = chatDao.getChatById("chat-001").first()

        assertEquals("Nombre Actualizado", chat!!.nombre)
        assertEquals(5, chat.mensajesNoLeidos)
    }

    // -- Tests de eliminacion --

    @Test
    fun eliminarChatLoRemoveDeLaBase() = runTest {
        val chat = crearChatEntity()
        chatDao.insertChat(chat)

        chatDao.deleteChat(chat)

        val chats = chatDao.getAllChats().first()
        assertTrue("Lista debe estar vacia despues de eliminar", chats.isEmpty())
    }

    // -- Tests de busqueda --

    @Test
    fun searchChatsFiltraPorNombre() = runTest {
        chatDao.insertChats(
            listOf(
                crearChatEntity(id = "1", nombre = "Maria Lopez"),
                crearChatEntity(id = "2", nombre = "Pedro Garcia"),
                crearChatEntity(id = "3", nombre = "Maria Fernandez")
            )
        )

        val resultado = chatDao.searchChats("Maria").first()

        assertEquals(2, resultado.size)
    }

    @Test
    fun searchChatsSinResultadosRetornaListaVacia() = runTest {
        chatDao.insertChat(crearChatEntity(nombre = "Juan"))

        val resultado = chatDao.searchChats("NoExiste").first()

        assertTrue("Lista debe estar vacia", resultado.isEmpty())
    }

    // -- Tests de conteo de no leidos --

    @Test
    fun getUnreadCountRetornaSumaTotal() = runTest {
        chatDao.insertChats(
            listOf(
                crearChatEntity(id = "1", mensajesNoLeidos = 3),
                crearChatEntity(id = "2", mensajesNoLeidos = 5),
                crearChatEntity(id = "3", mensajesNoLeidos = 0)
            )
        )

        val count = chatDao.getUnreadCount().first()

        assertEquals(8, count)
    }

    @Test
    fun getUnreadCountRetornaCeroCuandoNoHayChats() = runTest {
        val count = chatDao.getUnreadCount().first()

        assertEquals(0, count)
    }

    // -- Tests de Flow reactivo --

    @Test
    fun flowEmiteCuandoSeInsertaNuevoChat() = runTest {
        // Verificar que el Flow se actualiza al insertar
        val chatsInicial = chatDao.getAllChats().first()
        assertEquals(0, chatsInicial.size)

        chatDao.insertChat(crearChatEntity())

        val chatsActualizado = chatDao.getAllChats().first()
        assertEquals(1, chatsActualizado.size)
    }

    // -- Tests de REPLACE en conflicto --

    @Test
    fun insertConMismoIdReemplazaElChat() = runTest {
        chatDao.insertChat(crearChatEntity(id = "chat-001", nombre = "Original"))
        chatDao.insertChat(crearChatEntity(id = "chat-001", nombre = "Reemplazado"))

        val chats = chatDao.getAllChats().first()

        assertEquals(1, chats.size)
        assertEquals("Reemplazado", chats[0].nombre)
    }
}
