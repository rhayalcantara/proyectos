package com.clonewhatsapp.feature.status.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.clonewhatsapp.core.ui.theme.WhatsAppLightGreen

/**
 * Pantalla de creación de estados (T-090)
 *
 * @param state Estado actual del ViewModel
 * @param onEvent Callback para enviar eventos al ViewModel
 * @param onEstadoCreado Callback cuando el estado fue creado exitosamente
 * @param onBack Callback para volver atrás
 * @param modifier Modifier opcional
 */
@Composable
fun CreateStatusScreen(
    state: CreateStatusState,
    onEvent: (CreateStatusEvent) -> Unit,
    onEstadoCreado: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Launcher para seleccionar imagen de la galería
    val imagenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onEvent(CreateStatusEvent.OnImagenSeleccionada(it)) }
    }

    // Navegar de vuelta cuando el estado fue creado
    LaunchedEffect(state.estadoCreado) {
        if (state.estadoCreado != null) {
            onEstadoCreado()
        }
    }

    // Mostrar error como snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            onEvent(CreateStatusEvent.OnErrorDismiss)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (state.modo) {
                ModoCreacion.TEXTO -> {
                    PantallaEstadoTexto(
                        texto = state.textoContenido,
                        colorFondo = state.colorFondoActual,
                        onTextoChange = { onEvent(CreateStatusEvent.OnTextoChange(it)) },
                        onCambiarColor = { onEvent(CreateStatusEvent.OnCambiarColor) },
                        onSeleccionarImagen = { imagenLauncher.launch("image/*") },
                        onBack = onBack,
                        onPublicar = { onEvent(CreateStatusEvent.OnPublicar) },
                        isCreating = state.isCreating
                    )
                }

                ModoCreacion.IMAGEN -> {
                    PantallaEstadoImagen(
                        imagenUri = state.imagenUri,
                        caption = state.caption,
                        onCaptionChange = { onEvent(CreateStatusEvent.OnCaptionChange(it)) },
                        onBack = {
                            onEvent(CreateStatusEvent.OnModoTexto)
                        },
                        onPublicar = { onEvent(CreateStatusEvent.OnPublicar) },
                        isCreating = state.isCreating
                    )
                }
            }
        }
    }
}

/**
 * Pantalla para crear estado de texto con fondo de color
 */
@Composable
private fun PantallaEstadoTexto(
    texto: String,
    colorFondo: String,
    onTextoChange: (String) -> Unit,
    onCambiarColor: () -> Unit,
    onSeleccionarImagen: () -> Unit,
    onBack: () -> Unit,
    onPublicar: () -> Unit,
    isCreating: Boolean,
    modifier: Modifier = Modifier
) {
    val color = try {
        Color(android.graphics.Color.parseColor(colorFondo))
    } catch (e: Exception) {
        Color(0xFF075E54)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color)
    ) {
        // Campo de texto centrado
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, bottom = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = texto,
                onValueChange = onTextoChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(Color.White),
                decorationBox = { innerTextField ->
                    if (texto.isEmpty()) {
                        Text(
                            text = "Escribe un estado...",
                            style = TextStyle(
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    innerTextField()
                }
            )
        }

        // Barra superior con botones
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón para seleccionar imagen
            IconButton(onClick = onSeleccionarImagen) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Seleccionar imagen",
                    tint = Color.White
                )
            }

            // Botón para cambiar color
            IconButton(onClick = onCambiarColor) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Cambiar color",
                    tint = Color.White
                )
            }

            // Botón para cambiar a modo texto (selector de fuente placeholder)
            IconButton(onClick = { /* TODO: Cambiar fuente */ }) {
                Icon(
                    imageVector = Icons.Default.TextFields,
                    contentDescription = "Cambiar fuente",
                    tint = Color.White
                )
            }
        }

        // Botón de enviar
        FloatingActionButton(
            onClick = onPublicar,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = WhatsAppLightGreen,
            contentColor = Color.White
        ) {
            if (isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Publicar estado"
                )
            }
        }
    }
}

/**
 * Pantalla para crear estado con imagen y caption
 */
@Composable
private fun PantallaEstadoImagen(
    imagenUri: Uri?,
    caption: String,
    onCaptionChange: (String) -> Unit,
    onBack: () -> Unit,
    onPublicar: () -> Unit,
    isCreating: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Imagen seleccionada
        if (imagenUri != null) {
            AsyncImage(
                model = imagenUri,
                contentDescription = "Imagen del estado",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Barra superior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
        }

        // Campo de caption y botón de enviar en la parte inferior
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Campo de caption
                TextField(
                    value = caption,
                    onValueChange = onCaptionChange,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp)),
                    placeholder = {
                        Text(
                            text = "Agregar descripción...",
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.2f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Botón de enviar
                FloatingActionButton(
                    onClick = onPublicar,
                    modifier = Modifier.size(48.dp),
                    containerColor = WhatsAppLightGreen,
                    contentColor = Color.White
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Publicar estado"
                        )
                    }
                }
            }
        }
    }
}
