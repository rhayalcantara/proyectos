package com.clonewhatsapp.feature.chat.window

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.clonewhatsapp.core.ui.theme.ChatBackgroundDark
import com.clonewhatsapp.core.ui.theme.ChatBackgroundLight
import com.clonewhatsapp.domain.model.Mensaje
import com.clonewhatsapp.feature.chat.components.ChatBubble
import com.clonewhatsapp.feature.chat.components.MessageInput

/**
 * Pantalla de ventana de chat estilo WhatsApp (T-028)
 *
 * @param chatName Nombre del contacto o grupo
 * @param chatPhotoUrl URL de la foto del chat (puede ser null)
 * @param isOnline Si el contacto está en línea
 * @param messages Lista de mensajes del chat
 * @param currentUserId ID del usuario actual (para determinar mensajes propios)
 * @param messageText Texto actual en el campo de entrada
 * @param isLoading Si se están cargando los mensajes
 * @param onTextChange Callback cuando cambia el texto de entrada
 * @param onSendClick Callback al enviar un mensaje
 * @param onBackClick Callback al presionar botón atrás
 * @param modifier Modifier opcional
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatWindowScreen(
    chatName: String,
    chatPhotoUrl: String?,
    isOnline: Boolean,
    messages: List<Mensaje>,
    currentUserId: String,
    messageText: String,
    isLoading: Boolean,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val chatBackground = if (isDark) ChatBackgroundDark else ChatBackgroundLight
    val listState = rememberLazyListState()

    // Auto-scroll al último mensaje cuando llegan nuevos
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar pequeño en el TopBar
                        ChatWindowAvatar(
                            photoUrl = chatPhotoUrl,
                            nombre = chatName,
                            modifier = Modifier.size(40.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = chatName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1
                            )
                            if (isOnline) {
                                Text(
                                    text = "en línea",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { /* TODO: Videollamada */ }) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Videollamada"
                        )
                    }
                    IconButton(onClick = { /* TODO: Llamada */ }) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Llamada"
                        )
                    }
                    IconButton(onClick = { /* TODO: Más opciones */ }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Más opciones"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(chatBackground)
        ) {
            // Lista de mensajes (reversed para mostrar de abajo hacia arriba)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    isLoading && messages.isEmpty() -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    messages.isEmpty() -> {
                        Text(
                            text = "No hay mensajes aún. Envía el primero.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp)
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            reverseLayout = true,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(
                                items = messages,
                                key = { it.id }
                            ) { mensaje ->
                                ChatBubble(
                                    mensaje = mensaje,
                                    isFromMe = mensaje.remitenteId == currentUserId
                                )
                            }
                        }
                    }
                }
            }

            // Barra de entrada de mensaje
            MessageInput(
                text = messageText,
                onTextChange = onTextChange,
                onSendClick = onSendClick
            )
        }
    }
}

/**
 * Avatar circular pequeño para el TopBar del chat
 */
@Composable
private fun ChatWindowAvatar(
    photoUrl: String?,
    nombre: String,
    modifier: Modifier = Modifier
) {
    if (photoUrl != null) {
        AsyncImage(
            model = photoUrl,
            contentDescription = "Foto de $nombre",
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        val initials = nombre
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }

        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
