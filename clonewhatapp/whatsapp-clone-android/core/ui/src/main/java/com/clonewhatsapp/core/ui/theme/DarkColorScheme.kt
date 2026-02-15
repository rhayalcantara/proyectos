package com.clonewhatsapp.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Esquema de colores oscuros completo para Material 3,
 * basado en los colores de WhatsApp modo oscuro.
 *
 * T-111: Tema oscuro completo.
 *
 * Colores principales de WhatsApp oscuro:
 * - Fondo: #111B21
 * - Superficie: #1F2C34
 * - Primario (teal): #00A884
 * - Burbuja saliente: #005C4B
 * - Burbuja entrante: #202C33
 */

// Colores adicionales del tema oscuro de WhatsApp
private val DarkOnBackground = Color(0xFFE9EDEF)
private val DarkOnSurface = Color(0xFFE9EDEF)
private val DarkOnSurfaceVariant = Color(0xFF8696A0)
private val DarkOutline = Color(0xFF374045)
private val DarkError = Color(0xFFEF5350)
private val DarkOnError = Color(0xFFFFFFFF)
private val DarkErrorContainer = Color(0xFF93000A)
private val DarkOnErrorContainer = Color(0xFFFFDAD6)
private val DarkInverseSurface = Color(0xFFE9EDEF)
private val DarkInverseOnSurface = Color(0xFF1F2C34)
private val DarkInversePrimary = Color(0xFF075E54)
private val DarkSecondaryContainer = Color(0xFF2A3942)
private val DarkOnSecondaryContainer = Color(0xFFE9EDEF)
private val DarkTertiaryColor = Color(0xFF53BDEB)
private val DarkOnTertiary = Color(0xFF003544)
private val DarkTertiaryContainer = Color(0xFF004D64)
private val DarkOnTertiaryContainer = Color(0xFFBFE9FF)
private val DarkOnPrimaryContainer = Color(0xFFB2DFDB)
private val DarkScrim = Color(0xFF000000)
private val DarkSurfaceTint = WhatsAppTealGreen

/**
 * Esquema completo de colores oscuros estilo WhatsApp para Material 3.
 *
 * Se integra con el tema existente en Theme.kt como alternativa
 * al esquema oscuro basico ya definido, proporcionando cobertura
 * completa de todos los roles de color de Material 3.
 */
val WhatsAppDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,                               // #00A884 - Teal WhatsApp
    onPrimary = DarkOnPrimary,                           // #003731
    primaryContainer = DarkPrimaryContainer,             // #00504A
    onPrimaryContainer = DarkOnPrimaryContainer,         // #B2DFDB
    secondary = DarkSecondary,                           // #25D366 - Verde claro
    onSecondary = Color(0xFF003919),                     // Verde muy oscuro
    secondaryContainer = DarkSecondaryContainer,         // #2A3942
    onSecondaryContainer = DarkOnSecondaryContainer,     // #E9EDEF
    tertiary = DarkTertiaryColor,                        // #53BDEB - Azul de checks
    onTertiary = DarkOnTertiary,                         // #003544
    tertiaryContainer = DarkTertiaryContainer,           // #004D64
    onTertiaryContainer = DarkOnTertiaryContainer,       // #BFE9FF
    error = DarkError,                                   // #EF5350
    onError = DarkOnError,                               // Blanco
    errorContainer = DarkErrorContainer,                 // #93000A
    onErrorContainer = DarkOnErrorContainer,             // #FFDAD6
    background = DarkBackground,                         // #111B21
    onBackground = DarkOnBackground,                     // #E9EDEF
    surface = DarkSurface,                               // #1F2C34
    onSurface = DarkOnSurface,                           // #E9EDEF
    surfaceVariant = DarkSurfaceVariant,                 // #233138
    onSurfaceVariant = DarkOnSurfaceVariant,             // #8696A0
    outline = DarkOutline,                               // #374045
    outlineVariant = Color(0xFF2A3942),                  // Borde sutil
    inverseSurface = DarkInverseSurface,                 // #E9EDEF
    inverseOnSurface = DarkInverseOnSurface,             // #1F2C34
    inversePrimary = DarkInversePrimary,                 // #075E54
    scrim = DarkScrim,                                   // Negro
    surfaceTint = DarkSurfaceTint                        // #00A884
)
