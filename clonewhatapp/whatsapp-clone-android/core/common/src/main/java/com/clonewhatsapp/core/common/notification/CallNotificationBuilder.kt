package com.clonewhatsapp.core.common.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.provider.Settings
import androidx.core.app.NotificationCompat

/**
 * Constructor de notificaciones para llamadas entrantes y activas.
 *
 * Se encarga de:
 * - Crear notificaciones de pantalla completa para llamadas entrantes
 * - Crear notificaciones heads-up con alta prioridad
 * - Agregar acciones de Aceptar y Rechazar
 * - Crear notificaciones persistentes durante llamadas activas
 * - Usar el canal "llamadas" con importancia maxima
 */
object CallNotificationBuilder {

    // ID de notificacion para llamadas
    const val ID_NOTIFICACION_LLAMADA_ENTRANTE = 3001
    const val ID_NOTIFICACION_LLAMADA_ACTIVA = 3002

    // Acciones de los intents
    const val ACCION_ACEPTAR_LLAMADA = "com.clonewhatsapp.action.ACEPTAR_LLAMADA"
    const val ACCION_RECHAZAR_LLAMADA = "com.clonewhatsapp.action.RECHAZAR_LLAMADA"
    const val ACCION_FINALIZAR_LLAMADA = "com.clonewhatsapp.action.FINALIZAR_LLAMADA"

    // Extras para los intents
    const val EXTRA_ID_LLAMADA = "extra_id_llamada"
    const val EXTRA_NOMBRE_LLAMANTE = "extra_nombre_llamante"
    const val EXTRA_ES_VIDEO = "extra_es_video"
    const val EXTRA_MOSTRAR_LLAMADA_ENTRANTE = "extra_mostrar_llamada_entrante"

    // Request codes para PendingIntents
    private const val RC_PANTALLA_COMPLETA = 200
    private const val RC_ACEPTAR = 201
    private const val RC_RECHAZAR = 202
    private const val RC_FINALIZAR = 203

    /**
     * Construye y muestra una notificacion de llamada entrante.
     *
     * Crea una notificacion de pantalla completa (full-screen intent) con:
     * - Maximo prioridad para aparecer como heads-up
     * - Botones de Aceptar y Rechazar
     * - Sonido de tono de llamada
     * - Intent de pantalla completa para abrir IncomingCallScreen
     *
     * @param contexto Contexto de la aplicacion
     * @param idLlamada Identificador unico de la llamada
     * @param nombreLlamante Nombre del contacto que llama
     * @param esVideoLlamada Si es true, es una videollamada
     * @return La notificacion construida (util para servicios foreground)
     */
    fun construirNotificacionEntrante(
        contexto: Context,
        idLlamada: String,
        nombreLlamante: String,
        esVideoLlamada: Boolean = false
    ): Notification {
        val tipoLlamada = if (esVideoLlamada) "Videollamada" else "Llamada de voz"

        // Intent de pantalla completa para abrir la pantalla de llamada entrante
        val intentPantallaCompleta = crearIntentPantallaCompleta(
            contexto, idLlamada, nombreLlamante, esVideoLlamada
        )

        // Acciones: Aceptar y Rechazar
        val accionAceptar = crearAccionAceptar(contexto, idLlamada, nombreLlamante, esVideoLlamada)
        val accionRechazar = crearAccionRechazar(contexto, idLlamada)

        return NotificationCompat.Builder(contexto, NotificationChannels.CANAL_LLAMADAS)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("$tipoLlamada entrante")
            .setContentText(nombreLlamante)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(intentPantallaCompleta, true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Rechazar",
                accionRechazar
            )
            .addAction(
                android.R.drawable.ic_menu_call,
                "Aceptar",
                accionAceptar
            )
            .setSound(
                Settings.System.DEFAULT_RINGTONE_URI,
                AudioAttributes.AUDIO_CONTENT_TYPE_SONIFICATION
            )
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .build()
    }

    /**
     * Muestra la notificacion de llamada entrante en el sistema.
     */
    fun mostrarNotificacionEntrante(
        contexto: Context,
        idLlamada: String,
        nombreLlamante: String,
        esVideoLlamada: Boolean = false
    ) {
        val notificacion = construirNotificacionEntrante(
            contexto, idLlamada, nombreLlamante, esVideoLlamada
        )
        val administrador = contexto.getSystemService(NotificationManager::class.java)
        administrador.notify(ID_NOTIFICACION_LLAMADA_ENTRANTE, notificacion)
    }

