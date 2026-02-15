package com.clonewhatsapp.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.clonewhatsapp.core.ui.theme.WhatsAppDarkGreen
import com.clonewhatsapp.core.ui.theme.WhatsAppTealGreen

/**
 * Pantalla de perfil de usuario estilo WhatsApp (T-135)
 *
 * Permite ver y editar el perfil del usuario:
 * - Avatar circular grande con botón de cámara para cambiar foto
 * - Nombre (editable al tocar)
 * - Teléfono (solo lectura)
 * - Info/Acerca de (editable)
 * - Botón de guardar cuando hay cambios
 *
 * @param onBack Callback para navegar hacia atrás
 * @param viewModel ViewModel inyectado por Hilt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Launcher para seleccionar imagen de galería
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onEvent(ProfileEvent.OnAvatarSelected(it)) }
    }

    // Mostrar snackbar de éxito al guardar
    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar("Perfil actualizado correctamente")
            viewModel.onEvent(ProfileEvent.OnDismissSuccess)
        }
    }

    // Mostrar snackbar de error
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onEvent(ProfileEvent.OnDismissError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Perfil",
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
        },
        floatingActionButton = {
            // FAB de guardar (visible solo cuando hay cambios)
            if (state.hasChanges && !state.isSaving) {
                FloatingActionButton(
                    onClick = { viewModel.onEvent(ProfileEvent.OnSave) },
                    containerColor = WhatsAppTealGreen,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Guardar cambios"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WhatsAppTealGreen)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Avatar grande con botón de cámara
                AvatarSection(
                    avatarUrl = state.avatarUrl,
                    selectedUri = state.selectedAvatarUri,
                    onCameraClick = {
                        imagePickerLauncher.launch("image/*")
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Campo de nombre (editable)
                ProfileField(
                    icon = Icons.Default.Person,
                    label = "Nombre",
                    value = state.name,
                    isEditable = true,
                    onValueChange = { viewModel.onEvent(ProfileEvent.OnNameChange(it)) }
                )

                Divider(modifier = Modifier.padding(horizontal = 72.dp))

                // Campo de teléfono (solo lectura)
                ProfileField(
                    icon = Icons.Default.Phone,
                    label = "Teléfono",
                    value = state.phone,
                    isEditable = false,
                    onValueChange = {}
                )

                Divider(modifier = Modifier.padding(horizontal = 72.dp))

                // Campo de info/acerca de (editable)
                ProfileField(
                    icon = Icons.Default.Info,
                    label = "Acerca de",
                    value = state.info,
                    isEditable = true,
                    onValueChange = { viewModel.onEvent(ProfileEvent.OnInfoChange(it)) }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Indicador de guardado
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = WhatsAppTealGreen,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Guardando cambios...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

/**
 * Sección del avatar con overlay de cámara.
 * Muestra la foto de perfil actual o un placeholder,
 * con un botón de cámara superpuesto para cambiar la foto.
 */
@Composable
private fun AvatarSection(
    avatarUrl: String?,
    selectedUri: Uri?,
    onCameraClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.BottomEnd
    ) {
        // Avatar circular
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.2f))
                .clickable(onClick = onCameraClick),
            contentAlignment = Alignment.Center
        ) {
            val imageModel = selectedUri ?: avatarUrl

            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                )
            } else {
                // Placeholder sin foto
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Sin foto de perfil",
                    modifier = Modifier.size(80.dp),
                    tint = Color.Gray
                )
            }
        }

        // Botón de cámara superpuesto
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(WhatsAppTealGreen)
                .clickable(onClick = onCameraClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Cambiar foto de perfil",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Campo del perfil con icono, label y valor.
 * Puede ser editable (TextField) o solo lectura.
 *
 * @param icon Icono al inicio del campo
 * @param label Etiqueta del campo
 * @param value Valor actual
 * @param isEditable Si el campo es editable
 * @param onValueChange Callback al cambiar el valor
 */
@Composable
private fun ProfileField(
    icon: ImageVector,
    label: String,
    value: String,
    isEditable: Boolean,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Etiqueta con icono
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.Gray,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.CenterStart)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontSize = 12.sp
                )

                if (isEditable) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppTealGreen,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = WhatsAppTealGreen
                        ),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar $label",
                                tint = WhatsAppTealGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}
