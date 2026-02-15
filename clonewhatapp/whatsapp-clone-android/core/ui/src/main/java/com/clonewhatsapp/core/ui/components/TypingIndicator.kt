package com.clonewhatsapp.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Indicador animado de "escribiendo..." con tres puntos que rebotan
 * secuencialmente (estilo WhatsApp).
 *
 * T-109: Indicador de escritura animado.
 *
 * @param modifier Modificador opcional para personalizar el layout.
 * @param colorPunto Color de los puntos animados.
 * @param mostrarTexto Si se muestra el texto "escribiendo..." junto a los puntos.
 */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier,
    colorPunto: Color = Color(0xFF8696A0),
    mostrarTexto: Boolean = true
) {
    // Duracion total del ciclo de animacion
    val duracionCiclo = 900

    val transicionInfinita = rememberInfiniteTransition(label = "escribiendo")

    // Crear animacion de rebote para cada punto con desfase
    val desplazamientoPunto1 by animarPuntoRebote(
        transicion = transicionInfinita,
        desfaseMs = 0,
        duracionMs = duracionCiclo,
        label = "punto1"
    )

    val desplazamientoPunto2 by animarPuntoRebote(
        transicion = transicionInfinita,
        desfaseMs = 150,
        duracionMs = duracionCiclo,
        label = "punto2"
    )

    val desplazamientoPunto3 by animarPuntoRebote(
        transicion = transicionInfinita,
        desfaseMs = 300,
        duracionMs = duracionCiclo,
        label = "punto3"
    )

    // Contenedor con forma de burbuja de chat
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Tres puntos animados
        PuntoAnimado(desplazamientoY = desplazamientoPunto1, color = colorPunto)
        PuntoAnimado(desplazamientoY = desplazamientoPunto2, color = colorPunto)
        PuntoAnimado(desplazamientoY = desplazamientoPunto3, color = colorPunto)

        // Texto opcional "escribiendo..."
        if (mostrarTexto) {
            Text(
                text = "escribiendo...",
                style = MaterialTheme.typography.bodySmall,
                color = colorPunto,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Un punto circular individual con desplazamiento vertical animado.
 *
 * @param desplazamientoY Desplazamiento vertical en pixeles (negativo = arriba).
 * @param color Color del punto.
 */
@Composable
private fun PuntoAnimado(
    desplazamientoY: Float,
    color: Color
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(0, desplazamientoY.roundToInt()) }
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Crea una animacion de rebote infinita para un punto individual.
 *
 * El punto sube (desplazamiento negativo) y luego regresa a su posicion original.
 * Cada punto tiene un desfase diferente para crear el efecto secuencial.
 *
 * @param transicion Transicion infinita compartida.
 * @param desfaseMs Desfase en milisegundos respecto al inicio del ciclo.
 * @param duracionMs Duracion total del ciclo de animacion.
 * @param label Etiqueta para identificar la animacion.
 * @return Estado animado con el desplazamiento Y en pixeles.
 */
@Composable
private fun animarPuntoRebote(
    transicion: androidx.compose.animation.core.InfiniteTransition,
    desfaseMs: Int,
    duracionMs: Int,
    label: String
): androidx.compose.runtime.State<Float> {
    return transicion.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = duracionMs
                // Posicion inicial
                0f at desfaseMs
                // Subir (rebote)
                -12f at desfaseMs + 150
                // Bajar
                0f at desfaseMs + 300
            },
            repeatMode = RepeatMode.Restart
        ),
        label = label
    )
}
