package com.clonewhatsapp.feature.chat.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clonewhatsapp.core.ui.theme.WhatsAppTealGreen

/**
 * Duración del pin del mensaje
 */
enum class PinDuration(val label: String, val hours: Int) {
    ONE_DAY("24 horas", 24),
    SEVEN_DAYS("7 días", 168),
    THIRTY_DAYS("30 días", 720)
}

/**
 * Tipo de visibilidad del pin
 */
enum class PinVisibility(val label: String) {
    FOR_ALL("Fijar para todos"),
    ONLY_ME("Fijar solo para mí")
}

/**
 * Diálogo de confirmación para fijar un mensaje (T-132)
 *
 * Permite al usuario elegir la visibilidad (para todos o solo para mí)
 * y la duración del pin (24 horas, 7 días o 30 días).
 *
 * @param isVisible Si el diálogo está visible
 * @param isPinned Si el mensaje ya está fijado (para mostrar opción de desfijar)
 * @param messagePreview Vista previa del mensaje a fijar
 * @param onConfirmPin Callback al confirmar el pin con visibilidad y duración
 * @param onUnpin Callback al desfijar el mensaje
 * @param onDismiss Callback al cerrar el diálogo
 */
@Composable
fun PinMessageDialog(
    isVisible: Boolean,
    isPinned: Boolean,
    messagePreview: String,
    onConfirmPin: (visibility: PinVisibility, duration: PinDuration) -> Unit,
    onUnpin: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    if (isPinned) {
        // Diálogo para desfijar
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Desfijar mensaje",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Text(
                    text = "¿Quieres desfijar este mensaje?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onUnpin()
                    onDismiss()
                }) {
                    Text(
                        text = "Desfijar",
                        color = WhatsAppTealGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancelar",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        )
    } else {
        // Diálogo para fijar con opciones
        var selectedVisibility by remember { mutableStateOf(PinVisibility.FOR_ALL) }
        var selectedDurationIndex by remember { mutableIntStateOf(0) }
        val durations = PinDuration.entries

        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Fijar mensaje",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Vista previa del mensaje
                    Text(
                        text = "\"$messagePreview\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Opciones de visibilidad
                    Text(
                        text = "Visibilidad",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    PinVisibility.entries.forEach { visibility ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedVisibility = visibility }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedVisibility == visibility,
                                onClick = { selectedVisibility = visibility },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = WhatsAppTealGreen
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = visibility.label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Opciones de duración
                    Text(
                        text = "Duración",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        durations.forEachIndexed { index, duration ->
                            DurationChip(
                                label = duration.label,
                                isSelected = selectedDurationIndex == index,
                                onClick = { selectedDurationIndex = index }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onConfirmPin(selectedVisibility, durations[selectedDurationIndex])
                    onDismiss()
                }) {
                    Text(
                        text = "Fijar",
                        color = WhatsAppTealGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancelar",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        )
    }
}

/**
 * Chip de selección de duración del pin.
 *
 * @param label Texto del chip (ej. "24 horas")
 * @param isSelected Si el chip está seleccionado
 * @param onClick Callback al tocar el chip
 */
@Composable
private fun DurationChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        WhatsAppTealGreen.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isSelected) {
        WhatsAppTealGreen
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }

    androidx.compose.material3.Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
