package com.clonewhatsapp.feature.chat.location

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clonewhatsapp.core.ui.theme.WhatsAppDarkGreen
import com.clonewhatsapp.core.ui.theme.WhatsAppTealGreen

/**
 * Pantalla de compartir ubicación (T-134)
 *
 * Muestra un mapa estático con la ubicación actual del usuario,
 * un bottom sheet con detalles de dirección y coordenadas,
 * y botones para enviar la ubicación o compartir en tiempo real (placeholder).
 *
 * @param onLocationSend Callback cuando el usuario confirma enviar la ubicación
 * @param onBack Callback para navegar hacia atrás
 * @param viewModel ViewModel inyectado por Hilt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationShareScreen(
    onLocationSend: (lat: Double, lng: Double, address: String?) -> Unit,
    onBack: () -> Unit,
    viewModel: LocationShareViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Observar cuando la ubicación se envía para navegar de vuelta
    LaunchedEffect(state.locationSent) {
        if (state.locationSent) {
            onLocationSend(state.latitude, state.longitude, state.address)
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 220.dp,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContent = {
            // Contenido del bottom sheet con detalles de ubicación
            LocationDetailsSheet(
                state = state,
                onSendLocation = { viewModel.onEvent(LocationShareEvent.SendCurrentLocation) }
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ubicación",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WhatsAppDarkGreen
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Área del mapa (representación visual sin SDK de Google Maps)
            MapPlaceholder(
                latitude = state.latitude,
                longitude = state.longitude,
                hasLocation = state.hasLocation,
                isLoading = state.isLoading,
                error = state.error
            )

            // FAB para actualizar ubicación
            FloatingActionButton(
                onClick = { viewModel.onEvent(LocationShareEvent.RequestLocation) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 230.dp),
                containerColor = Color.White,
                contentColor = WhatsAppTealGreen
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Obtener mi ubicación"
                )
            }
        }
    }
}

/**
 * Placeholder visual del mapa.
 * Muestra las coordenadas y un pin sobre un fondo coloreado.
 * En una implementación real, aquí iría un MapView o Google Maps Compose.
 */
@Composable
private fun MapPlaceholder(
    latitude: Double,
    longitude: Double,
    hasLocation: Boolean,
    isLoading: Boolean,
    error: String?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8F5E9)),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = WhatsAppTealGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Obteniendo ubicación...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
            error != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
            hasLocation -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pin de ubicación grande
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Tu ubicación",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFE53935)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Coordenadas
                    Text(
                        text = "%.6f, %.6f".format(latitude, longitude),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Indicación de mapa placeholder
                    Text(
                        text = "Vista de mapa",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray.copy(alpha = 0.6f)
                    )
                }
            }
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toca el botón para obtener tu ubicación",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * Bottom sheet con detalles de la ubicación y botones de acción.
 */
@Composable
private fun LocationDetailsSheet(
    state: LocationShareState,
    onSendLocation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Botón de enviar ubicación actual
        Button(
            onClick = onSendLocation,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = state.hasLocation && !state.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = WhatsAppTealGreen,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Enviar ubicación actual",
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Detalles de dirección
        if (state.hasLocation) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono de ubicación
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(WhatsAppTealGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = WhatsAppTealGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.address ?: "Dirección no disponible",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Lat: %.4f, Lng: %.4f".format(state.latitude, state.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Opción de compartir ubicación en tiempo real (placeholder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(WhatsAppTealGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = WhatsAppTealGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Compartir ubicación en tiempo real",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Próximamente",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
