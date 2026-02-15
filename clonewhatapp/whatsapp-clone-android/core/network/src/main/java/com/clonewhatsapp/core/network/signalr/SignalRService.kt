package com.clonewhatsapp.core.network.signalr

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.clonewhatsapp.domain.model.ConnectionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Servicio en primer plano que mantiene la conexión SignalR activa
 * mientras la aplicación está en segundo plano.
 */
@AndroidEntryPoint
class SignalRService : Service() {

    @Inject
    lateinit var signalRManager: SignalRManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val NOTIFICATION_ID = 9001
        private const val CHANNEL_ID = "signalr_channel"
        private const val CHANNEL_NAME = "Conexión en tiempo real"

        /**
         * Inicia el servicio en primer plano para mantener la conexión SignalR.
         */
        fun start(context: Context) {
            val intent = Intent(context, SignalRService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Detiene el servicio en primer plano.
         */
        fun stop(context: Context) {
            val intent = Intent(context, SignalRService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        crearCanalNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = construirNotificacion("Conectando...")

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )

        // Conectar a SignalR
        serviceScope.launch {
            signalRManager.connect()
        }

        // Observar cambios de estado de conexión para actualizar la notificación
        serviceScope.launch {
            signalRManager.connectionState.collect { estado ->
                val textoNotificacion = when (estado) {
                    ConnectionState.CONNECTED -> "Conectado"
                    ConnectionState.CONNECTING -> "Conectando..."
                    ConnectionState.RECONNECTING -> "Reconectando..."
                    ConnectionState.DISCONNECTED -> "Desconectado"
                }
                actualizarNotificacion(textoNotificacion)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.launch {
            signalRManager.disconnect()
        }.invokeOnCompletion {
            serviceScope.cancel()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Crea el canal de notificación requerido para Android O+.
     */
    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene la conexión en tiempo real activa"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(canal)
        }
    }

    /**
     * Construye la notificación del servicio en primer plano.
     */
    private fun construirNotificacion(texto: String): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("WhatsApp Clone")
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    /**
     * Actualiza el texto de la notificación existente.
     */
    private fun actualizarNotificacion(texto: String) {
        val notificacion = construirNotificacion(texto)
        notificationManager.notify(NOTIFICATION_ID, notificacion)
    }
}
