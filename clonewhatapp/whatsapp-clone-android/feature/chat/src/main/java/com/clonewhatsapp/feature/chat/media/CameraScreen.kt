package com.clonewhatsapp.feature.chat.media

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Modos de captura disponibles en la pantalla de camara.
 */
enum class CaptureMode {
    PHOTO,
    VIDEO
}

/**
 * Estados del flash de la camara.
 */
enum class FlashMode {
    OFF,
    ON,
    AUTO
}

/**
 * Duracion maxima de grabacion de video en segundos.
 */
private const val MAX_RECORDING_DURATION_SECONDS = 60

/**
 * Pantalla de camara completa que permite capturar fotos y grabar videos
 * usando CameraX. Similar a la interfaz de camara de WhatsApp.
 *
 * @param onPhotoCaptured callback invocado con el URI de la foto capturada
 * @param onVideoRecorded callback invocado con el URI del video grabado
 * @param onClose callback invocado cuando el usuario cierra la pantalla
 * @param modifier modificador de Compose
 */
@Composable
fun CameraScreen(
    onPhotoCaptured: (Uri) -> Unit,
    onVideoRecorded: (Uri) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Estado de la camara
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var flashMode by remember { mutableStateOf(FlashMode.OFF) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }

    // Verificacion de permisos de camara
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PermissionChecker.PERMISSION_GRANTED
        )
    }

    // Referencias de CameraX
    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Temporizador de grabacion
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDurationSeconds = 0
            while (isRecording && recordingDurationSeconds < MAX_RECORDING_DURATION_SECONDS) {
                delay(1000L)
                recordingDurationSeconds++
            }
            // Detener grabacion automaticamente al alcanzar el limite
            if (isRecording && recordingDurationSeconds >= MAX_RECORDING_DURATION_SECONDS) {
                activeRecording?.stop()
            }
        }
    }

    // Configurar y vincular la camara al lifecycle
    LaunchedEffect(useFrontCamera, captureMode) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Preview use case
            val preview = Preview.Builder()
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            // Camera selector
            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            // Image capture use case
            val newImageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(
                    when (flashMode) {
                        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                    }
                )
                .build()
            imageCapture = newImageCapture

            // Video capture use case
            val qualitySelector = QualitySelector.from(
                Quality.HD,
                com.clonewhatsapp.feature.chat.media.fallbackStrategy()
            )
            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
            val newVideoCapture = VideoCapture.withOutput(recorder)
            videoCapture = newVideoCapture

            try {
                cameraProvider.unbindAll()

                if (captureMode == CaptureMode.PHOTO) {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        newImageCapture
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        newVideoCapture
                    )
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error al iniciar la camara: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Limpiar recursos al salir
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            activeRecording?.stop()
        }
    }

    if (!hasCameraPermission) {
        CameraPermissionRequired(
            onClose = onClose,
            modifier = modifier
        )
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Vista de previsualizacion de la camara
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Controles superiores: cerrar, flash, cambiar camara
        TopCameraControls(
            flashMode = flashMode,
            onFlashToggle = {
                flashMode = when (flashMode) {
                    FlashMode.OFF -> FlashMode.ON
                    FlashMode.ON -> FlashMode.AUTO
                    FlashMode.AUTO -> FlashMode.OFF
                }
                // Actualizar flash en el ImageCapture
                imageCapture?.flashMode = when (flashMode) {
                    FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                    FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                    FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                }
            },
            onSwitchCamera = { useFrontCamera = !useFrontCamera },
            onClose = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        )

        // Indicador de grabacion
        AnimatedVisibility(
            visible = isRecording,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
        ) {
            RecordingIndicator(durationSeconds = recordingDurationSeconds)
        }

        // Controles inferiores: modo, boton de captura
        BottomCameraControls(
            captureMode = captureMode,
            isRecording = isRecording,
            onCaptureModeChanged = { newMode ->
                if (!isRecording) {
                    captureMode = newMode
                }
            },
            onCapturePhoto = {
                takePhoto(
                    context = context,
                    imageCapture = imageCapture,
                    executor = cameraExecutor,
                    onPhotoCaptured = onPhotoCaptured
                )
            },
            onToggleRecording = {
                if (isRecording) {
                    // Detener grabacion
                    activeRecording?.stop()
                    activeRecording = null
                    isRecording = false
                } else {
                    // Iniciar grabacion
                    startVideoRecording(
                        context = context,
                        videoCapture = videoCapture,
                        onRecordingStarted = { recording ->
                            activeRecording = recording
                            isRecording = true
                        },
                        onVideoRecorded = { uri ->
                            isRecording = false
                            activeRecording = null
                            onVideoRecorded(uri)
                        },
                        onError = { error ->
                            isRecording = false
                            activeRecording = null
                            Toast.makeText(
                                context,
                                "Error al grabar video: $error",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }
}

/**
 * Estrategia de fallback para la calidad de video.
 */
internal fun fallbackStrategy() =
    androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)

/**
 * Controles superiores de la camara: cerrar, flash y cambiar camara.
 */
@Composable
private fun TopCameraControls(
    flashMode: FlashMode,
    onFlashToggle: () -> Unit,
    onSwitchCamera: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Boton cerrar
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cerrar camara",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Row {
            // Boton flash
            IconButton(onClick = onFlashToggle) {
                Icon(
                    imageVector = when (flashMode) {
                        FlashMode.OFF -> Icons.Default.FlashOff
                        FlashMode.ON -> Icons.Default.FlashOn
                        FlashMode.AUTO -> Icons.Default.FlashAuto
                    },
                    contentDescription = "Toggle flash",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Boton cambiar camara
            IconButton(onClick = onSwitchCamera) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Cambiar camara",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * Indicador de grabacion con punto rojo pulsante y temporizador.
 */
@Composable
private fun RecordingIndicator(
    durationSeconds: Int,
    modifier: Modifier = Modifier
) {
    val minutes = durationSeconds / 60
    val seconds = durationSeconds % 60
    val timeText = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Row(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.FiberManualRecord,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = timeText,
            color = Color.White,
            fontSize = 16.sp
        )
    }
}

/**
 * Controles inferiores: selector de modo (foto/video) y boton de captura.
 */
@Composable
private fun BottomCameraControls(
    captureMode: CaptureMode,
    isRecording: Boolean,
    onCaptureModeChanged: (CaptureMode) -> Unit,
    onCapturePhoto: () -> Unit,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Selector de modo foto/video
        Row(
            modifier = Modifier.padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Foto",
                color = if (captureMode == CaptureMode.PHOTO) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable(enabled = !isRecording) {
                        onCaptureModeChanged(CaptureMode.PHOTO)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                text = "Video",
                color = if (captureMode == CaptureMode.VIDEO) Color.White else Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable(enabled = !isRecording) {
                        onCaptureModeChanged(CaptureMode.VIDEO)
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Boton de captura
        when (captureMode) {
            CaptureMode.PHOTO -> {
                // Boton circular blanco para fotos
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .clickable { onCapturePhoto() }
                        .padding(4.dp)
                        .background(Color.White, CircleShape)
                )
            }
            CaptureMode.VIDEO -> {
                if (isRecording) {
                    // Boton de detener grabacion (cuadrado rojo)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color.Red, CircleShape)
                            .clickable { onToggleRecording() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Detener grabacion",
                            tint = Color.Red,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    // Boton de iniciar grabacion (circulo rojo)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(4.dp, Color.White, CircleShape)
                            .clickable { onToggleRecording() }
                            .padding(8.dp)
                            .background(Color.Red, CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * Pantalla mostrada cuando no se tiene permiso de camara.
 * El sistema completo de permisos se implementara en T-064.
 */
@Composable
private fun CameraPermissionRequired(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Se requiere permiso de camara",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Permite el acceso a la camara en la configuracion de la app",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Cerrar",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onClose() }
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Captura una foto usando ImageCapture y la guarda en el directorio de cache.
 */
private fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    executor: java.util.concurrent.Executor,
    onPhotoCaptured: (Uri) -> Unit
) {
    val capture = imageCapture ?: return

    val mediaDir = File(context.cacheDir, "media").apply { mkdirs() }
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val photoFile = File(mediaDir, "IMG_${timestamp}.jpg")

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    capture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                onPhotoCaptured(savedUri)
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(
                    context,
                    "Error al capturar foto: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )
}

/**
 * Inicia la grabacion de video usando VideoCapture con Recorder.
 */
@androidx.annotation.OptIn(androidx.camera.video.ExperimentalPersistentRecording::class)
private fun startVideoRecording(
    context: Context,
    videoCapture: VideoCapture<Recorder>?,
    onRecordingStarted: (Recording) -> Unit,
    onVideoRecorded: (Uri) -> Unit,
    onError: (String) -> Unit
) {
    val capture = videoCapture ?: run {
        onError("VideoCapture no disponible")
        return
    }

    val mediaDir = File(context.cacheDir, "media").apply { mkdirs() }
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val videoFile = File(mediaDir, "VID_${timestamp}.mp4")

    val outputOptions = FileOutputOptions.Builder(videoFile).build()

    val hasAudioPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PermissionChecker.PERMISSION_GRANTED

    val pendingRecording = capture.output
        .prepareRecording(context, outputOptions)
        .let { recording ->
            if (hasAudioPermission) {
                recording.withAudioEnabled()
            } else {
                recording
            }
        }

    val recording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
        when (event) {
            is VideoRecordEvent.Finalize -> {
                if (event.hasError()) {
                    onError("Error de grabacion: ${event.cause?.message}")
                    videoFile.delete()
                } else {
                    val savedUri = event.outputResults.outputUri
                    onVideoRecorded(savedUri)
                }
            }
        }
    }

    onRecordingStarted(recording)
}
