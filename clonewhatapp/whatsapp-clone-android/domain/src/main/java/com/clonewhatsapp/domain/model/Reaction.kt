package com.clonewhatsapp.domain.model

/**
 * Modelo de reacción a un mensaje (T-133)
 *
 * Representa la reacción de un usuario con un emoji a un mensaje específico.
 *
 * @param id Identificador único de la reacción
 * @param emoji El emoji utilizado como reacción
 * @param userId ID del usuario que reaccionó
 * @param userName Nombre visible del usuario que reaccionó
 * @param messageId ID del mensaje al que se reaccionó
 * @param timestamp Momento en que se creó la reacción (milisegundos)
 */
data class Reaction(
    val id: String,
    val emoji: String,
    val userId: String,
    val userName: String,
    val messageId: String,
    val timestamp: Long
)

/**
 * Agrupación de reacciones por emoji.
 *
 * Permite mostrar un resumen compacto de cuántas reacciones de cada tipo
 * tiene un mensaje y si el usuario actual ya reaccionó con ese emoji.
 *
 * @param emoji El emoji de la agrupación
 * @param count Cantidad total de reacciones con este emoji
 * @param userNames Lista de nombres de usuarios que reaccionaron
 * @param reactedByMe Si el usuario actual ya reaccionó con este emoji
 */
data class ReactionGroup(
    val emoji: String,
    val count: Int,
    val userNames: List<String>,
    val reactedByMe: Boolean
)
