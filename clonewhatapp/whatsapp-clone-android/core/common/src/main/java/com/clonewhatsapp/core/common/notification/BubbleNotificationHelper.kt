package com.clonewhatsapp.core.common.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import timber.log.Timber
import java.net.URL

/**
 * Helper para notificaciones tipo Bubble (API de Burbujas de Android 11+).
 *
 * Se encarga de:
 * - Crear y gestionar burbujas de conversacion activas
 * - Verificar si las burbujas estan permitidas por el usuario
 * - Crear shortcuts de conversacion (requerido para Android 11+)
 * - Configurar BubbleMetadata con icono, intent y altura deseada
 * - Actualizar y cancelar burbujas por chatId
 *
 * Requiere SDK 30 (Android 11) o superior para funcionar.
 */
object BubbleNotificationHelper {

    // Altura deseada para la burbuja expandida (en dp)
    private const val ALTURA_BURBUJA_DP = 600

    // Rango base para IDs de notificacion de burbujas
    private const val RANGO_BASE_ID_BURBUJA = 5000

    // Extras para el intent de la burbuja
    const val EXTRA_CHAT_ID = "extra_chat_id_burbuja"
    const val EXTRA_NOMBRE_CONTACTO = "extra_nombre_contacto_burbuja"
    const val EXTRA_ES_BURBUJA = "extra_es_burbuja"

    // Categoria de shortcut para conversaciones
    private const val CATEGORIA_CONVERSACION = "com.clonewhatsapp.category.CONVERSACION"

    /**
     * Verifica si las burbujas estan permitidas en el dispositivo y por el usuario.
     *
     * @param contexto Contexto de la aplicacion
     * @return true si las burbujas estan permitidas, false en caso contrario
     */
    fun estanBurbujaPermitidas(contexto: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false

        val administrador = contexto.getSystemService(NotificationManager::class.java)
        return administrador.areBubblesAllowed()
    }

    /**
     * Crea o actualiza una burbuja de notificacion para una conversacion activa.
     *
     * @param contexto Contexto de la aplicacion
     * @param chatId Identificador unico del chat
     * @param nombreContacto Nombre del contacto o grupo
     * @param contenidoMensaje Ultimo mensaje de la conversacion
     * @param urlAvatar URL del avatar del contacto (opcional)
     * @param actividadBurbuja Clase de la actividad que se abrira dentro de la burbuja
     * @param iconoPequeno Recurso del icono pequeno de la notificacion
     */
    fun mostrarBurbuja(
        contexto: Context,
        chatId: String,
        nombreContacto: String,
        contenidoMensaje: String,
        urlAvatar: String? = null,
        actividadBurbuja: Class<*>,
        iconoPequeno: Int = android.R.drawable.ic_dialog_email
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Timber.d("Burbujas no soportadas en SDK < 30")
            return
        }

        if (!estanBurbujaPermitidas(contexto)) {
            Timber.d("Las burbujas no estan permitidas por el usuario")
            return
        }

        // Crear shortcut de conversacion (requerido para Android 11+)
        val shortcutId = crearShortcutConversacion(
            contexto = contexto,
            chatId = chatId,
            nombreContacto = nombreContacto,
            urlAvatar = urlAvatar,
            actividadBurbuja = actividadBurbuja
        )

        // Cargar avatar para el icono de la burbuja
        val bitmapAvatar = cargarAvatar(urlAvatar)
        val iconoBurbuja = if (bitmapAvatar != null) {
            IconCompat.createWithAdaptiveBitmap(bitmapAvatar)
        } else {
            IconCompat.createWithResource(contexto, android.R.drawable.ic_menu_myplaces)
        }

        // Crear persona remitente
        val personaRemitente = Person.Builder()
            .setName(nombreContacto)
            .setIcon(iconoBurbuja)
            .setImportant(true)
            .build()

        // Intent para la actividad dentro de la burbuja
        val intentBurbuja = crearIntentBurbuja(contexto, chatId, nombreContacto, actividadBurbuja)

