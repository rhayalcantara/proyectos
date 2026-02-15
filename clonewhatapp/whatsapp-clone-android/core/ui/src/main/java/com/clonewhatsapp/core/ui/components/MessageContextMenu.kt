package com.clonewhatsapp.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * Acciones disponibles en el menu contextual de un mensaje.
 *
 * T-108: Menu contextual con pulsacion larga.
 */
enum class AccionMensaje {
    RESPONDER,
    COPIAR,
    REENVIAR,
    ELIMINAR,
    INFO
}

/**
 * Contenedor que muestra un menu desplegable al mantener pulsada
 * una burbuja de chat (estilo WhatsApp).
 *
 * T-108: Menu contextual de mensaje.
 *
 * @param messageId Identificador del mensaje.
 * @param onAction Callback que se invoca con la accion seleccionada y el ID del mensaje.
 * @param modifier Modificador opcional para personalizar el layout.
 * @param content Contenido composable (la burbuja de chat).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageContextMenu(
    messageId: String,
    onAction: (AccionMensaje, String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var menuExpandido by remember { mutableStateOf(false) }
    val retroalimentacionHaptica = LocalHapticFeedback.current

    Box(modifier = modifier) {
        // Contenido con deteccion de pulsacion larga
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = { /* Clic normal - no hacer nada especial */ },
                    onLongClick = {
                        retroalimentacionHaptica.performHapticFeedback(
                            HapticFeedbackType.LongPress
                        )
                        menuExpandido = true
                    }
                )
        ) {
            content()
        }

        // Menu desplegable contextual estilo WhatsApp
        DropdownMenu(
            expanded = menuExpandido,
            onDismissRequest = { menuExpandido = false },
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 8.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            // Opcion: Responder
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Responder",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = "Responder"
                    )
                },
                onClick = {
                    menuExpandido = false
                    onAction(AccionMensaje.RESPONDER, messageId)
                }
            )

            // Opcion: Copiar
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Copiar",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar"
                    )
                },
                onClick = {
                    menuExpandido = false
                    onAction(AccionMensaje.COPIAR, messageId)
                }
            )

            // Opcion: Reenviar
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Reenviar",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Forward,
                        contentDescription = "Reenviar"
                    )
                },
                onClick = {
                    menuExpandido = false
                    onAction(AccionMensaje.REENVIAR, messageId)
                }
            )

            // Opcion: Eliminar
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Eliminar",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    menuExpandido = false
                    onAction(AccionMensaje.ELIMINAR, messageId)
                }
            )

            // Opcion: Info
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Info",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Informacion del mensaje"
                    )
                },
                onClick = {
                    menuExpandido = false
                    onAction(AccionMensaje.INFO, messageId)
                }
            )
        }
    }
}
