package com.clonewhatsapp.feature.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonewhatsapp.core.ui.theme.WhatsAppTealGreen
import com.clonewhatsapp.domain.model.ReactionGroup

/**
 * Fila de reacciones mostrada debajo de una burbuja de mensaje (T-133)
 *
 * Cada grupo de reacción se muestra como una píldora con el emoji y el conteo.
 * - Toque simple: agregar/quitar la reacción propia
 * - Toque largo: ver quién reaccionó en un bottom sheet
 *
 * @param reactions Lista de grupos de reacciones agrupadas por emoji
 * @param isFromMe Si el mensaje es del usuario actual (para alineación)
 * @param onReactionToggle Callback al tocar una reacción para agregar/quitar
 * @param modifier Modifier opcional
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageReactions(
    reactions: List<ReactionGroup>,
    isFromMe: Boolean,
    onReactionToggle: (emoji: String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reactions.isEmpty()) return

    // Estado para el bottom sheet de detalles de reacciones
    var selectedReactionGroup by remember { mutableStateOf<ReactionGroup?>(null) }

    val horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start

    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        reactions.forEach { group ->
            ReactionPill(
                reactionGroup = group,
                onClick = { onReactionToggle(group.emoji) },
                onLongClick = { selectedReactionGroup = group }
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
    }

    // Bottom sheet para mostrar quién reaccionó
    selectedReactionGroup?.let { group ->
        ReactionDetailsBottomSheet(
            reactionGroup = group,
            onDismiss = { selectedReactionGroup = null }
        )
    }
}

/**
 * Píldora individual de reacción.
 * Muestra emoji + conteo con fondo sutil.
 * Resaltado si el usuario actual reaccionó con ese emoji.
 *
 * @param reactionGroup Grupo de reacción a mostrar
 * @param onClick Callback al tocar (toggle reacción)
 * @param onLongClick Callback al mantener presionado (ver quién reaccionó)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReactionPill(
    reactionGroup: ReactionGroup,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    // Color de fondo: resaltado si el usuario reaccionó
    val backgroundColor = when {
        reactionGroup.reactedByMe && isDark -> WhatsAppTealGreen.copy(alpha = 0.25f)
        reactionGroup.reactedByMe -> WhatsAppTealGreen.copy(alpha = 0.15f)
        isDark -> Color.White.copy(alpha = 0.08f)
        else -> Color.Black.copy(alpha = 0.06f)
    }

    // Borde sutil si el usuario reaccionó
    val borderColor = if (reactionGroup.reactedByMe) {
        WhatsAppTealGreen.copy(alpha = 0.4f)
    } else {
        Color.Transparent
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = if (reactionGroup.reactedByMe) {
            androidx.compose.foundation.BorderStroke(1.dp, borderColor)
        } else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Emoji
            Text(
                text = reactionGroup.emoji,
                fontSize = 14.sp
            )

            // Conteo (solo si es mayor a 1)
            if (reactionGroup.count > 1) {
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = reactionGroup.count.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (reactionGroup.reactedByMe) {
                        WhatsAppTealGreen
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    }
                )
            }
        }
    }
}

/**
 * Bottom sheet que muestra quién reaccionó con un emoji específico.
 *
 * @param reactionGroup Grupo de reacción con los nombres de usuarios
 * @param onDismiss Callback para cerrar el bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReactionDetailsBottomSheet(
    reactionGroup: ReactionGroup,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Encabezado con emoji y conteo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reactionGroup.emoji,
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Reacciones (${reactionGroup.count})",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Lista de usuarios que reaccionaron
            LazyColumn(
                modifier = Modifier.widthIn(max = 400.dp)
            ) {
                items(reactionGroup.userNames) { userName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar placeholder (círculo con inicial)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(WhatsAppTealGreen.copy(alpha = 0.2f))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userName.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = WhatsAppTealGreen
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = userName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
