package com.clonewhatsapp.core.common.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor de foco de audio para llamadas.
 *
 * Solicita y libera el foco de audio del sistema durante las llamadas,
 * configura el modo de audio para comunicacion y gestiona la ruta de audio
 * entre auricular y altavoz.
 */
@Singleton
class AudioFocusManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var audioFocusRequest: AudioFocusRequest? = null
    private var tieneFoco: Boolean = false
    private var altavozActivado: Boolean = false

    @Suppress("DEPRECATION")
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Foco de audio perdido: $focusChange")
                tieneFoco = false
            }
            AudioManager.AUDIOFOCUS_GAIN,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                Log.d(TAG, "Foco de audio obtenido: $focusChange")
                tieneFoco = true
            }
        }
    }

    /**
     * Solicita el foco de audio para una llamada.
     * Configura el modo de audio a MODE_IN_COMMUNICATION y dirige
     * el audio al auricular por defecto.
     *
     * @return true si el foco fue concedido.
     */
    fun requestCallAudioFocus(): Boolean {
        if (tieneFoco) return true

        val resultado: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener(focusChangeListener)
                .setAcceptsDelayedFocusGain(false)
                .build()

            audioFocusRequest = request
            resultado = audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            resultado = audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }

        if (resultado == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            tieneFoco = true
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

            // Por defecto: auricular (no altavoz)
            setSpeakerOn(false)

            Log.d(TAG, "Foco de audio concedido para llamada")
            return true
        }

        Log.w(TAG, "Foco de audio denegado para llamada")
        return false
    }

    /**
     * Libera el foco de audio y restaura el modo de audio normal.
     */
    fun abandonCallAudioFocus() {
        if (!tieneFoco) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
            }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }

        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
        altavozActivado = false
        tieneFoco = false

        Log.d(TAG, "Foco de audio liberado")
    }

    /**
     * Activa o desactiva el altavoz.
     *
     * @param enabled true para activar el altavoz, false para usar auricular.
     */
    fun setSpeakerOn(enabled: Boolean) {
        audioManager.isSpeakerphoneOn = enabled
        altavozActivado = enabled
        Log.d(TAG, "Altavoz ${if (enabled) "activado" else "desactivado"}")
    }

    /**
     * Indica si el altavoz esta activado.
     */
    fun isSpeakerOn(): Boolean = altavozActivado

    companion object {
        private const val TAG = "AudioFocusManager"
    }
}
