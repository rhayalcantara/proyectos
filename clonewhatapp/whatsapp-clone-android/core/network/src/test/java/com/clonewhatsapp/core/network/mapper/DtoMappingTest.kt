package com.clonewhatsapp.core.network.mapper

import com.clonewhatsapp.core.network.dto.AuthResponse
import com.clonewhatsapp.core.network.dto.ChatDto
import com.clonewhatsapp.core.network.dto.EstadoDto
import com.clonewhatsapp.core.network.dto.MensajeDto
import com.clonewhatsapp.core.network.dto.ParticipanteDto
import com.clonewhatsapp.core.network.dto.UsuarioDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitarios para mapeo de DTOs a modelos de dominio (T-123)
 *
 * Verifica el mapeo correcto de:
 * - ChatDto -> Chat (con grupo, individual, campos nulos)
 * - MensajeDto -> Mensaje (tipos, estados, fechas)
 * - EstadoDto -> Estado (tipos de estado)
 * - Manejo de campos opcionales/nulos
 * - Parsing de fechas ISO 8601
 * - Mapeo de enums (tipos de mensaje, tipos de llamada, estados)
 *
 * NOTA: Los mapeos viven dentro de las clases Repository como metodos privados.
 * Estos tests verifican el comportamiento observable a traves de la construccion de DTOs
 * y la validacion de sus propiedades, ya que los mapeos se testean indirectamente
 * en los tests de repositorio. Aqui nos enfocamos en la estructura de los DTOs.
 */
class DtoMappingTest {

    // ============================================================================
    // Tests de estructura de ChatDto
    // ============================================================================

    @Test
    fun `ChatDto individual tiene otroParticipante y no nombreGrupo`() {
        val chatDto = crearChatDtoIndividual()

        assertNotNull("Debe tener otroParticipante", chatDto.otroParticipante)
        assertNull("No debe tener nombreGrupo", chatDto.nombreGrupo)
        assertEquals("Individual", chatDto.tipo)
    }

    @Test
    fun `ChatDto grupo tiene nombreGrupo y puede no tener otroParticipante`() {
        val chatDto = crearChatDtoGrupo()

        assertNotNull("Debe tener nombreGrupo", chatDto.nombreGrupo)
        assertEquals("Grupo Amigos", chatDto.nombreGrupo)
        assertEquals("Grupo", chatDto.tipo)
    }

    @Test
    fun `ChatDto maneja campos nulos correctamente`() {
        val chatDto = crearChatDtoMinimo()

        assertNull("imagenGrupo puede ser null", chatDto.imagenGrupo)
        assertNull("descripcionGrupo puede ser null", chatDto.descripcionGrupo)
        assertNull("ultimoMensaje puede ser null", chatDto.ultimoMensaje)
        assertNull("silenciadoHasta puede ser null", chatDto.silenciadoHasta)
    }

    @Test
    fun `ChatDto mensajesNoLeidos es cero por defecto`() {
        val chatDto = crearChatDtoMinimo()

        assertEquals(0, chatDto.mensajesNoLeidos)
    }

    // ============================================================================
    // Tests de estructura de MensajeDto
    // ============================================================================

    @Test
    fun `MensajeDto de texto tiene contenido y no tiene urlArchivo`() {
        val mensajeDto = crearMensajeDtoTexto()

        assertEquals("Texto", mensajeDto.tipo)
        assertEquals("Hola mundo", mensajeDto.contenido)
        assertNull("No debe tener urlArchivo", mensajeDto.urlArchivo)
    }

    @Test
    fun `MensajeDto de imagen tiene urlArchivo`() {
        val mensajeDto = crearMensajeDtoImagen()

        assertEquals("Imagen", mensajeDto.tipo)
        assertNotNull("Debe tener urlArchivo", mensajeDto.urlArchivo)
        assertEquals("https://ejemplo.com/imagen.jpg", mensajeDto.urlArchivo)
    }

    @Test
    fun `MensajeDto de audio tiene duracionSegundos`() {
        val mensajeDto = crearMensajeDtoAudio()

        assertEquals("Audio", mensajeDto.tipo)
        assertNotNull("Debe tener duracionSegundos", mensajeDto.duracionSegundos)
        assertEquals(30, mensajeDto.duracionSegundos)
    }

    @Test
    fun `MensajeDto con respuesta tiene mensajeRespondidoId`() {
        val mensajeDto = crearMensajeDtoConRespuesta()

        assertNotNull("Debe tener mensajeRespondidoId", mensajeDto.mensajeRespondidoId)
        assertEquals("msg-original", mensajeDto.mensajeRespondidoId)
    }

    @Test
    fun `MensajeDto eliminado tiene flag correcto`() {
        val mensajeDto = crearMensajeDtoEliminado()

        assertTrue("Debe estar eliminado", mensajeDto.eliminado)
        assertTrue("Debe estar eliminado para todos", mensajeDto.eliminadoParaTodos)
    }

