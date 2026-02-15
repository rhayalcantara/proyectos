package com.clonewhatsapp.feature.chat.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.clonewhatsapp.domain.model.Chat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests de UI con Compose para ChatListScreen (T-124)
 *
 * Verifica:
 * - La lista de chats renderiza los elementos correctamente
 * - El estado vacio se muestra cuando no hay chats
 * - El click en un chat dispara el callback con el id correcto
 * - El boton de busqueda esta presente
 * - El FAB de nuevo chat esta presente
 */
class ChatListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // -- Helpers --

    private fun crearChatsTest(): List<Chat> = listOf(
        Chat(
            id = "chat-1",
            nombre = "Maria Lopez",
            ultimoMensaje = "Hola, como estas?",
            mensajesNoLeidos = 3,
            ultimoMensajeTiempo = System.currentTimeMillis()
        ),
        Chat(
            id = "chat-2",
            nombre = "Pedro Garcia",
            ultimoMensaje = "Nos vemos manana",
            mensajesNoLeidos = 0,
            ultimoMensajeTiempo = System.currentTimeMillis() - 3600_000
        ),
        Chat(
            id = "chat-3",
            nombre = "Grupo Trabajo",
            esGrupo = true,
            ultimoMensaje = "Reunion a las 3pm",
            mensajesNoLeidos = 5,
            ultimoMensajeTiempo = System.currentTimeMillis() - 7200_000
        )
    )

    private fun renderChatListScreen(
        chats: List<Chat> = emptyList(),
        isLoading: Boolean = false,
        isRefreshing: Boolean = false,
        onChatClick: (String) -> Unit = {},
        onNewChatClick: () -> Unit = {},
        onSearchClick: () -> Unit = {},
        onRefresh: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            ChatListScreen(
                chats = chats,
                isLoading = isLoading,
                isRefreshing = isRefreshing,
                onChatClick = onChatClick,
                onNewChatClick = onNewChatClick,
                onSearchClick = onSearchClick,
                onRefresh = onRefresh
            )
        }
    }

    // -- Tests de renderizado de la lista --

    @Test
    fun listaDeChatsRenderizaElementosCorrectamente() {
        val chats = crearChatsTest()
        renderChatListScreen(chats = chats)

        // Verificar que los nombres de los chats se muestran
        composeTestRule.onNodeWithText("Maria Lopez").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pedro Garcia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grupo Trabajo").assertIsDisplayed()
    }

    @Test
    fun listaDeChatsRenderizaUltimosMensajes() {
        val chats = crearChatsTest()
        renderChatListScreen(chats = chats)

        composeTestRule.onNodeWithText("Hola, como estas?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nos vemos manana").assertIsDisplayed()
    }

    // -- Tests de estado vacio --

    @Test
    fun estadoVacioSeMuestraCuandoNoHayChats() {
        renderChatListScreen(
            chats = emptyList(),
            isLoading = false
        )

        composeTestRule.onNodeWithText("No tienes chats aun").assertIsDisplayed()
    }

    // -- Tests de click en chat --

    @Test
    fun clickEnChatDisparaCallbackConIdCorrecto() {
        var chatIdClickeado = ""
        val chats = crearChatsTest()

        renderChatListScreen(
            chats = chats,
            onChatClick = { chatIdClickeado = it }
        )

        composeTestRule.onNodeWithText("Maria Lopez").performClick()

        assertEquals("chat-1", chatIdClickeado)
    }

    // -- Tests de barra de herramientas --

    @Test
    fun tituloWhatsAppCloneSeMuestra() {
        renderChatListScreen()

        composeTestRule.onNodeWithText("WhatsApp Clone").assertIsDisplayed()
    }

    @Test
    fun botonBuscarEstaPresente() {
        var buscarClickeado = false
        renderChatListScreen(
            onSearchClick = { buscarClickeado = true }
        )

        composeTestRule.onNodeWithContentDescription("Buscar").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Buscar").performClick()

        assertTrue("Callback de busqueda debe haberse llamado", buscarClickeado)
    }

    @Test
    fun fabNuevoChatEstaPresente() {
        var nuevoChatClickeado = false
        renderChatListScreen(
            onNewChatClick = { nuevoChatClickeado = true }
        )

        composeTestRule.onNodeWithContentDescription("Nuevo chat").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Nuevo chat").performClick()

        assertTrue("Callback de nuevo chat debe haberse llamado", nuevoChatClickeado)
    }
}
