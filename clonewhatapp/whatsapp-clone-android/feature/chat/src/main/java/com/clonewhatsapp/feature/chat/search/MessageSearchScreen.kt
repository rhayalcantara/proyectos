package com.clonewhatsapp.feature.chat.search

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Color verde WhatsApp principal
private val WhatsAppTealGreen = Color(0xFF00A884)
private val ColorTextoSecundario = Color(0xFF667781)
private val ColorResaltadoBusqueda = Color(0xFFFFEB3B)

/**
 * Pantalla de busqueda global de mensajes (T-131).
 *
 * Permite al usuario buscar en todos los mensajes de todas las conversaciones.
 * Muestra los resultados con:
 * - Nombre del chat
 * - Preview del mensaje con la consulta resaltada
 * - Timestamp del mensaje
 * - Nombre del remitente
 *
 * @param onBackClick Callback al presionar el boton de retroceso
 * @param onResultClick Callback al seleccionar un resultado (chatId, messageId)
 * @param viewModel ViewModel inyectado por Hilt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageSearchScreen(
    onBackClick: () -> Unit,
    onResultClick: (chatId: String, messageId: String) -> Unit,
    viewModel: MessageSearchViewModel = hiltViewModel()
) {
    val estado by viewModel.state.collectAsState()
    val focusRequester = remember { FocusRequester() }

    // Solicitar foco al campo de busqueda al abrir la pantalla
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            BarraBusqueda(
                query = estado.query,
                focusRequester = focusRequester,
                onQueryChange = { viewModel.onEvent(MessageSearchEvent.OnQueryChange(it)) },
                onBackClick = {
                    viewModel.onEvent(MessageSearchEvent.OnBackClick)
                    onBackClick()
                },
                onClearClick = { viewModel.onEvent(MessageSearchEvent.OnClearQuery) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Estado de carga
                estado.isSearching -> {
                    EstadoCargando()
                }

                // Sin resultados despues de buscar
                estado.hasSearched && estado.results.isEmpty() -> {
                    EstadoSinResultados()
                }

                // Resultados encontrados
                estado.hasSearched && estado.results.isNotEmpty() -> {
                    ListaResultados(
                        resultados = estado.results,
                        query = estado.query,
                        onResultClick = { resultado ->
                            viewModel.onEvent(
                                MessageSearchEvent.OnResultClick(
                                    resultado.chatId,
                                    resultado.messageId
                                )
                            )
                            onResultClick(resultado.chatId, resultado.messageId)
                        }
                    )
                }

                // Estado inicial (antes de buscar)
                else -> {
                    EstadoInicial()
                }
            }
        }
    }
}

/**
 * Barra de busqueda en la parte superior con campo de texto y boton de retroceso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BarraBusqueda(
    query: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onClearClick: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Buscar mensajes...",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Limpiar busqueda",
                        tint = Color.White
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = WhatsAppTealGreen
        )
    )
}

/**
 * Estado inicial: icono de busqueda con texto indicativo.
 */
@Composable
private fun EstadoInicial() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = ColorTextoSecundario.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Busca en tus mensajes",
                style = MaterialTheme.typography.bodyLarge,
                color = ColorTextoSecundario
            )
        }
    }
}

/**
 * Estado de carga con indicador circular.
 */
@Composable
private fun EstadoCargando() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = WhatsAppTealGreen
        )
    }
}

/**
 * Estado sin resultados encontrados.
 */
@Composable
private fun EstadoSinResultados() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = ColorTextoSecundario.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No se encontraron resultados",
                style = MaterialTheme.typography.bodyLarge,
                color = ColorTextoSecundario
            )
        }
    }
}

/**
 * Lista de resultados de busqueda.
 */
