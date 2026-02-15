package com.clonewhatsapp.core.network.push

import com.clonewhatsapp.core.common.notification.CallNotificationBuilder
import com.clonewhatsapp.core.common.notification.MessageNotificationBuilder
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Servicio de Firebase Cloud Messaging para recibir notificaciones push.
 *
 * Gestiona dos eventos principales:
 * - onNewToken: Cuando Firebase genera un nuevo token FCM
 * - onMessageReceived: Cuando llega una notificacion push
 *
 * Tipos de notificacion soportados:
 * - "message": Mensaje nuevo de un chat
 * - "missed_call": Llamada perdida
 * - "incoming_call": Llamada entrante (pantalla completa)
 * - "status_update": Actualizacion de estado de un contacto
 *
 * Usa Hilt para inyectar el FcmTokenManager.
 */
@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // -----------------------------------------------------------------------
    // Refresco de token
    // -----------------------------------------------------------------------

    /**
     * Se invoca cuando Firebase genera un nuevo token FCM.
     * Registra el nuevo token en el backend.
     *
     * @param token Nuevo token FCM generado por Firebase
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.i("Nuevo token FCM recibido: ${token.take(10)}...")

        serviceScope.launch {
            fcmTokenManager.registrarTokenEspecifico(token)
        }
    }

    // -----------------------------------------------------------------------
    // Recepcion de mensajes
    // -----------------------------------------------------------------------

    /**
     * Se invoca cuando se recibe un mensaje push de Firebase.
     * Determina el tipo de notificacion y la enruta al constructor apropiado.
     *
     * @param mensajeRemoto Mensaje push recibido de Firebase
     */
    override fun onMessageReceived(mensajeRemoto: RemoteMessage) {
        super.onMessageReceived(mensajeRemoto)
        Timber.d("Mensaje push recibido de: ${mensajeRemoto.from}")

        val datos = mensajeRemoto.data

        if (datos.isEmpty()) {
            // Si no hay datos personalizados, intentar mostrar la notificacion basica
            mensajeRemoto.notification?.let { notificacion ->
                mostrarNotificacionBasica(
                    titulo = notificacion.title ?: "Nuevo mensaje",
                    cuerpo = notificacion.body ?: ""
                )
            }
            return
        }

        // Determinar tipo de notificacion y enrutar
        val tipoNotificacion = datos["type"] ?: datos["tipo"] ?: "message"

        when (tipoNotificacion) {
            "message", "mensaje" -> procesarMensajeNuevo(datos)
            "missed_call", "llamada_perdida" -> procesarLlamadaPerdida(datos)
            "incoming_call", "llamada_entrante" -> procesarLlamadaEntrante(datos)
            "status_update", "actualizacion_estado" -> procesarActualizacionEstado(datos)
            else -> {
                Timber.w("Tipo de notificacion desconocido: $tipoNotificacion")
                mostrarNotificacionBasica(
                    titulo = datos["title"] ?: datos["titulo"] ?: "Notificacion",
                    cuerpo = datos["body"] ?: datos["cuerpo"] ?: ""
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    // -----------------------------------------------------------------------
    // Procesadores por tipo de notificacion
    // -----------------------------------------------------------------------

    /**
     * Procesa una notificacion de mensaje nuevo.
     * Extrae los datos del mensaje y construye la notificacion con avatar y respuesta directa.
     */
    private fun procesarMensajeNuevo(datos: Map<String, String>) {
        val chatId = datos["chatId"] ?: datos["chat_id"] ?: return
        val mensajeId = datos["messageId"] ?: datos["mensaje_id"] ?: ""
        val nombreRemitente = datos["senderName"] ?: datos["nombre_remitente"] ?: "Desconocido"
        val contenido = datos["content"] ?: datos["contenido"] ?: "Nuevo mensaje"
        val urlAvatar = datos["avatarUrl"] ?: datos["url_avatar"]

        Timber.d("Mensaje nuevo de $nombreRemitente en chat $chatId")

        MessageNotificationBuilder.mostrarNotificacion(
            contexto = applicationContext,
            chatId = chatId,
            mensajeId = mensajeId,
            nombreRemitente = nombreRemitente,
            contenidoMensaje = contenido,
            urlAvatar = urlAvatar
        )
    }

    /**
     * Procesa una notificacion de llamada perdida.
     * Muestra una notificacion simple con el nombre del llamante.
     */
    private fun procesarLlamadaPerdida(datos: Map<String, String>) {
        val nombreLlamante = datos["callerName"] ?: datos["nombre_llamante"] ?: "Desconocido"
        val esVideo = datos["isVideo"]?.toBoolean()
            ?: datos["es_video"]?.toBoolean()
            ?: false

        val tipoLlamada = if (esVideo) "Videollamada" else "Llamada de voz"

        Timber.d("Llamada perdida de $nombreLlamante")

        mostrarNotificacionBasica(
            titulo = "$tipoLlamada perdida",
            cuerpo = nombreLlamante
        )
    }

    /**
     * Procesa una notificacion de llamada entrante.
     * Crea una notificacion de pantalla completa con acciones de aceptar/rechazar.
     */
    private fun procesarLlamadaEntrante(datos: Map<String, String>) {
        val idLlamada = datos["callId"] ?: datos["id_llamada"] ?: ""
        val nombreLlamante = datos["callerName"] ?: datos["nombre_llamante"] ?: "Desconocido"
        val esVideo = datos["isVideo"]?.toBoolean()
            ?: datos["es_video"]?.toBoolean()
            ?: false

        Timber.i("Llamada entrante de $nombreLlamante (video: $esVideo)")

        CallNotificationBuilder.mostrarNotificacionEntrante(
            contexto = applicationContext,
            idLlamada = idLlamada,
            nombreLlamante = nombreLlamante,
            esVideoLlamada = esVideo
        )
    }

    /**
     * Procesa una notificacion de actualizacion de estado.
     * Muestra una notificacion discreta en el canal de estados.
     */
    private fun procesarActualizacionEstado(datos: Map<String, String>) {
        val nombreContacto = datos["contactName"] ?: datos["nombre_contacto"] ?: "Un contacto"

        Timber.d("Actualizacion de estado de $nombreContacto")

        mostrarNotificacionBasica(
            titulo = "Actualizacion de estado",
            cuerpo = "$nombreContacto ha actualizado su estado"
        )
    }

    // -----------------------------------------------------------------------
    // Notificacion basica
    // -----------------------------------------------------------------------

    /**
     * Muestra una notificacion basica con titulo y cuerpo.
     * Usada como fallback para tipos de notificacion desconocidos.
     */
    private fun mostrarNotificacionBasica(titulo: String, cuerpo: String) {
        val notificacion = androidx.core.app.NotificationCompat.Builder(
            applicationContext,
            com.clonewhatsapp.core.common.notification.NotificationChannels.CANAL_GENERAL
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(cuerpo)
            .setAutoCancel(true)
            .build()

        val administrador = getSystemService(
            android.app.NotificationManager::class.java
        )
        administrador.notify(
            System.currentTimeMillis().toInt(),
            notificacion
        )
    }
}
