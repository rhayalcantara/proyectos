package com.clonewhatsapp.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.clonewhatsapp.core.ui.theme.WhatsAppDarkGreen
import com.clonewhatsapp.core.ui.theme.WhatsAppTealGreen

/**
 * Pantalla principal de ajustes estilo WhatsApp (T-135)
 *
 * Incluye:
 * - Tarjeta de perfil en la parte superior (avatar + nombre + info)
 * - Grupos de configuración: Cuenta, Chats, Notificaciones, Almacenamiento, Ayuda
 * - Toggle de modo oscuro funcional
 * - Los demás elementos son placeholders que muestran toast o navegan
 *
 * @param onProfileClick Callback al tocar la tarjeta de perfil
 * @param onBack Callback para navegar hacia atrás
 * @param onDarkModeToggle Callback al cambiar el modo oscuro
 * @param viewModel ViewModel compartido para obtener datos del perfil
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onProfileClick: () -> Unit,
    onBack: () -> Unit,
    onDarkModeToggle: ((Boolean) -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isDarkMode = isSystemInDarkTheme()
    var darkModeEnabled by remember { mutableStateOf(isDarkMode) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajustes",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WhatsAppDarkGreen
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Tarjeta de perfil
            ProfileCard(
                name = state.name,
                info = state.info,
                avatarUrl = state.avatarUrl,
                onClick = onProfileClick
            )

            Divider()

            Spacer(modifier = Modifier.height(8.dp))

            // ---- Grupo: Cuenta ----
            SettingsSectionHeader(title = "Cuenta")

            SettingsItem(
                icon = Icons.Default.Lock,
                iconColor = WhatsAppTealGreen,
                title = "Privacidad",
                subtitle = "Última vez, foto de perfil, info",
                onClick = { /* Placeholder */ }
            )
            SettingsItem(
                icon = Icons.Default.Security,
                iconColor = WhatsAppTealGreen,
                title = "Seguridad",
                subtitle = "Notificaciones de seguridad",
                onClick = { /* Placeholder */ }
            )
            SettingsItem(
                icon = Icons.Default.Key,
                iconColor = WhatsAppTealGreen,
                title = "Verificación en dos pasos",
                subtitle = "Protege tu cuenta con un PIN",
                onClick = { /* Placeholder */ }
            )

            Divider(modifier = Modifier.padding(horizontal = 72.dp))
            Spacer(modifier = Modifier.height(8.dp))

            // ---- Grupo: Chats ----
            SettingsSectionHeader(title = "Chats")

            // Toggle de modo oscuro (funcional)
            SettingsItemWithSwitch(
                icon = Icons.Default.Brightness6,
                iconColor = Color(0xFF7C4DFF),
                title = "Tema oscuro",
                subtitle = if (darkModeEnabled) "Activado" else "Desactivado",
                isChecked = darkModeEnabled,
                onCheckedChange = { enabled ->
                    darkModeEnabled = enabled
                    onDarkModeToggle?.invoke(enabled)
                }
            )
            SettingsItem(
                icon = Icons.Default.Wallpaper,
                iconColor = Color(0xFF4CAF50),
                title = "Fondo de pantalla",
                subtitle = "Cambiar fondo de los chats",
                onClick = { /* Placeholder */ }
            )
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Chat,
                iconColor = Color(0xFF2196F3),
                title = "Historial de chats",
                subtitle = "Exportar, archivar, eliminar chats",
                onClick = { /* Placeholder */ }
            )

            Divider(modifier = Modifier.padding(horizontal = 72.dp))
            Spacer(modifier = Modifier.height(8.dp))

            // ---- Grupo: Notificaciones ----
            SettingsSectionHeader(title = "Notificaciones")

            SettingsItem(
                icon = Icons.Default.Notifications,
                iconColor = Color(0xFFFF5722),
                title = "Notificaciones",
                subtitle = "Tonos de mensaje, grupo y llamada",
                onClick = { /* Placeholder */ }
            )

            Divider(modifier = Modifier.padding(horizontal = 72.dp))
            Spacer(modifier = Modifier.height(8.dp))

            // ---- Grupo: Almacenamiento y datos ----
            SettingsSectionHeader(title = "Almacenamiento y datos")

            SettingsItem(
                icon = Icons.Default.DataUsage,
                iconColor = Color(0xFF009688),
                title = "Almacenamiento y datos",
                subtitle = "Uso de red, descarga automática",
                onClick = { /* Placeholder */ }
            )

            Divider(modifier = Modifier.padding(horizontal = 72.dp))
            Spacer(modifier = Modifier.height(8.dp))

            // ---- Grupo: Ayuda ----
            SettingsSectionHeader(title = "Ayuda")

            SettingsItem(
                icon = Icons.AutoMirrored.Filled.Help,
                iconColor = Color(0xFF607D8B),
                title = "Centro de ayuda",
                subtitle = "Preguntas frecuentes, contacto",
                onClick = { /* Placeholder */ }
            )
            SettingsItem(
                icon = Icons.Default.Security,
                iconColor = Color(0xFF607D8B),
                title = "Términos y política de privacidad",
                subtitle = null,
                onClick = { /* Placeholder */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Versión de la app
            Text(
                text = "WhatsApp Clone v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Tarjeta de perfil en la parte superior de ajustes.
 * Muestra avatar, nombre e info. Al tocar, abre la pantalla de perfil.
 */
@Composable
private fun ProfileCard(
    name: String,
    info: String,
    avatarUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circular
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .then(
                            Modifier.clickable(onClick = onClick)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Sin foto",
                        modifier = Modifier.size(36.dp),
                        tint = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Nombre e info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = name.ifEmpty { "Usuario" },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = info,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Encabezado de sección de ajustes.
 */
@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        ),
        color = WhatsAppTealGreen,
        modifier = Modifier.padding(start = 72.dp, top = 8.dp, bottom = 4.dp)
    )
}

/**
 * Elemento individual de ajustes.
 * Muestra icono coloreado + título + subtítulo opcional.
 *
 * @param icon Icono a mostrar
 * @param iconColor Color de fondo del icono
 * @param title Título del elemento
 * @param subtitle Subtítulo opcional
 * @param onClick Callback al tocar
 */
@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icono con fondo circular coloreado
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .then(Modifier.padding(0.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Texto
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Elemento de ajustes con switch (toggle).
 * Usado para el toggle de modo oscuro.
 *
 * @param icon Icono a mostrar
 * @param iconColor Color del icono
 * @param title Título del elemento
 * @param subtitle Subtítulo
 * @param isChecked Estado actual del switch
 * @param onCheckedChange Callback al cambiar el switch
 */
@Composable
private fun SettingsItemWithSwitch(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icono
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Texto
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // Switch
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = WhatsAppTealGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}
