package com.clonewhatsapp.core.common.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings

/**
 * Gestiona la creacion de canales de notificacion para la aplicacion.
 *
 * Define los canales necesarios para diferentes tipos de notificaciones:
 * - Mensajes: notificaciones de chat con alta importancia
 * - Llamadas: notificaciones de llamadas entrantes con importancia maxima
 * - Estados: actualizaciones de estados con importancia por defecto
 * - General: notificaciones generales con baja importancia
 *
 * Los canales se crean en el inicio de la aplicacion (requiere SDK 26+).
 */
object NotificationChannels {

    // IDs de los canales
    const val CANAL_MENSAJES = "canal_mensajes"
    const val CANAL_LLAMADAS = "canal_llamadas"
    const val CANAL_ESTADOS = "canal_estados"
    const val CANAL_GENERAL = "canal_general"

    // IDs de grupo de notificaciones
    const val GRUPO_MENSAJES = "grupo_mensajes"

    /**
     * Crea todos los canales de notificacion necesarios para la aplicacion.
     * Debe llamarse en el onCreate() de la Application.
     * Solo tiene efecto en SDK 26 (Android O) o superior.
     *
     * @param contexto Contexto de la aplicacion
     */
    fun crearTodosLosCanales(contexto: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val administradorNotificaciones = contexto.getSystemService(
            NotificationManager::class.java
        )

        val canales = listOf(
            crearCanalMensajes(),
            crearCanalLlamadas(),
            crearCanalEstados(),
            crearCanalGeneral()
        )

        administradorNotificaciones.createNotificationChannels(canales)
    }

    /**
     * Canal para notificaciones de mensajes nuevos.
     * Alta importancia con vibracion y sonido por defecto.
     */
    private fun crearCanalMensajes(): NotificationChannel {
        return NotificationChannel(
            CANAL_MENSAJES,
            "Mensajes",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificaciones de mensajes nuevos"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 250, 250, 250)
            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            setShowBadge(true)
        }
    }

    /**
     * Canal para notificaciones de llamadas entrantes.
     * Importancia maxima con intent de pantalla completa.
     */
    private fun crearCanalLlamadas(): NotificationChannel {
        return NotificationChannel(
            CANAL_LLAMADAS,
            "Llamadas",
            NotificationManager.IMPORTANCE_MAX
        ).apply {
            description = "Notificaciones de llamadas entrantes de voz y video"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            setSound(
                Settings.System.DEFAULT_RINGTONE_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
            setBypassDnd(true)
        }
    }

    /**
     * Canal para notificaciones de actualizaciones de estados.
     * Importancia por defecto, sin vibracion intrusiva.
     */
    private fun crearCanalEstados(): NotificationChannel {
        return NotificationChannel(
            CANAL_ESTADOS,
            "Estados",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notificaciones de actualizaciones de estados de contactos"
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            setShowBadge(false)
        }
    }

    /**
     * Canal para notificaciones generales de la aplicacion.
     * Baja importancia, sin sonido ni vibracion.
     */
    private fun crearCanalGeneral(): NotificationChannel {
        return NotificationChannel(
            CANAL_GENERAL,
            "General",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificaciones generales de la aplicacion"
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
            setShowBadge(false)
        }
    }
}
