package com.clonewhatsapp.feature.calls.incoming

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.clonewhatsapp.core.ui.theme.WhatsAppDarkGreen
import com.clonewhatsapp.core.ui.theme.WhatsAppTealGreen
import com.clonewhatsapp.domain.model.CallType

/**
 * Pantalla de llamada entrante a pantalla completa.
 *
 * Muestra la informacion del llamante con opciones para aceptar o rechazar.
 * Incluye animaciones de entrada para los botones y texto pulsante.
 */
@Composable
fun IncomingCallScreen(
    callerName: String,
    callerPhoto: String?,
    callType: CallType,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showButtons by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showButtons = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B141A),
                        Color(0xFF1A2C34),
                        Color(0xFF0B141A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Avatar del llamante
            AvatarLlamante(
                nombre = callerName,
                fotoUrl = callerPhoto,
                tamano = 120
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Nombre del llamante
            Text(
                text = callerName,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tipo de llamada
            Text(
                text = if (callType == CallType.AUDIO) "Llamada de voz" else "Videollamada",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Texto pulsante "Llamada entrante..."
            TextoPulsante()

            Spacer(modifier = Modifier.weight(1f))

            // Botones de aceptar/rechazar con animacion
            AnimatedVisibility(
                visible = showButtons,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 500)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 64.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Boton rechazar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = Color(0xFFFF3B30),
                            shadowElevation = 8.dp
                        ) {
                            IconButton(
                                onClick = onReject,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CallEnd,
                                    contentDescription = "Rechazar llamada",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Rechazar",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }

                    // Boton aceptar
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = WhatsAppTealGreen,
                            shadowElevation = 8.dp
                        ) {
                            IconButton(
                                onClick = onAccept,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = if (callType == CallType.VIDEO)
                                        Icons.Filled.Videocam
                                    else
                                        Icons.Filled.Call,
                                    contentDescription = "Aceptar llamada",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aceptar",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Avatar circular del llamante. Muestra la foto si esta disponible,
 * o las iniciales del nombre sobre un fondo de color.
 */
@Composable
private fun AvatarLlamante(
    nombre: String,
    fotoUrl: String?,
    tamano: Int
) {
    if (!fotoUrl.isNullOrBlank()) {
        AsyncImage(
            model = fotoUrl,
            contentDescription = "Foto de $nombre",
            modifier = Modifier
                .size(tamano.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        // Mostrar iniciales
        val iniciales = nombre
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")

        Surface(
            modifier = Modifier.size(tamano.dp),
            shape = CircleShape,
            color = WhatsAppDarkGreen
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = iniciales,
                    color = Color.White,
                    fontSize = (tamano / 3).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Texto "Llamada entrante..." con efecto de pulso en la opacidad.
 */
@Composable
private fun TextoPulsante() {
    val transicionInfinita = rememberInfiniteTransition(label = "pulso_texto")
    val alpha by transicionInfinita.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_pulso"
    )

    Text(
        text = "Llamada entrante...",
        color = WhatsAppTealGreen.copy(alpha = alpha),
        fontSize = 14.sp,
        textAlign = TextAlign.Center
    )
}
