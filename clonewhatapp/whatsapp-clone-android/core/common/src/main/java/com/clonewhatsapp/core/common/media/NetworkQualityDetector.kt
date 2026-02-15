package com.clonewhatsapp.core.common.media

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Niveles de calidad de red detectados.
 */
enum class NetworkQuality {
    /** Conexion WiFi con alta velocidad */
    EXCELLENT,
    /** Conexion WiFi o 4G/LTE con buena velocidad */
    GOOD,
    /** Conexion 4G/LTE con velocidad moderada o WiFi con baja velocidad */
    FAIR,
    /** Conexion 3G o red con baja velocidad */
    POOR,
    /** Estado de red desconocido o sin conexion */
    UNKNOWN
}

/**
 * Detector de calidad de red que monitorea la conexion del dispositivo
 * y clasifica la calidad basandose en heuristicas de ancho de banda
 * y tipo de conexion.
 *
 * Utiliza [ConnectivityManager.registerDefaultNetworkCallback] para
 * recibir actualizaciones de red en tiempo real (SDK 24+).
 *
 * El monitoreo se actualiza cada 5 segundos cuando esta activo.
 *
 * @param context Contexto de la aplicacion inyectado por Hilt.
 */
@Singleton
class NetworkQualityDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _networkQuality = MutableStateFlow(NetworkQuality.UNKNOWN)

    /** Flujo observable de la calidad de red actual. */
    val networkQuality: StateFlow<NetworkQuality> = _networkQuality.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var monitoringScope: CoroutineScope? = null
    private var pollingJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * Inicia el monitoreo de calidad de red.
     * Registra un callback de red y comienza un sondeo periodico cada 5 segundos.
     */
    fun startMonitoring() {
        // Evitar registrar multiples callbacks
        if (networkCallback != null) return

        monitoringScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val quality = evaluateNetworkQuality(networkCapabilities)
                _networkQuality.value = quality
            }

            override fun onLost(network: Network) {
                _networkQuality.value = NetworkQuality.POOR
            }

            override fun onUnavailable() {
                _networkQuality.value = NetworkQuality.UNKNOWN
            }
        }

        networkCallback = callback

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
        } catch (e: SecurityException) {
            _networkQuality.value = NetworkQuality.UNKNOWN
        }

        // Sondeo periodico para mantener la calidad actualizada
        pollingJob = monitoringScope?.launch {
            while (isActive) {
                delay(POLLING_INTERVAL_MS)
                updateCurrentQuality()
            }
        }
    }

    /**
     * Detiene el monitoreo de calidad de red y libera recursos.
     */
    fun stopMonitoring() {
        pollingJob?.cancel()
        pollingJob = null

        networkCallback?.let { callback ->
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: IllegalArgumentException) {
                // Callback ya no registrado
            }
        }
        networkCallback = null

        monitoringScope?.cancel()
        monitoringScope = null

        _networkQuality.value = NetworkQuality.UNKNOWN
    }

    /**
     * Actualiza la calidad de red basandose en las capacidades actuales.
     */
    private fun updateCurrentQuality() {
        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork == null) {
            _networkQuality.value = NetworkQuality.POOR
            return
        }

        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (capabilities == null) {
            _networkQuality.value = NetworkQuality.UNKNOWN
            return
        }

        _networkQuality.value = evaluateNetworkQuality(capabilities)
    }

    /**
     * Evalua la calidad de red basandose en las capacidades reportadas.
     *
     * Heuristica de clasificacion:
     * - WiFi con >20 Mbps: EXCELLENT
     * - WiFi con >5 Mbps o celular con >10 Mbps: GOOD
     * - Celular con >2 Mbps: FAIR
     * - Todo lo demas: POOR
     *
     * @param capabilities Las capacidades de la red activa.
     * @return La calidad estimada de la red.
     */
    private fun evaluateNetworkQuality(capabilities: NetworkCapabilities): NetworkQuality {
        val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

        // Obtener ancho de banda descendente en Kbps
        val downstreamBandwidthKbps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            capabilities.linkDownstreamBandwidthKbps
        } else {
            // Fallback: estimar basado en tipo de transporte
            when {
                hasWifi -> ESTIMATED_WIFI_BANDWIDTH_KBPS
                hasCellular -> ESTIMATED_CELLULAR_BANDWIDTH_KBPS
                else -> 0
            }
        }

        return when {
            // WiFi con alta velocidad
            hasWifi && downstreamBandwidthKbps >= HIGH_BANDWIDTH_THRESHOLD_KBPS -> {
                NetworkQuality.EXCELLENT
            }
            // WiFi con velocidad moderada o celular con alta velocidad
            hasWifi && downstreamBandwidthKbps >= MEDIUM_BANDWIDTH_THRESHOLD_KBPS -> {
                NetworkQuality.GOOD
            }
            hasCellular && downstreamBandwidthKbps >= HIGH_BANDWIDTH_THRESHOLD_KBPS -> {
                NetworkQuality.GOOD
            }
            // Celular con velocidad moderada
            hasCellular && downstreamBandwidthKbps >= LOW_BANDWIDTH_THRESHOLD_KBPS -> {
                NetworkQuality.FAIR
            }
            // WiFi con baja velocidad
            hasWifi && downstreamBandwidthKbps >= LOW_BANDWIDTH_THRESHOLD_KBPS -> {
                NetworkQuality.FAIR
            }
            // Velocidad muy baja o desconocida
            downstreamBandwidthKbps > 0 -> {
                NetworkQuality.POOR
            }
            else -> {
                NetworkQuality.UNKNOWN
            }
        }
    }

    companion object {
        /** Intervalo de sondeo en milisegundos (5 segundos). */
        private const val POLLING_INTERVAL_MS = 5_000L

        /** Umbral de ancho de banda alto: 20 Mbps en Kbps. */
        private const val HIGH_BANDWIDTH_THRESHOLD_KBPS = 20_000

        /** Umbral de ancho de banda medio: 5 Mbps en Kbps. */
        private const val MEDIUM_BANDWIDTH_THRESHOLD_KBPS = 5_000

        /** Umbral de ancho de banda bajo: 2 Mbps en Kbps. */
        private const val LOW_BANDWIDTH_THRESHOLD_KBPS = 2_000

        /** Estimacion de ancho de banda WiFi cuando no se puede obtener el real. */
        private const val ESTIMATED_WIFI_BANDWIDTH_KBPS = 15_000

        /** Estimacion de ancho de banda celular cuando no se puede obtener el real. */
        private const val ESTIMATED_CELLULAR_BANDWIDTH_KBPS = 5_000
    }
}
