package com.clonewhatsapp.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Overlay que muestra el progreso de subida de un archivo.
 * Se superpone sobre la burbuja del mensaje mientras sube.
 *
 * Muestra un indicador circular de progreso con el porcentaje en el centro
 * y un boton de cancelar opcional.
 *
 * @param progress progreso de 0.0 a 1.0
 * @param onCancel callback opcional para cancelar la subida; si es null no se muestra el boton
 * @param modifier Modifier opcional
 */
@Composable
fun UploadProgressOverlay(
    progress: Float,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        // Indicador de progreso circular (track de fondo)
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(60.dp),
            strokeWidth = 3.dp,
            color = Color.White.copy(alpha = 0.25f),
            trackColor = Color.Transparent
        )

        // Indicador de progreso circular (progreso real)
        CircularProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.size(60.dp),
            strokeWidth = 3.dp,
            color = Color.White,
            trackColor = Color.Transparent
        )

        // Texto del porcentaje en el centro
        Text(
            text = "${(progress * 100).toInt()}%",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )

        // Boton de cancelar (esquina superior derecha)
        if (onCancel != null) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancelar subida",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
