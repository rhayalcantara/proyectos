package com.clonewhatsapp.core.network.repository

import android.content.Context
import android.net.Uri
import com.clonewhatsapp.core.network.api.StatusApi
import com.clonewhatsapp.core.network.dto.CrearEstadoTextoRequest
import com.clonewhatsapp.core.network.dto.EstadoDto
import com.clonewhatsapp.core.network.dto.EstadosContactoDto
import com.clonewhatsapp.core.network.dto.MisEstadosDto
import com.clonewhatsapp.core.network.dto.VistaEstadoDto
import com.clonewhatsapp.domain.model.EstadoCompleto
import com.clonewhatsapp.domain.model.EstadosContacto
import com.clonewhatsapp.domain.model.MisEstados
import com.clonewhatsapp.domain.model.TipoEstado
import com.clonewhatsapp.domain.model.VistaEstado
import com.clonewhatsapp.domain.repository.StatusRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación del repositorio de estados (T-087)
 * Maneja las llamadas a la API y mapea DTOs a modelos de dominio
 */
@Singleton
class StatusRepositoryImpl @Inject constructor(
    private val statusApi: StatusApi,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) : StatusRepository {

    /**
     * Obtiene mis estados publicados
     */
    override suspend fun getMyStatuses(): Result<MisEstados> {
        return try {
            val response = statusApi.getMyStatuses()
            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    Result.success(dto.toDomain())
                } else {
                    Result.success(MisEstados(estados = emptyList(), totalVistas = 0))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene los estados de los contactos
     */
    override suspend fun getContactsStatuses(): Result<List<EstadosContacto>> {
        return try {
            val response = statusApi.getContactsStatuses()
            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                Result.success(dtos.map { it.toDomain() })
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea un estado de texto
     */
    override suspend fun createTextStatus(contenido: String, colorFondo: String?): Result<EstadoCompleto> {
        return try {
            val request = CrearEstadoTextoRequest(
                contenido = contenido,
                colorFondo = colorFondo
            )
            val response = statusApi.createTextStatus(request)

            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    Result.success(dto.toDomain())
                } else {
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea un estado con imagen (multipart upload)
     */
    override suspend fun createImageStatus(imageUri: Uri, caption: String?): Result<EstadoCompleto> {
        return try {
            // Leer bytes de la imagen desde el ContentResolver
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return Result.failure(Exception("No se pudo leer la imagen"))
            val imageBytes = inputStream.readBytes()
            inputStream.close()

            // Determinar el nombre del archivo
            val nombreArchivo = "estado_imagen_${System.currentTimeMillis()}.jpg"

            // Crear la parte multipart para la imagen
            val requestBody = imageBytes.toRequestBody("image/*".toMediaTypeOrNull())
            val imagenPart = MultipartBody.Part.createFormData("imagen", nombreArchivo, requestBody)

            // Crear la parte del caption si existe
            val captionPart = caption?.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = statusApi.createImageStatus(imagenPart, captionPart)

            if (response.isSuccessful) {
                val dto = response.body()
                if (dto != null) {
                    Result.success(dto.toDomain())
                } else {
                    Result.failure(Exception("Respuesta vacía del servidor"))
                }
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Marca un estado como visto
     */
    override suspend fun markAsViewed(statusId: String): Result<Unit> {
        return try {
            val response = statusApi.markAsViewed(statusId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene quiénes vieron un estado
     */
    override suspend fun getViewers(statusId: String): Result<List<VistaEstado>> {
        return try {
            val response = statusApi.getViewers(statusId)
            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                Result.success(dtos.map { it.toDomain() })
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Elimina un estado propio
     */
    override suspend fun deleteStatus(statusId: String): Result<Unit> {
        return try {
            val response = statusApi.deleteStatus(statusId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- Funciones de mapeo ----

    /**
     * Mapea MisEstadosDto a modelo de dominio
     */
    private fun MisEstadosDto.toDomain(): MisEstados {
        return MisEstados(
            estados = estados.map { it.toDomain() },
            totalVistas = totalVistas
        )
    }

    /**
     * Mapea EstadoDto a modelo de dominio
     */
    private fun EstadoDto.toDomain(): EstadoCompleto {
        return EstadoCompleto(
            id = id,
            usuarioId = usuarioId,
            usuarioNombre = usuarioNombre,
            usuarioFoto = usuarioFoto,
            tipo = when (tipo) {
                "Imagen" -> TipoEstado.IMAGEN
                else -> TipoEstado.TEXTO
            },
            contenido = contenido,
            urlArchivo = urlArchivo,
            colorFondo = colorFondo,
            fechaCreacion = parseIso8601ToMillis(fechaCreacion),
            fechaExpiracion = parseIso8601ToMillis(fechaExpiracion),
            totalVistas = totalVistas,
            vistoPorMi = vioPorMi
        )
    }

    /**
     * Mapea EstadosContactoDto a modelo de dominio
     */
    private fun EstadosContactoDto.toDomain(): EstadosContacto {
        return EstadosContacto(
            usuarioId = usuarioId,
            usuarioNombre = usuarioNombre,
            usuarioFoto = usuarioFoto,
            estados = estados.map { it.toDomain() },
            todosVistos = todosVistos,
            ultimaActualizacion = parseIso8601ToMillis(ultimaActualizacion)
        )
    }

    /**
     * Mapea VistaEstadoDto a modelo de dominio
     */
    private fun VistaEstadoDto.toDomain(): VistaEstado {
        return VistaEstado(
            id = id,
            usuarioId = usuarioId,
            usuarioNombre = usuarioNombre,
            usuarioFoto = usuarioFoto,
            fechaVista = parseIso8601ToMillis(fechaVista)
        )
    }

    /**
     * Parsea mensaje de error de la respuesta HTTP
     */
    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                val errorResponse = gson.fromJson(errorBody, Map::class.java)
                errorResponse["message"]?.toString() ?: "Error desconocido"
            } else {
                when (response.code()) {
                    400 -> "Solicitud inválida"
                    401 -> "No autorizado"
                    403 -> "Acceso denegado"
                    404 -> "No encontrado"
                    500 -> "Error del servidor"
                    else -> "Error de conexión (${response.code()})"
                }
            }
        } catch (e: Exception) {
            "Error de conexión"
        }
    }

    /**
     * Parsea fecha ISO 8601 a milisegundos
     */
    private fun parseIso8601ToMillis(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