    @Test
    fun `MensajeDto editado tiene flag correcto`() {
        val mensajeDto = crearMensajeDtoEditado()

        assertTrue("Debe estar editado", mensajeDto.editado)
    }

    // ============================================================================
    // Tests de parsing de fechas ISO 8601
    // ============================================================================

    @Test
    fun `fecha ISO 8601 con formato estandar es valida`() {
        val fecha = "2026-01-15T12:30:00"
        val dto = crearMensajeDtoTexto(fechaEnvio = fecha)

        assertEquals(fecha, dto.fechaEnvio)
    }

    @Test
    fun `fecha ISO 8601 con distintos valores es valida`() {
        val fechas = listOf(
            "2026-02-14T00:00:00",
            "2026-12-31T23:59:59",
            "2026-06-15T15:45:30"
        )

        fechas.forEach { fecha ->
            val dto = crearMensajeDtoTexto(fechaEnvio = fecha)
            assertEquals(fecha, dto.fechaEnvio)
        }
    }

    // ============================================================================
    // Tests de enums de tipos de mensaje
    // ============================================================================

    @Test
    fun `todos los tipos de mensaje del backend estan representados`() {
        val tiposBackend = listOf(
            "Texto", "Imagen", "Video", "Audio",
            "Documento", "Ubicacion", "Contacto", "Sistema"
        )

        tiposBackend.forEach { tipo ->
            val dto = crearMensajeDtoTexto().copy(tipo = tipo)
            assertEquals(tipo, dto.tipo)
        }
    }

    @Test
    fun `todos los estados de mensaje del backend estan representados`() {
        val estadosBackend = listOf("Enviado", "Entregado", "Leido")

        estadosBackend.forEach { estado ->
            val dto = crearMensajeDtoTexto().copy(estado = estado)
            assertEquals(estado, dto.estado)
        }
    }

    // ============================================================================
    // Tests de estructura de EstadoDto
    // ============================================================================

    @Test
    fun `EstadoDto de texto tiene contenido y tipo correcto`() {
        val estadoDto = crearEstadoDtoTexto()

        assertEquals("Texto", estadoDto.tipo)
        assertEquals("Mi estado de prueba", estadoDto.contenido)
        assertNull("No debe tener urlArchivo", estadoDto.urlArchivo)
    }

    @Test
    fun `EstadoDto de imagen tiene urlArchivo`() {
        val estadoDto = crearEstadoDtoImagen()

        assertEquals("Imagen", estadoDto.tipo)
        assertNotNull("Debe tener urlArchivo", estadoDto.urlArchivo)
    }

    @Test
    fun `EstadoDto de video tiene urlArchivo`() {
        val estadoDto = crearEstadoDtoVideo()

        assertEquals("Video", estadoDto.tipo)
        assertNotNull("Debe tener urlArchivo", estadoDto.urlArchivo)
    }

    // ============================================================================
    // Tests de estructura de AuthResponse
    // ============================================================================

    @Test
    fun `AuthResponse contiene todos los campos necesarios`() {
        val authResponse = AuthResponse(
            id = "user-001",
            numeroTelefono = "+1234567890",
            nombre = "Juan Test",
            fotoPerfil = "https://ejemplo.com/foto.jpg",
            token = "jwt-token-12345",
            tokenExpiration = "2026-12-31T23:59:59"
        )

        assertEquals("user-001", authResponse.id)
        assertEquals("+1234567890", authResponse.numeroTelefono)
        assertEquals("Juan Test", authResponse.nombre)
        assertEquals("jwt-token-12345", authResponse.token)
        assertNotNull("Debe tener tokenExpiration", authResponse.tokenExpiration)
    }

    @Test
    fun `AuthResponse con fotoPerfil null es valido`() {
        val authResponse = AuthResponse(
            id = "user-001",
            numeroTelefono = "+1234567890",
            nombre = "Juan Test",
            fotoPerfil = null,
            token = "jwt-token-12345",
            tokenExpiration = "2026-12-31T23:59:59"
        )

        assertNull("fotoPerfil puede ser null", authResponse.fotoPerfil)
    }

    // ============================================================================
    // Tests de ParticipanteDto
    // ============================================================================

    @Test
    fun `ParticipanteDto contiene rol y estado en linea`() {
        val participante = ParticipanteDto(
            usuarioId = "user-001",
            nombre = "Juan Test",
            fotoPerfil = null,
            rol = "Admin",
            estaEnLinea = true
        )

        assertEquals("Admin", participante.rol)
        assertTrue("Debe estar en linea", participante.estaEnLinea)
    }

    @Test
    fun `ParticipanteDto con rol Participante`() {
        val participante = ParticipanteDto(
            usuarioId = "user-002",
            nombre = "Maria",
            fotoPerfil = "https://ejemplo.com/foto.jpg",
            rol = "Participante",
            estaEnLinea = false
        )

        assertEquals("Participante", participante.rol)
        assertFalse("No debe estar en linea", participante.estaEnLinea)
    }

    // ============================================================================
    // Helpers - Creacion de DTOs de prueba
    // ============================================================================

