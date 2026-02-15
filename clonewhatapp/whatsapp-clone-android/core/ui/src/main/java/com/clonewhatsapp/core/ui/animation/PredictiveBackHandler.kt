package com.clonewhatsapp.core.ui.animation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import kotlinx.coroutines.launch

/**
 * Composable que maneja el gesto de retroceso predictivo de Android 14+.
 *
 * Aplica una animacion de escala y desvanecimiento al contenido
 * cuando el usuario realiza el gesto de retroceso. En versiones
 * anteriores a Android 14, simplemente intercepta el boton de retroceso.
 *
 * T-114: Gesto de retroceso predictivo.
 *
 * @param habilitado Si el manejador de retroceso esta habilitado.
 * @param onBack Callback que se invoca cuando se completa el gesto de retroceso.
 * @param modifier Modificador opcional para personalizar el layout.
 * @param content Contenido composable que se anima durante el retroceso.
 */
@Composable
fun PredictiveBackContainer(
    habilitado: Boolean = true,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alcance = rememberCoroutineScope()

    // Valores animables para escala y opacidad
    val escala = remember { Animatable(1f) }
    val opacidad = remember { Animatable(1f) }

    // Manejador del gesto de retroceso
    BackHandler(enabled = habilitado) {
        alcance.launch {
            // Ejecutar animaciones de salida en paralelo
            launch {
                escala.animateTo(
                    targetValue = 0.9f,
                    animationSpec = tween(durationMillis = 200)
                )
            }
            launch {
                opacidad.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 200)
                )
            }
        }.invokeOnCompletion {
            onBack()
            // Restaurar valores para futuras animaciones
            alcance.launch {
                escala.snapTo(1f)
                opacidad.snapTo(1f)
            }
        }
    }

    Box(
        modifier = modifier
            .scale(escala.value)
            .alpha(opacidad.value)
    ) {
        content()
    }
}
