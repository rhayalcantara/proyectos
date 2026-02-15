package com.clonewhatsapp.core.network.webrtc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.clonewhatsapp.domain.model.CallState
import com.clonewhatsapp.domain.repository.CallRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Servicio en primer plano para gestionar llamadas de audio y video.
 *
 * Mantiene notificaciones persistentes durante la llamada y gestiona
 * las acciones del usuario desde la notificacion (aceptar, rechazar, finalizar).
 */
@AndroidEntryPoint
class CallService : Service() {

    @Inject
    lateinit var callRepository: CallRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var durationJob: Job? = null
    private var callStartTime: Long = 0L
    private var callerName: String = ""
    private var isIncoming: Boolean = false

    private val callActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_ACCEPT_CALL -> {
                    serviceScope.launch {
                        callRepository.answerCall("", "")
                    }
                }
                ACTION_REJECT_CALL -> {
                    serviceScope.launch {
                        callRepository.rejectCall("", "Rechazada")
                    }
                }
                ACTION_END_CALL -> {
                    serviceScope.launch {
                        callRepository.endCall()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()
        registrarReceptorAcciones()
        observarEstadoLlamada()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        callerName = intent?.getStringExtra(EXTRA_CALLER_NAME) ?: "Desconocido"
        isIncoming = intent?.getBooleanExtra(EXTRA_IS_INCOMING, false) ?: false

        val notificacion = if (isIncoming) {
            crearNotificacionEntrante(callerName)
        } else {
            crearNotificacionSaliente(callerName)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notificacion,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notificacion)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        durationJob?.cancel()
        try {
            unregisterReceiver(callActionReceiver)
        } catch (_: IllegalArgumentException) {
            // Receptor no registrado, ignorar
        }
        serviceScope.cancel()
    }

    // -----------------------------------------------------------------------
    // Canal de notificacion
    // -----------------------------------------------------------------------

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID,
                "Llamadas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de llamadas de voz y video"
                setSound(null, null)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(canal)
        }
    }

    // -----------------------------------------------------------------------
    // Receptor de acciones
    // -----------------------------------------------------------------------

    private fun registrarReceptorAcciones() {
        val filtro = IntentFilter().apply {
            addAction(ACTION_ACCEPT_CALL)
            addAction(ACTION_REJECT_CALL)
            addAction(ACTION_END_CALL)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(callActionReceiver, filtro, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(callActionReceiver, filtro)
        }
    }

    // -----------------------------------------------------------------------
    // Observar estado de llamada
    // -----------------------------------------------------------------------

    private fun observarEstadoLlamada() {
        serviceScope.launch {
            callRepository.callState.collectLatest { estado ->
                when (estado) {
                    CallState.RINGING -> {
                        actualizarNotificacion(crearNotificacionEntrante(callerName))
                    }
                    CallState.CALLING -> {
                        actualizarNotificacion(crearNotificacionSaliente(callerName))
                    }
                    CallState.CONNECTING -> {
                        actualizarNotificacion(
                            crearNotificacionActiva(callerName, "Conectando...")
                        )
                    }
                    CallState.CONNECTED -> {
                        callStartTime = System.currentTimeMillis()
                        iniciarContadorDuracion()
                    }
                    CallState.ENDED, CallState.FAILED, CallState.REJECTED -> {
                        durationJob?.cancel()
                        stopSelf()
                    }
                    CallState.IDLE -> {
                        durationJob?.cancel()
                        stopSelf()
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Contador de duracion
    // -----------------------------------------------------------------------

    private fun iniciarContadorDuracion() {
        durationJob?.cancel()
        durationJob = serviceScope.launch {
            while (true) {
                val duracionMs = System.currentTimeMillis() - callStartTime
                val duracionTexto = formatearDuracion(duracionMs)
                actualizarNotificacion(
                    crearNotificacionActiva(callerName, duracionTexto)
                )
                delay(1_000L)
            }
        }
    }

    private fun formatearDuracion(milisegundos: Long): String {
        val totalSegundos = milisegundos / 1_000
        val horas = totalSegundos / 3_600
        val minutos = (totalSegundos % 3_600) / 60
        val segundos = totalSegundos % 60
        return if (horas > 0) {
            String.format("%d:%02d:%02d", horas, minutos, segundos)
        } else {
            String.format("%02d:%02d", minutos, segundos)
        }
    }

    // -----------------------------------------------------------------------
    // Notificaciones
    // -----------------------------------------------------------------------

    private fun actualizarNotificacion(notificacion: Notification) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notificacion)
    }

    private fun crearNotificacionEntrante(nombre: String): Notification {
        val fullScreenIntent = crearIntentPantallaCompleta()
        val aceptarIntent = crearPendingIntentAccion(ACTION_ACCEPT_CALL, REQUEST_CODE_ACCEPT)
        val rechazarIntent = crearPendingIntentAccion(ACTION_REJECT_CALL, REQUEST_CODE_REJECT)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Llamada entrante")
            .setContentText(nombre)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenIntent, true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Rechazar",
                rechazarIntent
            )
            .addAction(
                android.R.drawable.ic_menu_call,
                "Aceptar",
                aceptarIntent
            )
            .build()
    }

    private fun crearNotificacionSaliente(nombre: String): Notification {
        val cancelarIntent = crearPendingIntentAccion(ACTION_END_CALL, REQUEST_CODE_END)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Llamando...")
            .setContentText(nombre)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancelar",
                cancelarIntent
            )
            .build()
    }

    private fun crearNotificacionActiva(nombre: String, duracion: String): Notification {
        val finalizarIntent = crearPendingIntentAccion(ACTION_END_CALL, REQUEST_CODE_END)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(nombre)
            .setContentText("En llamada - $duracion")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setUsesChronometer(true)
            .setWhen(callStartTime)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Finalizar",
                finalizarIntent
            )
            .build()
    }

    private fun crearIntentPantallaCompleta(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_SHOW_INCOMING_CALL, true)
        } ?: Intent()

        return PendingIntent.getActivity(
            this,
            REQUEST_CODE_FULL_SCREEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun crearPendingIntentAccion(accion: String, requestCode: Int): PendingIntent {
        val intent = Intent(accion).setPackage(packageName)
        return PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val CHANNEL_ID = "call_channel"
        private const val NOTIFICATION_ID = 2001

        private const val ACTION_ACCEPT_CALL = "com.clonewhatsapp.action.ACCEPT_CALL"
        private const val ACTION_REJECT_CALL = "com.clonewhatsapp.action.REJECT_CALL"
        private const val ACTION_END_CALL = "com.clonewhatsapp.action.END_CALL"

        private const val EXTRA_CALLER_NAME = "extra_caller_name"
        private const val EXTRA_IS_INCOMING = "extra_is_incoming"
        const val EXTRA_SHOW_INCOMING_CALL = "extra_show_incoming_call"

        private const val REQUEST_CODE_ACCEPT = 100
        private const val REQUEST_CODE_REJECT = 101
        private const val REQUEST_CODE_END = 102
        private const val REQUEST_CODE_FULL_SCREEN = 103

        /**
         * Inicia el servicio para una llamada entrante.
         */
        fun startIncoming(context: Context, callerName: String) {
            val intent = Intent(context, CallService::class.java).apply {
                putExtra(EXTRA_CALLER_NAME, callerName)
                putExtra(EXTRA_IS_INCOMING, true)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Inicia el servicio para una llamada saliente.
         */
        fun startOutgoing(context: Context, callerName: String) {
            val intent = Intent(context, CallService::class.java).apply {
                putExtra(EXTRA_CALLER_NAME, callerName)
                putExtra(EXTRA_IS_INCOMING, false)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Detiene el servicio de llamadas.
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, CallService::class.java))
        }
    }
}
