package com.clonewhatsapp.core.database.dao

import androidx.room.Dao
import androidx.room.Query

/**
 * Modelo de resultado de busqueda a nivel de base de datos.
 *
 * Contiene la informacion del mensaje junto con datos del chat
 * obtenidos mediante JOIN para evitar consultas adicionales.
 */
data class SearchResultEntity(
    val chatId: String,
    val chatName: String,
    val messageId: String,
    val contenido: String?,
    val fechaEnvio: Long,
    val remitenteNombre: String?
)

/**
 * DAO dedicado a consultas de busqueda de mensajes (T-131).
 *
 * Proporciona busqueda full-text en los mensajes almacenados localmente
 * en Room, con JOIN a la tabla de chats para obtener el nombre del chat.
 *
 * Las consultas estan optimizadas para:
 * - Buscar coincidencias parciales (LIKE con comodines)
 * - Limitar resultados a 50 para rendimiento
 * - Ordenar por fecha descendente (mas recientes primero)
 * - Excluir mensajes eliminados
 */
@Dao
interface SearchDao {

    /**
     * Busca mensajes cuyo contenido coincida parcialmente con la consulta.
     *
     * Realiza un JOIN con la tabla de chats para obtener el nombre del chat
     * y retorna los resultados ordenados por fecha descendente.
     *
     * @param query Texto a buscar (se busca con LIKE %query%)
     * @return Lista de resultados con informacion del chat y mensaje
     */
    @Query(
        """
        SELECT
            m.chatId AS chatId,
            c.nombre AS chatName,
            m.id AS messageId,
            m.contenido AS contenido,
            m.fechaEnvio AS fechaEnvio,
            m.nombreRemitente AS remitenteNombre
        FROM mensajes m
        INNER JOIN chats c ON m.chatId = c.id
        WHERE m.contenido LIKE '%' || :query || '%'
            AND m.eliminado = 0
        ORDER BY m.fechaEnvio DESC
        LIMIT 50
        """
    )
    suspend fun buscarMensajes(query: String): List<SearchResultEntity>

    /**
     * Busca mensajes dentro de un chat especifico.
     *
     * Similar a [buscarMensajes] pero filtrado a un solo chat.
     *
     * @param chatId Identificador del chat donde buscar
     * @param query Texto a buscar
     * @return Lista de resultados limitada a 50
     */
    @Query(
        """
        SELECT
            m.chatId AS chatId,
            c.nombre AS chatName,
            m.id AS messageId,
            m.contenido AS contenido,
            m.fechaEnvio AS fechaEnvio,
            m.nombreRemitente AS remitenteNombre
        FROM mensajes m
        INNER JOIN chats c ON m.chatId = c.id
        WHERE m.chatId = :chatId
            AND m.contenido LIKE '%' || :query || '%'
            AND m.eliminado = 0
        ORDER BY m.fechaEnvio DESC
        LIMIT 50
        """
    )
    suspend fun buscarMensajesEnChat(chatId: String, query: String): List<SearchResultEntity>

    /**
     * Cuenta la cantidad total de resultados para una consulta.
     * Util para mostrar la cantidad de coincidencias sin cargar todos los datos.
     *
     * @param query Texto a buscar
     * @return Cantidad de mensajes que coinciden con la consulta
     */
    @Query(
        """
        SELECT COUNT(*)
        FROM mensajes
        WHERE contenido LIKE '%' || :query || '%'
            AND eliminado = 0
        """
    )
    suspend fun contarResultados(query: String): Int
}
