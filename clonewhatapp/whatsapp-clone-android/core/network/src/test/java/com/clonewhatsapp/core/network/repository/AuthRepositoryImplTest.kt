package com.clonewhatsapp.core.network.repository

import com.clonewhatsapp.core.network.api.AuthApi
import com.clonewhatsapp.core.network.auth.SecureTokenStorage
import com.clonewhatsapp.core.network.dto.AuthResponse
import com.clonewhatsapp.core.network.dto.LoginRequest
import com.clonewhatsapp.core.network.dto.RegisterRequest
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

/**
 * Tests unitarios para AuthRepositoryImpl (T-122)
 *
 * Verifica:
 * - Login exitoso (API retorna token, se guarda, retorna usuario)
 * - Login fallido (error de API)
 * - Registro exitoso/fallido
 * - Logout (limpia tokens)
 * - Estado de sesion
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryImplTest {

    private lateinit var authApi: AuthApi
    private lateinit var tokenStorage: SecureTokenStorage
    private lateinit var gson: Gson
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setUp() {
        authApi = mock()
        tokenStorage = mock()
        gson = Gson()
        repository = AuthRepositoryImpl(authApi, tokenStorage, gson)
    }

    // -- Helpers --

    private fun crearAuthResponse(): AuthResponse = AuthResponse(
        id = "user-001",
        numeroTelefono = "+1234567890",
        nombre = "Juan Test",
        fotoPerfil = "https://ejemplo.com/foto.jpg",
        token = "jwt-token-12345",
        tokenExpiration = "2026-12-31T23:59:59"
    )

    // -- Tests de login exitoso --

    @Test
    fun `login exitoso retorna usuario y guarda token`() = runTest {
        val authResponse = crearAuthResponse()
        whenever(authApi.login(any())).thenReturn(Response.success(authResponse))

        val resultado = repository.login("+1234567890", "password123")

        assertTrue("Login debe ser exitoso", resultado.isSuccess)
        val usuario = resultado.getOrNull()
        assertNotNull("Usuario no debe ser null", usuario)
        assertEquals("user-001", usuario!!.id)
        assertEquals("Juan Test", usuario.nombre)
        assertEquals("+1234567890", usuario.telefono)

        // Verificar que se guardo el token
        verify(tokenStorage).saveToken("jwt-token-12345")
        verify(tokenStorage).saveUserId("user-001")
        verify(tokenStorage).saveUserName("Juan Test")
        verify(tokenStorage).saveUserPhone("+1234567890")
    }

    @Test
    fun `login exitoso guarda la expiracion del token`() = runTest {
        val authResponse = crearAuthResponse()
        whenever(authApi.login(any())).thenReturn(Response.success(authResponse))

        repository.login("+1234567890", "password123")

        verify(tokenStorage).saveTokenExpiration(any())
    }

    // -- Tests de login fallido --

    @Test
    fun `login con credenciales invalidas retorna error`() = runTest {
        val errorBody = """{"message": "Credenciales invalidas"}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        whenever(authApi.login(any()))
            .thenReturn(Response.error(401, errorBody))

        val resultado = repository.login("+1234567890", "wrongPassword")

        assertTrue("Login debe fallar", resultado.isFailure)
        // No debe guardar token
        verify(tokenStorage, never()).saveToken(any())
    }

    @Test
    fun `login con respuesta vacia retorna error`() = runTest {
        whenever(authApi.login(any()))
            .thenReturn(Response.success(null))

        val resultado = repository.login("+1234567890", "password123")

        assertTrue("Login debe fallar con respuesta vacia", resultado.isFailure)
        assertTrue(
            "Mensaje debe indicar respuesta vacia",
            resultado.exceptionOrNull()?.message?.contains("vac") == true
        )
    }

    @Test
    fun `login con excepcion de red retorna error`() = runTest {
        whenever(authApi.login(any())).thenThrow(RuntimeException("Sin conexion"))

        val resultado = repository.login("+1234567890", "password123")

        assertTrue("Login debe fallar", resultado.isFailure)
        assertEquals("Sin conexion", resultado.exceptionOrNull()?.message)
    }

    // -- Tests de registro --

    @Test
    fun `registro exitoso retorna usuario y guarda token`() = runTest {
        val authResponse = crearAuthResponse()
        whenever(authApi.register(any())).thenReturn(Response.success(authResponse))

        val resultado = repository.register("Juan Test", "+1234567890", "password123")

        assertTrue("Registro debe ser exitoso", resultado.isSuccess)
        val usuario = resultado.getOrNull()
        assertNotNull("Usuario no debe ser null", usuario)
        assertEquals("Juan Test", usuario!!.nombre)

        // Verificar que se guardo el token
        verify(tokenStorage).saveToken("jwt-token-12345")
    }

    @Test
    fun `registro con telefono duplicado retorna error`() = runTest {
        val errorBody = """{"message": "El numero ya esta registrado"}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        whenever(authApi.register(any()))
            .thenReturn(Response.error(400, errorBody))

        val resultado = repository.register("Juan", "+1234567890", "password")

        assertTrue("Registro debe fallar", resultado.isFailure)
    }

    @Test
    fun `registro con excepcion de red retorna error`() = runTest {
        whenever(authApi.register(any())).thenThrow(RuntimeException("Error de red"))

        val resultado = repository.register("Juan", "+1234567890", "password")

        assertTrue("Registro debe fallar", resultado.isFailure)
    }

    // -- Tests de logout --

    @Test
    fun `logout limpia todos los datos almacenados`() = runTest {
        repository.logout()

        verify(tokenStorage).clear()
    }

    // -- Tests de estado de sesion --

    @Test
    fun `isLoggedIn retorna el valor del tokenStorage`() {
        whenever(tokenStorage.isLoggedIn()).thenReturn(true)
        assertTrue(repository.isLoggedIn())

        whenever(tokenStorage.isLoggedIn()).thenReturn(false)
        assertFalse(repository.isLoggedIn())
    }

    @Test
    fun `getCurrentUserId retorna el id del tokenStorage`() {
        whenever(tokenStorage.getUserId()).thenReturn("user-001")
        assertEquals("user-001", repository.getCurrentUserId())
    }

    @Test
    fun `getCurrentUserName retorna el nombre del tokenStorage`() {
        whenever(tokenStorage.getUserName()).thenReturn("Juan Test")
        assertEquals("Juan Test", repository.getCurrentUserName())
    }

    @Test
    fun `getCurrentUserPhone retorna el telefono del tokenStorage`() {
        whenever(tokenStorage.getUserPhone()).thenReturn("+1234567890")
        assertEquals("+1234567890", repository.getCurrentUserPhone())
    }
}
