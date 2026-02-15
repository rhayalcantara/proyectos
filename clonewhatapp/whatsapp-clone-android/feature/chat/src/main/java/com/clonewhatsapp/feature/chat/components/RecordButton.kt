package com.clonewhatsapp.feature.chat.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonewhatsapp.core.ui.theme.WhatsAppTealGreen

/**
 * Boton de grabacion de audio estilo WhatsApp.
 *
 * Cuando no esta grabando, muestra un FAB verde con icono de microfono.
 * Al tocar, cambia a una barra de grabacion completa con:
 * - Boton de eliminar/cancelar a la izquierda
 * - Indicador rojo pulsante de grabacion
 * - Timer de duracion
 * - Visualizacion de forma de onda (ultimas 30 amplitudes)
 * - Boton de detener a la derecha
 *
 * @param isRecording true si se esta grabando actualmente
 * @param recordingDurationMs duracion actual de la grabacion en milisegundos
 * @param amplitudes lista de amplitudes normalizadas (0-100) para la forma de onda
 * @param onStartRecording callback al iniciar grabacion
 * @param onStopRecording callback al detener grabacion
 * @param onCancelRecording callback al cancelar grabacion
 * @param modifier Modifier opcional
 */
@Composable
fun RecordButton(
    isRecording: Boolean,
    recordingDurationMs: Long,
    amplitudes: List<Int>,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = isRecording,
        modifier = modifier,
        transitionSpec = {
            if (targetState) {
                // Entrando al modo grabacion
                (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it } + fadeOut()) using
                    SizeTransform(clip = false)
            } else {
                // Saliendo del modo grabacion
                (slideInHorizontally { -it } + fadeIn()) togetherWith
                    (slideOutHorizontally { it } + fadeOut()) using
                    SizeTransform(clip = false)
            }
        },
        label = "RecordButtonTransition"
    ) { recording ->
        if (recording) {
            RecordingBar(
                durationMs = recordingDurationMs,
                amplitudes = amplitudes,
                onStop = onStopRecording,
                onCancel = onCancelRecording
            )
        } else {
            MicButton(onClick = onStartRecording)
        }
    }
}

/**
 * FAB verde con icono de microfono (estado inactivo).
 */
@Composable
private fun MicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        containerColor = WhatsAppTealGreen,
        contentColor = Color.White,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 2.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Grabar audio",
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Barra de grabacion activa con indicador pulsante, timer, forma de onda y controles.
 */
@Composable
private fun RecordingBar(
    durationMs: Long,
    amplitudes: List<Int>,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animacion pulsante para el indicador rojo
    val infiniteTransition = rememberInfiniteTransition(label = "RecordingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Boton cancelar/eliminar
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Cancelar grabacion",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Indicador rojo pulsante
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(Color(0xFFE53935))
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Timer de duracion
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Forma de onda
            MiniWaveform(
                amplitudes = amplitudes.takeLast(30),
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Boton de detener
            IconButton(
                onClick = onStop,
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(WhatsAppTealGreen)
                )
            }
        }
    }
}

/**
 * Visualizacion miniatura de forma de onda con barras verticales.
 */
@Composable
private fun MiniWaveform(
    amplitudes: List<Int>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (amplitudes.isEmpty()) {
            // Placeholder cuando no hay datos
            repeat(15) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        } else {
            amplitudes.forEach { amplitude ->
                val barHeight = ((amplitude / 100f) * 28f).coerceIn(3f, 28f)
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(barHeight.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(WhatsAppTealGreen)
                )
            }
        }
    }
}

/**
 * Formatea duracion en milisegundos a formato "M:SS".
 */
private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
