package com.clonewhatsapp.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonewhatsapp.domain.model.EstadoSync
import com.clonewhatsapp.domain.model.SyncState

/**
 * Indicador visual del estado de sincronizacion de mensajes.
 *
 * Muestra un banner compacto cuando hay mensajes pendientes de envio
 * o cuando se esta sincronizando activamente. Se oculta automaticamente
 * cuando no hay mensajes pendientes.
 *
 * T-096: Indicador de sincronizacion en la UI.
 *
 * @param syncState Estado actual de la sincronizacion.
 * @param modifier Modificador opcional para personalizar el layout.
 */
@Composable
fun SyncIndicator(
    syncState: SyncState,
    modifier: Modifier = Modifier
) {
    val esVisible = syncState.cantidadPendientes > 0 || syncState.estado == EstadoSync.SINCRONIZANDO

    AnimatedVisibility(
        visible = esVisible,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
        modifier = modifier
    ) {
        val (colorFondo, texto, mostrarAnimacion) = when (syncState.estado) {
            EstadoSync.SINCRONIZANDO -> Triple(
                Color(0xFF2196F3), // Azul
                "Sincronizando...",
                true
            )
            EstadoSync.ERROR -> Triple(
                Color(0xFFFF5722), // Naranja oscuro
                "${syncState.cantidadPendientes} mensajes pendientes (error)",
                false
            )
            EstadoSync.INACTIVO -> Triple(
                Color(0xFF757575), // Gris
                "${syncState.cantidadPendientes} mensajes pendientes",
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
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                if (mostrarAnimacion) {
                    // Icono de sincronizacion con rotacion animada
                    val transicionInfinita = rememberInfiniteTransition(
                        label = "rotacionSync"
                    )
                    val angulo by transicionInfinita.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 1000,
                                easing = LinearEasing
                            ),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "anguloRotacion"
                    )

                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sincronizando",
                        tint = Color.White,
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(angulo)
                    )
                } else {
                    // Icono de nube sin conexion
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Sin conexion",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = texto,
                    color = Color.White,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}
