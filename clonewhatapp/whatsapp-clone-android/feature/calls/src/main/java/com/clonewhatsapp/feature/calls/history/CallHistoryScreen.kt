package com.clonewhatsapp.feature.calls.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.AddIcCall
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.clonewhatsapp.core.ui.theme.WhatsAppTealGreen
import com.clonewhatsapp.domain.model.CallHistoryItem
import com.clonewhatsapp.domain.model.CallType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pantalla del historial de llamadas.
 *
 * Muestra una lista de llamadas realizadas, recibidas y perdidas con la
 * informacion del contacto, tipo de llamada, direccion y marca de tiempo.
 *
 * @param calls Lista de elementos del historial de llamadas.
 * @param onCallClick Callback al hacer clic en un elemento de la lista.
 * @param onCallBack Callback para devolver la llamada a un contacto.
 * @param modifier Modificador de Compose.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    calls: List<CallHistoryItem>,
    onCallClick: (CallHistoryItem) -> Unit,
    onCallBack: (String, CallType) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Llamadas",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Iniciar nueva llamada */ },
                containerColor = WhatsAppTealGreen,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Filled.AddIcCall,
                    contentDescription = "Nueva llamada"
                )
            }
        },
        modifier = modifier
    ) { paddingValues ->
        if (calls.isEmpty()) {
            // Estado vacio
            EmptyCallHistory(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            // Agrupar llamadas consecutivas del mismo contacto
            val groupedCalls = groupConsecutiveCalls(calls)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(
                    items = groupedCalls,
                    key = { it.first().id }
                ) { callGroup ->
                    CallHistoryItemRow(
                        callGroup = callGroup,
                        onClick = { onCallClick(callGroup.first()) },
                        onCallBack = { onCallBack(callGroup.first().contactId, callGroup.first().callType) }
                    )
                }
            }
        }
    }
}

/**
 * Fila individual del historial de llamadas.
 * Si hay multiples llamadas agrupadas, muestra el conteo.
 */
@Composable
private fun CallHistoryItemRow(
    callGroup: List<CallHistoryItem>,
    onClick: () -> Unit,
    onCallBack: () -> Unit
) {
    val call = callGroup.first()
    val callCount = callGroup.size
    val isMissed = !call.wasAnswered && !call.isOutgoing

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Avatar circular
        ContactAvatar(
            photoUrl = call.contactPhoto,
            contactName = call.contactName,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Informacion de la llamada
        Column(modifier = Modifier.weight(1f)) {
            // Nombre del contacto
            Text(
                text = call.contactName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = if (isMissed) Color(0xFFEA4335) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Icono de direccion + tipo + conteo
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icono de direccion
                Icon(
                    imageVector = when {
                        isMissed -> Icons.AutoMirrored.Filled.CallMissed
                        call.isOutgoing -> Icons.AutoMirrored.Filled.CallMade
                        else -> Icons.AutoMirrored.Filled.CallReceived
                    },
                    contentDescription = when {
                        isMissed -> "Perdida"
                        call.isOutgoing -> "Saliente"
                        else -> "Entrante"
                    },
                    tint = if (isMissed) Color(0xFFEA4335) else WhatsAppTealGreen,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Conteo si hay multiples llamadas
                val countText = if (callCount > 1) "($callCount) " else ""

                // Timestamp formateado
                Text(
                    text = "$countText${formatTimestamp(call.timestamp)}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Icono de tipo de llamada (tappable para devolver llamada)
        IconButton(onClick = onCallBack) {
            Icon(
                imageVector = when (call.callType) {
                    CallType.VIDEO -> Icons.Filled.Videocam
                    CallType.AUDIO -> Icons.Filled.Phone
                },
                contentDescription = when (call.callType) {
                    CallType.VIDEO -> "Videollamada"
                    CallType.AUDIO -> "Llamada de voz"
                },
                tint = WhatsAppTealGreen,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Avatar circular del contacto.
 * Muestra la foto del contacto si esta disponible, o las iniciales si no.
 */
@Composable
private fun ContactAvatar(
    photoUrl: String?,
    contactName: String,
    modifier: Modifier = Modifier
) {
    if (!photoUrl.isNullOrBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = contactName,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        // Placeholder con icono de persona
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .clip(CircleShape)
                .background(Color(0xFFD9D9D9))
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Estado vacio cuando no hay llamadas en el historial.
 */
@Composable
private fun EmptyCallHistory(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Phone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No hay llamadas recientes",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==================== Funciones utilitarias ====================

/**
 * Agrupa llamadas consecutivas del mismo contacto.
 *
 * Las llamadas del mismo contacto que aparecen consecutivamente en la lista
 * se agrupan para mostrar un contador en lugar de entradas individuales.
 */
private fun groupConsecutiveCalls(
    calls: List<CallHistoryItem>
): List<List<CallHistoryItem>> {
    if (calls.isEmpty()) return emptyList()

    val groups = mutableListOf<MutableList<CallHistoryItem>>()
    var currentGroup = mutableListOf(calls.first())

    for (i in 1 until calls.size) {
        val current = calls[i]
        val previous = calls[i - 1]

        if (current.contactId == previous.contactId &&
            current.callType == previous.callType &&
            current.isOutgoing == previous.isOutgoing &&
            current.wasAnswered == previous.wasAnswered
        ) {
            currentGroup.add(current)
        } else {
            groups.add(currentGroup)
            currentGroup = mutableListOf(current)
        }
    }
    groups.add(currentGroup)

    return groups
}

/**
 * Formatea un timestamp en milisegundos a un texto legible.
 *
 * - Hoy: "Hoy 14:30"
 * - Ayer: "Ayer"
 * - Esta semana: Nombre del dia (ej. "Lunes")
 * - Mas antiguo: "dd/MM"
 */
private fun formatTimestamp(timestampMs: Long): String {
    val now = Calendar.getInstance()
    val callTime = Calendar.getInstance().apply {
        timeInMillis = timestampMs
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale("es"))
    val dateFormat = SimpleDateFormat("dd/MM", Locale("es"))

    return when {
        // Mismo dia
        isSameDay(now, callTime) -> {
            "Hoy ${timeFormat.format(Date(timestampMs))}"
        }
        // Ayer
        isYesterday(now, callTime) -> {
            "Ayer"
        }
        // Esta semana (ultimos 7 dias)
        isWithinLastWeek(now, callTime) -> {
            val dayFormat = SimpleDateFormat("EEEE", Locale("es"))
            dayFormat.format(Date(timestampMs)).replaceFirstChar { it.uppercase() }
        }
        // Mas antiguo
        else -> {
            dateFormat.format(Date(timestampMs))
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(now: Calendar, other: Calendar): Boolean {
    val yesterday = Calendar.getInstance().apply {
        timeInMillis = now.timeInMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return isSameDay(yesterday, other)
}

private fun isWithinLastWeek(now: Calendar, other: Calendar): Boolean {
    val weekAgo = Calendar.getInstance().apply {
        timeInMillis = now.timeInMillis
        add(Calendar.DAY_OF_YEAR, -7)
    }
    return other.after(weekAgo)
}
