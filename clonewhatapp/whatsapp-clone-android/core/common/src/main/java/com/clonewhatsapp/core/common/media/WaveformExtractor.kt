package com.clonewhatsapp.core.common.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Extrae datos de forma de onda de un archivo de audio para visualizacion.
 *
 * Usa MediaExtractor y MediaCodec para decodificar el audio a PCM
 * y calcula amplitudes RMS por segmento. El resultado es una lista
 * de valores normalizados (0-100) que se pueden usar para dibujar
 * barras de forma de onda en la UI.
 *
 * Si la extraccion falla por cualquier razon, retorna valores
 * aleatorios como fallback para mantener la visualizacion funcional.
 */
object WaveformExtractor {

    /**
     * Genera una lista de amplitudes normalizadas (0-100) para visualizar.
     *
     * @param context Context para acceder a ContentResolver
     * @param uri URI del archivo de audio
     * @param samples Numero de muestras a generar (default 50)
     * @return Lista de amplitudes normalizadas entre 0 y 100
     */
    suspend fun extractWaveform(
        context: Context,
        uri: Uri,
        samples: Int = 50
    ): List<Int> = withContext(Dispatchers.Default) {
        try {
            extractWaveformInternal(context, uri, samples)
        } catch (e: Exception) {
            // Fallback: generar forma de onda aleatoria para mantener la visualizacion
            generateFallbackWaveform(samples)
        }
    }

    private fun extractWaveformInternal(
        context: Context,
        uri: Uri,
        samples: Int
    ): List<Int> {
        val extractor = MediaExtractor()

        try {
            extractor.setDataSource(context, uri, null)

            // Buscar la pista de audio
            val audioTrackIndex = findAudioTrack(extractor)
            if (audioTrackIndex < 0) {
                return generateFallbackWaveform(samples)
            }

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return generateFallbackWaveform(samples)

            // Configurar el decodificador
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val allSamples = mutableListOf<Short>()
            val bufferInfo = MediaCodec.BufferInfo()
            var isEOS = false

            // Decodificar todo el audio a PCM
            while (!isEOS) {
                // Alimentar el decodificador
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)
                    if (inputBuffer != null) {
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inputIndex, 0, 0, 0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            isEOS = true
                        } else {
                            codec.queueInputBuffer(
                                inputIndex, 0, sampleSize,
                                extractor.sampleTime, 0
                            )
                            extractor.advance()
                        }
                    }
                }

                // Leer salida decodificada
                var outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                while (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        // Extraer muestras PCM de 16 bits
                        val pcmData = extractPcmSamples(outputBuffer, bufferInfo.size)
                        allSamples.addAll(pcmData)
                    }

                    codec.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isEOS = true
                        break
                    }

                    outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                }
            }

            codec.stop()
            codec.release()

            if (allSamples.isEmpty()) {
                return generateFallbackWaveform(samples)
            }

            // Dividir las muestras en segmentos y calcular RMS por segmento
            return calculateRmsAmplitudes(allSamples, samples)
        } finally {
            extractor.release()
        }
    }

    /**
     * Busca el indice de la primera pista de audio en el extractor.
     */
    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                return i
            }
        }
        return -1
    }

    /**
     * Extrae muestras PCM de 16 bits del ByteBuffer decodificado.
     */
    private fun extractPcmSamples(buffer: ByteBuffer, size: Int): List<Short> {
        val samples = mutableListOf<Short>()
        val shortBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val numSamples = size / 2 // 16 bits = 2 bytes por muestra

        for (i in 0 until numSamples.coerceAtMost(shortBuffer.remaining())) {
            samples.add(shortBuffer.get())
        }

        return samples
    }

    /**
     * Calcula amplitudes RMS normalizadas (0-100) dividiendo las muestras
     * en segmentos iguales.
     */
    private fun calculateRmsAmplitudes(
        allSamples: List<Short>,
        targetSamples: Int
    ): List<Int> {
        val chunkSize = allSamples.size / targetSamples
        if (chunkSize <= 0) {
            return generateFallbackWaveform(targetSamples)
        }

        val amplitudes = mutableListOf<Double>()

        for (i in 0 until targetSamples) {
            val start = i * chunkSize
            val end = (start + chunkSize).coerceAtMost(allSamples.size)

            if (start >= allSamples.size) break

            // Calcular RMS (Root Mean Square)
            var sumSquares = 0.0
            for (j in start until end) {
                val sample = allSamples[j].toDouble()
                sumSquares += sample * sample
            }
            val rms = sqrt(sumSquares / (end - start))
            amplitudes.add(rms)
        }

        // Normalizar a rango 0-100
        val maxAmplitude = amplitudes.maxOrNull() ?: 1.0
        return amplitudes.map { amplitude ->
            ((amplitude / maxAmplitude) * 100).toInt().coerceIn(0, 100)
        }
    }

    /**
     * Genera una forma de onda aleatoria como fallback cuando la extraccion falla.
     * Los valores simulan un patron natural de audio con variaciones suaves.
     */
    private fun generateFallbackWaveform(samples: Int): List<Int> {
        val random = Random(System.currentTimeMillis())
        var current = 50
        return List(samples) {
            // Variacion suave para simular un patron natural
            val delta = random.nextInt(-15, 16)
            current = (current + delta).coerceIn(10, 90)
            current
        }
    }
}
