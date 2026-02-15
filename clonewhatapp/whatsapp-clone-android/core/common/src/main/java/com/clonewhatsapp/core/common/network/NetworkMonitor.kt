package com.clonewhatsapp.core.common.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitor de conectividad de red.
 *
 * Observa cambios en la conectividad del dispositivo y expone el estado
 * como un StateFlow reactivo. Utilizado por el SyncManager para determinar
 * cuando enviar mensajes pendientes.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val contexto: Context
) {
    private val administradorConectividad =
        contexto.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _estaEnLinea = MutableStateFlow(verificarConectividadActual())

    /** Estado reactivo de la conectividad. true si hay red disponible. */
    val estaEnLinea: StateFlow<Boolean> = _estaEnLinea.asStateFlow()

    private var callbackRegistrado = false

    private val callbackRed = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(red: Network) {
            Timber.d("Red disponible")
            _estaEnLinea.value = true
        }

        override fun onLost(red: Network) {
            Timber.d("Red perdida")
            _estaEnLinea.value = false
        }

        override fun onCapabilitiesChanged(
            red: Network,
            capacidades: NetworkCapabilities
        ) {
            val tieneInternet = capacidades.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )
            Timber.d("Capacidades de red cambiaron: tieneInternet=$tieneInternet")
            _estaEnLinea.value = tieneInternet
        }
    }

    /**
     * Inicia el monitoreo de cambios en la conectividad.
     * Registra un callback con ConnectivityManager.
     */
    fun iniciarMonitoreo() {
        if (callbackRegistrado) return

        try {
            val solicitud = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            administradorConectividad.registerNetworkCallback(solicitud, callbackRed)
            callbackRegistrado = true
            Timber.d("Monitoreo de red iniciado")
        } catch (e: Exception) {
            Timber.e(e, "Error al registrar callback de red")
        }
    }

    /**
     * Detiene el monitoreo de cambios en la conectividad.
     * Desregistra el callback de ConnectivityManager.
     */
    fun detenerMonitoreo() {
        if (!callbackRegistrado) return

        try {
            administradorConectividad.unregisterNetworkCallback(callbackRed)
            callbackRegistrado = false
            Timber.d("Monitoreo de red detenido")
        } catch (e: Exception) {
            Timber.e(e, "Error al desregistrar callback de red")
        }
    }

    /**
     * Verifica la conectividad actual del dispositivo.
     * @return true si hay una red activa con acceso a internet.
     */
    private fun verificarConectividadActual(): Boolean {
        return try {
            val redActiva = administradorConectividad.activeNetwork ?: return false
            val capacidades = administradorConectividad.getNetworkCapabilities(redActiva)
                ?: return false
            capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Timber.e(e, "Error al verificar conectividad actual")
            false
        }
    }
}