@Composable
private fun ListaResultados(
    resultados: List<SearchResult>,
    query: String,
    onResultClick: (SearchResult) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = resultados,
            key = { it.messageId }
        ) { resultado ->
            FilaResultado(
                resultado = resultado,
                query = query,
                onClick = { onResultClick(resultado) }
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 72.dp),
                thickness = 0.5.dp,
                color = Color.LightGray.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Fila individual de resultado de busqueda.
 *
 * Muestra:
 * - Avatar con inicial del chat
 * - Nombre del chat
 * - Preview del mensaje con la consulta resaltada en amarillo
 * - Hora/fecha del mensaje
 * - Nombre del remitente
 */
@Composable
private fun FilaResultado(
    resultado: SearchResult,
    query: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar con inicial
        AvatarInicialBusqueda(nombre = resultado.chatName)

        Spacer(modifier = Modifier.width(12.dp))

        // Contenido del resultado
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Nombre del chat
            Text(
                text = resultado.chatName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Nombre del remitente (si aplica)
            if (resultado.senderName.isNotEmpty()) {
                Text(
                    text = resultado.senderName,
                    style = MaterialTheme.typography.bodySmall,
                    color = WhatsAppTealGreen,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))
            }

            // Preview del mensaje con resaltado
            TextoConResaltado(
                texto = resultado.messagePreview,
                query = query
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Hora del mensaje
        Text(
            text = formatearFechaBusqueda(resultado.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = ColorTextoSecundario
        )
    }
}

/**
 * Avatar circular con la inicial del nombre del chat.
 */
@Composable
private fun AvatarInicialBusqueda(nombre: String) {
    val inicial = nombre.firstOrNull()?.uppercase() ?: "?"

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(WhatsAppTealGreen),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = inicial,
            style = MaterialTheme.typography.titleMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/**
 * Muestra texto con la consulta de busqueda resaltada en amarillo.
 *
 * @param texto Texto completo del mensaje
 * @param query Consulta a resaltar
 */
@Composable
private fun TextoConResaltado(
    texto: String,
    query: String
) {
    val textoAnotado = remember(texto, query) {
        buildAnnotatedString {
            if (query.isBlank()) {
                append(texto)
                return@buildAnnotatedString
            }

            val textoLower = texto.lowercase()
            val queryLower = query.lowercase()
            var indiceInicio = 0

            while (indiceInicio < texto.length) {
                val indiceBusqueda = textoLower.indexOf(queryLower, indiceInicio)

                if (indiceBusqueda == -1) {
                    // No hay mas coincidencias, agregar el resto del texto
                    append(texto.substring(indiceInicio))
                    break
                }

                // Agregar texto antes de la coincidencia
                if (indiceBusqueda > indiceInicio) {
                    append(texto.substring(indiceInicio, indiceBusqueda))
                }

                // Agregar texto resaltado
                withStyle(
                    SpanStyle(
                        background = ColorResaltadoBusqueda,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(texto.substring(indiceBusqueda, indiceBusqueda + query.length))
                }

                indiceInicio = indiceBusqueda + query.length
            }
        }
    }

    Text(
        text = textoAnotado,
        style = MaterialTheme.typography.bodyMedium,
        color = ColorTextoSecundario,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

/**
 * Formatea un timestamp a un formato legible para los resultados de busqueda.
 * Muestra hora si es hoy, dia de la semana si es esta semana, o fecha completa.
 */
private fun formatearFechaBusqueda(timestamp: Long): String {
    if (timestamp == 0L) return ""

    val ahora = System.currentTimeMillis()
    val diferencia = ahora - timestamp
    val unDia = 24 * 60 * 60 * 1000L

    return when {
        diferencia < unDia -> {
            SimpleDateFormat("HH:mm", Locale("es")).format(Date(timestamp))
        }
        diferencia < 7 * unDia -> {
            SimpleDateFormat("EEE", Locale("es")).format(Date(timestamp))
        }
        else -> {
            SimpleDateFormat("dd/MM/yyyy", Locale("es")).format(Date(timestamp))
        }
    }
}
