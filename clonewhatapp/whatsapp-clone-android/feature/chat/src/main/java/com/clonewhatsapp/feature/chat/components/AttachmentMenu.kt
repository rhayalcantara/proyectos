package com.clonewhatsapp.feature.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tipos de adjunto disponibles en el menu
 */
enum class AttachmentType {
    CAMERA,
    GALLERY,
    DOCUMENT,
    AUDIO
}

// Colores vibrantes estilo WhatsApp para cada tipo de adjunto
private val CameraColor = Color(0xFF7C4DFF)     // Violeta
private val GalleryColor = Color(0xFFE91E63)     // Rosa/Magenta
private val DocumentColor = Color(0xFF2979FF)    // Azul
private val AudioColor = Color(0xFFFF9800)       // Naranja
private val LocationColor = Color(0xFF4CAF50)    // Verde
private val ContactColor = Color(0xFF607D8B)     // Azul grisaceo

/**
 * Menu de adjuntos estilo WhatsApp.
 * Aparece como un grid de iconos con animacion de deslizamiento desde abajo.
 *
 * @param isVisible controla la visibilidad del menu
 * @param onAttachmentSelected callback cuando se selecciona un tipo de adjunto
 * @param onDismiss callback para cerrar el menu
 * @param modifier Modifier opcional
 */
@Composable
fun AttachmentMenu(
    isVisible: Boolean,
    onAttachmentSelected: (AttachmentType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Scrim semi-transparente
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(
                    indication = null,
                    interactionSource = null
                ) { onDismiss() }
        )
    }

    // Panel del menu con animacion de deslizamiento
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primera fila: Camara, Galeria, Documento
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachmentMenuItem(
                        icon = Icons.Default.CameraAlt,
                        label = "Camara",
                        backgroundColor = CameraColor,
                        onClick = { onAttachmentSelected(AttachmentType.CAMERA) }
                    )
                    AttachmentMenuItem(
                        icon = Icons.Default.Image,
                        label = "Galeria",
                        backgroundColor = GalleryColor,
                        onClick = { onAttachmentSelected(AttachmentType.GALLERY) }
                    )
                    AttachmentMenuItem(
                        icon = Icons.Default.Description,
                        label = "Documento",
                        backgroundColor = DocumentColor,
                        onClick = { onAttachmentSelected(AttachmentType.DOCUMENT) }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Segunda fila: Audio, Ubicacion (proximamente), Contacto (proximamente)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachmentMenuItem(
                        icon = Icons.Default.Headphones,
                        label = "Audio",
                        backgroundColor = AudioColor,
                        onClick = { onAttachmentSelected(AttachmentType.AUDIO) }
                    )
                    AttachmentMenuItem(
                        icon = Icons.Default.LocationOn,
                        label = "Ubicacion",
                        backgroundColor = LocationColor,
                        enabled = false,
                        onClick = { /* Proximamente */ }
                    )
                    AttachmentMenuItem(
                        icon = Icons.Default.Person,
                        label = "Contacto",
                        backgroundColor = ContactColor,
                        enabled = false,
                        onClick = { /* Proximamente */ }
                    )
                }
            }
        }
    }
}

/**
 * Elemento individual del menu de adjuntos.
 * Muestra un icono circular coloreado con una etiqueta debajo.
 *
 * @param icon icono a mostrar
 * @param label texto debajo del icono
 * @param backgroundColor color de fondo del circulo
 * @param enabled si esta habilitado para interaccion
 * @param onClick callback al hacer clic
 */
@Composable
private fun AttachmentMenuItem(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp)
    ) {
        // Circulo con icono
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(backgroundColor.copy(alpha = alpha)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White.copy(alpha = alpha),
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Etiqueta
        Text(
            text = if (!enabled) "$label\n(Proximamente)" else label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    }
}
