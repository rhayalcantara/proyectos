package com.clonewhatsapp.core.common.media

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor del sensor de proximidad para llamadas de voz.
 *
 * Detecta cuando el telefono esta cerca del oido del usuario
 * y apaga la pantalla automaticamente usando un WakeLock de proximidad.
 * Esto evita toques accidentales durante una llamada de voz.
 */
@Singleton
class ProximitySensorManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val powerManager: PowerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val sensorProximidad: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private var wakeLock: PowerManager.WakeLock? = null
    private var estaActivo: Boolean = false

    private val _isNear = MutableStateFlow(false)

    /** Indica si el sensor detecta un objeto cercano (telefono cerca del oido). */
    val isNear: StateFlow<Boolean> = _isNear.asStateFlow()

    /**
     * Inicia la escucha del sensor de proximidad.
     * Adquiere un WakeLock de proximidad para apagar la pantalla
     * cuando el sensor detecta un objeto cercano.
     */
    fun start() {
        if (estaActivo) return

        if (sensorProximidad == null) {
            Log.w(TAG, "Sensor de proximidad no disponible en este dispositivo")
            return
        }

        // Crear WakeLock de proximidad
        @Suppress("DEPRECATION")
        wakeLock = powerManager.newWakeLock(
            PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
            "$TAG::ProximityWakeLock"
        )

        sensorManager.registerListener(
            this,
            sensorProximidad,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        estaActivo = true
        Log.d(TAG, "Sensor de proximidad iniciado")
    }

    /**
     * Detiene la escucha del sensor y libera el WakeLock.
     */
    fun stop() {
        if (!estaActivo) return

        sensorManager.unregisterListener(this)

        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
        wakeLock = null

        _isNear.value = false
        estaActivo = false
        Log.d(TAG, "Sensor de proximidad detenido")
    }

    // -----------------------------------------------------------------------
    // SensorEventListener
    // -----------------------------------------------------------------------

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_PROXIMITY) return

        val distancia = event.values[0]
        val distanciaMaxima = sensorProximidad?.maximumRange ?: 5f
        val estaCerca = distancia < distanciaMaxima

        _isNear.value = estaCerca

        if (estaCerca) {
            adquirirWakeLock()
        } else {
            liberarWakeLock()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No se requiere accion para cambios de precision
    }

    // -----------------------------------------------------------------------
    // WakeLock
    // -----------------------------------------------------------------------

    private fun adquirirWakeLock() {
        wakeLock?.let { lock ->
            if (!lock.isHeld) {
                lock.acquire(WAKELOCK_TIMEOUT_MS)
                Log.d(TAG, "WakeLock de proximidad adquirido - pantalla apagada")
            }
        }
    }

    private fun liberarWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
                Log.d(TAG, "WakeLock de proximidad liberado - pantalla encendida")
            }
        }
    }

    companion object {
        private const val TAG = "ProximitySensorManager"
        private const val WAKELOCK_TIMEOUT_MS = 60L * 60L * 1000L // 1 hora maximo
    }
}
