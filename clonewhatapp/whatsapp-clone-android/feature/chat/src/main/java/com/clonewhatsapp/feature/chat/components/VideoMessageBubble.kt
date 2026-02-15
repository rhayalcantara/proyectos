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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.clonewhatsapp.core.ui.theme.ChatBubbleIncoming
import com.clonewhatsapp.core.ui.theme.ChatBubbleIncomingDark
import com.clonewhatsapp.core.ui.theme.ChatBubbleOutgoing
import com.clonewhatsapp.core.ui.theme.ChatBubbleOutgoingDark
import com.clonewhatsapp.core.ui.theme.CheckDelivered
import com.clonewhatsapp.core.ui.theme.CheckRead
import com.clonewhatsapp.core.ui.theme.CheckSent
import com.clonewhatsapp.domain.model.EstadoMensaje
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Burbuja de mensaje con video estilo WhatsApp (T-052)
 *
 * Muestra thumbnail del video con boton de play centrado, duracion,
 * caption opcional y metadatos de timestamp/checks.
 *
 * @param videoUrl URL del video
 * @param thumbnailUrl URL del thumbnail del video (puede ser null)
 * @param duration Duracion del video en segundos (puede ser null)
 * @param caption Texto descriptivo opcional debajo del thumbnail
 * @param timestamp Tiempo de envio en milisegundos
 * @param isFromMe Si el mensaje fue enviado por el usuario actual
 * @param estado Estado del mensaje (ENVIADO, ENTREGADO, LEIDO)
 * @param onClick Callback al tocar para abrir el reproductor de video
 * @param modifier Modifier opcional
 */
@Composable
fun VideoMessageBubble(
    videoUrl: String,
    thumbnailUrl: String?,
    duration: Int?,
    caption: String?,
    timestamp: Long,
    isFromMe: Boolean,
    estado: EstadoMensaje,
    onClick: () -> Unit,
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = horizontalArrangement
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(4.dp)
            ) {
                // Thumbnail del video con boton de play
                Box(
                    modifier = Modifier
                        .widthIn(max = 272.dp)
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClick)
                ) {
                    if (!thumbnailUrl.isNullOrBlank()) {
                        // Thumbnail del video
                        SubcomposeAsyncImage(
                            model = thumbnailUrl,
                            contentDescription = "Thumbnail del video",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .widthIn(min = 200.dp, max = 272.dp)
                                .heightIn(min = 150.dp, max = 300.dp),
                            loading = {
                                VideoDarkPlaceholder()
                            },
                            error = {
                                VideoErrorPlaceholder()
                            }
                        )
                    } else {
                        // Placeholder oscuro cuando no hay thumbnail
                        VideoDarkPlaceholder()
                    }

                    // Boton de play centrado
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(56.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.8f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproducir video",
                            modifier = Modifier.size(36.dp),
                            tint = Color.DarkGray
                        )
                    }

                    // Badge de duracion en la esquina inferior izquierda
                    if (duration != null && duration > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = formatDuration(duration),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    // Overlay de timestamp y checks sobre el thumbnail (si no hay caption)
                    if (caption.isNullOrBlank()) {
                        VideoTimestampOverlay(
                            timestamp = timestamp,
                            isFromMe = isFromMe,
                            estado = estado,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }

                // Caption debajo del thumbnail
                if (!caption.isNullOrBlank()) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDark) Color.White else Color.Black,
                        modifier = Modifier.padding(
                            start = 6.dp,
                            end = 6.dp,
                            top = 4.dp
                        )
                    )

                    // Timestamp y checks debajo del caption
                    Row(
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(end = 6.dp, bottom = 2.dp, top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VideoTimestampText(
                            timestamp = timestamp,
                            isDark = isDark
                        )
                        if (isFromMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            VideoCheckMarks(estado = estado)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Placeholder oscuro para video sin thumbnail o mientras carga
 */
@Composable
private fun VideoDarkPlaceholder() {
    Box(
        modifier = Modifier
            .widthIn(min = 200.dp)
            .heightIn(min = 150.dp)
            .background(Color(0xFF2C2C2C)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Videocam,
            contentDescription = "Video",
            modifier = Modifier.size(48.dp),
            tint = Color.Gray
        )
    }
}

/**
 * Placeholder de error para video con thumbnail que fallo al cargar
 */
@Composable
private fun VideoErrorPlaceholder() {
    Box(
        modifier = Modifier
            .widthIn(min = 200.dp)
            .heightIn(min = 150.dp)
            .background(Color(0xFF2C2C2C)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "Error al cargar thumbnail",
                modifier = Modifier.size(40.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "Error al cargar",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}

/**
 * Overlay semi-transparente con timestamp y checks sobre el thumbnail del video
 */
@Composable
private fun VideoTimestampOverlay(
    timestamp: Long,
    isFromMe: Boolean,
    estado: EstadoMensaje,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(6.dp)
            .background(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            VideoTimestampText(
                timestamp = timestamp,
                isDark = true
            )
            if (isFromMe) {
                Spacer(modifier = Modifier.width(4.dp))
                VideoCheckMarks(estado = estado)
            }
        }
    }
}

/**
 * Texto de timestamp formateado en HH:mm
 */
@Composable
private fun VideoTimestampText(
    timestamp: Long,
    isDark: Boolean
) {
    Text(
        text = formatVideoTimestamp(timestamp),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = if (isDark) Color.LightGray else Color.Gray
    )
}

/**
 * Iconos de check de estado del mensaje
 */
@Composable
private fun VideoCheckMarks(estado: EstadoMensaje) {
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

/**
 * Formatea una duracion en segundos al formato M:SS
 * Ejemplo: 85 -> "1:25", 45 -> "0:45"
 */
private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "$minutes:${remainingSeconds.toString().padStart(2, '0')}"
}

/**
 * Formatea un timestamp en milisegundos a formato de hora HH:mm
 */
private fun formatVideoTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
