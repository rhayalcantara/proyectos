package com.clonewhatsapp.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clonewhatsapp.core.ui.theme.ChatBubbleIncoming
import com.clonewhatsapp.core.ui.theme.ChatBubbleIncomingDark
import com.clonewhatsapp.core.ui.theme.ChatBubbleOutgoing
import com.clonewhatsapp.core.ui.theme.ChatBubbleOutgoingDark
import com.clonewhatsapp.core.ui.theme.CheckDelivered
import com.clonewhatsapp.core.ui.theme.CheckRead
import com.clonewhatsapp.core.ui.theme.CheckSent
import com.clonewhatsapp.domain.model.EstadoMensaje
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Estado de descarga de un documento
 */
enum class DownloadState {
    /** No descargado — se muestra flecha de descarga */
    NOT_DOWNLOADED,
    /** Descargando — se muestra indicador de progreso circular */
    DOWNLOADING,
    /** Descargado — se muestra check de completado */
    DOWNLOADED,
    /** Error en la descarga — se muestra icono de error */
    ERROR
}

// Colores para iconos de documentos segun tipo de archivo
private val PdfColor = Color(0xFFF44336)
private val DocColor = Color(0xFF2196F3)
private val XlsColor = Color(0xFF4CAF50)
private val PptColor = Color(0xFFFF9800)
private val ZipColor = Color(0xFFFFC107)
private val DefaultDocColor = Color(0xFF9E9E9E)

/**
 * Retorna el icono y color apropiado para una extension de archivo.
 *
 * - PDF: icono rojo
 * - DOC/DOCX: icono azul
 * - XLS/XLSX: icono verde
 * - PPT/PPTX: icono naranja
 * - ZIP/RAR/7Z: icono ambar
 * - Otros: icono gris por defecto
 *
 * @param extension Extension del archivo en minusculas (sin punto)
 * @return Par con el [ImageVector] del icono y el [Color] correspondiente
 */
fun getDocumentIcon(extension: String): Pair<ImageVector, Color> {
    return when (extension.lowercase()) {
        "pdf" -> Icons.Default.PictureAsPdf to PdfColor
        "doc", "docx" -> Icons.Default.Description to DocColor
        "xls", "xlsx", "csv" -> Icons.Default.TableChart to XlsColor
        "ppt", "pptx" -> Icons.Default.Description to PptColor
        "zip", "rar", "7z", "tar", "gz" -> Icons.Default.InsertDriveFile to ZipColor
        else -> Icons.Default.InsertDriveFile to DefaultDocColor
    }
}

/**
 * Burbuja de mensaje con documento adjunto estilo WhatsApp (T-062)
 *
 * Muestra un contenedor con icono del tipo de documento, nombre del archivo,
 * tamano, badge de extension y estado de descarga. Compatible con tema claro y oscuro.
 *
 * @param fileName Nombre del archivo a mostrar (truncado si es muy largo)
 * @param fileSize Tamano del archivo formateado (e.g., "2.5 MB"), null si no disponible
 * @param fileExtension Extension del archivo (e.g., "pdf", "xlsx")
 * @param downloadState Estado actual de la descarga del documento
 * @param timestamp Tiempo de envio en milisegundos
 * @param isFromMe Si el mensaje fue enviado por el usuario actual
 * @param estado Estado del mensaje (ENVIADO, ENTREGADO, LEIDO)
 * @param onClick Callback al tocar el documento para descargar o abrir
 * @param modifier Modifier opcional
 */
