package com.clonewhatsapp.feature.main.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modelo de datos para un chat reciente en el widget.
 * Contiene la informacion minima necesaria para mostrar en el widget.
 */
data class ChatRecienteWidget(
    val chatId: String,
    val nombre: String,
    val ultimoMensaje: String,
    val ultimoMensajeTiempo: Long,
    val mensajesNoLeidos: Int = 0
)

/**
 * Widget de chats recientes para la pantalla de inicio.
 *
 * Muestra los 5 chats mas recientes con:
 * - Nombre del contacto/grupo
 * - Preview del ultimo mensaje (truncado)
 * - Badge de mensajes no leidos
 * - Hora del ultimo mensaje
 *
 * Al tocar un chat, abre la aplicacion en esa conversacion.
 * Se actualiza cada 30 minutos o cuando llega un nuevo mensaje via WorkManager.
 */
class RecentChatsWidget : GlanceAppWidget() {

    companion object {
        // Cantidad maxima de chats a mostrar
        private const val MAXIMO_CHATS = 5

        // Longitud maxima del preview del mensaje
        private const val LONGITUD_MAXIMA_MENSAJE = 35

        // Clave para pasar el chatId al abrir la app
        val parametroChatId = ActionParameters.Key<String>("chat_id_widget")

        // Color verde WhatsApp para el widget
        private val colorVerdeWhatsApp = Color(0xFF00A884)
        private val colorFondoWidget = Color(0xF2FFFFFF)
        private val colorTextoSecundario = Color(0xFF667781)
        private val colorBadge = Color(0xFF25D366)
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Obtener chats recientes desde SharedPreferences o base de datos
        val chatsRecientes = obtenerChatsRecientes(context)

        provideContent {
            ContenidoWidget(chatsRecientes = chatsRecientes)
        }
    }

