package com.clonewhatsapp.core.common.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estado de la grabacion de audio.
 */
sealed interface RecordingState {
    /** Sin actividad de grabacion. */
    data object Idle : RecordingState

    /** Grabando activamente. */
    data class Recording(
        val durationMs: Long,
        val amplitudes: List<Int>
    ) : RecordingState

    /** Grabacion pausada (SDK 24+). */
    data object Paused : RecordingState

    /** Grabacion completada exitosamente. */
    data class Completed(
        val uri: Uri,
        val durationMs: Long
    ) : RecordingState

    /** Error durante la grabacion. */
    data class Error(val message: String) : RecordingState
}

/**
 * Gestor de grabacion de audio usando MediaRecorder.
 *
 * Graba audio en formato M4A (MPEG_4 + AAC) y expone el estado
 * como un StateFlow reactivo para la UI.
 *
 * Caracteristicas:
 * - Grabacion con seguimiento de duracion en tiempo real (cada 100ms)
 * - Captura de amplitudes para visualizacion de forma de onda
 * - Soporte para pausar/reanudar (SDK 24+)
 * - Duracion maxima de 5 minutos (300 segundos)
 * - Limpieza automatica de archivos temporales al cancelar
 */
@Singleton
class AudioRecorderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Duracion maxima de grabacion en milisegundos (5 minutos). */
        private const val MAX_RECORDING_DURATION_MS = 300_000L

        /** Intervalo de actualizacion del timer y amplitud en milisegundos. */
        private const val UPDATE_INTERVAL_MS = 100L

        /** Cantidad maxima de muestras de amplitud a mantener en el estado. */
        private const val MAX_AMPLITUDE_SAMPLES = 50

        /** Directorio de salida para archivos de audio. */
        private const val AUDIO_DIR = "audio"

        /** Prefijo de archivos de audio. */
        private const val FILE_PREFIX = "AUD_"

        /** Extension de archivos de audio. */
        private const val FILE_EXTENSION = ".m4a"
    }

    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var timerJob: Job? = null
    private var recordingStartTime: Long = 0L
    private var pausedDurationMs: Long = 0L
    private val amplitudeList = mutableListOf<Int>()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)

    /** Estado reactivo de la grabacion. */
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    /**
     * Inicia una nueva grabacion de audio.
     *
     * Verifica que el permiso RECORD_AUDIO este concedido antes de iniciar.
     * Si ya hay una grabacion en progreso, se detiene primero.
     */
    fun startRecording() {
        // Verificar permiso de grabacion
        if (!hasRecordPermission()) {
            _recordingState.value = RecordingState.Error(
                "Se requiere permiso de grabacion de audio"
            )
            return
        }

        // Detener grabacion previa si existe
        if (mediaRecorder != null) {
            stopAndCleanup()
        }

        try {
            val audioFile = createOutputFile()
            currentFile = audioFile

            val recorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setMaxDuration(MAX_RECORDING_DURATION_MS.toInt())
                setOutputFile(audioFile.absolutePath)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        stopRecording()
                    }
                }
                prepare()
                start()
            }

            mediaRecorder = recorder
            amplitudeList.clear()
            pausedDurationMs = 0L
            recordingStartTime = System.currentTimeMillis()

            _recordingState.value = RecordingState.Recording(
                durationMs = 0L,
                amplitudes = emptyList()
            )

            startTimer()
        } catch (e: Exception) {
            cleanupCurrentFile()
            _recordingState.value = RecordingState.Error(
                "Error al iniciar grabacion: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Detiene la grabacion actual y retorna el URI del archivo generado.
     *
     * @return URI del archivo de audio grabado, o null si hubo un error.
     */
    fun stopRecording(): Uri? {
        val recorder = mediaRecorder ?: return null
        val file = currentFile ?: return null

        return try {
            timerJob?.cancel()
            timerJob = null

            recorder.stop()
            recorder.release()
            mediaRecorder = null

            val durationMs = calculateCurrentDuration()
            val uri = file.toUri()

            _recordingState.value = RecordingState.Completed(
                uri = uri,
                durationMs = durationMs
            )

            uri
        } catch (e: Exception) {
            cleanupCurrentFile()
            mediaRecorder = null
            _recordingState.value = RecordingState.Error(
                "Error al detener grabacion: ${e.localizedMessage}"
            )
            null
        }
    }

    /**
     * Cancela la grabacion actual y elimina el archivo temporal.
     */
    fun cancelRecording() {
        stopAndCleanup()
        cleanupCurrentFile()
        _recordingState.value = RecordingState.Idle
    }

    /**
     * Pausa la grabacion actual (disponible desde SDK 24).
     */
    fun pauseRecording() {
        val recorder = mediaRecorder ?: return

        try {
            recorder.pause()
            pausedDurationMs = calculateCurrentDuration()
            timerJob?.cancel()
            timerJob = null
            _recordingState.value = RecordingState.Paused
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error(
                "Error al pausar grabacion: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Reanuda una grabacion pausada.
     */
    fun resumeRecording() {
        val recorder = mediaRecorder ?: return

        try {
            recorder.resume()
            recordingStartTime = System.currentTimeMillis()
            _recordingState.value = RecordingState.Recording(
                durationMs = pausedDurationMs,
                amplitudes = amplitudeList.toList()
            )
            startTimer()
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error(
                "Error al reanudar grabacion: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Restablece el estado a Idle sin limpiar archivos.
     * Util despues de procesar un estado Completed o Error.
     */
    fun resetState() {
        _recordingState.value = RecordingState.Idle
    }

    // ---------- Metodos privados ----------

    private fun hasRecordPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    private fun createOutputFile(): File {
        val audioDir = File(context.cacheDir, AUDIO_DIR).apply {
            if (!exists()) mkdirs()
        }
        val timestamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())
        return File(audioDir, "$FILE_PREFIX$timestamp$FILE_EXTENSION")
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(UPDATE_INTERVAL_MS)

                val currentDuration = calculateCurrentDuration()

                // Verificar duracion maxima
                if (currentDuration >= MAX_RECORDING_DURATION_MS) {
                    stopRecording()
                    return@launch
                }

                // Capturar amplitud
                val amplitude = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    0
                }

                // Normalizar amplitud a rango 0-100
                val normalizedAmplitude = (amplitude / 327.67).toInt().coerceIn(0, 100)
                amplitudeList.add(normalizedAmplitude)

                // Mantener solo las ultimas MAX_AMPLITUDE_SAMPLES muestras
                if (amplitudeList.size > MAX_AMPLITUDE_SAMPLES) {
                    amplitudeList.removeAt(0)
                }

                _recordingState.value = RecordingState.Recording(
                    durationMs = currentDuration,
                    amplitudes = amplitudeList.toList()
                )
            }
        }
    }

    private fun calculateCurrentDuration(): Long {
        return pausedDurationMs + (System.currentTimeMillis() - recordingStartTime)
    }

    private fun stopAndCleanup() {
        timerJob?.cancel()
        timerJob = null
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
            // Ignorar errores al detener — puede ocurrir si no se inicio correctamente
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {
                // Ignorar
            }
        }
        mediaRecorder = null
    }

    private fun cleanupCurrentFile() {
        currentFile?.let { file ->
            if (file.exists()) {
                file.delete()
            }
        }
        currentFile = null
    }
}
