package com.clonewhatsapp.feature.chat.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla de visualizacion de imagen a pantalla completa con zoom (T-050)
 *
 * Permite ver la imagen en pantalla completa con funcionalidades de:
 * - Pellizcar para hacer zoom (pinch-to-zoom)
 * - Doble tap para alternar entre zoom 1x y 2.5x
 * - Desplazamiento (pan) cuando hay zoom activo
 * - Barra superior semi-transparente con nombre del remitente y timestamp
 *
 * @param imageUrl URL de la imagen a visualizar
 * @param senderName Nombre del remitente (opcional)
 * @param timestamp Tiempo de envio en milisegundos
 * @param onBack Callback al presionar boton de retroceso
 * @param modifier Modifier opcional
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    imageUrl: String,
    senderName: String?,
    timestamp: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Estado del zoom y desplazamiento
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var showControls by remember { mutableStateOf(true) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        scale = newScale

        if (newScale > 1f) {
            // Limitar el desplazamiento para que la imagen no se salga demasiado
            val maxOffsetX = (newScale - 1f) * 500f
            val maxOffsetY = (newScale - 1f) * 500f
            offset = Offset(
                x = (offset.x + panChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                y = (offset.y + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
            )
        } else {
            offset = Offset.Zero
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Imagen con zoom y desplazamiento
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = "Imagen a pantalla completa",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .transformable(state = transformableState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            // Alternar entre zoom 1x y 2.5x
                            if (scale > 1.5f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        },
                        onTap = {
                            // Mostrar/ocultar controles
                            showControls = !showControls
                        }
                    )
                },
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color.White
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error al cargar la imagen",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        )

        // Barra superior semi-transparente
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = {
                    if (senderName != null) {
                        Text(
                            text = "$senderName  -  ${formatViewerTimestamp(timestamp)}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
                        Text(
                            text = formatViewerTimestamp(timestamp),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // Reiniciar zoom al salir
                        scale = 1f
                        offset = Offset.Zero
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                )
            )
        }
    }
}

/**
 * Formatea un timestamp para mostrar en el visor de imagen.
 * Muestra fecha y hora en formato "dd/MM/yyyy HH:mm"
 */
private fun formatViewerTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