    /**
     * Contenido principal del widget con Jetpack Glance.
     */
    @Composable
    private fun ContenidoWidget(chatsRecientes: List<ChatRecienteWidget>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colorFondoWidget)
                .cornerRadius(16.dp)
                .padding(8.dp)
        ) {
            // Encabezado del widget
            EncabezadoWidget()

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Lista de chats recientes
            if (chatsRecientes.isEmpty()) {
                EstadoVacio()
            } else {
                LazyColumn(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    items(chatsRecientes) { chat ->
                        FilaChatReciente(chat = chat)
                    }
                }
            }
        }
    }

    /**
     * Encabezado del widget con el titulo "WhatsApp".
     */
    @Composable
    private fun EncabezadoWidget() {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WhatsApp",
                style = TextStyle(
                    color = ColorProvider(colorVerdeWhatsApp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    /**
     * Fila individual de un chat reciente.
     * Muestra la inicial del avatar, nombre, mensaje truncado, hora y badge.
     */
    @Composable
    private fun FilaChatReciente(chat: ChatRecienteWidget) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clickable(
                    actionStartActivity<android.app.Activity>(
                        parameters = actionParametersOf(
                            parametroChatId to chat.chatId
                        )
                    )
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar con inicial del nombre
            AvatarInicial(nombre = chat.nombre)

            Spacer(modifier = GlanceModifier.width(10.dp))

            // Nombre y mensaje
            Column(
                modifier = GlanceModifier.defaultWeight()
            ) {
                Text(
                    text = chat.nombre,
                    style = TextStyle(
                        color = ColorProvider(Color.Black),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1
                )

                Text(
                    text = truncarMensaje(chat.ultimoMensaje),
                    style = TextStyle(
                        color = ColorProvider(colorTextoSecundario),
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )
            }

            // Columna derecha: hora y badge
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatearHora(chat.ultimoMensajeTiempo),
                    style = TextStyle(
                        color = ColorProvider(
                            if (chat.mensajesNoLeidos > 0) colorVerdeWhatsApp
                            else colorTextoSecundario
                        ),
                        fontSize = 11.sp
                    )
                )

                if (chat.mensajesNoLeidos > 0) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    BadgeMensajesNoLeidos(cantidad = chat.mensajesNoLeidos)
                }
            }
        }
    }

    /**
     * Muestra la inicial del nombre del contacto como avatar circular.
     */
    @Composable
    private fun AvatarInicial(nombre: String) {
        val inicial = nombre.firstOrNull()?.uppercase() ?: "?"

        Box(
            modifier = GlanceModifier
                .size(40.dp)
                .cornerRadius(20.dp)
                .background(colorVerdeWhatsApp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = inicial,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    /**
     * Badge que muestra la cantidad de mensajes no leidos.
     */
    @Composable
    private fun BadgeMensajesNoLeidos(cantidad: Int) {
        val textoContador = if (cantidad > 99) "99+" else cantidad.toString()

        Box(
            modifier = GlanceModifier
                .size(20.dp)
                .cornerRadius(10.dp)
                .background(colorBadge),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = textoContador,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
    }

    /**
     * Estado vacio cuando no hay chats recientes.
     */
    @Composable
    private fun EstadoVacio() {
        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay chats recientes",
                style = TextStyle(
                    color = ColorProvider(colorTextoSecundario),
                    fontSize = 14.sp
                )
            )
        }
    }

    // -----------------------------------------------------------------------
    // Metodos auxiliares
    // -----------------------------------------------------------------------

    /**
     * Obtiene los chats recientes desde SharedPreferences.
     * En produccion, se leerian desde la base de datos Room directamente.
     */
    private fun obtenerChatsRecientes(context: Context): List<ChatRecienteWidget> {
        val preferencias = context.getSharedPreferences(
            "widget_chats_recientes",
            Context.MODE_PRIVATE
        )

        val chats = mutableListOf<ChatRecienteWidget>()
        val cantidadChats = preferencias.getInt("cantidad_chats", 0)

        for (i in 0 until minOf(cantidadChats, MAXIMO_CHATS)) {
            val chatId = preferencias.getString("chat_${i}_id", null) ?: continue
            val nombre = preferencias.getString("chat_${i}_nombre", "") ?: ""
            val mensaje = preferencias.getString("chat_${i}_mensaje", "") ?: ""
            val tiempo = preferencias.getLong("chat_${i}_tiempo", 0L)
            val noLeidos = preferencias.getInt("chat_${i}_no_leidos", 0)

            chats.add(
                ChatRecienteWidget(
                    chatId = chatId,
                    nombre = nombre,
                    ultimoMensaje = mensaje,
                    ultimoMensajeTiempo = tiempo,
                    mensajesNoLeidos = noLeidos
                )
            )
        }

        return chats
    }

    /**
     * Trunca el mensaje si excede la longitud maxima.
     */
    private fun truncarMensaje(mensaje: String): String {
        return if (mensaje.length > LONGITUD_MAXIMA_MENSAJE) {
            mensaje.take(LONGITUD_MAXIMA_MENSAJE) + "..."
        } else {
            mensaje
        }
    }

    /**
     * Formatea un timestamp a formato de hora legible (HH:mm o dia de la semana).
     */
    private fun formatearHora(timestamp: Long): String {
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
                SimpleDateFormat("dd/MM/yy", Locale("es")).format(Date(timestamp))
            }
        }
    }

    /**
     * Guarda los chats recientes en SharedPreferences para que el widget los lea.
     * Debe llamarse cuando se actualiza la lista de chats.
     *
     * @param context Contexto de la aplicacion
     * @param chats Lista de chats a guardar (se toman los primeros 5)
     */
    // Funciones auxiliares para guardar chats en SharedPreferences
    object GuardarChats {
        fun guardarChatsRecientes(context: Context, chats: List<ChatRecienteWidget>) {
            val preferencias = context.getSharedPreferences(
                "widget_chats_recientes",
                Context.MODE_PRIVATE
            ).edit()

            val chatsTruncados = chats.take(5)
            preferencias.putInt("cantidad_chats", chatsTruncados.size)

            chatsTruncados.forEachIndexed { indice, chat ->
                preferencias.putString("chat_${indice}_id", chat.chatId)
                preferencias.putString("chat_${indice}_nombre", chat.nombre)
                preferencias.putString("chat_${indice}_mensaje", chat.ultimoMensaje)
                preferencias.putLong("chat_${indice}_tiempo", chat.ultimoMensajeTiempo)
                preferencias.putInt("chat_${indice}_no_leidos", chat.mensajesNoLeidos)
            }

            preferencias.apply()
        }
    }
}
