package com.clonewhatsapp.feature.calls.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonewhatsapp.domain.model.CallState
import kotlinx.coroutines.delay
import org.webrtc.VideoTrack
import kotlin.math.roundToInt

/**
 * Pantalla de videollamada a pantalla completa.
 *
 * Muestra el video remoto como fondo de pantalla completa y el video local
 * en una ventana pip-style arrastrable en la esquina superior derecha.
 *
 * Los controles se ocultan automaticamente despues de 5 segundos y se
 * muestran nuevamente al tocar la pantalla.
 *
 * @param callState Estado actual de la llamada.
 * @param callerName Nombre del contacto en la llamada.
 * @param callDuration Duracion de la llamada en segundos.
 * @param isMuted Si el microfono esta silenciado.
 * @param isSpeakerOn Si el altavoz esta activado.
 * @param isVideoEnabled Si la camara esta activada.
 * @param localVideoTrack Pista de video local (camara propia).
 * @param remoteVideoTrack Pista de video remoto (contacto).
 * @param onToggleMute Callback para silenciar/activar microfono.
 * @param onToggleSpeaker Callback para activar/desactivar altavoz.
 * @param onToggleVideo Callback para activar/desactivar camara.
 * @param onEndCall Callback para finalizar la llamada.
 * @param onSwitchCamera Callback para cambiar entre camara frontal/trasera.
 * @param onMinimize Callback para minimizar (entrar en modo PiP).
 * @param modifier Modificador de Compose.
 */