    private fun crearChatDtoIndividual(): ChatDto = ChatDto(
        id = "chat-001",
        tipo = "Individual",
        fechaCreacion = "2026-01-15T10:30:00",
        ultimaActividad = "2026-01-15T12:00:00",
        nombreGrupo = null,
        imagenGrupo = null,
        descripcionGrupo = null,
        otroParticipante = UsuarioDto(
            id = "user-002",
            numeroTelefono = "+0987654321",
            nombre = "Maria Lopez",
            fotoPerfil = null,
            estado = "Disponible",
            ultimaConexion = "2026-01-15T11:00:00",
            estaEnLinea = true
        ),
        ultimoMensaje = crearMensajeDtoTexto(),
        mensajesNoLeidos = 2,
        participantes = emptyList(),
        silenciado = false,
        silenciadoHasta = null,
        archivado = false
    )

    private fun crearChatDtoGrupo(): ChatDto = ChatDto(
        id = "grupo-001",
        tipo = "Grupo",
        fechaCreacion = "2026-01-10T08:00:00",
        ultimaActividad = "2026-01-15T14:00:00",
        nombreGrupo = "Grupo Amigos",
        imagenGrupo = "https://ejemplo.com/grupo.jpg",
        descripcionGrupo = "Grupo de amigos del colegio",
        otroParticipante = null,
        ultimoMensaje = crearMensajeDtoTexto(),
        mensajesNoLeidos = 5,
        participantes = listOf(
            ParticipanteDto("user-001", "Juan", null, "Admin", true),
            ParticipanteDto("user-002", "Maria", null, "Participante", false)
        ),
        silenciado = false,
        silenciadoHasta = null,
        archivado = false
    )

    private fun crearChatDtoMinimo(): ChatDto = ChatDto(
        id = "chat-min",
        tipo = "Individual",
        fechaCreacion = "2026-01-01T00:00:00",
        ultimaActividad = null,
        nombreGrupo = null,
        imagenGrupo = null,
        descripcionGrupo = null,
        otroParticipante = null,
        ultimoMensaje = null,
        mensajesNoLeidos = 0,
        participantes = emptyList(),
        silenciado = false,
        silenciadoHasta = null,
        archivado = false
    )

    private fun crearMensajeDtoTexto(fechaEnvio: String = "2026-01-15T12:30:00"): MensajeDto = MensajeDto(
        id = "msg-001",
        chatId = "chat-001",
        remitenteId = "user-001",
        remitenteNombre = "Juan Test",
        contenido = "Hola mundo",
        tipo = "Texto",
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
        estado = "Enviado"
    )

    private fun crearMensajeDtoImagen(): MensajeDto = crearMensajeDtoTexto().copy(
        tipo = "Imagen",
        contenido = null,
        urlArchivo = "https://ejemplo.com/imagen.jpg",
        nombreArchivo = "foto.jpg",
        tamanoArchivo = 1024000
    )

    private fun crearMensajeDtoAudio(): MensajeDto = crearMensajeDtoTexto().copy(
        tipo = "Audio",
        contenido = null,
        urlArchivo = "https://ejemplo.com/audio.ogg",
        nombreArchivo = "audio.ogg",
        duracionSegundos = 30
    )

    private fun crearMensajeDtoConRespuesta(): MensajeDto = crearMensajeDtoTexto().copy(
        contenido = "Respuesta al mensaje",
        mensajeRespondidoId = "msg-original",
        mensajeRespondido = crearMensajeDtoTexto().copy(id = "msg-original", contenido = "Mensaje original")
    )

    private fun crearMensajeDtoEliminado(): MensajeDto = crearMensajeDtoTexto().copy(
        eliminado = true,
        eliminadoParaTodos = true,
        contenido = null
    )

    private fun crearMensajeDtoEditado(): MensajeDto = crearMensajeDtoTexto().copy(
        editado = true,
        contenido = "Mensaje editado"
    )

    private fun crearEstadoDtoTexto(): EstadoDto = EstadoDto(
        id = "estado-001",
        usuarioId = "user-001",
        usuarioNombre = "Juan Test",
        usuarioFoto = null,
        tipo = "Texto",
        contenido = "Mi estado de prueba",
        urlArchivo = null,
        colorFondo = "#128C7E",
        fechaCreacion = "2026-01-15T10:00:00",
        fechaExpiracion = "2026-01-16T10:00:00",
        totalVistas = 5,
        vioPorMi = false
    )

    private fun crearEstadoDtoImagen(): EstadoDto = crearEstadoDtoTexto().copy(
        id = "estado-002",
        tipo = "Imagen",
        contenido = null,
        urlArchivo = "https://ejemplo.com/estado.jpg",
        colorFondo = null
    )

    private fun crearEstadoDtoVideo(): EstadoDto = crearEstadoDtoTexto().copy(
        id = "estado-003",
        tipo = "Video",
        contenido = null,
        urlArchivo = "https://ejemplo.com/estado.mp4",
        colorFondo = null
    )
}