@Composable
fun DocumentMessageBubble(
    fileName: String,
    fileSize: String?,
    fileExtension: String,
    downloadState: DownloadState,
    timestamp: Long,
    isFromMe: Boolean,
    estado: EstadoMensaje,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    val bubbleColor = when {
        isFromMe && isDark -> ChatBubbleOutgoingDark
        isFromMe -> ChatBubbleOutgoing
        isDark -> ChatBubbleIncomingDark
        else -> ChatBubbleIncoming
    }

    val bubbleShape = if (isFromMe) {
        RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 4.dp,
            bottomStart = 12.dp,
            bottomEnd = 12.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 12.dp,
            bottomStart = 12.dp,
            bottomEnd = 12.dp
        )
    }

    val horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start

    val (docIcon, docColor) = getDocumentIcon(fileExtension)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = horizontalArrangement
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(6.dp)
            ) {
                // Contenedor del documento (fondo sutil)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isDark) Color(0xFF1A2B33) else Color(0xFFF0F0F0)
                        )
                        .clickable(onClick = onClick)
                        .padding(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icono del tipo de documento
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = docColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = docIcon,
                                contentDescription = "Documento $fileExtension",
                                modifier = Modifier.size(24.dp),
                                tint = docColor
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Nombre del archivo y tamano
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDark) Color.White else Color.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Badge de extension
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = docColor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = fileExtension.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = docColor
                                    )
                                }

                                // Tamano del archivo
                                if (!fileSize.isNullOrBlank()) {
                                    Text(
                                        text = " \u00B7 $fileSize",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp
                                        ),
                                        color = if (isDark) Color.LightGray else Color.Gray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Indicador de descarga
                        DocumentDownloadIndicator(
                            downloadState = downloadState,
                            docColor = docColor,
                            isDark = isDark
                        )
                    }
                }

                // Fila inferior: timestamp + checks
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(end = 4.dp, top = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DocumentTimestampText(
                        timestamp = timestamp,
                        isDark = isDark
                    )
                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        DocumentCheckMarks(estado = estado)
                    }
                }
            }
        }
    }
}

/**
 * Indicador del estado de descarga del documento.
 *
 * - NOT_DOWNLOADED: Flecha de descarga
 * - DOWNLOADING: Indicador de progreso circular
 * - DOWNLOADED: Check de completado
 * - ERROR: Icono de error
 */
@Composable
private fun DocumentDownloadIndicator(
    downloadState: DownloadState,
    docColor: Color,
    isDark: Boolean
) {
    Box(
        modifier = Modifier.size(32.dp),
        contentAlignment = Alignment.Center
    ) {
        when (downloadState) {
            DownloadState.NOT_DOWNLOADED -> {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = docColor.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Descargar documento",
                        modifier = Modifier.size(18.dp),
                        tint = docColor
                    )
                }
            }

            DownloadState.DOWNLOADING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = docColor
                )
            }

            DownloadState.DOWNLOADED -> {
                Icon(
                    imageVector = Icons.Default.DownloadDone,
                    contentDescription = "Documento descargado",
                    modifier = Modifier.size(20.dp),
                    tint = if (isDark) Color.LightGray else Color.Gray
                )
            }

            DownloadState.ERROR -> {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error en la descarga",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFF44336)
                )
            }
        }
    }
}

/**
 * Texto de timestamp formateado en HH:mm para la burbuja de documento
 */
@Composable
private fun DocumentTimestampText(
    timestamp: Long,
    isDark: Boolean
) {
    Text(
        text = formatDocumentTimestamp(timestamp),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = if (isDark) Color.LightGray else Color.Gray
    )
}

/**
 * Iconos de check de estado del mensaje para la burbuja de documento
 */
@Composable
private fun DocumentCheckMarks(estado: EstadoMensaje) {
    when (estado) {
        EstadoMensaje.ENVIADO -> {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Enviado",
                modifier = Modifier.size(14.dp),
                tint = CheckSent
            )
        }
        EstadoMensaje.ENTREGADO -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Entregado",
                modifier = Modifier.size(14.dp),
                tint = CheckDelivered
            )
        }
        EstadoMensaje.LEIDO -> {
            Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Leido",
                modifier = Modifier.size(14.dp),
                tint = CheckRead
            )
        }
    }
}

/**
 * Formatea un timestamp en milisegundos a formato de hora HH:mm
 */
private fun formatDocumentTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
