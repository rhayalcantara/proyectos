package com.clonewhatsapp.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clonewhatsapp.core.database.WhatsAppDatabase
import com.clonewhatsapp.core.database.entity.ChatEntity
import com.clonewhatsapp.core.database.entity.MensajeEntity
import com.clonewhatsapp.domain.model.EstadoMensaje
import com.clonewhatsapp.domain.model.SyncStatus
import com.clonewhatsapp.domain.model.TipoMensaje
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
 * Tests de integracion para MensajeDao con Room (T-125)
 *
 * Usa Room.inMemoryDatabaseBuilder para crear una base de datos en memoria.
 * Verifica las operaciones CRUD sobre la tabla de mensajes.
 *
 * NOTA: MensajeEntity tiene ForeignKey hacia ChatEntity,
 * por lo que se debe insertar un chat antes de insertar mensajes.
 */
@RunWith(AndroidJUnit4::class)
class MensajeDaoTest {

    private lateinit var database: WhatsAppDatabase
    private lateinit var mensajeDao: MensajeDao
    private lateinit var chatDao: ChatDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            WhatsAppDatabase::class.java
        ).allowMainThreadQueries().build()

        mensajeDao = database.mensajeDao()
        chatDao = database.chatDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // -- Helpers --

    private suspend fun insertarChatPadre(chatId: String = "chat-001") {
        chatDao.insertChat(
            ChatEntity(
                id = chatId,
                nombre = "Chat de Prueba",
                esGrupo = false
            )
        )
    }

    private fun crearMensajeEntity(
        id: String = "msg-001",
        chatId: String = "chat-001",
        remitenteId: String = "user-001",
        contenido: String? = "Mensaje de prueba",
        tipo: String = TipoMensaje.TEXTO.name,
        estado: String = EstadoMensaje.ENVIADO.name,
        fechaEnvio: Long = System.currentTimeMillis(),
        syncStatus: String = SyncStatus.PENDING.name
    ): MensajeEntity = MensajeEntity(
        id = id,
        chatId = chatId,
        remitenteId = remitenteId,
        contenido = contenido,
        tipo = tipo,
        estado = estado,
        fechaEnvio = fechaEnvio,
        syncStatus = syncStatus
    )

    // -- Tests de insercion y consulta --

    @Test
    fun insertarYConsultarMensaje() = runTest {
        insertarChatPadre()
        val mensaje = crearMensajeEntity()
        mensajeDao.insertMensaje(mensaje)

        val mensajes = mensajeDao.getMensajesByChatId("chat-001").first()

        assertEquals(1, mensajes.size)
        assertEquals("msg-001", mensajes[0].id)
        assertEquals("Mensaje de prueba", mensajes[0].contenido)
    }

    @Test
    fun insertarMultiplesMensajes() = runTest {
        insertarChatPadre()
        val mensajes = listOf(
            crearMensajeEntity(id = "msg-1", fechaEnvio = 1000L),
            crearMensajeEntity(id = "msg-2", fechaEnvio = 2000L),
            crearMensajeEntity(id = "msg-3", fechaEnvio = 3000L)
        )
        mensajeDao.insertMensajes(mensajes)

        val resultado = mensajeDao.getMensajesByChatId("chat-001").first()

        assertEquals(3, resultado.size)
    }

    // -- Tests de consulta por chatId --

    @Test
    fun getMensajesByChatIdRetornaSoloLosMensajesDelChat() = runTest {
        insertarChatPadre("chat-001")
        insertarChatPadre("chat-002")

        mensajeDao.insertMensaje(crearMensajeEntity(id = "msg-1", chatId = "chat-001"))
        mensajeDao.insertMensaje(crearMensajeEntity(id = "msg-2", chatId = "chat-001"))
        mensajeDao.insertMensaje(crearMensajeEntity(id = "msg-3", chatId = "chat-002"))

        val mensajesChat1 = mensajeDao.getMensajesByChatId("chat-001").first()
        val mensajesChat2 = mensajeDao.getMensajesByChatId("chat-002").first()

        assertEquals(2, mensajesChat1.size)
        assertEquals(1, mensajesChat2.size)
    }

    // -- Tests de ordenamiento por fecha --

    @Test
    fun mensajesOrdenadosPorFechaAscendente() = runTest {
        insertarChatPadre()
        mensajeDao.insertMensajes(
            listOf(
                crearMensajeEntity(id = "msg-3", contenido = "Tercero", fechaEnvio = 3000L),
                crearMensajeEntity(id = "msg-1", contenido = "Primero", fechaEnvio = 1000L),
                crearMensajeEntity(id = "msg-2", contenido = "Segundo", fechaEnvio = 2000L)
            )
        )

        val mensajes = mensajeDao.getMensajesByChatId("chat-001").first()

        assertEquals("Primero", mensajes[0].contenido)
        assertEquals("Segundo", mensajes[1].contenido)
        assertEquals("Tercero", mensajes[2].contenido)
    }

    // -- Tests de getMensajeById --

    @Test
    fun getMensajeByIdRetornaElMensajeCorrecto() = runTest {
        insertarChatPadre()
        mensajeDao.insertMensaje(crearMensajeEntity(id = "msg-test", contenido = "Test especifico"))

        val mensaje = mensajeDao.getMensajeById("msg-test")

        assertNotNull("Mensaje no debe ser null", mensaje)
        assertEquals("Test especifico", mensaje!!.contenido)
    }

    @Test
    fun getMensajeByIdRetornaNullSiNoExiste() = runTest {
        val mensaje = mensajeDao.getMensajeById("msg-inexistente")

        assertNull("Mensaje debe ser null", mensaje)
    }

    // -- Tests de eliminacion --

    @Test
    fun deleteMensajesByChatIdEliminaTodosLosMensajesDelChat() = runTest {
        insertarChatPadre()
        mensajeDao.insertMensajes(
            listOf(
                crearMensajeEntity(id = "msg-1"),
                crearMensajeEntity(id = "msg-2"),
                crearMensajeEntity(id = "msg-3")
            )
        )

        mensajeDao.deleteMensajesByChatId("chat-001")

        val mensajes = mensajeDao.getMensajesByChatId("chat-001").first()
        assertTrue("Lista debe estar vacia", mensajes.isEmpty())
    }

    // -- Tests de actualizacion --

    @Test
    fun updateMensajeModificaLosDatos() = runTest {
        insertarChatPadre()
        val original = crearMensajeEntity(id = "msg-001", contenido = "Original")
        mensajeDao.insertMensaje(original)

        val actualizado = original.copy(contenido = "Editado", editado = true)
        mensajeDao.updateMensaje(actualizado)

        val mensaje = mensajeDao.getMensajeById("msg-001")
        assertEquals("Editado", mensaje!!.contenido)
        assertTrue("Debe estar marcado como editado", mensaje.editado)
    }

    @Test
    fun updateSyncStatusCambiaElEstado() = runTest {
        insertarChatPadre()
        mensajeDao.insertMensaje(crearMensajeEntity(syncStatus = SyncStatus.PENDING.name))

        mensajeDao.updateSyncStatus("msg-001", SyncStatus.SYNCED.name)

        val mensaje = mensajeDao.getMensajeById("msg-001")
        assertEquals(SyncStatus.SYNCED.name, mensaje!!.syncStatus)
    }

    // -- Tests de mensajes pendientes --

    @Test
    fun getPendingMessagesRetornaSoloLosPendientes() = runTest {
        insertarChatPadre()
        mensajeDao.insertMensajes(
            listOf(
                crearMensajeEntity(id = "msg-1", syncStatus = SyncStatus.PENDING.name),
                crearMensajeEntity(id = "msg-2", syncStatus = SyncStatus.SYNCED.name),
                crearMensajeEntity(id = "msg-3", syncStatus = SyncStatus.PENDING.name)
            )
        )

        val pendientes = mensajeDao.getPendingMessages()

        assertEquals(2, pendientes.size)
    }

    // -- Tests de cascade delete --

    @Test
    fun eliminarChatEliminaSusMensajesPorCascade() = runTest {
        val chatEntity = ChatEntity(id = "chat-cascade", nombre = "Chat Cascade")
        chatDao.insertChat(chatEntity)
        mensajeDao.insertMensaje(crearMensajeEntity(id = "msg-cascade", chatId = "chat-cascade"))

        chatDao.deleteChat(chatEntity)

        val mensajes = mensajeDao.getMensajesByChatId("chat-cascade").first()
        assertTrue("Mensajes deben eliminarse por cascade", mensajes.isEmpty())
    }
}
