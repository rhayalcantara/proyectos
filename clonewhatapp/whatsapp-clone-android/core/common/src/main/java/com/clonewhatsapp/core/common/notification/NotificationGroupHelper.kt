package com.clonewhatsapp.core.common.notification

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Ayudante para agrupar notificaciones por chatId.
 *
 * Gestiona la agrupacion de notificaciones de mensajes para que
 * multiples mensajes del mismo chat aparezcan agrupados, y un
 * resumen muestre la cantidad total de mensajes y conversaciones.
 *
 * Utiliza NotificationCompat.InboxStyle para el resumen del grupo.
 */
object NotificationGroupHelper {

    private const val PREFIJO_GRUPO = "grupo_chat_"
    private const val ID_NOTIFICACION_RESUMEN = 0

    // Almacena los mensajes activos por chatId para el resumen
    private val mensajesPorChat = mutableMapOf<String, MutableList<String>>()

    /**
     * Obtiene la clave de grupo para un chatId especifico.
     *
     * @param chatId Identificador del chat
     * @return Clave de grupo para NotificationCompat
     */
    fun obtenerClaveGrupo(chatId: String): String {
        return "$PREFIJO_GRUPO$chatId"
    }

    /**
     * Registra un mensaje nuevo para el resumen de notificaciones.
     *
     * @param chatId Identificador del chat
     * @param nombreRemitente Nombre del remitente del mensaje
     * @param contenido Contenido del mensaje (sera truncado si es necesario)
     */
    fun registrarMensaje(chatId: String, nombreRemitente: String, contenido: String) {
        val listaChat = mensajesPorChat.getOrPut(chatId) { mutableListOf() }
        val textoResumen = "$nombreRemitente: $contenido"
        listaChat.add(textoResumen)
    }

    /**
     * Crea y muestra la notificacion de resumen del grupo.
     *
     * Muestra un resumen con estilo InboxStyle que incluye:
     * - Titulo con la cantidad de mensajes nuevos
     * - Subtitulo con la cantidad de conversaciones
     * - Lineas individuales con los ultimos mensajes
     *
     * @param contexto Contexto de la aplicacion
     * @param icono Recurso del icono pequeno de la notificacion
     */
    fun mostrarResumenGrupo(contexto: Context, icono: Int) {
        val totalMensajes = mensajesPorChat.values.sumOf { it.size }
        val totalConversaciones = mensajesPorChat.size

        if (totalMensajes == 0) return

        val textoTitulo = "$totalMensajes mensajes nuevos"
        val textoResumen = "$totalMensajes mensajes de $totalConversaciones conversaciones"

        val estiloInbox = NotificationCompat.InboxStyle()
            .setBigContentTitle(textoTitulo)
            .setSummaryText(textoResumen)

        // Agregar las ultimas lineas de cada chat al estilo inbox
        mensajesPorChat.values.forEach { mensajes ->
            mensajes.takeLast(LINEAS_MAXIMO_RESUMEN).forEach { linea ->
                estiloInbox.addLine(linea)
            }
        }

        val notificacionResumen = NotificationCompat.Builder(
            contexto,
            NotificationChannels.CANAL_MENSAJES
        )
            .setSmallIcon(icono)
            .setContentTitle(textoTitulo)
            .setContentText(textoResumen)
            .setStyle(estiloInbox)
            .setGroup(GRUPO_MENSAJES_GLOBAL)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setNumber(totalMensajes)
            .build()

        val administrador = contexto.getSystemService(
            NotificationManager::class.java
        )
        administrador.notify(ID_NOTIFICACION_RESUMEN, notificacionResumen)
    }

    /**
     * Limpia los mensajes registrados de un chat especifico.
     * Debe llamarse cuando el usuario abre el chat correspondiente.
     *
     * @param chatId Identificador del chat a limpiar
     */
    fun limpiarMensajesChat(chatId: String) {
        mensajesPorChat.remove(chatId)
    }

    /**
     * Limpia todos los mensajes registrados.
     * Debe llamarse al abrir la pantalla principal de chats.
     */
    fun limpiarTodosLosMensajes() {
        mensajesPorChat.clear()
    }

    /**
     * Cancela todas las notificaciones de un chat especifico.
     *
     * @param contexto Contexto de la aplicacion
     * @param chatId Identificador del chat
     */
    fun cancelarNotificacionesChat(contexto: Context, chatId: String) {
        limpiarMensajesChat(chatId)

        val administrador = contexto.getSystemService(NotificationManager::class.java)

        // Cancelar notificaciones del grupo del chat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            administrador.activeNotifications
                .filter { it.notification.group == obtenerClaveGrupo(chatId) }
                .forEach { administrador.cancel(it.id) }
        }

        // Actualizar resumen si quedan mensajes de otros chats
        if (mensajesPorChat.isNotEmpty()) {
            mostrarResumenGrupo(contexto, android.R.drawable.ic_dialog_email)
        } else {
            administrador.cancel(ID_NOTIFICACION_RESUMEN)
        }
    }

    // Clave de grupo global para el resumen
    private const val GRUPO_MENSAJES_GLOBAL = "grupo_todos_mensajes"

    // Maximo de lineas en el resumen del inbox
    private const val LINEAS_MAXIMO_RESUMEN = 5
}
