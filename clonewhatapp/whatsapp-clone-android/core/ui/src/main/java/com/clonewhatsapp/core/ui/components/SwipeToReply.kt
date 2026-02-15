package com.clonewhatsapp.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Contenedor que permite deslizar una burbuja de chat hacia la derecha
 * para activar la accion de responder (estilo WhatsApp).
 *
 * T-107: Deslizar para responder.
 *
 * @param messageId Identificador del mensaje asociado.
 * @param onReply Callback que se invoca cuando el usuario supera el umbral de deslizamiento.
 * @param modifier Modificador opcional para personalizar el layout.
 * @param content Contenido composable (la burbuja de chat).
 */
@Composable
fun SwipeToReply(
    messageId: String,
    onReply: (messageId: String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Umbral de deslizamiento en dp
    val umbralDp = 80.dp
    val densidad = LocalDensity.current
    val umbralPx = with(densidad) { umbralDp.toPx() }

    val retroalimentacionHaptica = LocalHapticFeedback.current
    val alcance = rememberCoroutineScope()

    // Desplazamiento animable para el eje X
    val desplazamientoX = remember { Animatable(0f) }
    // Controla si ya se disparo la retroalimentacion haptica en este gesto
    var hapticoDisparado by remember { mutableStateOf(false) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        // Icono de respuesta que aparece detras de la burbuja
        val progreso = (desplazamientoX.value / umbralPx).coerceIn(0f, 1f)
        Icon(
            imageVector = Icons.Default.Reply,
            contentDescription = "Responder",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 12.dp)
                .size(24.dp)
                .alpha(progreso)
        )

        // Contenido deslizable (burbuja de chat)
        Box(
            modifier = Modifier
                .offset { IntOffset(desplazamientoX.value.roundToInt(), 0) }
                .pointerInput(messageId) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            hapticoDisparado = false
                        },
                        onDragEnd = {
                            // Si supero el umbral, invocar el callback
                            if (desplazamientoX.value >= umbralPx) {
                                onReply(messageId)
                            }
                            // Animar de vuelta a la posicion original con efecto de resorte
                            alcance.launch {
                                desplazamientoX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.6f,
                                        stiffness = 600f
                                    )
                                )
                            }
                        },
                        onDragCancel = {
                            alcance.launch {
                                desplazamientoX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.6f,
                                        stiffness = 600f
                                    )
                                )
                            }
                        },
                        onHorizontalDrag = { cambio, arrastre ->
                            alcance.launch {
                                // Solo permitir deslizar hacia la derecha, maximo el umbral + margen
                                val nuevoValor = (desplazamientoX.value + arrastre)
                                    .coerceIn(0f, umbralPx * 1.2f)
                                desplazamientoX.snapTo(nuevoValor)

                                // Retroalimentacion haptica al alcanzar el umbral
                                if (nuevoValor >= umbralPx && !hapticoDisparado) {
                                    retroalimentacionHaptica.performHapticFeedback(
                                        HapticFeedbackType.LongPress
                                    )
                                    hapticoDisparado = true
                                }
                            }
                            cambio.consume()
                        }
                    )
                }
        ) {
            content()
        }
    }
}
