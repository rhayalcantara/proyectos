package com.clonewhatsapp.feature.chat.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * Burbuja de mensaje de ubicación compartida (T-134)
 *
 * Muestra una miniatura visual de mapa (fondo coloreado con pin)
 * y la dirección debajo. Al tocar, abre la ubicación en Google Maps.
 *
 * @param latitude Latitud de la ubicación compartida
 * @param longitude Longitud de la ubicación compartida
 * @param address Dirección legible (puede ser null)
 * @param timestamp Tiempo de envío en milisegundos
 * @param isFromMe Si el mensaje fue enviado por el usuario actual
 * @param estado Estado del mensaje (ENVIADO, ENTREGADO, LEIDO)
 * @param modifier Modifier opcional
 */
@Composable
fun LocationMessageBubble(
    latitude: Double,
    longitude: Double,
    address: String?,
    timestamp: Long,
    isFromMe: Boolean,
    estado: EstadoMensaje,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                modifier = Modifier
                    .clickable {
                        // Abrir ubicación en Google Maps
                        val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
                        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        // Si Google Maps no está instalado, abrir en navegador
                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(mapIntent)
                        } else {
                            val webUri = Uri.parse(
                                "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                            )
                            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                        }
                    }
                    .padding(4.dp)
            ) {
                // Miniatura de mapa (fondo con gradiente verde y pin)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFA5D6A7),
                                    Color(0xFF81C784),
                                    Color(0xFF66BB6A)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Pin de ubicación sobre el mapa
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Ubicación compartida",
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFFE53935)
                        )

                        // Coordenadas en formato compacto
                        Text(
                            text = "%.4f, %.4f".format(latitude, longitude),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp
                            ),
                            color = Color.White,
                            modifier = Modifier
                                .background(
                                    color = Color.Black.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Overlay con timestamp (esquina inferior derecha)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
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
                            Text(
                                text = formatTimestampLocation(timestamp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp
                                ),
                                color = Color.LightGray
                            )
                            if (isFromMe) {
                                Spacer(modifier = Modifier.width(4.dp))
                                LocationCheckMarks(estado = estado)
                            }
                        }
                    }
                }

                // Dirección debajo de la miniatura de mapa
                Column(
                    modifier = Modifier.padding(
                        start = 6.dp,
                        end = 6.dp,
                        top = 6.dp,
                        bottom = 4.dp
                    )
                ) {
                    Text(
                        text = address ?: "Ubicación compartida",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (isDark) Color.White else Color.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Toca para ver en Google Maps",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp
                        ),
                        color = WhatsAppTealGreen
                    )
                }
            }
        }
    }
}

/**
 * Iconos de check de estado del mensaje de ubicación
 */
@Composable
private fun LocationCheckMarks(estado: EstadoMensaje) {
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
                contentDescription = "Leído",
                modifier = Modifier.size(14.dp),
                tint = CheckRead
            )
        }
    }
}

/**
 * Formatea un timestamp en milisegundos a formato de hora HH:mm
 */
private fun formatTimestampLocation(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
