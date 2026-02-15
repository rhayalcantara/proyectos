package com.clonewhatsapp.feature.calls.video

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

/**
 * Tipos de escalado para el renderizador de video WebRTC.
 */
enum class ScalingType {
    SCALE_ASPECT_FIT,
    SCALE_ASPECT_FILL,
    SCALE_ASPECT_BALANCED;

    /**
     * Convierte al tipo de escalado nativo de WebRTC.
     */
    fun toWebRTCScalingType(): RendererCommon.ScalingType = when (this) {
        SCALE_ASPECT_FIT -> RendererCommon.ScalingType.SCALE_ASPECT_FIT
        SCALE_ASPECT_FILL -> RendererCommon.ScalingType.SCALE_ASPECT_FILL
        SCALE_ASPECT_BALANCED -> RendererCommon.ScalingType.SCALE_ASPECT_BALANCED
    }
}

/**
 * Composable que envuelve el SurfaceViewRenderer de WebRTC para renderizar
 * pistas de video en Jetpack Compose.
 *
 * @param videoTrack La pista de video a renderizar, null muestra fondo oscuro.
 * @param isMirror Si se debe espejar el video (para camara frontal/vista local).
 * @param scalingType Tipo de escalado del video en el contenedor.
 * @param modifier Modificador de Compose.
 */
@Composable
fun WebRTCVideoRenderer(
    videoTrack: VideoTrack?,
    isMirror: Boolean = false,
    scalingType: ScalingType = ScalingType.SCALE_ASPECT_FILL,
    modifier: Modifier = Modifier
) {
    // Contexto EGL compartido para la inicializacion del renderer
    val eglBaseContext = remember {
        EglBase.create().eglBaseContext
    }

    // Referencia al renderer actual para manejar el ciclo de vida
    var currentRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var currentTrack by remember { mutableStateOf<VideoTrack?>(null) }

    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                init(eglBaseContext, null)
                setMirror(isMirror)
                setScalingType(scalingType.toWebRTCScalingType())
                setEnableHardwareScaler(true)
                currentRenderer = this
            }
        },
        update = { renderer ->
            // Actualizar configuracion de espejado
            renderer.setMirror(isMirror)
            renderer.setScalingType(scalingType.toWebRTCScalingType())

            // Si la pista cambio, remover la anterior y agregar la nueva
            if (currentTrack != videoTrack) {
                currentTrack?.removeSink(renderer)
                videoTrack?.addSink(renderer)
                currentTrack = videoTrack
            }
        },
        modifier = modifier.background(Color.Black)
    )

    // Limpiar recursos al descomponer
    DisposableEffect(Unit) {
        onDispose {
            currentTrack?.removeSink(currentRenderer)
            currentRenderer?.release()
            currentRenderer = null
            currentTrack = null
        }
    }
}
