package com.clonewhatsapp.core.common.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado de reproduccion de audio.
 *
 * @property isPlaying true si se esta reproduciendo activamente
 * @property currentPositionMs posicion actual en milisegundos
 * @property durationMs duracion total del audio en milisegundos
 * @property currentAudioId identificador del audio actualmente cargado
 */
data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val currentAudioId: String? = null
)

/**
 * Gestor centralizado de reproduccion de audio.
 *
 * Singleton que garantiza que solo un audio se reproduce a la vez.
 * Usa Media3 ExoPlayer internamente y expone el estado de reproduccion
 * como un StateFlow reactivo.
 *
 * Caracteristicas:
 * - Reproduccion unica: si se llama play() mientras otro audio suena, se detiene el anterior
 * - Seguimiento de posicion cada 200ms para actualizar seekbar
 * - Auto-stop al completar la reproduccion
 * - Compatible con DisposableEffect (llamar stop() al disponer)
 */
@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Intervalo de actualizacion de posicion en milisegundos. */
        private const val POSITION_UPDATE_INTERVAL_MS = 200L
    }

    private var exoPlayer: ExoPlayer? = null
    private var positionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _playbackState = MutableStateFlow(AudioPlaybackState())

    /** Estado reactivo de la reproduccion. */
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    /**
     * Reproduce un archivo de audio.
     *
     * Si otro audio esta reproduciendose, se detiene primero.
     * Si el mismo audio esta reproduciendose, actua como toggle (pausa/resume).
     *
     * @param audioId identificador unico del mensaje de audio
     * @param uri URI del archivo de audio (puede ser local o remota)
     */
    @OptIn(UnstableApi::class)
    fun play(audioId: String, uri: String) {
        val currentState = _playbackState.value

        // Si es el mismo audio y esta reproduciendose, pausar
        if (currentState.currentAudioId == audioId && currentState.isPlaying) {
            pause()
            return
        }

        // Si es el mismo audio pero pausado, reanudar
        if (currentState.currentAudioId == audioId && !currentState.isPlaying && currentState.currentPositionMs > 0) {
            resume()
            return
        }

        // Detener reproduccion anterior si existe
        stopInternal()

        try {
            val player = ExoPlayer.Builder(context).build().apply {
                val mediaItem = MediaItem.fromUri(uri)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                _playbackState.value = _playbackState.value.copy(
                                    durationMs = duration.coerceAtLeast(0)
                                )
                            }
                            Player.STATE_ENDED -> {
                                onPlaybackCompleted()
                            }
                            else -> { /* No-op */ }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playbackState.value = _playbackState.value.copy(
                            isPlaying = isPlaying
                        )
                    }
                })
            }

            exoPlayer = player

            _playbackState.value = AudioPlaybackState(
                isPlaying = true,
                currentPositionMs = 0,
                durationMs = 0,
                currentAudioId = audioId
            )

            startPositionTracking()
        } catch (e: Exception) {
            _playbackState.value = AudioPlaybackState()
        }
    }

    /**
     * Pausa la reproduccion actual.
     */
    fun pause() {
        exoPlayer?.pause()
        positionJob?.cancel()
        positionJob = null
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false
        )
    }

    /**
     * Reanuda la reproduccion pausada.
     */
    fun resume() {
        exoPlayer?.play()
        _playbackState.value = _playbackState.value.copy(
            isPlaying = true
        )
        startPositionTracking()
    }

    /**
     * Detiene la reproduccion y libera recursos.
     * Llamar este metodo en DisposableEffect onDispose.
     */
    fun stop() {
        stopInternal()
        _playbackState.value = AudioPlaybackState()
    }

    /**
     * Busca a una posicion especifica en el audio actual.
     *
     * @param positionMs posicion en milisegundos a la que saltar
     */
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(
            currentPositionMs = positionMs
        )
    }

    // ---------- Metodos privados ----------

    private fun stopInternal() {
        positionJob?.cancel()
        positionJob = null
        exoPlayer?.apply {
            stop()
            release()
        }
        exoPlayer = null
    }

    private fun onPlaybackCompleted() {
        positionJob?.cancel()
        positionJob = null
        val duration = _playbackState.value.durationMs
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            currentPositionMs = duration
        )
        // Liberar el player al completar
        exoPlayer?.apply {
            stop()
            release()
        }
        exoPlayer = null
    }

    private fun startPositionTracking() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                val player = exoPlayer ?: break
                val position = player.currentPosition.coerceAtLeast(0)
                val duration = player.duration.coerceAtLeast(0)

                _playbackState.value = _playbackState.value.copy(
                    currentPositionMs = position,
                    durationMs = if (duration > 0) duration else _playbackState.value.durationMs
                )

                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }
}
