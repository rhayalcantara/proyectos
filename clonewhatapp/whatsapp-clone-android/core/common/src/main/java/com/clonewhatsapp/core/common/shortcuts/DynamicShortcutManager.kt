package com.clonewhatsapp.core.common.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.clonewhatsapp.domain.model.Chat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestor de shortcuts dinamicos para la aplicacion.
 *
 * Crea y mantiene shortcuts dinamicos en el launcher del dispositivo
 * para los 4 contactos con los que mas se chatea. Los shortcuts permiten:
 * - Acceso rapido a conversaciones frecuentes desde el long-press del icono
 * - Integracion con la funcion de compartir del sistema (CATEGORY_SHARE)
 * - Ranking automatico basado en uso reportado
 *
 * Los shortcuts se actualizan cuando cambia la lista de chats y se
 * eliminan cuando se borra un chat.
 */
@Singleton
class DynamicShortcutManager @Inject constructor(
    @ApplicationContext private val contexto: Context
) {

    companion object {
        // Cantidad maxima de shortcuts dinamicos
        private const val MAXIMO_SHORTCUTS = 4

        // Prefijo para los IDs de shortcuts
        private const val PREFIJO_SHORTCUT = "shortcut_chat_"

        // Categorias de shortcut
        private val CATEGORIAS_SHORTCUT = setOf(
            "android.shortcut.conversation",
            ShortcutManagerCompat.EXTRA_SHORTCUT_ID
        )
    }

    /**
     * Actualiza los shortcuts dinamicos con los chats mas recientes/frecuentes.
     *
     * Toma los primeros [MAXIMO_SHORTCUTS] chats de la lista (que deberia estar
     * ordenada por frecuencia o ultima actividad) y crea shortcuts para cada uno.
     *
     * @param chats Lista de chats ordenada por relevancia (los primeros son los mas frecuentes)
     */
    fun updateShortcuts(chats: List<Chat>) {
        try {
            val chatsParaShortcuts = chats.take(MAXIMO_SHORTCUTS)
            val shortcuts = chatsParaShortcuts.map { chat ->
                crearShortcut(chat)
            }

            ShortcutManagerCompat.setDynamicShortcuts(contexto, shortcuts)
            Timber.d("Shortcuts dinamicos actualizados: ${shortcuts.size} shortcuts")
        } catch (excepcion: Exception) {
            Timber.e(excepcion, "Error al actualizar shortcuts dinamicos")
        }
    }

    /**
     * Agrega un shortcut individual sin reemplazar los existentes.
     *
     * Si ya existen [MAXIMO_SHORTCUTS] shortcuts, el menos usado sera reemplazado
     * automaticamente por el sistema.
     *
     * @param chat Chat para el cual crear el shortcut
     */
    fun agregarShortcut(chat: Chat) {
        try {
            val shortcut = crearShortcut(chat)
            ShortcutManagerCompat.pushDynamicShortcut(contexto, shortcut)
            Timber.d("Shortcut agregado para chat: ${chat.nombre}")
        } catch (excepcion: Exception) {
            Timber.e(excepcion, "Error al agregar shortcut para chat: ${chat.id}")
        }
    }

    /**
     * Elimina el shortcut asociado a un chat.
     * Se debe llamar cuando se elimina un chat.
     *
     * @param chatId Identificador del chat cuyo shortcut se desea eliminar
     */
    fun removeShortcut(chatId: String) {
        try {
            val shortcutId = "$PREFIJO_SHORTCUT$chatId"
            ShortcutManagerCompat.removeDynamicShortcuts(
                contexto,
                listOf(shortcutId)
            )
            Timber.d("Shortcut eliminado para chat: $chatId")
        } catch (excepcion: Exception) {
            Timber.e(excepcion, "Error al eliminar shortcut para chat: $chatId")
        }
    }

    /**
     * Reporta el uso de un shortcut al sistema para mejorar su ranking.
     *
     * Debe llamarse cada vez que el usuario abre un chat, para que el sistema
     * pueda ordenar los shortcuts por frecuencia de uso.
     *
     * @param chatId Identificador del chat que fue abierto
     */
    fun reportShortcutUsed(chatId: String) {
        try {
            val shortcutId = "$PREFIJO_SHORTCUT$chatId"
            ShortcutManagerCompat.reportShortcutUsed(contexto, shortcutId)
            Timber.d("Uso de shortcut reportado para chat: $chatId")
        } catch (excepcion: Exception) {
            Timber.e(excepcion, "Error al reportar uso de shortcut: $chatId")
        }
    }

    /**
     * Elimina todos los shortcuts dinamicos.
     * Util cuando el usuario cierra sesion.
     */
    fun eliminarTodosLosShortcuts() {
        try {
            ShortcutManagerCompat.removeAllDynamicShortcuts(contexto)
            Timber.d("Todos los shortcuts dinamicos eliminados")
        } catch (excepcion: Exception) {
            Timber.e(excepcion, "Error al eliminar todos los shortcuts")
        }
    }

    /**
     * Obtiene la cantidad maxima de shortcuts que el launcher soporta.
     *
     * @return Cantidad maxima de shortcuts permitidos
     */
    fun obtenerMaximoShortcuts(): Int {
        return ShortcutManagerCompat.getMaxShortcutCountPerActivity(contexto)
    }

    // -----------------------------------------------------------------------
    // Metodos auxiliares privados
    // -----------------------------------------------------------------------

    /**
     * Crea un ShortcutInfoCompat a partir de un Chat.
     *
     * El shortcut incluye:
     * - Icono con la inicial del contacto/grupo
     * - Nombre del contacto como etiqueta corta
     * - "Chatear con [nombre]" como etiqueta larga (para accesibilidad)
     * - Intent para abrir directamente la conversacion
     * - Categoria SHARE para integracion con el sistema de compartir
     */
    private fun crearShortcut(chat: Chat): ShortcutInfoCompat {
        val shortcutId = "$PREFIJO_SHORTCUT${chat.id}"

        // Icono del shortcut (usa icono generico del sistema)
        val icono = IconCompat.createWithResource(
            contexto,
            android.R.drawable.ic_menu_myplaces
        )

        // Intent para abrir el chat directamente
        val intentAbrirChat = contexto.packageManager.getLaunchIntentForPackage(
            contexto.packageName
        )?.apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_chat_id", chat.id)
            putExtra("extra_nombre_chat", chat.nombre)
            putExtra("extra_desde_shortcut", true)
        } ?: Intent(Intent.ACTION_VIEW).apply {
            setPackage(contexto.packageName)
            putExtra("extra_chat_id", chat.id)
        }

        // Construir el shortcut
        return ShortcutInfoCompat.Builder(contexto, shortcutId)
            .setShortLabel(chat.nombre)
            .setLongLabel("Chatear con ${chat.nombre}")
            .setIcon(icono)
            .setIntent(intentAbrirChat)
            .setLongLived(true)
            .setRank(0) // El sistema ajusta el rank basado en reportShortcutUsed()
            .setCategories(CATEGORIAS_SHORTCUT)
            .build()
    }
}
