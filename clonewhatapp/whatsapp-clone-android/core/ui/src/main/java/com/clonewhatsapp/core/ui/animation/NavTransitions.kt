package com.clonewhatsapp.core.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavBackStackEntry

/**
 * Duracion predeterminada para las transiciones de navegacion (milisegundos).
 *
 * T-110: Animaciones de transicion entre pantallas.
 */
private const val DURACION_TRANSICION_MS = 300

/**
 * Transicion de entrada: desliza desde la derecha con desvanecimiento.
 *
 * Se usa cuando se navega hacia adelante (push).
 */
val enterTransicion: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        initialOffsetX = { anchoCompleto -> anchoCompleto },
        animationSpec = tween(
            durationMillis = DURACION_TRANSICION_MS,
            easing = FastOutSlowInEasing
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = DURACION_TRANSICION_MS,
            easing = FastOutSlowInEasing
        )
    )
}

/**
 * Transicion de salida: desliza hacia la izquierda con desvanecimiento.
 *
 * Se usa cuando se navega hacia adelante y la pantalla actual sale.
 */
val exitTransicion: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        targetOffsetX = { anchoCompleto -> -anchoCompleto },
        animationSpec = tween(
            durationMillis = DURACION_TRANSICION_MS,
            easing = FastOutSlowInEasing
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = DURACION_TRANSICION_MS,
            easing = FastOutSlowInEasing
        )
    )
}

/**
 * Transicion de entrada al retroceder: desliza desde la izquierda con desvanecimiento.
 *
 * Se usa cuando se presiona "atras" y la pantalla anterior reaparece.
 */
val popEnterTransicion: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    slideInHorizontally(
        initialOffsetX = { anchoCompleto -> -anchoCompleto },
        animationSpec = tween(
            durationMillis = DURACION_TRANSICION_MS,
            easing = FastOutSlowInEasing
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = DURACION_TRANSICION_MS,
            easing = FastOutSlowInEasing
        )
    )
}

/**
 * Transicion de salida al retroceder: desliza hacia la derecha con desvanecimiento.
 *
 * Se usa cuando se presiona "atras" y la pantalla actual se cierra.
 */
val popExitTransicion: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    slideOutHorizontally(
        targetOffsetX = { anchoCompleto -> anchoCompleto },
        animationSpec = tween(
            durationMillis = DURACION_TRANSICION_MS,
            easing = FastOutSlowInEasing
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = DURACION_TRANSICION_MS,
            easing = FastOutSlowInEasing
        )
    )
}

/**
 * Objeto auxiliar que agrupa todas las transiciones de navegacion
 * para aplicarlas facilmente a un NavHost o composable de ruta.
 *
 * Uso tipico:
 * ```kotlin
 * NavHost(
 *     navController = navController,
 *     startDestination = "inicio",
 *     enterTransition = TransicionesNav.enterTransicion,
 *     exitTransition = TransicionesNav.exitTransicion,
 *     popEnterTransition = TransicionesNav.popEnterTransicion,
 *     popExitTransition = TransicionesNav.popExitTransicion
 * ) { ... }
 * ```
 */
object TransicionesNav {
    /** Transicion de entrada al navegar hacia adelante */
    val enterTransicion = com.clonewhatsapp.core.ui.animation.enterTransicion

    /** Transicion de salida al navegar hacia adelante */
    val exitTransicion = com.clonewhatsapp.core.ui.animation.exitTransicion

    /** Transicion de entrada al retroceder */
    val popEnterTransicion = com.clonewhatsapp.core.ui.animation.popEnterTransicion

    /** Transicion de salida al retroceder */
    val popExitTransicion = com.clonewhatsapp.core.ui.animation.popExitTransicion
}
