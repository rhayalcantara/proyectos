package com.clonewhatsapp.app.navigation

import kotlinx.serialization.Serializable

sealed interface Routes {
    @Serializable data object Splash : Routes
    @Serializable data object Login : Routes
    @Serializable data object Register : Routes
    @Serializable data object Main : Routes
    @Serializable data class Chat(val chatId: String) : Routes
    @Serializable data class Profile(val userId: String) : Routes
    @Serializable data class Call(val contactId: String, val isVideo: Boolean) : Routes
    @Serializable data object Settings : Routes
}
