package com.clonewhatsapp.feature.main.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Receptor del widget de chats recientes.
 *
 * Extiende GlanceAppWidgetReceiver para conectar el sistema de widgets
 * de Android con la implementacion de Jetpack Glance.
 *
 * Este receptor se registra en el AndroidManifest.xml y apunta
 * al widget [RecentChatsWidget] que muestra los chats mas recientes.
 */
class RecentChatsWidgetReceiver : GlanceAppWidgetReceiver() {

    /**
     * Retorna la instancia del widget de chats recientes.
     */
    override val glanceAppWidget: GlanceAppWidget
        get() = RecentChatsWidget()
}
