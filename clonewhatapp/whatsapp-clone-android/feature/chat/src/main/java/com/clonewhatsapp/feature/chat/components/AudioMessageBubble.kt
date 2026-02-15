package com.clonewhatsapp.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonewhatsapp.core.common.media.AudioPlaybackState
import com.clonewhatsapp.core.ui.theme.ChatBubbleIncoming
import com.clonewhatsapp.core.ui.theme.ChatBubbleIncomingDark
import com.clonewhatsapp.core.ui.theme.ChatBubbleOutgoing
import com.clonewhatsapp.core.ui.theme.ChatBubbleOutgoingDark
import com.clonewhatsapp.core.ui.theme.CheckDelivered
import com.clonewhatsapp.core.ui.theme.CheckRead
import com.clonewhatsapp.core.ui.theme.CheckSent
import com.clonewhatsapp.core.ui.theme.WhatsAppTealGreen
import com.clonewhatsapp.domain.model.EstadoMensaje
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Burbuja de mensaje de audio estilo WhatsApp.
 *
 * Muestra un reproductor de audio integrado dentro de una burbuja de chat
 * con boton play/pausa, visualizacion de forma de onda interactiva,
 * duracion y marcas de estado.
 *
 * La forma de onda funciona como seekbar: el usuario puede tocar en
 * cualquier posicion para saltar a ese punto del audio.
 *
 * @param audioUrl URL del archivo de audio
 * @param audioId identificador unico del mensaje de audio
 * @param duration duracion del audio en segundos (puede ser null)
 * @param waveform lista de amplitudes normalizadas (0-100) para la forma de onda
 * @param timestamp marca de tiempo del mensaje en milisegundos
 * @param isFromMe true si el mensaje fue enviado por el usuario actual
 * @param estado estado de entrega del mensaje
 * @param playbackState estado actual de reproduccion del AudioPlayerManager
 * @param onPlayPause callback al presionar play/pausa
 * @param onSeek callback al buscar una posicion (valor normalizado 0.0 a 1.0)
 * @param modifier Modifier opcional
 */
@Composable
fun AudioMessageBubble(
    audioUrl: String,
    audioId: String,
    duration: Int?,
    waveform: List<Int>,
    timestamp: Long,
    isFromMe: Boolean,
    estado: EstadoMensaje,
    playbackState: AudioPlaybackState,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val bubbleColor = when {
        isFromMe && isDark -> ChatBubbleOutgoingDark
        isFromMe -> ChatBubbleOutgoing
        isDark -> ChatBubbleIncomingDark
        else -> ChatBubbleIncoming
    }

    val bubbleShape = if (isFromMe) {
        RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 4.dp,
            bottomStart = 12.dp,
            bottomEnd = 12.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 12.dp,
            bottomStart = 12.dp,
            bottomEnd = 12.dp
        )
    }

    val horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start

    // Determinar si este audio es el que esta reproduciendose actualmente
    val isThisAudioActive = playbackState.currentAudioId == audioId
    val isPlaying = isThisAudioActive && playbackState.isPlaying

    // Calcular progreso de reproduccion
    val progress = if (isThisAudioActive && playbackState.durationMs > 0) {
        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat())
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    // Duracion a mostrar
    val displayDuration = if (isThisAudioActive && isPlaying) {
        // Mostrar tiempo restante mientras se reproduce
        val remainingMs = (playbackState.durationMs - playbackState.currentPositionMs)
            .coerceAtLeast(0)
        formatDurationSeconds((remainingMs / 1000).toInt())
    } else {
        // Mostrar duracion total
        formatDurationSeconds(duration ?: 0)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = horizontalArrangement
    ) {
        Surface(
            modifier = Modifier.width(280.dp),
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 6.dp,
                    end = 10.dp,
                    top = 8.dp,
                    bottom = 4.dp
                )
            ) {
                // Fila principal: Play + Waveform + Duracion
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Boton Play/Pausa
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(36.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = WhatsAppTealGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause
                                else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Forma de onda con seekbar
                    AudioWaveform(
                        waveform = waveform,
                        progress = progress,
                        isFromMe = isFromMe,
                        onSeek = onSeek,
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Duracion
                    Text(
                        text = displayDuration,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp
                        ),
                        color = if (isDark) Color.LightGray else Color.Gray
                    )
                }

                // Fila inferior: timestamp + checks
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Timestamp
                    Text(
                        text = formatTimestamp(timestamp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp
                        ),
                        color = if (isDark) Color.LightGray else Color.Gray
                    )

                    // Check marks (solo para mensajes enviados)
                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (estado) {
                            EstadoMensaje.ENVIADO -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Enviado",
                                    modifier = Modifier.size(14.dp),
                                    tint = CheckSent
                                )
                            }
                            EstadoMensaje.ENTREGADO -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Entregado",
                                    modifier = Modifier.size(14.dp),
                                    tint = CheckDelivered
                                )
                            }
                            EstadoMensaje.LEIDO -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Leido",
                                    modifier = Modifier.size(14.dp),
                                    tint = CheckRead
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Visualizacion de forma de onda interactiva.
 *
 * Muestra barras verticales cuya altura es proporcional a la amplitud.
 * Las barras antes de la posicion actual se muestran en color activo (verde),
 * las barras despues de la posicion en color inactivo (gris).
 * El usuario puede tocar en cualquier posicion para hacer seek.
 */
@Composable
private fun AudioWaveform(
    waveform: List<Int>,
    progress: Float,
    isFromMe: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val activeColor = WhatsAppTealGreen
    val inactiveColor = if (isDark) {
        Color.White.copy(alpha = 0.3f)
    } else {
        Color.Gray.copy(alpha = 0.4f)
    }

    val displayWaveform = if (waveform.isEmpty()) {
        List(40) { 20 } // Placeholder silencioso
    } else {
        waveform
    }

    val progressIndex = (progress * displayWaveform.size).toInt()
        .coerceIn(0, displayWaveform.size)

    Row(
        modifier = modifier
            .clickable { /* Capturar click general - seek mas preciso abajo */ },
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        displayWaveform.forEachIndexed { index, amplitude ->
            val barHeight = ((amplitude / 100f) * 27f).coerceIn(3f, 30f)
            val isActive = index < progressIndex
            val barColor = if (isActive) activeColor else inactiveColor

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(barHeight.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(barColor)
                    .clickable {
                        val seekPosition = (index.toFloat() + 0.5f) / displayWaveform.size
                        onSeek(seekPosition.coerceIn(0f, 1f))
                    }
            )
        }
    }
}

/**
 * Formatea duracion en segundos a formato "M:SS".
 */
private fun formatDurationSeconds(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "$minutes:${secs.toString().padStart(2, '0')}"
}

/**
 * Formatea un timestamp en milisegundos a formato de hora HH:mm.
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
