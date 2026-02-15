package com.clonewhatsapp.feature.chat.location

import android.annotation.SuppressLint
import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clonewhatsapp.domain.model.SharedLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

/**
 * Estado de la pantalla de compartir ubicación
 */
data class LocationShareState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String? = null,
    val isLoading: Boolean = false,
    val hasLocation: Boolean = false,
    val error: String? = null,
    val locationSent: Boolean = false
)

/**
 * Eventos de la pantalla de compartir ubicación (MVI)
 */
sealed interface LocationShareEvent {
    data object RequestLocation : LocationShareEvent
    data object SendCurrentLocation : LocationShareEvent
    data object DismissError : LocationShareEvent
}

/**
 * ViewModel para la pantalla de compartir ubicación (T-134)
 *
 * Gestiona la obtención de la ubicación actual del dispositivo
 * usando FusedLocationProviderClient y realiza geocodificación
 * inversa para obtener la dirección legible.
 * Sigue el patrón MVI con StateFlow.
 */
@HiltViewModel
class LocationShareViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private val _state = MutableStateFlow(LocationShareState())
    val state: StateFlow<LocationShareState> = _state.asStateFlow()

    // Cliente de ubicación fusionada de Google Play Services
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    private val geocoder = Geocoder(application, Locale("es", "ES"))

    init {
        // Solicitar ubicación automáticamente al abrir la pantalla
        obtenerUbicacion()
    }

    /**
     * Procesa un evento de la UI.
     */
    fun onEvent(event: LocationShareEvent) {
        when (event) {
            is LocationShareEvent.RequestLocation -> obtenerUbicacion()
            is LocationShareEvent.SendCurrentLocation -> enviarUbicacion()
            is LocationShareEvent.DismissError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    /**
     * Obtiene la ubicación actual del dispositivo.
     * Usa FusedLocationProviderClient para alta precisión.
     * Luego realiza geocodificación inversa para obtener la dirección.
     */
    @SuppressLint("MissingPermission")
    private fun obtenerUbicacion() {
        _state.update { it.copy(isLoading = true, error = null) }

        val cancellationToken = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationToken.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                _state.update {
                    it.copy(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        hasLocation = true,
                        isLoading = false
                    )
                }
                // Geocodificación inversa en hilo de fondo
                resolverDireccion(location.latitude, location.longitude)
            } else {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "No se pudo obtener la ubicación. Verifica que el GPS esté activado."
                    )
                }
            }
        }.addOnFailureListener { exception ->
            _state.update {
                it.copy(
                    isLoading = false,
                    error = "Error al obtener ubicación: ${exception.message}"
                )
            }
        }
    }

    /**
     * Resuelve la dirección a partir de las coordenadas usando geocodificación inversa.
     */
    private fun resolverDireccion(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                val direccion = withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    addresses?.firstOrNull()?.let { addr ->
                        buildString {
                            // Construir dirección legible
                            addr.thoroughfare?.let { append(it) }
                            addr.subThoroughfare?.let { append(" $it") }
                            addr.locality?.let {
                                if (isNotEmpty()) append(", ")
                                append(it)
                            }
                            addr.adminArea?.let {
                                if (isNotEmpty()) append(", ")
                                append(it)
                            }
                            addr.countryName?.let {
                                if (isNotEmpty()) append(", ")
                                append(it)
                            }
                        }.ifEmpty { null }
                    }
                }

                _state.update { it.copy(address = direccion) }
            } catch (e: Exception) {
                // Si falla la geocodificación, no es crítico; las coordenadas son suficientes
                _state.update {
                    it.copy(address = "Lat: $lat, Lng: $lng")
                }
            }
        }
    }

    /**
     * Envía la ubicación actual como un mensaje de ubicación.
     * Marca locationSent como true para que la pantalla navegue de vuelta.
     */
    private fun enviarUbicacion() {
        val currentState = _state.value
        if (!currentState.hasLocation) return

        _state.update { it.copy(locationSent = true) }
    }

    /**
     * Obtiene la ubicación compartida actual como modelo de dominio.
     */
    fun getSharedLocation(): SharedLocation? {
        val currentState = _state.value
        if (!currentState.hasLocation) return null

        return SharedLocation(
            latitude = currentState.latitude,
            longitude = currentState.longitude,
            address = currentState.address,
            timestamp = System.currentTimeMillis()
        )
    }
}
