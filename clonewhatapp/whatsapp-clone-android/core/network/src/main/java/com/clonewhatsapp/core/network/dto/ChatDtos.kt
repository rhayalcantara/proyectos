package com.clonewhatsapp.core.network.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO for chat data from backend
 * Matches backend ChatDTO
 */
data class ChatDto(
    @SerializedName("Id")
    val id: String,

    @SerializedName("Tipo")
    val tipo: String,

    @SerializedName("FechaCreacion")
    val fechaCreacion: String,

    @SerializedName("UltimaActividad")
    val ultimaActividad: String?,

    @SerializedName("NombreGrupo")
    val nombreGrupo: String?,

    @SerializedName("ImagenGrupo")
    val imagenGrupo: String?,

    @SerializedName("DescripcionGrupo")
    val descripcionGrupo: String?,

    @SerializedName("OtroParticipante")
    val otroParticipante: UsuarioDto?,

    @SerializedName("UltimoMensaje")
    val ultimoMensaje: MensajeDto?,

    @SerializedName("MensajesNoLeidos")
    val mensajesNoLeidos: Int,

    @SerializedName("Participantes")
    val participantes: List<ParticipanteDto>,

    @SerializedName("Silenciado")
    val silenciado: Boolean,

    @SerializedName("SilenciadoHasta")
    val silenciadoHasta: String?,

    @SerializedName("Archivado")
    val archivado: Boolean
)

/**
 * DTO for group/chat participant info
 * Matches backend ParticipanteDTO
 */
data class ParticipanteDto(
    @SerializedName("UsuarioId")
    val usuarioId: String,

    @SerializedName("Nombre")
    val nombre: String,

    @SerializedName("FotoPerfil")
    val fotoPerfil: String?,

    @SerializedName("Rol")
    val rol: String,

    @SerializedName("EstaEnLinea")
    val estaEnLinea: Boolean
)

/**
 * DTO for creating an individual chat
 * Matches backend CrearChatIndividualDTO
 */
data class CrearChatIndividualRequest(
    @SerializedName("ContactoId")
    val contactoId: String
)

/**
 * DTO for creating a group chat
 * Matches backend CrearGrupoDTO
 */
data class CrearGrupoRequest(
    @SerializedName("Nombre")
    val nombre: String,

    @SerializedName("Descripcion")
    val descripcion: String?,

    @SerializedName("ParticipantesIds")
    val participantesIds: List<String>
)

/**
 * DTO for updating a group
 * Matches backend ActualizarGrupoDTO
 */
data class ActualizarGrupoRequest(
    @SerializedName("Nombre")
    val nombre: String?,

    @SerializedName("Descripcion")
    val descripcion: String?
)

/**
 * DTO for adding participants to a group
 * Matches backend AgregarParticipantesDTO
 */
data class AgregarParticipantesRequest(
    @SerializedName("ParticipantesIds")
    val participantesIds: List<String>
)

/**
 * DTO for changing a participant's role
 * Matches backend CambiarRolDTO
 */
data class CambiarRolRequest(
    @SerializedName("UsuarioId")
    val usuarioId: String,

    @SerializedName("Rol")
    val rol: String
)

/**
 * DTO for muting a chat
 * Matches backend SilenciarChatDTO
 */
data class SilenciarChatRequest(
    @SerializedName("Silenciar")
    val silenciar: Boolean = true,

    @SerializedName("Duracion")
    val duracion: String? // "8h", "1w", "always"
)

/**
 * DTO for archiving a chat
 * Matches backend ArchivarChatDTO
 */
data class ArchivarChatRequest(
    @SerializedName("Archivar")
    val archivar: Boolean = true
)
