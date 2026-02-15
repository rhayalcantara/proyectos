package com.clonewhatsapp.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonewhatsapp.domain.model.ConnectionState

/**
 * Indicador visual del estado de conexión SignalR.
 *
 * Muestra un banner de color en la parte superior de la pantalla
 * cuando la conexión no está activa. Se oculta automáticamente
 * cuando el estado es CONNECTED.
 *
 * @param connectionState Estado actual de la conexión SignalR.
 * @param modifier Modificador opcional para personalizar el layout.
 */
@Composable
fun ConnectionIndicator(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val esVisible = connectionState != ConnectionState.CONNECTED

    AnimatedVisibility(
        visible = esVisible,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier
    ) {
        val (colorFondo, texto, mostrarProgreso) = when (connectionState) {
            ConnectionState.CONNECTING -> Triple(
                Color(0xFFFFC107), // Amarillo/ámbar
                "Conectando...",
                true
            )
            ConnectionState.RECONNECTING -> Triple(
                Color(0xFFFF9800), // Naranja
                "Reconectando...",
                true
            )
            ConnectionState.DISCONNECTED -> Triple(
                Color(0xFFF44336), // Rojo
                "Sin conexión",
                false
            )
            ConnectionState.CONNECTED -> Triple(
                Color.Transparent,
                "",
                false
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(colorFondo),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mostrarProgreso) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(14.dp)
                            .padding(end = 0.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Box(modifier = Modifier.size(6.dp))
                }
                Text(
                    text = texto,
                    color = Color.White,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
