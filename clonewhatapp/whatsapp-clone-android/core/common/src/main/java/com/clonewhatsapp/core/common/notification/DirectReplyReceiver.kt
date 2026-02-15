package com.clonewhatsapp.core.common.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import timber.log.Timber

/**
 * BroadcastReceiver para gestionar respuestas directas desde notificaciones.
 *
 * Cuando el usuario responde inline desde una notificacion de mensaje,
 * este receptor:
 * 1. Extrae el texto de la respuesta desde el RemoteInput
 * 2. Envia el mensaje a traves de un callback configurado
 * 3. Actualiza la notificacion mostrando confirmacion de envio
 *
 * Nota: La comunicacion con el repositorio se realiza a traves del callback
 * [configurarCallbackEnvio] que debe ser configurado por el modulo de red
 * al iniciar la aplicacion.
 */
class DirectReplyReceiver : BroadcastReceiver() {

    override fun onReceive(contexto: Context, intent: Intent) {
        // Extraer texto de la respuesta directa
        val resultadosRemoteInput = RemoteInput.getResultsFromIntent(intent)
        val textoRespuesta = resultadosRemoteInput
            ?.getCharSequence(MessageNotificationBuilder.CLAVE_RESPUESTA_DIRECTA)
            ?.toString()

        if (textoRespuesta.isNullOrBlank()) {
            Timber.w("Respuesta directa recibida pero el texto esta vacio")
            return
        }

        val chatId = intent.getStringExtra(MessageNotificationBuilder.EXTRA_CHAT_ID)
        val nombreRemitente = intent.getStringExtra(
            MessageNotificationBuilder.EXTRA_NOMBRE_REMITENTE
        ) ?: "Chat"

        if (chatId.isNullOrBlank()) {
            Timber.e("Respuesta directa sin chatId")
            return
        }

        Timber.d("Respuesta directa para chat $chatId: ${textoRespuesta.take(20)}...")

        // Intentar enviar el mensaje a traves del callback
        val enviado = callbackEnvio?.invoke(chatId, textoRespuesta) ?: false

        if (enviado) {
            // Actualizar notificacion mostrando confirmacion
            mostrarConfirmacionEnvio(contexto, chatId, nombreRemitente, textoRespuesta)
        } else {
            // Mostrar notificacion de error
            mostrarErrorEnvio(contexto, chatId, nombreRemitente)
        }
    }

    // -----------------------------------------------------------------------
    // Actualizar notificacion despues de respuesta
    // -----------------------------------------------------------------------

    /**
     * Actualiza la notificacion mostrando que el mensaje fue enviado.
     */
    private fun mostrarConfirmacionEnvio(
        contexto: Context,
        chatId: String,
        nombreRemitente: String,
        textoEnviado: String
    ) {
        val idNotificacion = generarIdNotificacion(chatId)

        val notificacionActualizada = NotificationCompat.Builder(
            contexto,
            NotificationChannels.CANAL_MENSAJES
        )
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(nombreRemitente)
            .setContentText("Tu: $textoEnviado")
            .setAutoCancel(true)
            .setTimeoutAfter(3000) // Desaparecer despues de 3 segundos
            .build()

        val administrador = contexto.getSystemService(NotificationManager::class.java)
        administrador.notify(idNotificacion, notificacionActualizada)
    }

    /**
     * Muestra notificacion de error si no se pudo enviar el mensaje.
     */
    private fun mostrarErrorEnvio(
        contexto: Context,
        chatId: String,
        nombreRemitente: String
    ) {
        val idNotificacion = generarIdNotificacion(chatId)

        val notificacionError = NotificationCompat.Builder(
            contexto,
            NotificationChannels.CANAL_MENSAJES
        )
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(nombreRemitente)
            .setContentText("No se pudo enviar el mensaje. Toca para reintentar.")
            .setAutoCancel(true)
            .build()

        val administrador = contexto.getSystemService(NotificationManager::class.java)
        administrador.notify(idNotificacion, notificacionError)
    }

    /**
     * Genera un ID de notificacion unico basado en el chatId.
     */
    private fun generarIdNotificacion(chatId: String): Int {
        return 1000 + (chatId.hashCode() and 0x7FFFFFFF) % 10000
    }

    companion object {
        /**
         * Accion del intent para respuestas directas.
         */
        const val ACCION_RESPUESTA_DIRECTA = "com.clonewhatsapp.action.RESPUESTA_DIRECTA"

        /**
         * Accion del intent para marcar mensajes como leidos.
         */
        const val ACCION_MARCAR_LEIDO = "com.clonewhatsapp.action.MARCAR_LEIDO"

        /**
         * Callback para enviar mensajes desde la respuesta directa.
         * Debe ser configurado por el modulo de red al iniciar la aplicacion.
         *
         * @param chatId Identificador del chat
         * @param texto Texto del mensaje a enviar
         * @return true si el mensaje fue encolado exitosamente
         */
        private var callbackEnvio: ((chatId: String, texto: String) -> Boolean)? = null

        /**
         * Callback para marcar mensajes como leidos.
         */
        private var callbackMarcarLeido: ((chatId: String) -> Unit)? = null

        /**
         * Configura el callback para enviar mensajes desde respuestas directas.
         * Debe llamarse al iniciar la aplicacion, tipicamente desde el modulo de red.
         *
         * @param callback Funcion que recibe chatId y texto, retorna true si fue exitoso
         */
        fun configurarCallbackEnvio(callback: (chatId: String, texto: String) -> Boolean) {
            callbackEnvio = callback
        }

        /**
         * Configura el callback para marcar mensajes como leidos.
         *
         * @param callback Funcion que recibe chatId
         */
        fun configurarCallbackMarcarLeido(callback: (chatId: String) -> Unit) {
            callbackMarcarLeido = callback
        }
    }
}
