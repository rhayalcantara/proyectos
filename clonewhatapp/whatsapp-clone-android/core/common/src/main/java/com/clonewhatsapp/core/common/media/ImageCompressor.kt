package com.clonewhatsapp.core.common.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprime imagenes antes de enviar.
 * Reduce resolucion y calidad JPEG para imagenes mayores a maxSizeBytes.
 */
@Singleton
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Tamano maximo antes de comprimir: 1 MB */
        const val MAX_IMAGE_SIZE_BYTES = 1_048_576L
        /** Dimension maxima (ancho o alto) en pixeles */
        const val MAX_DIMENSION = 1920
        /** Calidad JPEG de salida (0-100) */
        const val JPEG_QUALITY = 80
    }

    /**
     * Comprime una imagen si excede el tamano maximo.
     * @param uri URI de la imagen original
     * @return URI del archivo comprimido en cache, o el original si no necesita compresion
     */
    suspend fun compressIfNeeded(uri: Uri): Uri = withContext(Dispatchers.IO) {
        // Verificar tamano del archivo original
        val fileSize = getFileSize(uri)
        if (fileSize in 1..MAX_IMAGE_SIZE_BYTES) {
            // No necesita compresion
            return@withContext uri
        }

        // Obtener dimensiones sin cargar en memoria
        val (width, height) = getImageDimensions(uri)
        if (width <= 0 || height <= 0) {
            // No se pudieron leer las dimensiones, devolver original
            return@withContext uri
        }

        // Calcular sampleSize para decodificar eficientemente
        val inSampleSize = calculateInSampleSize(width, height, MAX_DIMENSION)

        // Decodificar bitmap con el sampleSize calculado
        val options = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }

        val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        } ?: return@withContext uri

        // Escalar al tamano final si aun excede MAX_DIMENSION
        val scaledBitmap = scaleToFit(bitmap, MAX_DIMENSION)

        // Guardar el bitmap comprimido en cache
        val compressedFile = saveCompressedBitmap(scaledBitmap)

        // Reciclar bitmaps si son diferentes
        if (scaledBitmap !== bitmap) {
            bitmap.recycle()
        }
        scaledBitmap.recycle()

        compressedFile.toUri()
    }

    /**
     * Obtiene el tamano del archivo en bytes.
     */
    private fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.available().toLong()
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Obtiene dimensiones de la imagen sin cargarla en memoria.
     * @return Pair(ancho, alto) o Pair(0, 0) si no se pueden leer
     */
    private fun getImageDimensions(uri: Uri): Pair<Int, Int> {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
            Pair(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    /**
     * Calcula el sampleSize optimo para decodificar.
     * El sampleSize debe ser potencia de 2 para mayor eficiencia.
     */
    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var inSampleSize = 1
        if (width > maxDimension || height > maxDimension) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while ((halfWidth / inSampleSize) >= maxDimension &&
                (halfHeight / inSampleSize) >= maxDimension
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Escala el bitmap para que quepa dentro de maxDimension manteniendo la relacion de aspecto.
     */
    private fun scaleToFit(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }

        val ratio = minOf(
            maxDimension.toFloat() / width,
            maxDimension.toFloat() / height
        )

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Guarda el bitmap comprimido en el directorio de cache.
     * @return File del archivo comprimido
     */
    private fun saveCompressedBitmap(bitmap: Bitmap): File {
        val compressedDir = File(context.cacheDir, "compressed").also {
            if (!it.exists()) it.mkdirs()
        }

        val outputFile = File(compressedDir, "${UUID.randomUUID()}.jpg")

        FileOutputStream(outputFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
            fos.flush()
        }

        return outputFile
    }
}
