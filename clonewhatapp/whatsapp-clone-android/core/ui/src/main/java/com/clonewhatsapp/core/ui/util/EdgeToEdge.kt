package com.clonewhatsapp.core.ui.util

import android.app.Activity
import android.graphics.Color
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Funciones auxiliares para habilitar la visualizacion
 * de borde a borde (edge-to-edge) en la aplicacion.
 *
 * T-113: Soporte edge-to-edge.
 *
 * Nota: La Activity principal (MainActivity) ya llama a enableEdgeToEdge()
 * de la biblioteca de Activity. Estas funciones proporcionan control
 * adicional y modificadores de padding para Compose.
 */

/**
 * Habilita la visualizacion de borde a borde en la Activity dada.
 *
 * Configura la ventana para que el contenido se dibuje detras de
 * las barras del sistema (barra de estado y barra de navegacion),
 * haciendolas transparentes.
 *
 * @param activity La Activity donde se habilitara el modo edge-to-edge.
 * @param barrasOscuras Si es true, los iconos de las barras del sistema
 *                      seran oscuros (para fondos claros). Si es false,
 *                      seran claros (para fondos oscuros).
 */
fun habilitarEdgeToEdge(
    activity: Activity,
    barrasOscuras: Boolean = true
) {
    // Permitir que el contenido se dibuje detras de las barras del sistema
    WindowCompat.setDecorFitsSystemWindows(activity.window, false)

    // Hacer transparentes las barras del sistema
    activity.window.statusBarColor = Color.TRANSPARENT
    activity.window.navigationBarColor = Color.TRANSPARENT

    // Configurar el color de los iconos de las barras del sistema
    val controlador = WindowInsetsControllerCompat(
        activity.window,
        activity.window.decorView
    )
    controlador.isAppearanceLightStatusBars = barrasOscuras
    controlador.isAppearanceLightNavigationBars = barrasOscuras
}

/**
 * Actualiza la apariencia de los iconos de las barras del sistema
 * segun el tema actual (claro u oscuro).
 *
 * Debe llamarse cuando cambie el tema de la aplicacion para
 * mantener la visibilidad correcta de los iconos.
 *
 * @param activity La Activity donde actualizar la apariencia.
 * @param esOscuro Si el tema actual es oscuro.
 */
fun actualizarBarrasSistema(
    activity: Activity,
    esOscuro: Boolean
) {
    val controlador = WindowInsetsControllerCompat(
        activity.window,
        activity.window.decorView
    )
    // Iconos claros en tema oscuro, iconos oscuros en tema claro
    controlador.isAppearanceLightStatusBars = !esOscuro
    controlador.isAppearanceLightNavigationBars = !esOscuro
}

/**
 * Modificador que agrega padding para la barra de estado.
 *
 * Uso:
 * ```kotlin
 * Box(modifier = Modifier.paddingBarraEstado()) { ... }
 * ```
 */
@Composable
fun Modifier.paddingBarraEstado(): Modifier {
    return this.windowInsetsPadding(WindowInsets.statusBars)
}

/**
 * Modificador que agrega padding para la barra de navegacion.
 *
 * Uso:
 * ```kotlin
 * Box(modifier = Modifier.paddingBarraNavegacion()) { ... }
 * ```
 */
@Composable
fun Modifier.paddingBarraNavegacion(): Modifier {
    return this.windowInsetsPadding(WindowInsets.navigationBars)
}

/**
 * Modificador que agrega padding para ambas barras del sistema
 * (barra de estado y barra de navegacion).
 *
 * Uso:
 * ```kotlin
 * Scaffold(modifier = Modifier.paddingBarrasSistema()) { ... }
 * ```
 */
@Composable
fun Modifier.paddingBarrasSistema(): Modifier {
    return this
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.navigationBars)
}
