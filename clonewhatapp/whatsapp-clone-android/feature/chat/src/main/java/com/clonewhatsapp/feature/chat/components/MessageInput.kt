package com.clonewhatsapp.feature.chat.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonewhatsapp.core.ui.theme.WhatsAppTealGreen
import kotlinx.coroutines.delay

/**
 * Barra de entrada de mensaje estilo WhatsApp con soporte para grabación de audio.
 *
 * Tiene dos modos de visualización:
 * - Modo normal: [Adjuntar] [Campo de texto] [Enviar/Micrófono FAB]
 * - Modo grabación: [Cancelar X] [Punto rojo] [Temporizador] [Forma de onda] [Detener]
 *
 * @param text Texto actual del campo de entrada
 * @param onTextChange Callback cuando cambia el texto
 * @param onSendClick Callback al presionar enviar
 * @param onAttachClick Callback al presionar adjuntar archivo
 * @param isRecording Indica si se está grabando audio actualmente
 * @param recordingDurationMs Duración de la grabación en milisegundos
 * @param recordingAmplitudes Lista de amplitudes (0-100) para la visualización de onda
 * @param onStartRecording Callback al iniciar grabación (presionar micrófono)
 * @param onStopRecording Callback al detener grabación (enviar audio)
 * @param onCancelRecording Callback al cancelar grabación (descartar audio)
 * @param modifier Modifier opcional
 */
@Composable
fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit = {},
    isRecording: Boolean = false,
    recordingDurationMs: Long = 0L,
    recordingAmplitudes: List<Int> = emptyList(),
    onStartRecording: () -> Unit = {},
    onStopRecording: () -> Unit = {},
    onCancelRecording: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Contenido principal: alterna entre modo normal y modo grabación
        AnimatedContent(
            targetState = isRecording,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                (slideInHorizontally { width -> -width } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
                    .using(SizeTransform(clip = false))
            },
            label = "ModoEntrada"
        ) { grabando ->
            if (grabando) {
                // === MODO GRABACIÓN ===
                RecordingBar(
                    durationMs = recordingDurationMs,
                    amplitudes = recordingAmplitudes,
                    onCancel = onCancelRecording
                )
            } else {
                // === MODO NORMAL ===
                NormalInputBar(
                    text = text,
                    onTextChange = onTextChange,
                    onSendClick = onSendClick,
                    onAttachClick = onAttachClick
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Botón FAB derecho: alterna entre Enviar, Micrófono y Detener
        if (isRecording) {
            // Botón de detener grabación
            FloatingActionButton(
                onClick = onStopRecording,
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                containerColor = Color.Red,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Detener grabación",
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            // Transición animada entre micrófono y enviar
            Crossfade(
                targetState = text.isNotBlank(),
                label = "MicEnviar"
            ) { tieneTexto ->
                if (tieneTexto) {
                    // Botón de enviar
                    FloatingActionButton(
                        onClick = onSendClick,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        containerColor = WhatsAppTealGreen,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    // Botón de micrófono
                    FloatingActionButton(
                        onClick = onStartRecording,
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        containerColor = WhatsAppTealGreen,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Grabar audio",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Barra de entrada normal con campo de texto y botón de adjuntar.
 */
@Composable
private fun NormalInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 1.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón de adjuntar
            IconButton(
                onClick = onAttachClick
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Adjuntar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Campo de texto
            TextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = "Mensaje",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (text.isNotBlank()) {
                            onSendClick()
                        }
                    }
                ),
                maxLines = 4,
                singleLine = false
            )
        }
    }
}

/**
 * Barra de grabación de audio con indicador visual, temporizador y forma de onda.
 *
 * @param durationMs Duración actual de la grabación en milisegundos
 * @param amplitudes Lista de amplitudes (0-100) para la visualización de forma de onda
 * @param onCancel Callback al cancelar la grabación
 */
@Composable
private fun RecordingBar(
    durationMs: Long,
    amplitudes: List<Int>,
    onCancel: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón cancelar
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancelar grabación",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Punto rojo pulsante
            PulsingRedDot()

            Spacer(modifier = Modifier.width(8.dp))

            // Temporizador de grabación
            val minutos = durationMs / 60000
            val segundos = (durationMs / 1000) % 60
            Text(
                text = String.format("%d:%02d", minutos, segundos),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = Color.Red
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Forma de onda (visualización de amplitudes)
            RecordingWaveform(
                amplitudes = amplitudes,
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

/**
 * Punto rojo con animación de pulsación para indicar grabación activa.
 */
@Composable
private fun PulsingRedDot() {
    var alpha by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) {
        while (true) {
            // Ciclo de pulsación: de opaco a semi-transparente y viceversa
            alpha = 0.3f
            delay(500L)
            alpha = 1f
            delay(500L)
        }
    }

    Canvas(
        modifier = Modifier.size(10.dp)
    ) {
        drawCircle(
            color = Color.Red.copy(alpha = alpha),
            radius = size.minDimension / 2f,
            center = Offset(size.width / 2f, size.height / 2f)
        )
    }
}

/**
 * Visualización de forma de onda como barras verticales delgadas.
 * Cada barra representa una amplitud mapeada a una altura entre 3.dp y 24.dp.
 *
 * @param amplitudes Lista de valores de amplitud (0-100)
 * @param modifier Modifier para dimensiones y posicionamiento
 */
@Composable
private fun RecordingWaveform(
    amplitudes: List<Int>,
    modifier: Modifier = Modifier
) {
    // Mostrar las últimas amplitudes que quepan en el espacio disponible
    val displayAmplitudes = if (amplitudes.size > 40) {
        amplitudes.takeLast(40)
    } else {
        amplitudes
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        displayAmplitudes.forEach { amplitude ->
            // Mapear amplitud (0-100) a altura (3.dp - 24.dp)
            val alturaBarraDp = (3 + (amplitude.coerceIn(0, 100) / 100f * 21f)).dp

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(alturaBarraDp)
                    .background(
                        color = Color.Red.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}
