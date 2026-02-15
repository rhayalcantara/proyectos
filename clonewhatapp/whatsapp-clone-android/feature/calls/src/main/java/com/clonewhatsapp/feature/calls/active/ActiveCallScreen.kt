package com.clonewhatsapp.feature.calls.active

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.clonewhatsapp.core.ui.theme.WhatsAppDarkGreen
import com.clonewhatsapp.core.ui.theme.WhatsAppTealGreen
import com.clonewhatsapp.domain.model.CallState
import com.clonewhatsapp.domain.model.CallType

/**
 * Pantalla principal de llamada activa.
 *
 * Muestra la informacion del contacto, estado de la llamada, duracion
 * y controles para silenciar, altavoz, video y finalizar la llamada.
 */
@Composable
fun ActiveCallScreen(
    callState: CallState,
    callerName: String,
    callerPhoto: String?,
    callType: CallType,
    callDuration: Long,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isVideoEnabled: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onEndCall: () -> Unit,
    onSwitchCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        // Boton de cambiar camara (solo para videollamadas)
        if (callType == CallType.VIDEO && isVideoEnabled) {
            IconButton(
                onClick = onSwitchCamera,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Cameraswitch,
                    contentDescription = "Cambiar camara",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Avatar
            AvatarContacto(
                nombre = callerName,
                fotoUrl = callerPhoto,
                tamano = 100
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Nombre del contacto
            Text(
                text = callerName,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Estado de la llamada / duracion
            Text(
                text = obtenerTextoEstado(callState, callDuration),
                color = when (callState) {
                    CallState.CONNECTED -> WhatsAppTealGreen
                    CallState.ENDED, CallState.FAILED -> Color(0xFFFF6B6B)
                    else -> Color.White.copy(alpha = 0.7f)
                },
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // Barra de controles
            BarraControles(
                callType = callType,
                isMuted = isMuted,
                isSpeakerOn = isSpeakerOn,
                isVideoEnabled = isVideoEnabled,
                onToggleMute = onToggleMute,
                onToggleSpeaker = onToggleSpeaker,
                onToggleVideo = onToggleVideo,
                onEndCall = onEndCall
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

/**
 * Obtiene el texto a mostrar segun el estado de la llamada.
 */
private fun obtenerTextoEstado(estado: CallState, duracionMs: Long): String {
    return when (estado) {
        CallState.CALLING -> "Llamando..."
        CallState.RINGING -> "Sonando..."
        CallState.CONNECTING -> "Conectando..."
        CallState.CONNECTED -> formatearDuracion(duracionMs)
        CallState.ENDED -> "Llamada finalizada"
        CallState.FAILED -> "Llamada fallida"
        CallState.REJECTED -> "Llamada rechazada"
        CallState.IDLE -> ""
    }
}

/**
 * Formatea la duracion en milisegundos a texto legible MM:SS o H:MM:SS.
 */
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

/**
 * Avatar circular del contacto.
 */
@Composable
private fun AvatarContacto(
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
 * Barra inferior con los controles de la llamada.
 */
@Composable
private fun BarraControles(
    callType: CallType,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isVideoEnabled: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onEndCall: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Silenciar microfono
        BotonControlLlamada(
            icono = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
            descripcion = if (isMuted) "Activar microfono" else "Silenciar",
            estaActivo = isMuted,
            onClick = onToggleMute
        )

        // Altavoz
        BotonControlLlamada(
            icono = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
            descripcion = if (isSpeakerOn) "Desactivar altavoz" else "Activar altavoz",
            estaActivo = isSpeakerOn,
            onClick = onToggleSpeaker
        )

        // Video (solo para videollamadas)
        if (callType == CallType.VIDEO) {
            BotonControlLlamada(
                icono = if (isVideoEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                descripcion = if (isVideoEnabled) "Desactivar video" else "Activar video",
                estaActivo = !isVideoEnabled,
                onClick = onToggleVideo
            )
        }

        // Finalizar llamada
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = Color(0xFFFF3B30),
            shadowElevation = 4.dp
        ) {
            IconButton(
                onClick = onEndCall,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.CallEnd,
                    contentDescription = "Finalizar llamada",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Boton circular individual para los controles de la llamada.
 * Cambia de apariencia cuando esta activo (resaltado).
 */
@Composable
private fun BotonControlLlamada(
    icono: ImageVector,
    descripcion: String,
    estaActivo: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(52.dp),
        shape = CircleShape,
        color = if (estaActivo) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
        shadowElevation = 2.dp
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icono,
                contentDescription = descripcion,
                tint = if (estaActivo) WhatsAppTealGreen else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