        // Construir BubbleMetadata
        val metadatosBurbuja = crearBubbleMetadata(
            contexto = contexto,
            intentBurbuja = intentBurbuja,
            iconoBurbuja = iconoBurbuja,
            chatId = chatId
        )

        // Estilo de mensajeria para la notificacion
        val estiloMensajeria = NotificationCompat.MessagingStyle(personaRemitente)
            .setConversationTitle(nombreContacto)
            .addMessage(
                contenidoMensaje,
                System.currentTimeMillis(),
                personaRemitente
            )

        // Intent para abrir el chat al tocar la notificacion (fuera de burbuja)
        val intentAbrir = crearIntentAbrirChat(contexto, chatId, nombreContacto)

        // Construir notificacion con burbuja
        val notificacion = NotificationCompat.Builder(
            contexto,
            NotificationChannels.CANAL_MENSAJES
        )
            .setSmallIcon(iconoPequeno)
            .setContentTitle(nombreContacto)
            .setContentText(contenidoMensaje)
            .setStyle(estiloMensajeria)
            .setContentIntent(intentAbrir)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShortcutId(shortcutId)
            .setBubbleMetadata(metadatosBurbuja)
            .addPerson(personaRemitente)
            .build()

        // Mostrar la notificacion con burbuja
        val administrador = contexto.getSystemService(NotificationManager::class.java)
        val idNotificacion = generarIdNotificacionBurbuja(chatId)
        administrador.notify(idNotificacion, notificacion)

