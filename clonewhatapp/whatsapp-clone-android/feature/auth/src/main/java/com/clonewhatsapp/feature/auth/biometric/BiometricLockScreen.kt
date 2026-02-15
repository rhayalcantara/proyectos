package com.clonewhatsapp.feature.auth.biometric

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Pantalla de bloqueo biometrico.
 *
 * Se muestra cuando la aplicacion esta bloqueada y requiere autenticacion
 * biometrica (huella digital, rostro) o credencial de dispositivo (PIN/patron)
 * para acceder al contenido.
 *
 * La autenticacion se dispara automaticamente al aparecer la pantalla.
 * El usuario tambien puede tocar la pantalla para reiniciar el proceso.
 *
 * @param onAuthenticateRequest Callback para solicitar autenticacion biometrica.
 *        Debe invocar BiometricAuthManager.authenticate() en la Activity contenedora.
 * @param modifier Modificador de Compose para personalizar el layout
 */
@Composable
fun BiometricLockScreen(
    onAuthenticateRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Disparar autenticacion automaticamente al aparecer la pantalla
    LaunchedEffect(Unit) {
        onAuthenticateRequest()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    // Tocar la pantalla reinicia la autenticacion
                    onAuthenticateRequest()
                }
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icono de candado (representa WhatsApp bloqueado)
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Aplicacion bloqueada",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Nombre de la aplicacion
            Text(
                text = "WhatsApp Clone",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Icono de huella digital
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Autenticacion biometrica",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Instruccion para el usuario
            Text(
                text = "Toca para desbloquear",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtexto con informacion adicional
            Text(
                text = "Usa tu huella, rostro o PIN para acceder",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