    /**
     * Construye una notificacion persistente para llamada activa.
     *
     * Se muestra mientras la llamada esta en curso con:
     * - Cronometro mostrando la duracion
     * - Boton para finalizar la llamada
     * - Prioridad baja para no molestar
     *
     * @param contexto Contexto de la aplicacion
     * @param nombreLlamante Nombre del contacto en la llamada
     * @param duracion Texto con la duracion de la llamada
     * @param tiempoInicioLlamada Timestamp del inicio de la llamada (para cronometro)
     * @return La notificacion construida
     */
    fun construirNotificacionActiva(
        contexto: Context,
        nombreLlamante: String,
        duracion: String = "En llamada",
        tiempoInicioLlamada: Long = System.currentTimeMillis()
    ): Notification {
        val accionFinalizar = crearAccionFinalizar(contexto)

        return NotificationCompat.Builder(contexto, NotificationChannels.CANAL_LLAMADAS)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle(nombreLlamante)
            .setContentText("En llamada - $duracion")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setUsesChronometer(true)
            .setWhen(tiempoInicioLlamada)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Finalizar",
                accionFinalizar
            )
            .build()
    }

    /**
     * Cancela la notificacion de llamada entrante.
     */
    fun cancelarNotificacionEntrante(contexto: Context) {
        val administrador = contexto.getSystemService(NotificationManager::class.java)
        administrador.cancel(ID_NOTIFICACION_LLAMADA_ENTRANTE)
    }

    /**
     * Cancela la notificacion de llamada activa.
     */
    fun cancelarNotificacionActiva(contexto: Context) {
        val administrador = contexto.getSystemService(NotificationManager::class.java)
        administrador.cancel(ID_NOTIFICACION_LLAMADA_ACTIVA)
    }

    /**
     * Cancela todas las notificaciones de llamadas.
     */
    fun cancelarTodasLasNotificaciones(contexto: Context) {
        cancelarNotificacionEntrante(contexto)
        cancelarNotificacionActiva(contexto)
    }

    // -----------------------------------------------------------------------
    // Creacion de PendingIntents
    // -----------------------------------------------------------------------

    /**
     * Crea el PendingIntent de pantalla completa para la llamada entrante.
     * Abre la actividad principal con extras para mostrar la pantalla de llamada.
     */
    private fun crearIntentPantallaCompleta(
        contexto: Context,
        idLlamada: String,
        nombreLlamante: String,
        esVideoLlamada: Boolean
    ): PendingIntent {
        val intent = contexto.packageManager.getLaunchIntentForPackage(
            contexto.packageName
        )?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_MOSTRAR_LLAMADA_ENTRANTE, true)
            putExtra(EXTRA_ID_LLAMADA, idLlamada)
            putExtra(EXTRA_NOMBRE_LLAMANTE, nombreLlamante)
            putExtra(EXTRA_ES_VIDEO, esVideoLlamada)
        } ?: Intent()

        return PendingIntent.getActivity(
            contexto,
            RC_PANTALLA_COMPLETA,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Crea el PendingIntent para la accion de aceptar llamada.
     */
    private fun crearAccionAceptar(
        contexto: Context,
        idLlamada: String,
        nombreLlamante: String,
        esVideoLlamada: Boolean
    ): PendingIntent {
        val intent = Intent(ACCION_ACEPTAR_LLAMADA).apply {
            setPackage(contexto.packageName)
            putExtra(EXTRA_ID_LLAMADA, idLlamada)
            putExtra(EXTRA_NOMBRE_LLAMANTE, nombreLlamante)
            putExtra(EXTRA_ES_VIDEO, esVideoLlamada)
        }

        return PendingIntent.getBroadcast(
            contexto,
            RC_ACEPTAR,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Crea el PendingIntent para la accion de rechazar llamada.
     */
    private fun crearAccionRechazar(
        contexto: Context,
        idLlamada: String
    ): PendingIntent {
        val intent = Intent(ACCION_RECHAZAR_LLAMADA).apply {
            setPackage(contexto.packageName)
            putExtra(EXTRA_ID_LLAMADA, idLlamada)
        }

        return PendingIntent.getBroadcast(
            contexto,
            RC_RECHAZAR,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Crea el PendingIntent para la accion de finalizar llamada.
     */
    private fun crearAccionFinalizar(contexto: Context): PendingIntent {
        val intent = Intent(ACCION_FINALIZAR_LLAMADA).apply {
            setPackage(contexto.packageName)
        }

        return PendingIntent.getBroadcast(
            contexto,
            RC_FINALIZAR,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
