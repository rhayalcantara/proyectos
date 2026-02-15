package com.clonewhatsapp.feature.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonewhatsapp.core.ui.theme.WhatsAppTealGreen

/**
 * Banner de mensaje fijado que aparece en la parte superior del chat (T-132)
 *
 * Muestra una vista previa del mensaje fijado con icono de pin.
 * Soporta múltiples mensajes fijados con navegación izquierda/derecha.
 * Incluye animación suave de entrada/salida.
 *
 * @param isVisible Si el banner está visible
 * @param messagePreview Texto de vista previa del mensaje fijado (truncado)
 * @param senderName Nombre del remitente del mensaje fijado
 * @param pinnedCount Cantidad total de mensajes fijados
 * @param currentIndex Índice del mensaje fijado actualmente mostrado (0-based)
 * @param onBannerClick Callback al tocar el banner para desplazarse al mensaje
 * @param onDismiss Callback al tocar el botón de cerrar
 * @param onPreviousPin Callback para ir al mensaje fijado anterior
 * @param onNextPin Callback para ir al siguiente mensaje fijado
 * @param modifier Modifier opcional
 */
@Composable
fun PinnedMessageBanner(
    isVisible: Boolean,
    messagePreview: String,
    senderName: String,
    pinnedCount: Int,
    currentIndex: Int,
    onBannerClick: () -> Unit,
    onDismiss: () -> Unit,
    onPreviousPin: () -> Unit,
    onNextPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBannerClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono de pin rotado 45 grados (estilo WhatsApp)
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Mensaje fijado",
                    tint = WhatsAppTealGreen,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(45f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Contenido del mensaje fijado
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Si hay múltiples mensajes fijados, mostrar contador
                    if (pinnedCount > 1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$pinnedCount mensajes fijados",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = WhatsAppTealGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(${currentIndex + 1}/$pinnedCount)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        Text(
                            text = senderName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = WhatsAppTealGreen
                        )
                    }

                    // Vista previa del mensaje truncada
                    Text(
                        text = messagePreview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Flechas de navegación para múltiples mensajes fijados
                if (pinnedCount > 1) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPreviousPin,
                            modifier = Modifier.size(28.dp),
                            enabled = currentIndex > 0
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Mensaje fijado anterior",
                                modifier = Modifier.size(18.dp),
                                tint = if (currentIndex > 0) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
                            )
                        }
                        IconButton(
                            onClick = onNextPin,
                            modifier = Modifier.size(28.dp),
                            enabled = currentIndex < pinnedCount - 1
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Siguiente mensaje fijado",
                                modifier = Modifier.size(18.dp),
                                tint = if (currentIndex < pinnedCount - 1) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                }
                            )
                        }
                    }
                }

                // Botón de cerrar/descartar
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar banner de mensaje fijado",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
