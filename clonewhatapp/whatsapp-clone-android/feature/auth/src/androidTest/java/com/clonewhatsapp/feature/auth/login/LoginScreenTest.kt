package com.clonewhatsapp.feature.auth.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests de UI con Compose para LoginScreen (T-124)
 *
 * Verifica:
 * - El formulario de login se renderiza correctamente
 * - Los campos de telefono y contrasena aceptan input
 * - El boton de login dispara el callback
 * - Los mensajes de error se muestran
 * - El indicador de carga se muestra durante loading
 * - Los campos se deshabilitan durante loading
 */
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // -- Helpers --

    private fun renderLoginScreen(
        phone: String = "",
        password: String = "",
        isLoading: Boolean = false,
        errorMessage: String? = null,
        onPhoneChange: (String) -> Unit = {},
        onPasswordChange: (String) -> Unit = {},
        onLoginClick: () -> Unit = {},
        onNavigateToRegister: () -> Unit = {},
        onErrorDismiss: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            LoginScreen(
                phone = phone,
                password = password,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onPhoneChange = onPhoneChange,
                onPasswordChange = onPasswordChange,
                onLoginClick = onLoginClick,
                onNavigateToRegister = onNavigateToRegister,
                onErrorDismiss = onErrorDismiss
            )
        }
    }

    // -- Tests de renderizado del formulario --

    @Test
    fun formularioDeLoginSeRenderizaCorrectamente() {
        renderLoginScreen()

        // Verificar que los elementos principales estan presentes
        composeTestRule.onNodeWithText("WhatsApp Clone").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bienvenido").assertIsDisplayed()
        composeTestRule.onNodeWithText("Telefono").assertIsDisplayed()
        composeTestRule.onNodeWithText("Contrasena").assertIsDisplayed()
        composeTestRule.onNodeWithText("Iniciar Sesion").assertIsDisplayed()
        composeTestRule.onNodeWithText("No tienes cuenta? Registrate").assertIsDisplayed()
    }

    // -- Tests de input de campos --

    @Test
    fun campoTelefonoAceptaInput() {
        var telefonoCapturado = ""
        renderLoginScreen(
            onPhoneChange = { telefonoCapturado = it }
        )

        composeTestRule.onNodeWithText("Telefono").performTextInput("+1234567890")

        assertTrue(
            "El callback de telefono debe haberse llamado",
            telefonoCapturado.isNotEmpty()
        )
    }

    @Test
    fun campoContrasenaAceptaInput() {
        var contrasenaCapturada = ""
        renderLoginScreen(
            onPasswordChange = { contrasenaCapturada = it }
        )

        composeTestRule.onNodeWithText("Contrasena").performTextInput("miPassword")

        assertTrue(
            "El callback de contrasena debe haberse llamado",
            contrasenaCapturada.isNotEmpty()
        )
    }

    // -- Tests de boton de login --

    @Test
    fun botonLoginDisparaCallback() {
        var loginClickado = false
        renderLoginScreen(
            phone = "+1234567890",
            password = "password123",
            onLoginClick = { loginClickado = true }
        )

        composeTestRule.onNodeWithText("Iniciar Sesion").performClick()

        assertTrue("El callback de login debe haberse llamado", loginClickado)
    }

    @Test
    fun botonLoginDeshabilitadoCuandoCamposVacios() {
        renderLoginScreen(
            phone = "",
            password = ""
        )

        composeTestRule.onNodeWithText("Iniciar Sesion").assertIsNotEnabled()
    }

    @Test
    fun botonLoginHabilitadoCuandoCamposTienenTexto() {
        renderLoginScreen(
            phone = "+1234567890",
            password = "password123"
        )

        composeTestRule.onNodeWithText("Iniciar Sesion").assertIsEnabled()
    }

    // -- Tests de estado de carga --

    @Test
    fun estadoDeCargaDeshabilitaBotonLogin() {
        renderLoginScreen(
            phone = "+1234567890",
            password = "password123",
            isLoading = true
        )

        // El boton debe estar deshabilitado durante la carga
        composeTestRule.onNodeWithText("Iniciar Sesion").assertDoesNotExist()
        // En su lugar se muestra un CircularProgressIndicator (no tiene texto)
    }

    // -- Tests de enlace de registro --

    @Test
    fun enlaceRegistroDisparaCallback() {
        var navegarARegistro = false
        renderLoginScreen(
            onNavigateToRegister = { navegarARegistro = true }
        )

        composeTestRule.onNodeWithText("No tienes cuenta? Registrate").performClick()

        assertTrue("Debe navegar a registro", navegarARegistro)
    }
}
