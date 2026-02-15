package com.clonewhatsapp.core.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.clonewhatsapp.core.database.WhatsAppDatabase
import com.clonewhatsapp.core.database.entity.MensajePendienteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Tests de integracion para MensajePendienteDao con Room (T-125)
 *
 * Usa Room.inMemoryDatabaseBuilder para crear una base de datos en memoria.
 * Verifica las operaciones sobre la tabla de mensajes pendientes (cola offline).
 */
@RunWith(AndroidJUnit4::class)
class MensajePendienteDaoTest {

    private lateinit var database: WhatsAppDatabase
    private lateinit var mensajePendienteDao: MensajePendienteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            WhatsAppDatabase::class.java
        ).allowMainThreadQueries().build()

        mensajePendienteDao = database.mensajePendienteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // -- Helpers --

    private fun crearMensajePendiente(
        id: String = UUID.randomUUID().toString(),
        chatId: String = "chat-001",
        contenido: String = "Mensaje pendiente",
        tipoMensaje: String = "Texto",
        estado: String = "pendiente",
        fechaCreacion: Long = System.currentTimeMillis(),
        intentos: Int = 0
    ): MensajePendienteEntity = MensajePendienteEntity(
        id = id,
        chatId = chatId,
        contenido = contenido,
        tipoMensaje = tipoMensaje,
        estado = estado,
        fechaCreacion = fechaCreacion,
        intentos = intentos
    )

    // -- Tests de insercion --

    @Test
    fun insertarMensajePendiente() = runTest {
        val mensaje = crearMensajePendiente(id = "pend-001")
        mensajePendienteDao.insertar(mensaje)

        val pendientes = mensajePendienteDao.obtenerPendientes().first()

        assertEquals(1, pendientes.size)
        assertEquals("pend-001", pendientes[0].id)
        assertEquals("Mensaje pendiente", pendientes[0].contenido)
    }

    @Test
    fun insertarConMismoIdReemplaza() = runTest {
        mensajePendienteDao.insertar(crearMensajePendiente(id = "pend-001", contenido = "Original"))
        mensajePendienteDao.insertar(crearMensajePendiente(id = "pend-001", contenido = "Reemplazado"))

        val pendientes = mensajePendienteDao.obtenerPendientes().first()

        assertEquals(1, pendientes.size)
        assertEquals("Reemplazado", pendientes[0].contenido)
    }

    // -- Tests de consulta de pendientes --

    @Test
    fun obtenerPendientesExcluyeEnviados() = runTest {
        mensajePendienteDao.insertar(crearMensajePendiente(id = "1", estado = "pendiente"))
        mensajePendienteDao.insertar(crearMensajePendiente(id = "2", estado = "enviado"))
        mensajePendienteDao.insertar(crearMensajePendiente(id = "3", estado = "fallido"))

        val pendientes = mensajePendienteDao.obtenerPendientes().first()

        assertEquals(
            "Debe retornar solo pendientes y fallidos (no enviados)",
            2,
            pendientes.size
        )
    }

    @Test
    fun obtenerPendientesOrdenadosPorFechaAscendente() = runTest {
        mensajePendienteDao.insertar(crearMensajePendiente(id = "3", contenido = "Tercero", fechaCreacion = 3000L))
        mensajePendienteDao.insertar(crearMensajePendiente(id = "1", contenido = "Primero", fechaCreacion = 1000L))
        mensajePendienteDao.insertar(crearMensajePendiente(id = "2", contenido = "Segundo", fechaCreacion = 2000L))

        val pendientes = mensajePendienteDao.obtenerPendientes().first()

        assertEquals("Primero", pendientes[0].contenido)
        assertEquals("Segundo", pendientes[1].contenido)
        assertEquals("Tercero", pendientes[2].contenido)
    }

    // -- Tests de obtenerParaEnviar --

    @Test
    fun obtenerParaEnviarRetornaPendientesYFallidos() = runTest {
        mensajePendienteDao.insertar(crearMensajePendiente(id = "1", estado = "pendiente"))
        mensajePendienteDao.insertar(crearMensajePendiente(id = "2", estado = "fallido"))
        mensajePendienteDao.insertar(crearMensajePendiente(id = "3", estado = "enviado"))
        mensajePendienteDao.insertar(crearMensajePendiente(id = "4", estado = "enviando"))

        val paraEnviar = mensajePendienteDao.obtenerParaEnviar()

        assertEquals(
            "Solo debe retornar pendientes y fallidos",
            2,
            paraEnviar.size
        )
    }

    @Test
    fun obtenerParaEnviarRespetaLimite() = runTest {
        (1..20).forEach { i ->
            mensajePendienteDao.insertar(
                crearMensajePendiente(
                    id = "pend-$i",
                    estado = "pendiente",
                    fechaCreacion = i.toLong()
                )
            )
        }

        val paraEnviar = mensajePendienteDao.obtenerParaEnviar(limite = 5)

        assertEquals(5, paraEnviar.size)
    }

    // -- Tests de actualizacion de estado --

    @Test
    fun actualizarEstadoCambiaElEstadoEIncrementaIntentos() = runTest {
        mensajePendienteDao.insertar(
            crearMensajePendiente(id = "pend-001", estado = "pendiente", intentos = 0)
        )

        mensajePendienteDao.actualizarEstado("pend-001", "fallido")

        // Obtener para verificar (como obtenerPendientes filtra enviados, esto funciona)
        val pendientes = mensajePendienteDao.obtenerPendientes().first()
        val mensaje = pendientes.find { it.id == "pend-001" }

        assertNotNull("Mensaje debe existir", mensaje)
        assertEquals("fallido", mensaje!!.estado)
        assertEquals(1, mensaje.intentos)
    }

    @Test
    fun actualizarEstadoAEnviadoLoExcluyeDePendientes() = runTest {
        mensajePendienteDao.insertar(crearMensajePendiente(id = "pend-001"))

        mensajePendienteDao.actualizarEstado("pend-001", "enviado")

        val pendientes = mensajePendienteDao.obtenerPendientes().first()
        assertTrue("No debe aparecer en pendientes", pendientes.isEmpty())
    }

    // -- Tests de eliminacion --

    @Test
    fun eliminarPorIdRemueveElMensaje() = runTest {
        mensajePendienteDao.insertar(crearMensajePendiente(id = "pend-001"))
        mensajePendienteDao.insertar(crearMensajePendiente(id = "pend-002"))

        mensajePendienteDao.eliminarPorId("pend-001")

        val pendientes = mensajePendienteDao.obtenerPendientes().first()
        assertEquals(1, pendientes.size)
        assertEquals("pend-002", pendientes[0].id)
    }

    @Test
    fun eliminarEntidadRemueveElMensaje() = runTest {
        val mensaje = crearMensajePendiente(id = "pend-001")
        mensajePendienteDao.insertar(mensaje)

        mensajePendienteDao.eliminar(mensaje)

        val pendientes = mensajePendienteDao.obtenerPendientes().first()
        assertTrue("Lista debe estar vacia", pendientes.isEmpty())
    }

    // -- Tests de conteo --

    @Test
    fun contarPendientesRetornaCantidadCorrecta() = runTest {
        mensajePendienteDao.insertar(crearMensajePendiente(id = "1", estado = "pendiente"))
        mensajePendienteDao.insertar(crearMensajePendiente(id = "2", estado = "fallido"))
        mensajePendienteDao.insertar(crearMensajePendiente(id = "3", estado = "enviado"))

        val count = mensajePendienteDao.contarPendientes().first()

        assertEquals(
            "Debe contar solo pendientes y fallidos",
            2,
            count
        )
    }

    @Test
    fun contarPendientesRetornaCeroCuandoNoHay() = runTest {
        val count = mensajePendienteDao.contarPendientes().first()

        assertEquals(0, count)
    }

    // -- Helpers adicionales para assertNotNull --

    private fun assertNotNull(message: String, value: Any?) {
        assertTrue(message, value != null)
    }
}
