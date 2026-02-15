package com.clonewhatsapp.feature.status.list

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.clonewhatsapp.core.ui.theme.WhatsAppLightGreen
import com.clonewhatsapp.core.ui.theme.WhatsAppTealGreen
import com.clonewhatsapp.domain.model.EstadosContacto
import com.clonewhatsapp.domain.model.MisEstados
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Pantalla de lista de estados estilo WhatsApp (T-088)
 *
 * @param misEstados Mis estados publicados
 * @param estadosContactos Estados de contactos
 * @param isLoading Si se están cargando los estados
 * @param isRefreshing Si se está haciendo pull-to-refresh
 * @param onMyStatusClick Callback al hacer clic en mi estado
 * @param onContactStatusClick Callback al hacer clic en el estado de un contacto
 * @param onCreateTextStatus Callback para crear estado de texto
 * @param onCreateImageStatus Callback para crear estado con imagen
 * @param onRefresh Callback al hacer pull-to-refresh
 * @param modifier Modifier opcional
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusListScreen(
    misEstados: MisEstados,
    estadosContactos: List<EstadosContacto>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    onMyStatusClick: () -> Unit,
    onContactStatusClick: (String) -> Unit,
    onCreateTextStatus: () -> Unit,
    onCreateImageStatus: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Estados",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { /* TODO: Menú de opciones */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // FAB pequeño para estado de texto
                SmallFloatingActionButton(
                    onClick = onCreateTextStatus,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Estado de texto"
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                // FAB principal para estado con cámara/imagen
                FloatingActionButton(
                    onClick = onCreateImageStatus,
                    containerColor = WhatsAppLightGreen,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Estado con imagen"
                    )
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Cargando por primera vez
                isLoading && misEstados.estados.isEmpty() && estadosContactos.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Sección: Mi estado
                        item {
                            MiEstadoItem(
                                misEstados = misEstados,
                                onClick = onMyStatusClick,
                                onAddClick = onCreateTextStatus
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        // Separador de sección: Actualizaciones recientes
                        if (estadosContactos.any { !it.todosVistos }) {
                            item {
                                Text(
                                    text = "Actualizaciones recientes",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    )
                                )
                            }
                        }

                        // Estados no vistos de contactos
                        val noVistos = estadosContactos.filter { !it.todosVistos }
                        items(
                            items = noVistos,
                            key = { "noVisto_${it.usuarioId}" }
                        ) { contacto ->
                            ContactoEstadoItem(
                                contacto = contacto,
                                onClick = { onContactStatusClick(contacto.usuarioId) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 80.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        // Separador de sección: Actualizaciones vistas
                        val vistos = estadosContactos.filter { it.todosVistos }
                        if (vistos.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Actualizaciones vistas",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(
                                        horizontal = 16.dp,
                                        vertical = 8.dp
                                    )
                                )
                            }
                        }

                        // Estados vistos de contactos
                        items(
                            items = vistos,
                            key = { "visto_${it.usuarioId}" }
                        ) { contacto ->
                            ContactoEstadoItem(
                                contacto = contacto,
                                onClick = { onContactStatusClick(contacto.usuarioId) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 80.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        // Espacio para el FAB
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Item de "Mi estado" en la parte superior
 */
@Composable
private fun MiEstadoItem(
    misEstados: MisEstados,
    onClick: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = if (misEstados.estados.isNotEmpty()) onClick else onAddClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar con botón de agregar
        Box {
            // Foto de perfil o placeholder
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (misEstados.estados.isNotEmpty()) {
                    // Mostrar indicador de que hay estados
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = WhatsAppTealGreen,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Circle,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Circle,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Botón de agregar
            if (misEstados.estados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(WhatsAppTealGreen)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar estado",
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Texto
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Mi estado",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (misEstados.estados.isNotEmpty()) {
                    val totalVistas = misEstados.totalVistas
                    "${misEstados.estados.size} estado${if (misEstados.estados.size > 1) "s" else ""} · $totalVistas vista${if (totalVistas != 1) "s" else ""}"
                } else {
                    "Toca para agregar tu estado"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Item de estado de un contacto
 */
@Composable
private fun ContactoEstadoItem(
    contacto: EstadosContacto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar con anillo indicador
        val colorAnillo = if (contacto.todosVistos) {
            MaterialTheme.colorScheme.outlineVariant
        } else {
            WhatsAppTealGreen
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = colorAnillo,
                    shape = CircleShape
                )
                .padding(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (contacto.usuarioFoto != null) {
                AsyncImage(
                    model = contacto.usuarioFoto,
                    contentDescription = "Foto de ${contacto.usuarioNombre}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = contacto.usuarioNombre.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Info del contacto
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = contacto.usuarioNombre,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatearTiempoRelativo(contacto.ultimaActualizacion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * Estado vacío cuando no hay estados de contactos
 */
@Composable
private fun EstadosVaciosState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Circle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sin actualizaciones recientes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Los estados de tus contactos aparecerán aquí",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Formatea un timestamp en milisegundos a tiempo relativo ("hace 5 min", "hoy", etc.)
 */
private fun formatearTiempoRelativo(timestampMs: Long): String {
    if (timestampMs == 0L) return ""

    val ahora = System.currentTimeMillis()
    val diferencia = ahora - timestampMs

    val minutos = TimeUnit.MILLISECONDS.toMinutes(diferencia)
    val horas = TimeUnit.MILLISECONDS.toHours(diferencia)

    return when {
        minutos < 1 -> "justo ahora"
        minutos < 60 -> "hace $minutos min"
        horas < 24 -> {
            val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
            "hoy, ${formato.format(Date(timestampMs))}"
        }
        horas < 48 -> {
            val formato = SimpleDateFormat("HH:mm", Locale.getDefault())
            "ayer, ${formato.format(Date(timestampMs))}"
        }
        else -> {
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formato.format(Date(timestampMs))
        }
    }
}
