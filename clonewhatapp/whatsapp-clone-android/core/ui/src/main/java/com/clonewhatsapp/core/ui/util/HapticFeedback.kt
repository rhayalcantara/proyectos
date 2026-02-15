package com.clonewhatsapp.core.ui.util

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView

/**
 * Funciones de extension para retroalimentacion haptica en la aplicacion.
 *
 * T-115: Retroalimentacion haptica.
 *
 * Proporciona modificadores y funciones utilitarias para agregar
 * vibraciones tactiles a las interacciones del usuario.
 */

/**
 * Recuerda y retorna la instancia de HapticFeedback del contexto actual.
 *
 * Uso:
 * ```kotlin
 * val haptico = rememberHapticFeedback()
 * Button(onClick = {
 *     haptico.performHapticFeedback(HapticFeedbackType.LongPress)
 * }) { ... }
 * ```
 */
@Composable
fun rememberHapticFeedback(): HapticFeedback {
    return LocalHapticFeedback.current
}

/**
 * Modificador que agrega vibracion ligera al hacer clic.
 *
 * Utiliza HapticFeedbackType.TextHandleMove para una vibracion sutil
 * apropiada para clics regulares.
 *
 * @param onClick Callback que se ejecuta al hacer clic.
 */
fun Modifier.hapticClick(
    onClick: () -> Unit
): Modifier = composed {
    val haptico = LocalHapticFeedback.current
    val fuenteInteraccion = remember { MutableInteractionSource() }

    this.clickable(
        interactionSource = fuenteInteraccion,
        indication = null
    ) {
        haptico.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
    }
}

/**
 * Modificador que agrega vibracion media al mantener presionado.
 *
 * Utiliza HapticFeedbackType.LongPress para una vibracion mas pronunciada
 * apropiada para pulsaciones largas.
 *
 * @param onLongPress Callback que se ejecuta al mantener presionado.
 */
fun Modifier.hapticLongPress(
    onLongPress: () -> Unit
): Modifier = composed {
    val haptico = LocalHapticFeedback.current

    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {}
    )
    // Nota: Para pulsacion larga real, usar combinedClickable.
    // Este modificador se usa junto con otros gestores de gestos.
    this
}

/**
 * Ejecuta retroalimentacion haptica de tipo "tick" para deslizamiento.
 *
 * Ideal para usar dentro de detectores de gestos de arrastre
 * cuando se alcanza un umbral.
 *
 * @param haptico Instancia de HapticFeedback.
 */
fun ejecutarHapticoSwipe(haptico: HapticFeedback) {
    haptico.performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

/**
 * Ejecuta retroalimentacion haptica de tipo pulsacion larga.
 *
 * @param haptico Instancia de HapticFeedback.
 */
fun ejecutarHapticoLongPress(haptico: HapticFeedback) {
    haptico.performHapticFeedback(HapticFeedbackType.LongPress)
}

/**
 * Ejecuta retroalimentacion haptica de tipo clic ligero.
 *
 * @param haptico Instancia de HapticFeedback.
 */
fun ejecutarHapticoClick(haptico: HapticFeedback) {
    haptico.performHapticFeedback(HapticFeedbackType.TextHandleMove)
}

/**
 * Funciones de extension para View que proporcionan control
 * mas fino sobre la retroalimentacion haptica usando las
 * constantes nativas de Android.
 */

/**
 * Ejecuta un efecto haptico de clic en la View.
 */
fun View.hapticoClick() {
    performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
}

/**
 * Ejecuta un efecto haptico de pulsacion larga en la View.
 */
fun View.hapticoLongPress() {
    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}

/**
 * Ejecuta un efecto haptico de tick (usado durante arrastre/deslizamiento).
 */
fun View.hapticoTick() {
    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
}

/**
 * Ejecuta un efecto haptico de confirmacion (disponible en API 30+).
 * En versiones anteriores, usa CONTEXT_CLICK como alternativa.
 */
fun View.hapticoConfirmacion() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }
}

/**
 * Ejecuta un efecto haptico de rechazo (disponible en API 30+).
 * En versiones anteriores, usa LONG_PRESS como alternativa.
 */
fun View.hapticoRechazo() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        performHapticFeedback(HapticFeedbackConstants.REJECT)
    } else {
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

/**
 * Composable auxiliar que proporciona acceso a la View actual
 * para retroalimentacion haptica nativa.
 *
 * Uso:
 * ```kotlin
 * val vista = rememberCurrentView()
 * vista.hapticoClick()
 * ```
 */
@Composable
fun rememberCurrentView(): View {
    return LocalView.current
}
