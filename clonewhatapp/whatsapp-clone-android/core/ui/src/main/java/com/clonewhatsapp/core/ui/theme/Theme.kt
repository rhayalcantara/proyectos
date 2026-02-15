package com.clonewhatsapp.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Esquema de colores claros personalizado estilo WhatsApp.
 *
 * Utiliza los colores definidos en Color.kt para el tema claro.
 */
private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    secondary = LightSecondary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
)

/**
 * Tema principal de la aplicacion WhatsApp Clone.
 *
 * T-112: Soporte de colores dinamicos para Android 12+ (SDK 31).
 *
 * Logica de seleccion de esquema de colores:
 * 1. Si colores dinamicos estan habilitados Y el dispositivo es Android 12+,
 *    usa los colores dinamicos del sistema (Material You).
 * 2. Si no hay colores dinamicos disponibles, usa los colores personalizados
 *    de WhatsApp (oscuro u claro segun la preferencia del sistema).
 *
 * @param darkTheme Si se debe usar el tema oscuro. Por defecto sigue la
 *                  preferencia del sistema con isSystemInDarkTheme().
 * @param dynamicColor Si se deben usar colores dinamicos de Android 12+.
 *                     Por defecto esta habilitado (true).
 * @param content Contenido composable al que se aplica el tema.
 */
@Composable
fun WhatsAppCloneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Colores dinamicos de Material You (Android 12+)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val contexto = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(contexto)
            else dynamicLightColorScheme(contexto)
        }
        // Tema oscuro personalizado de WhatsApp (esquema completo de T-111)
        darkTheme -> WhatsAppDarkColorScheme
        // Tema claro personalizado de WhatsApp
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
