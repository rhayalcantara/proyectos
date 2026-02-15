package com.clonewhatsapp.core.network.sync

import com.clonewhatsapp.core.common.network.NetworkMonitor
import com.clonewhatsapp.core.database.dao.MensajePendienteDao
import com.clonewhatsapp.core.database.entity.MensajePendienteEntity
import com.clonewhatsapp.core.network.api.MessagesApi
import com.clonewhatsapp.core.network.dto.EnviarMensajeRequest
import com.clonewhatsapp.domain.model.EstadoSync
import com.clonewhatsapp.domain.model.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor de sincronizacion de mensajes pendientes.
 *
 * Observa el estado de la red y la cola de mensajes pendientes.
 * Cuando hay conectividad disponible, envía automaticamente los mensajes
 * pendientes con reintentos y backoff exponencial.
 *
 * T-093: Cola de mensajes offline
 * T-094: Sincronizacion automatica
 * T-097: Reintentos con backoff exponencial
 */
@Singleton
class SyncManager @Inject constructor(
    private val mensajePendienteDao: MensajePendienteDao,
    private val messagesApi: MessagesApi,
    private val networkMonitor: NetworkMonitor
) {
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _estadoSync = MutableStateFlow(SyncState())

    /** Estado reactivo de la sincronizacion */
    val estadoSync: StateFlow<SyncState> = _estadoSync.asStateFlow()

    private var sincronizando = false

    /** Delay base para backoff exponencial en milisegundos */
    private val delayBaseMs = 1_000L

    /** Delay maximo para backoff exponencial en milisegundos */
    private val delayMaximoMs = 16_000L

    /**
     * Inicia la sincronizacion.
     * Observa el estado de la red y los mensajes pendientes.
     * Cuando la red esta disponible, intenta enviar los mensajes pendientes.
     */
    fun iniciarSync() {
        Timber.d("Iniciando SyncManager")

        // Iniciar monitoreo de red
        networkMonitor.iniciarMonitoreo()

        // Observar cambios en la conectividad
        alcance.launch {
            networkMonitor.estaEnLinea.collect { enLinea ->
                Timber.d("Estado de red: enLinea=$enLinea")
                if (enLinea) {
                    procesarMensajesPendientes()
                }
            }
        }

        // Observar cantidad de mensajes pendientes para actualizar el estado
        alcance.launch {
            mensajePendienteDao.contarPendientes().collect { cantidad ->
                _estadoSync.value = _estadoSync.value.copy(
                    cantidadPendientes = cantidad
                )
            }
        }
    }

    /**
     * Procesa todos los mensajes pendientes de la cola.
     * Envia cada mensaje al servidor con reintentos y backoff exponencial.
     */
    suspend fun procesarMensajesPendientes() {
        if (sincronizando) {
            Timber.d("Ya se esta sincronizando, ignorando solicitud")
            return
        }

        sincronizando = true
        _estadoSync.value = _estadoSync.value.copy(estado = EstadoSync.SINCRONIZANDO)

        try {
            var mensajesPendientes = mensajePendienteDao.obtenerParaEnviar(10)

            while (mensajesPendientes.isNotEmpty()) {
                for (mensaje in mensajesPendientes) {
                    // Saltar mensajes que excedieron el maximo de intentos
                    if (mensaje.intentos >= mensaje.maxIntentos) {
                        Timber.w("Mensaje ${mensaje.id} excedio maximo de intentos (${mensaje.maxIntentos})")
                        continue
                    }

                    enviarMensaje(mensaje)
                }

                // Obtener siguiente lote
                mensajesPendientes = mensajePendienteDao.obtenerParaEnviar(10)
            }

            _estadoSync.value = _estadoSync.value.copy(
                estado = EstadoSync.INACTIVO,
                ultimaSincronizacion = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Timber.e(e, "Error durante la sincronizacion")
            _estadoSync.value = _estadoSync.value.copy(estado = EstadoSync.ERROR)
        } finally {
            sincronizando = false
        }
    }

    /**
     * Envia un mensaje individual al servidor.
     * Implementa backoff exponencial en caso de fallo.
     *
     * @param mensaje Entidad del mensaje pendiente a enviar.
     */
    private suspend fun enviarMensaje(mensaje: MensajePendienteEntity) {
        try {
            // Marcar como enviando
            mensajePendienteDao.actualizarEstado(mensaje.id, "enviando")

            val request = EnviarMensajeRequest(
                chatId = mensaje.chatId,
                contenido = mensaje.contenido,
                tipo = mensaje.tipoMensaje,
                mensajeRespondidoId = null
            )

            val respuesta = messagesApi.sendMessage(request)

            if (respuesta.isSuccessful) {
                // Envio exitoso: eliminar de la cola
                mensajePendienteDao.eliminarPorId(mensaje.id)
                Timber.d("Mensaje ${mensaje.id} enviado exitosamente")
            } else {
                // Error del servidor: marcar como fallido
                mensajePendienteDao.actualizarEstado(mensaje.id, "fallido")
                Timber.w("Error al enviar mensaje ${mensaje.id}: ${respuesta.code()}")

                // Aplicar backoff exponencial
                val delayMs = calcularBackoff(mensaje.intentos)
                Timber.d("Esperando ${delayMs}ms antes del proximo reintento")
                delay(delayMs)
            }
        } catch (e: Exception) {
            // Error de red u otro: marcar como fallido
            Timber.e(e, "Excepcion al enviar mensaje ${mensaje.id}")
            try {
                mensajePendienteDao.actualizarEstado(mensaje.id, "fallido")
            } catch (dbError: Exception) {
                Timber.e(dbError, "Error al actualizar estado del mensaje ${mensaje.id}")
            }

            // Aplicar backoff exponencial
            val delayMs = calcularBackoff(mensaje.intentos)
            delay(delayMs)
        }
    }

    /**
     * Calcula el delay de backoff exponencial.
     * Secuencia: 1s, 2s, 4s, 8s, 16s (maximo).
     *
     * @param intentos Numero de intentos realizados.
     * @return Delay en milisegundos.
     */
    private fun calcularBackoff(intentos: Int): Long {
        val delay = delayBaseMs * (1L shl intentos.coerceAtMost(4))
        return delay.coerceAtMost(delayMaximoMs)
    }

    /**
     * Agrega un mensaje a la cola de pendientes.
     * Usado cuando no hay conexion al intentar enviar un mensaje.
     *
     * @param chatId ID del chat destino.
     * @param contenido Contenido del mensaje.
     * @param tipoMensaje Tipo del mensaje (Texto, Imagen, etc.).
     * @param archivoLocalUri URI local del archivo adjunto, si aplica.
     */
    suspend fun encolarMensaje(
        chatId: String,
        contenido: String,
        tipoMensaje: String = "Texto",
        archivoLocalUri: String? = null
    ) {
        val mensajePendiente = MensajePendienteEntity(
            chatId = chatId,
            contenido = contenido,
            tipoMensaje = tipoMensaje,
            archivoLocalUri = archivoLocalUri
        )
        mensajePendienteDao.insertar(mensajePendiente)
        Timber.d("Mensaje encolado: ${mensajePendiente.id} para chat $chatId")
    }
}
