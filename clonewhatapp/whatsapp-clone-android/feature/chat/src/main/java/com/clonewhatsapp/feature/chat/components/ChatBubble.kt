package com.clonewhatsapp.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonewhatsapp.core.ui.theme.ChatBubbleIncoming
import com.clonewhatsapp.core.ui.theme.ChatBubbleIncomingDark
import com.clonewhatsapp.core.ui.theme.ChatBubbleOutgoing
import com.clonewhatsapp.core.ui.theme.ChatBubbleOutgoingDark
import com.clonewhatsapp.core.ui.theme.CheckDelivered
import com.clonewhatsapp.core.ui.theme.CheckRead
import com.clonewhatsapp.core.ui.theme.CheckSent
import com.clonewhatsapp.domain.model.EstadoMensaje
import com.clonewhatsapp.domain.model.Mensaje
import com.clonewhatsapp.domain.model.TipoMensaje
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Burbuja de mensaje estilo WhatsApp (T-030)
 *
 * @param mensaje El mensaje del dominio a mostrar
 * @param isFromMe Si el mensaje es del usuario actual
 * @param modifier Modifier opcional
 */
@Composable
fun ChatBubble(
    mensaje: Mensaje,
    isFromMe: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val bubbleColor = when {
        isFromMe && isDark -> ChatBubbleOutgoingDark
        isFromMe -> ChatBubbleOutgoing
        isDark -> ChatBubbleIncomingDark
        else -> ChatBubbleIncoming
    }

    // Forma con cola: esquinas redondeadas excepto la esquina del lado del emisor
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

    val horizontalArrangement = if (isFromMe) {
        Arrangement.End
    } else {
        Arrangement.Start
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = horizontalArrangement
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 10.dp,
                    end = 10.dp,
                    top = 6.dp,
                    bottom = 4.dp
                )
            ) {
                // Nombre del remitente (solo en mensajes recibidos de grupos)
                if (!isFromMe && mensaje.nombreRemitente != null) {
                    Text(
                        text = mensaje.nombreRemitente!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Contenido del mensaje
                when {
                    mensaje.eliminado || mensaje.eliminadoParaTodos -> {
                        // Mensaje eliminado
                        Text(
                            text = if (isFromMe) "Eliminaste este mensaje" else "Se eliminó este mensaje",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = if (isDark) Color.Gray else Color.DarkGray
                        )
                    }
                    mensaje.tipo == TipoMensaje.TEXTO -> {
                        Text(
                            text = mensaje.contenido ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color.White else Color.Black
                        )
                    }
                    mensaje.tipo == TipoMensaje.IMAGEN -> {
                        Text(
                            text = "\uD83D\uDCF7 Imagen",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    }
                    mensaje.tipo == TipoMensaje.VIDEO -> {
                        Text(
                            text = "\uD83C\uDFA5 Video",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    }
                    mensaje.tipo == TipoMensaje.AUDIO -> {
                        Text(
                            text = "\uD83C\uDFA4 Audio",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    }
                    mensaje.tipo == TipoMensaje.DOCUMENTO -> {
                        Text(
                            text = "\uD83D\uDCC4 Documento",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    }
                    mensaje.tipo == TipoMensaje.UBICACION -> {
                        Text(
                            text = "\uD83D\uDCCD Ubicación",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    }
                    mensaje.tipo == TipoMensaje.CONTACTO -> {
                        Text(
                            text = "\uD83D\uDC64 Contacto",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = if (isDark) Color.LightGray else Color.DarkGray
                        )
                    }
                    mensaje.tipo == TipoMensaje.SISTEMA -> {
                        // Mensaje de sistema centrado
                        Text(
                            text = mensaje.contenido ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = if (isDark) Color.LightGray else Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Fila inferior: editado + hora + checks
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Label "editado"
                    if (mensaje.editado && !mensaje.eliminado && !mensaje.eliminadoParaTodos) {
                        Text(
                            text = "editado",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontStyle = FontStyle.Italic
                            ),
                            color = if (isDark) Color.LightGray else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    // Timestamp
                    Text(
                        text = formatTimestamp(mensaje.fechaEnvio),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp
                        ),
                        color = if (isDark) Color.LightGray else Color.Gray
                    )

                    // Check marks (solo para mensajes enviados)
                    if (isFromMe && !mensaje.eliminado && !mensaje.eliminadoParaTodos) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (mensaje.estado) {
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
                                    contentDescription = "Leído",
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
 * Formatea un timestamp en milisegundos a formato de hora HH:mm
 */
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
