package com.clonewhatsapp.feature.calls.pip

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Utilidad para manejar el modo Picture-in-Picture (PiP) en llamadas de video.
 * Solo funciona en Android 8.0 (SDK 26) y superiores.
 */
object PipHelper {

    /**
     * Entra en modo Picture-in-Picture para la actividad dada.
     *
     * @param activity La actividad que entrara en modo PiP.
     */
    fun enterPipMode(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(9, 16))
                .build()
            activity.enterPictureInPictureMode(params)
        }
    }

    /**
     * Verifica si el modo PiP esta soportado en el dispositivo.
     *
     * @param context El contexto de la aplicacion.
     * @return true si PiP esta soportado, false en caso contrario.
     */
    fun isPipSupported(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        } else {
            false
        }
    }

    /**
     * Actualiza los parametros de PiP, por ejemplo cuando cambia la orientacion del video.
     *
     * @param activity La actividad en modo PiP.
     * @param aspectRatio La nueva relacion de aspecto.
     */
    fun updatePipParams(activity: Activity, aspectRatio: Rational) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
            activity.setPictureInPictureParams(params)
        }
    }
}

/**
 * Composable que detecta cambios en el modo Picture-in-Picture.
 * Retorna true si la actividad actual esta en modo PiP.
 *
 * Utiliza el ciclo de vida de la actividad para detectar transiciones
 * hacia y desde el modo PiP.
 *
 * @return true si esta en modo PiP, false en caso contrario.
 */
@Composable
fun rememberIsInPipMode(): Boolean {
    val context = LocalContext.current
    val activity = context as? Activity
    var isInPipMode by remember { mutableStateOf(false) }

    if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        // Verificar si entramos en PiP cuando se pausa la actividad
                        isInPipMode = activity.isInPictureInPictureMode
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        // Al volver del PiP, la actividad se reanuda
                        isInPipMode = false
                    }
                    else -> { /* No hacer nada */ }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
    }

    return isInPipMode
}