        Timber.d("Burbuja mostrada para chat: $chatId ($nombreContacto)")
    }

    /**
     * Actualiza el contenido de una burbuja existente.
     * Si la burbuja no existe, crea una nueva.
     *
     * @param contexto Contexto de la aplicacion
     * @param chatId Identificador unico del chat
     * @param nombreContacto Nombre del contacto
     * @param contenidoMensaje Nuevo contenido del mensaje
     * @param urlAvatar URL del avatar del contacto
     * @param actividadBurbuja Clase de la actividad de la burbuja
     * @param iconoPequeno Recurso del icono pequeno
     */
    fun actualizarBurbuja(
        contexto: Context,
        chatId: String,
        nombreContacto: String,
        contenidoMensaje: String,
        urlAvatar: String? = null,
        actividadBurbuja: Class<*>,
        iconoPequeno: Int = android.R.drawable.ic_dialog_email
    ) {
        // Reutiliza mostrarBurbuja ya que notify() con el mismo ID actualiza
        mostrarBurbuja(
            contexto = contexto,
            chatId = chatId,
            nombreContacto = nombreContacto,
            contenidoMensaje = contenidoMensaje,
            urlAvatar = urlAvatar,
            actividadBurbuja = actividadBurbuja,
            iconoPequeno = iconoPequeno
        )
    }

    /**
     * Cancela una burbuja de notificacion por chatId.
     *
     * @param contexto Contexto de la aplicacion
     * @param chatId Identificador del chat cuya burbuja se desea cancelar
     */
    fun cancelarBurbuja(contexto: Context, chatId: String) {
        val administrador = contexto.getSystemService(NotificationManager::class.java)
        val idNotificacion = generarIdNotificacionBurbuja(chatId)
        administrador.cancel(idNotificacion)
        Timber.d("Burbuja cancelada para chat: $chatId")
    }

    /**
     * Cancela todas las burbujas activas.
     *
     * @param contexto Contexto de la aplicacion
     */
    fun cancelarTodasLasBurbujas(contexto: Context) {
        val administrador = contexto.getSystemService(NotificationManager::class.java)
        administrador.cancelAll()
        Timber.d("Todas las burbujas canceladas")
    }

    // -----------------------------------------------------------------------
    // Metodos auxiliares privados
    // -----------------------------------------------------------------------

    /**
     * Crea un shortcut de conversacion requerido para las burbujas en Android 11+.
     * El shortcut permite al sistema asociar la burbuja con una conversacion.
     *
     * @return ID del shortcut creado
     */
    private fun crearShortcutConversacion(
        contexto: Context,
        chatId: String,
        nombreContacto: String,
        urlAvatar: String?,
        actividadBurbuja: Class<*>
    ): String {
        val shortcutId = "burbuja_$chatId"

        // Cargar icono del contacto
        val bitmapAvatar = cargarAvatar(urlAvatar)
        val iconoShortcut = if (bitmapAvatar != null) {
            IconCompat.createWithAdaptiveBitmap(bitmapAvatar)
        } else {
            IconCompat.createWithResource(contexto, android.R.drawable.ic_menu_myplaces)
        }

        // Persona asociada al shortcut
        val persona = Person.Builder()
            .setName(nombreContacto)
            .setIcon(iconoShortcut)
            .build()

        // Intent que abre la actividad de la burbuja
        val intentShortcut = Intent(contexto, actividadBurbuja).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_NOMBRE_CONTACTO, nombreContacto)
            putExtra(EXTRA_ES_BURBUJA, true)
        }

        // Construir el shortcut
        val shortcut = ShortcutInfoCompat.Builder(contexto, shortcutId)
            .setShortLabel(nombreContacto)
            .setLongLabel("Conversacion con $nombreContacto")
            .setIcon(iconoShortcut)
            .setIntent(intentShortcut)
            .setLongLived(true)
            .setCategories(setOf(CATEGORIA_CONVERSACION))
            .setPerson(persona)
            .build()

        // Publicar el shortcut
        ShortcutManagerCompat.pushDynamicShortcut(contexto, shortcut)

        Timber.d("Shortcut de conversacion creado: $shortcutId")
        return shortcutId
    }

    /**
     * Crea el BubbleMetadata con la configuracion de la burbuja.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun crearBubbleMetadata(
        contexto: Context,
        intentBurbuja: PendingIntent,
        iconoBurbuja: IconCompat,
        chatId: String
    ): NotificationCompat.BubbleMetadata {
        return NotificationCompat.BubbleMetadata.Builder(
            intentBurbuja,
            iconoBurbuja
        )
            .setDesiredHeight(ALTURA_BURBUJA_DP)
            .setAutoExpandBubble(false)
            .setSuppressNotification(false)
            .build()
    }

    /**
     * Crea el PendingIntent para la actividad que se muestra dentro de la burbuja.
     */
    private fun crearIntentBurbuja(
        contexto: Context,
        chatId: String,
        nombreContacto: String,
        actividadBurbuja: Class<*>
    ): PendingIntent {
        val intent = Intent(contexto, actividadBurbuja).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_NOMBRE_CONTACTO, nombreContacto)
            putExtra(EXTRA_ES_BURBUJA, true)
        }

        return PendingIntent.getActivity(
            contexto,
            chatId.hashCode() + RANGO_BASE_ID_BURBUJA,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Crea el PendingIntent para abrir el chat desde la notificacion (no burbuja).
     */
    private fun crearIntentAbrirChat(
        contexto: Context,
        chatId: String,
        nombreContacto: String
    ): PendingIntent {
        val intent = contexto.packageManager.getLaunchIntentForPackage(
            contexto.packageName
        )?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CHAT_ID, chatId)
            putExtra(EXTRA_NOMBRE_CONTACTO, nombreContacto)
        } ?: Intent()

        return PendingIntent.getActivity(
            contexto,
            chatId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Carga un avatar desde una URL.
     * Retorna null si la URL es nula o si ocurre un error.
     */
    private fun cargarAvatar(urlAvatar: String?): Bitmap? {
        if (urlAvatar.isNullOrBlank()) return null

        return try {
            val conexion = URL(urlAvatar).openStream()
            BitmapFactory.decodeStream(conexion)
        } catch (excepcion: Exception) {
            Timber.w(excepcion, "No se pudo cargar el avatar para burbuja: $urlAvatar")
            null
        }
    }

    /**
     * Genera un ID unico de notificacion para burbujas basado en el chatId.
     */
    private fun generarIdNotificacionBurbuja(chatId: String): Int {
        return RANGO_BASE_ID_BURBUJA + (chatId.hashCode() and 0x7FFFFFFF) % 10000
    }
}
