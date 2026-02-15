package com.clonewhatsapp.core.network.signalr

import com.clonewhatsapp.domain.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Gestiona la reconexión automática a SignalR con backoff exponencial.
 *
 * Observa el estado de conexión del SignalRManager y cuando detecta
 * una desconexión inesperada, inicia un ciclo de reintentos con
 * backoff exponencial y jitter aleatorio.
 */
@Singleton
class ReconnectionManager @Inject constructor(
    private val signalRManager: SignalRManager
) {

    companion object {
        private const val BACKOFF_INICIAL_MS = 1_000L
        private const val BACKOFF_MAXIMO_MS = 30_000L
        private const val MAX_REINTENTOS = 10
        private const val JITTER_MAXIMO_MS = 500L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()

    private var trabajoReconexion: Job? = null
    private var trabajoObservador: Job? = null
    private var backoffActualMs = BACKOFF_INICIAL_MS
    private var intentosRealizados = 0

    // Indica si la desconexión fue intencional (llamada explícita a disconnect)
    private var desconexionIntencional = false

    /**
     * Comienza a observar el estado de conexión para reconectar automáticamente.
     * Debe llamarse una vez al inicializar la aplicación.
     */
    fun iniciarObservacion() {
        trabajoObservador?.cancel()
        trabajoObservador = scope.launch {
            signalRManager.connectionState.collect { estado ->
                when (estado) {
                    ConnectionState.CONNECTED -> {
                        resetearBackoff()
                        _isReconnecting.value = false
                        desconexionIntencional = false
                    }
                    ConnectionState.DISCONNECTED -> {
                        if (!desconexionIntencional) {
                            startReconnecting()
                        }
                    }
                    else -> { /* No acción para CONNECTING y RECONNECTING */ }
                }
            }
        }
    }

    /**
     * Marca la siguiente desconexión como intencional para evitar reconexión automática.
     */
    fun marcarDesconexionIntencional() {
        desconexionIntencional = true
        stopReconnecting()
    }

    /**
     * Inicia el ciclo de reconexión con backoff exponencial.
     */
    fun startReconnecting() {
        if (_isReconnecting.value) return

        _isReconnecting.value = true
        intentosRealizados = 0
        backoffActualMs = BACKOFF_INICIAL_MS

        trabajoReconexion?.cancel()
        trabajoReconexion = scope.launch {
            while (intentosRealizados < MAX_REINTENTOS) {
                intentosRealizados++

                // Agregar jitter aleatorio al backoff
                val jitter = Random.nextLong(0, JITTER_MAXIMO_MS)
                val esperaTotal = backoffActualMs + jitter

                delay(esperaTotal)

                // Verificar si ya estamos conectados o si se canceló
                if (signalRManager.isConnected()) {
                    _isReconnecting.value = false
                    return@launch
                }

                try {
                    signalRManager.connect()

                    // Si la conexión fue exitosa, salir del bucle
                    if (signalRManager.isConnected()) {
                        _isReconnecting.value = false
                        return@launch
                    }
                } catch (e: Exception) {
                    // Incrementar backoff exponencialmente
                    backoffActualMs = (backoffActualMs * 2).coerceAtMost(BACKOFF_MAXIMO_MS)
                }
            }

            // Se agotaron los reintentos
            _isReconnecting.value = false
        }
    }

    /**
     * Detiene el ciclo de reconexión actual.
     */
    fun stopReconnecting() {
        trabajoReconexion?.cancel()
        trabajoReconexion = null
        _isReconnecting.value = false
        resetearBackoff()
    }

    /**
     * Resetea el backoff a su valor inicial.
     */
    private fun resetearBackoff() {
        backoffActualMs = BACKOFF_INICIAL_MS
        intentosRealizados = 0
    }
}
