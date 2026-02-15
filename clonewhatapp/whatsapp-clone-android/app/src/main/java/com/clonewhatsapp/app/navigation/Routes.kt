package com.clonewhatsapp.app.navigation

object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val Register = "register"
    const val Main = "main"
    const val Chat = "chat/{chatId}"
    const val Profile = "profile/{userId}"
    const val Call = "call/{contactId}/{isVideo}"
    const val Settings = "settings"

    fun chatRoute(chatId: String) = "chat/$chatId"
    fun profileRoute(userId: String) = "profile/$userId"
    fun callRoute(contactId: String, isVideo: Boolean) = "call/$contactId/$isVideo"
}
