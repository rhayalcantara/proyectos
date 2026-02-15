package com.clonewhatsapp.core.common.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import timber.log.Timber
import java.net.URL

/**
 * Constructor de notificaciones para mensajes nuevos.
 *
 * Se encarga de:
 * - Construir notificaciones con nombre del remitente y preview del mensaje
 * - Cargar el avatar del remitente para el icono de la notificacion
 * - Crear PendingIntent para abrir el chat correspondiente
 * - Agrupar notificaciones por chatId
 * - Agregar accion de respuesta directa
 */
object MessageNotificationBuilder {

    // Longitud maxima del preview del mensaje
    private const val LONGITUD_MAXIMA_PREVIEW = 100

    // Rango base para IDs de notificacion de mensajes
    private const val RANGO_BASE_ID_NOTIFICACION = 1000

    // Clave para el RemoteInput de respuesta directa
    const val CLAVE_RESPUESTA_DIRECTA = "clave_respuesta_texto"

    // Extras para el intent del chat
    const val EXTRA_CHAT_ID = "extra_chat_id"
    const val EXTRA_NOMBRE_REMITENTE = "extra_nombre_remitente"
    const val EXTRA_MENSAJE_ID = "extra_mensaje_id"

    /**
     * Construye y muestra una notificacion para un mensaje nuevo.
     *
     * @param contexto Contexto de la aplicacion
     * @param chatId Identificador del chat
     * @param mensajeId Identificador del mensaje
     * @param nombreRemitente Nombre del remitente del mensaje
     * @param contenidoMensaje Texto del mensaje (se trunca a 100 caracteres)
     * @param urlAvatar URL del avatar del remitente (opcional)
     * @param iconoPequeno Recurso del icono pequeno de la notificacion
     */
    fun mostrarNotificacion(
        contexto: Context,
        chatId: String,
        mensajeId: String,
        nombreRemitente: String,
        contenidoMensaje: String,
        urlAvatar: String? = null,
        iconoPequeno: Int = android.R.drawable.ic_dialog_email
    ) {
        val contenidoTruncado = truncarContenido(contenidoMensaje)

        // Registrar mensaje en el helper de agrupacion
        NotificationGroupHelper.registrarMensaje(chatId, nombreRemitente, contenidoTruncado)

        // Cargar avatar del remitente (en hilo de fondo)
        val bitmapAvatar = cargarAvatar(urlAvatar)

        // Crear persona para el estilo de mensajeria
        val personaRemitente = Person.Builder()
            .setName(nombreRemitente)
            .apply {
                bitmapAvatar?.let { /* Se usa como icono grande */ }
            }
            .build()

        // Crear estilo de mensajeria
        val estiloMensajeria = NotificationCompat.MessagingStyle(personaRemitente)
            .setConversationTitle(nombreRemitente)
            .addMessage(
                contenidoTruncado,
                System.currentTimeMillis(),
                personaRemitente
            )

        // Intent para abrir el chat
        val intentAbrirChat = crearIntentAbrirChat(contexto, chatId, nombreRemitente)

        // Accion de respuesta directa
        val accionRespuesta = crearAccionRespuestaDirecta(contexto, chatId, nombreRemitente)

        // Accion de marcar como leido
        val accionMarcarLeido = crearAccionMarcarLeido(contexto, chatId)

        // ID de notificacion unico basado en el chatId
        val idNotificacion = generarIdNotificacion(chatId)

        // Construir la notificacion
        val notificacion = NotificationCompat.Builder(
            contexto,
            NotificationChannels.CANAL_MENSAJES
        )
            .setSmallIcon(iconoPequeno)
            .setContentTitle(nombreRemitente)
            .setContentText(contenidoTruncado)
            .setStyle(estiloMensajeria)
            .setContentIntent(intentAbrirChat)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setGroup(NotificationGroupHelper.obtenerClaveGrupo(chatId))
            .addAction(accionRespuesta)
            .addAction(accionMarcarLeido)
            .apply {
                bitmapAvatar?.let { setLargeIcon(it) }
            }
            .build()

        // Mostrar notificacion individual
        val administrador = contexto.getSystemService(NotificationManager::class.java)
        administrador.notify(idNotificacion, notificacion)

        // Mostrar/actualizar resumen del grupo
        NotificationGroupHelper.mostrarResumenGrupo(contexto, iconoPequeno)
    }

    // -----------------------------------------------------------------------
    // Metodos auxiliares privados
    // -----------------------------------------------------------------------

    /**
     * Trunca el contenido del mensaje si excede la longitud maxima.
     */
    private fun truncarContenido(contenido: String): String {
        return if (contenido.length > LONGITUD_MAXIMA_PREVIEW) {
            contenido.take(LONGITUD_MAXIMA_PREVIEW) + "..."
        } else {
            contenido
        }
    }

    /**
     * Carga el avatar del remitente desde una URL.
     * Retorna null si la URL es nula o si ocurre un error.
     */
    private fun cargarAvatar(urlAvatar: String?): Bitmap? {
        if (urlAvatar.isNullOrBlank()) return null

        return try {
            val conexion = URL(urlAvatar).openStream()
            BitmapFactory.decodeStream(conexion)
        } catch (excepcion: Exception) {
            Timber.w(excepcion, "No se pudo cargar el avatar: $urlAvatar")
            null
        }
    }

    /**
     * Crea el PendingIntent para abrir el chat al tocar la notificacion.
     */
    private fun crearIntentAbrirChat(
        contexto: Context,
        chatId: String,
        nombreRemitente: String
    ): PendingIntent {
        // Intent que abre la actividad principal con los datos del chat
        val intent = contexto.packageManager.getLaunchIntentForPackage(
            contexto.packageName
        )?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_NOMBRE_REMITENTE, nombreRemitente)
        } ?: Intent()

        return PendingIntent.getActivity(
            contexto,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Crea la accion de respuesta directa con RemoteInput.
     */
    private fun crearAccionRespuestaDirecta(
        contexto: Context,
        chatId: String,
        nombreRemitente: String
    ): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(CLAVE_RESPUESTA_DIRECTA)
            .setLabel("Responder a $nombreRemitente")
            .build()

        val intentRespuesta = Intent(
            "com.clonewhatsapp.action.RESPUESTA_DIRECTA"
        ).apply {
            setPackage(contexto.packageName)
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_NOMBRE_REMITENTE, nombreRemitente)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            contexto,
            chatId.hashCode() + 1,
            intentRespuesta,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Responder",
            pendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(true)
            .build()
    }

    /**
     * Crea la accion de marcar como leido.
     */
    private fun crearAccionMarcarLeido(
        contexto: Context,
        chatId: String
    ): NotificationCompat.Action {
        val intentMarcarLeido = Intent(
            "com.clonewhatsapp.action.MARCAR_LEIDO"
        ).apply {
            setPackage(contexto.packageName)
            putExtra(EXTRA_CHAT_ID, chatId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            contexto,
            chatId.hashCode() + 2,
            intentMarcarLeido,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_view,
            "Marcar como leido",
            pendingIntent
        ).build()
    }

    /**
     * Genera un ID de notificacion unico basado en el chatId.
     * Usa el hashCode del chatId + un rango base para evitar colisiones.
     */
    private fun generarIdNotificacion(chatId: String): Int {
        return RANGO_BASE_ID_NOTIFICACION + (chatId.hashCode() and 0x7FFFFFFF) % 10000
    }
}
