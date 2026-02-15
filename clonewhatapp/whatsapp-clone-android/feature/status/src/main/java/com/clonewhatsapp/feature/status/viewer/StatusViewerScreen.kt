package com.clonewhatsapp.feature.status.viewer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.clonewhatsapp.domain.model.EstadoCompleto
import com.clonewhatsapp.domain.model.TipoEstado
import com.clonewhatsapp.domain.model.VistaEstado
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pantalla de visor de estados a pantalla completa (T-089)
 *
 * @param estados Lista de estados a mostrar
 * @param indiceActual Índice del estado visible
 * @param progreso Progreso del temporizador (0f a 1f)
 * @param usuarioNombre Nombre del usuario
 * @param usuarioFoto Foto del usuario
 * @param esMiEstado Si son mis propios estados
 * @param vistas Lista de quiénes vieron (solo para mis estados)
 * @param mostrarVistas Si se muestra la lista de vistas
 * @param onAnterior Callback para ir al estado anterior
 * @param onSiguiente Callback para ir al siguiente estado
 * @param onPausar Callback para pausar
 * @param onReanudar Callback para reanudar
 * @param onCerrar Callback para cerrar el visor
 * @param onToggleVistas Callback para mostrar/ocultar vistas
 * @param onEliminar Callback para eliminar estado
 * @param modifier Modifier opcional
 */
@Composable
fun StatusViewerScreen(
    estados: List<EstadoCompleto>,
    indiceActual: Int,
    progreso: Float,
    usuarioNombre: String,
    usuarioFoto: String?,
    esMiEstado: Boolean,
    vistas: List<VistaEstado>,
    mostrarVistas: Boolean,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onPausar: () -> Unit,
    onReanudar: () -> Unit,
    onCerrar: () -> Unit,
    onToggleVistas: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estadoActual = estados.getOrNull(indiceActual) ?: return

    // Offset vertical para swipe down to close
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { onPausar() },
                    onDragEnd = {
                        if (offsetY > 200f) {
                            onCerrar()
                        } else {
                            offsetY = 0f
                            onReanudar()
                        }
                    },
                    onVerticalDrag = { _, dragAmount ->
                        if (dragAmount > 0) {
                            offsetY += dragAmount
                        }
                    }
                )
            }
    ) {
        // Fondo del estado
        when (estadoActual.tipo) {
            TipoEstado.TEXTO -> {
                // Estado de texto con fondo de color
                val colorFondo = try {
                    Color(android.graphics.Color.parseColor(estadoActual.colorFondo ?: "#075E54"))
                } catch (e: Exception) {
                    Color(0xFF075E54)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorFondo),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = estadoActual.contenido ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }

            TipoEstado.IMAGEN -> {
                // Estado con imagen
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = estadoActual.urlArchivo,
                        contentDescription = "Estado de ${usuarioNombre}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    // Caption en la parte inferior si existe
                    if (!estadoActual.contenido.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                    )
                                )
                                .padding(16.dp)
                                .padding(bottom = 48.dp)
                        ) {
                            Text(
                                text = estadoActual.contenido,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }

        // Zonas de toque izquierda/derecha para navegar
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Zona izquierda - estado anterior
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onAnterior() },
                            onLongPress = { onPausar() }
                        )
                    }
            )
            // Zona derecha - siguiente estado
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { onSiguiente() },
                            onLongPress = { onPausar() }
                        )
                    }
            )
        }

        // Barra de progreso superior (segmentos estilo WhatsApp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
                .padding(top = 8.dp, start = 8.dp, end = 8.dp)
        ) {
            // Segmentos de progreso
            BarraProgresoSegmentada(
                totalSegmentos = estados.size,
                segmentoActual = indiceActual,
                progresoSegmentoActual = progreso
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Info del usuario
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón atrás
                IconButton(onClick = onCerrar) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }

                // Foto del usuario
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (usuarioFoto != null) {
                        AsyncImage(
                            model = usuarioFoto,
                            contentDescription = "Foto de $usuarioNombre",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = usuarioNombre.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Nombre y hora
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (esMiEstado) "Mi estado" else usuarioNombre,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatearHoraEstado(estadoActual.fechaCreacion),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Acciones para mis estados
                if (esMiEstado) {
                    // Botón ver vistas
                    IconButton(onClick = onToggleVistas) {
                        Icon(
                            imageVector = Icons.Default.RemoveRedEye,
                            contentDescription = "Ver quién vio",
                            tint = Color.White
                        )
                    }
                    // Botón eliminar
                    IconButton(onClick = onEliminar) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar estado",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Panel de vistas (para mis estados)
        if (mostrarVistas && esMiEstado) {
            PanelVistas(
                vistas = vistas,
                totalVistas = estadoActual.totalVistas,
                onCerrar = onToggleVistas,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Barra de progreso segmentada estilo WhatsApp
 */
@Composable
private fun BarraProgresoSegmentada(
    totalSegmentos: Int,
    segmentoActual: Int,
    progresoSegmentoActual: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(totalSegmentos) { indice ->
            val progresoSegmento = when {
                indice < segmentoActual -> 1f
                indice == segmentoActual -> progresoSegmentoActual
                else -> 0f
            }

            LinearProgressIndicator(
                progress = { progresoSegmento },
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * Panel inferior que muestra quiénes vieron el estado
 */
@Composable
private fun PanelVistas(
    vistas: List<VistaEstado>,
    totalVistas: Int,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
            .background(
                Color.White,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        // Encabezado
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.RemoveRedEye,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$totalVistas vista${if (totalVistas != 1) "s" else ""}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black
            )
        }

        // Lista de quiénes vieron
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = vistas,
                key = { it.id }
            ) { vista ->
                VistaItem(vista = vista)
            }
        }
    }
}

/**
 * Item individual de una vista
 */
@Composable
private fun VistaItem(
    vista: VistaEstado,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            if (vista.usuarioFoto != null) {
                AsyncImage(
                    model = vista.usuarioFoto,
                    contentDescription = "Foto de ${vista.usuarioNombre}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = vista.usuarioNombre.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = vista.usuarioNombre,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatearHoraEstado(vista.fechaVista),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

/**
 * Formatea la hora de un estado para mostrar en el visor
 */
private fun formatearHoraEstado(timestampMs: Long): String {
    if (timestampMs == 0L) return ""
    val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formato.format(Date(timestampMs))
}
