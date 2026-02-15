package com.clonewhatsapp.feature.calls.video

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Boton circular semitransparente para cambiar entre camara frontal y trasera.
 * Incluye animacion de rotacion de 360 grados al hacer clic.
 *
 * @param onSwitch Callback que se ejecuta al presionar el boton.
 * @param modifier Modificador de Compose.
 */
@Composable
fun CameraSwitchButton(
    onSwitch: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Contador de clics para animar la rotacion
    var clickCount by remember { mutableIntStateOf(0) }

    // Angulo de rotacion animado: cada clic agrega 360 grados
    val rotationAngle by animateFloatAsState(
        targetValue = clickCount * 360f,
        animationSpec = tween(durationMillis = 400),
        label = "rotacion_camara"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable {
                clickCount++
                onSwitch()
            }
    ) {
        Icon(
            imageVector = Icons.Filled.FlipCameraAndroid,
            contentDescription = "Cambiar camara",
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
                .rotate(rotationAngle)
        )
    }
}