@Composable
fun VideoCallScreen(
    callState: CallState,
    callerName: String,
    callDuration: Long,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isVideoEnabled: Boolean,
    localVideoTrack: VideoTrack?,
    remoteVideoTrack: VideoTrack?,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleVideo: () -> Unit,
    onEndCall: () -> Unit,
    onSwitchCamera: () -> Unit,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Estado de visibilidad de los controles (auto-ocultar)
    var areControlsVisible by remember { mutableStateOf(true) }

    // Temporizador para auto-ocultar controles
    LaunchedEffect(areControlsVisible) {
        if (areControlsVisible && callState == CallState.CONNECTED) {
            delay(AUTO_HIDE_CONTROLS_MS)
            areControlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B1B))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                areControlsVisible = !areControlsVisible
            }
    ) {
        // ---- Video remoto (fondo de pantalla completa) ----
        if (remoteVideoTrack != null && callState == CallState.CONNECTED) {
            WebRTCVideoRenderer(
                videoTrack = remoteVideoTrack,
                isMirror = false,
                scalingType = ScalingType.SCALE_ASPECT_FILL,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Sin video remoto: mostrar avatar + nombre sobre fondo oscuro
            RemoteVideoPlaceholder(
                callerName = callerName,
                callState = callState
            )
        }

        // ---- Video local (ventana pip arrastrable) ----
        if (callState == CallState.CONNECTED || callState == CallState.CONNECTING) {
            DraggableLocalVideoView(
                localVideoTrack = localVideoTrack,
                isVideoEnabled = isVideoEnabled,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 16.dp)
            )
        }

        // ---- Superposicion de estado cuando no esta conectado ----
        if (callState != CallState.CONNECTED && callState != CallState.ENDED) {
            CallStateOverlay(callState = callState)
        }

        // ---- Controles con auto-ocultar ----
        AnimatedVisibility(
            visible = areControlsVisible || callState != CallState.CONNECTED,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Barra superior semitransparente
                TopCallBar(
                    callerName = callerName,
                    callDuration = callDuration,
                    callState = callState,
                    onMinimize = onMinimize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )

                // Barra inferior de controles
                BottomControlsBar(
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    isVideoEnabled = isVideoEnabled,
                    onToggleMute = onToggleMute,
                    onToggleSpeaker = onToggleSpeaker,
                    onToggleVideo = onToggleVideo,
                    onEndCall = onEndCall,
                    onSwitchCamera = onSwitchCamera,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

// ==================== Componentes internos ====================

/**
 * Placeholder cuando no hay video remoto disponible.
 * Muestra un icono de persona y el nombre del contacto.
 */
@Composable
private fun RemoteVideoPlaceholder(
    callerName: String,
    callState: CallState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Avatar circular
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFF374045))
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Color(0xFF8696A0),
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = callerName,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = getCallStateText(callState),
            color = Color(0xFF8696A0),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Vista local de video en ventana pip-style arrastrable.
 */
@Composable
private fun DraggableLocalVideoView(
    localVideoTrack: VideoTrack?,
    isVideoEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(150.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A2A2A))
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        if (isVideoEnabled && localVideoTrack != null) {
            WebRTCVideoRenderer(
                videoTrack = localVideoTrack,
                isMirror = true, // Espejo para camara frontal
                scalingType = ScalingType.SCALE_ASPECT_FILL,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Camara desactivada: mostrar icono
            Icon(
                imageVector = Icons.Filled.VideocamOff,
                contentDescription = "Camara desactivada",
                tint = Color(0xFF8696A0),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

/**
 * Superposicion de texto del estado de la llamada cuando no esta conectada.
 */
@Composable
private fun CallStateOverlay(callState: CallState) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // No renderizamos nada adicional aqui porque el placeholder ya muestra el estado
    }
}

/**
 * Barra superior con nombre del contacto, duracion y boton de minimizar.
 */
@Composable
private fun TopCallBar(
    callerName: String,
    callDuration: Long,
    callState: CallState,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.6f),
                        Color.Transparent
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 48.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = callerName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (callState == CallState.CONNECTED) {
                    formatDuration(callDuration)
                } else {
                    getCallStateText(callState)
                },
                color = Color(0xFFB0B0B0),
                fontSize = 14.sp
            )
        }

        IconButton(onClick = onMinimize) {
            Icon(
                imageVector = Icons.Filled.CloseFullscreen,
                contentDescription = "Minimizar",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Barra inferior con los controles de la llamada.
 */
@Composable
private fun BottomControlsBar(
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
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.6f)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 32.dp)
    ) {
        // Cambiar camara (solo visible si video esta activado)
        if (isVideoEnabled) {
            CallControlButton(
                icon = Icons.Filled.FlipCameraAndroid,
                description = "Cambiar camara",
                isActive = false,
                onClick = onSwitchCamera
            )
        } else {
            // Espacio invisible para mantener el layout
            Spacer(modifier = Modifier.size(56.dp))
        }

        // Toggle video
        CallControlButton(
            icon = if (isVideoEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
            description = if (isVideoEnabled) "Desactivar camara" else "Activar camara",
            isActive = !isVideoEnabled,
            onClick = onToggleVideo
        )

        // Toggle mute
        CallControlButton(
            icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
            description = if (isMuted) "Activar microfono" else "Silenciar",
            isActive = isMuted,
            onClick = onToggleMute
        )

        // Toggle speaker
        CallControlButton(
            icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp
                else Icons.AutoMirrored.Filled.VolumeOff,
            description = if (isSpeakerOn) "Desactivar altavoz" else "Activar altavoz",
            isActive = isSpeakerOn,
            onClick = onToggleSpeaker
        )

        // Colgar llamada (boton rojo mas grande)
        EndCallButton(onClick = onEndCall)
    }
}

/**
 * Boton individual de control de llamada.
 */
@Composable
private fun CallControlButton(
    icon: ImageVector,
    description: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                if (isActive) Color.White.copy(alpha = 0.3f)
                else Color.Black.copy(alpha = 0.4f)
            )
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Boton rojo de finalizar llamada, ligeramente mas grande.
 */
@Composable
private fun EndCallButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(0xFFEA4335))
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = Icons.Filled.CallEnd,
            contentDescription = "Finalizar llamada",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

// ==================== Funciones utilitarias ====================

/**
 * Obtiene el texto de estado de la llamada en espanol.
 */
private fun getCallStateText(callState: CallState): String = when (callState) {
    CallState.IDLE -> ""
    CallState.CALLING -> "Llamando..."
    CallState.RINGING -> "Sonando..."
    CallState.CONNECTING -> "Conectando..."
    CallState.CONNECTED -> "Conectada"
    CallState.ENDED -> "Llamada finalizada"
    CallState.FAILED -> "Llamada fallida"
    CallState.REJECTED -> "Llamada rechazada"
}

/**
 * Formatea la duracion en segundos a formato mm:ss o hh:mm:ss.
 */
private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

/** Tiempo en milisegundos antes de ocultar automaticamente los controles. */
private const val AUTO_HIDE_CONTROLS_MS = 5_000L
