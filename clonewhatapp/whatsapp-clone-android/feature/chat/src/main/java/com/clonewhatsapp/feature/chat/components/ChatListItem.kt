package com.clonewhatsapp.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.clonewhatsapp.core.ui.theme.WhatsAppLightGreen
import com.clonewhatsapp.domain.model.Chat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Item de lista de chats estilo WhatsApp
 *
 * @param chat El chat del dominio a mostrar
 * @param onClick Callback al hacer clic en el chat
 * @param modifier Modifier opcional
 */
@Composable
fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circular
        ChatAvatar(
            fotoUrl = chat.fotoUrl,
            nombre = chat.nombre,
            modifier = Modifier.size(52.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Contenido del chat (nombre + último mensaje)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Nombre del chat
            Text(
                text = chat.nombre,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Último mensaje
            if (chat.ultimoMensaje != null) {
                Text(
                    text = chat.ultimoMensaje!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Columna derecha: hora + badge
        Column(
            horizontalAlignment = Alignment.End
        ) {
            // Timestamp del último mensaje
            if (chat.ultimoMensajeTiempo != null) {
                Text(
                    text = formatChatTimestamp(chat.ultimoMensajeTiempo!!),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp
                    ),
                    color = if (chat.mensajesNoLeidos > 0) {
                        WhatsAppLightGreen
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Badge de mensajes no leídos
            if (chat.mensajesNoLeidos > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(WhatsAppLightGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (chat.mensajesNoLeidos > 99) "99+" else chat.mensajesNoLeidos.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Avatar circular con imagen o iniciales de fallback
 */
@Composable
private fun ChatAvatar(
    fotoUrl: String?,
    nombre: String,
    modifier: Modifier = Modifier
) {
    if (fotoUrl != null) {
        AsyncImage(
            model = fotoUrl,
            contentDescription = "Foto de $nombre",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        // Fallback con iniciales
        val initials = nombre
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }

        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Formatea el timestamp del chat para la lista
 * Muestra hora si es hoy, o fecha corta si es otro día
 */
private fun formatChatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val oneDay = 24 * 60 * 60 * 1000L

    return when {
        diff < oneDay -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        diff < 2 * oneDay -> {
            "Ayer"
        }
        diff < 7 * oneDay -> {
            SimpleDateFormat("EEEE", Locale("es")).format(Date(timestamp))
        }
        else -> {
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
