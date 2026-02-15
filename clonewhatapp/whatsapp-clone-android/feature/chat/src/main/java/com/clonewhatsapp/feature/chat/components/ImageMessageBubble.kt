package com.clonewhatsapp.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
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
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
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
 * Burbuja de mensaje con imagen estilo WhatsApp (T-049)
 *
 * Muestra imagen con esquinas redondeadas, caption opcional, timestamp y checks.
 * Utiliza Coil 3 para la carga asíncrona de imágenes.
 *
 * @param imageUrl URL de la imagen a mostrar
 * @param caption Texto descriptivo opcional debajo de la imagen
 * @param timestamp Tiempo de envío en milisegundos
 * @param isFromMe Si el mensaje fue enviado por el usuario actual
 * @param estado Estado del mensaje (ENVIADO, ENTREGADO, LEIDO)
 * @param onClick Callback al tocar la imagen para abrir el visor
 * @param modifier Modifier opcional
 */
@Composable
fun ImageMessageBubble(
    imageUrl: String,
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
                // Imagen con placeholder y estado de error
                Box(
                    modifier = Modifier
                        .widthIn(max = 272.dp)
                        .heightIn(max = 300.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClick)
                ) {
                    SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = "Imagen del mensaje",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .widthIn(min = 200.dp, max = 272.dp)
                            .heightIn(min = 150.dp, max = 300.dp),
                        loading = {
                            // Placeholder mientras carga
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 200.dp)
                                    .heightIn(min = 150.dp)
                                    .background(Color.Gray.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Cargando imagen",
                                        modifier = Modifier.size(40.dp),
                                        tint = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        },
                        error = {
                            // Estado de error
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 200.dp)
                                    .heightIn(min = 150.dp)
                                    .background(Color.Gray.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = "Error al cargar imagen",
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
                    )

                    // Overlay de timestamp y checks sobre la imagen (si no hay caption)
                    if (caption.isNullOrBlank()) {
                        TimestampOverlay(
                            timestamp = timestamp,
                            isFromMe = isFromMe,
                            estado = estado,
                            isDark = isDark,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }
                }

                // Caption debajo de la imagen
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
                        TimestampText(
                            timestamp = timestamp,
                            isDark = isDark
                        )
                        if (isFromMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            CheckMarks(estado = estado)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Overlay semi-transparente con timestamp y checks,
 * mostrado sobre la imagen cuando no hay caption
 */
@Composable
private fun TimestampOverlay(
    timestamp: Long,
    isFromMe: Boolean,
    estado: EstadoMensaje,
    isDark: Boolean,
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
            TimestampText(
                timestamp = timestamp,
                isDark = true // Siempre claro sobre fondo oscuro del overlay
            )
            if (isFromMe) {
                Spacer(modifier = Modifier.width(4.dp))
                CheckMarks(estado = estado)
            }
        }
    }
}

/**
 * Texto de timestamp formateado en HH:mm
 */
@Composable
private fun TimestampText(
    timestamp: Long,
    isDark: Boolean
) {
    Text(
        text = formatTimestampMedia(timestamp),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = if (isDark) Color.LightGray else Color.Gray
    )
}

/**
 * Iconos de check de estado del mensaje
 */
@Composable
private fun CheckMarks(estado: EstadoMensaje) {
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
 * Formatea un timestamp en milisegundos a formato de hora HH:mm
 */
private fun formatTimestampMedia(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
