package com.clonewhatsapp.feature.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Emojis de reacción rápida por defecto de WhatsApp
 */
private val reaccionesRapidas = listOf(
    "\uD83D\uDC4D", // 👍
    "\u2764\uFE0F",  // ❤️
    "\uD83D\uDE02", // 😂
    "\uD83D\uDE2E", // 😮
    "\uD83D\uDE22", // 😢
    "\uD83D\uDE4F"  // 🙏
)

/**
 * Barra flotante de reacciones rápidas (T-133)
 *
 * Aparece al mantener presionado un mensaje. Muestra 6 emojis rápidos
 * de WhatsApp más un botón (+) para abrir el selector completo de emojis.
 * Usa animación de escala (scaleIn/scaleOut) con efecto spring.
 *
 * @param isVisible Si la barra de reacciones es visible
 * @param onReactionSelected Callback cuando se selecciona un emoji de reacción
 * @param onShowFullPicker Callback para abrir el selector completo de emojis
 * @param onDismiss Callback para cerrar la barra de reacciones
 * @param modifier Modifier opcional
 */
@Composable
fun ReactionPicker(
    isVisible: Boolean,
    onReactionSelected: (emoji: String) -> Unit,
    onShowFullPicker: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = scaleOut(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emojis de reacción rápida
                reaccionesRapidas.forEach { emoji ->
                    EmojiReactionButton(
                        emoji = emoji,
                        onClick = {
                            onReactionSelected(emoji)
                            onDismiss()
                        }
                    )
                }

                // Botón (+) para abrir selector completo
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            onShowFullPicker()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Más reacciones",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Botón individual de emoji en la barra de reacciones.
 *
 * @param emoji El emoji a mostrar
 * @param onClick Callback al tocar el emoji
 */
@Composable
private fun EmojiReactionButton(
    emoji: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
    }
}
