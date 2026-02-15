package com.clonewhatsapp.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.clonewhatsapp.feature.auth.login.LoginEvent
import com.clonewhatsapp.feature.auth.login.LoginScreen
import com.clonewhatsapp.feature.auth.login.LoginViewModel
import com.clonewhatsapp.feature.auth.register.RegisterEvent
import com.clonewhatsapp.feature.auth.register.RegisterScreen
import com.clonewhatsapp.feature.auth.register.RegisterViewModel
import com.clonewhatsapp.feature.auth.splash.SplashScreen
import com.clonewhatsapp.feature.auth.splash.SplashViewModel
import com.clonewhatsapp.feature.chat.window.ChatWindowEvent
import com.clonewhatsapp.feature.chat.window.ChatWindowScreen
import com.clonewhatsapp.feature.chat.window.ChatWindowViewModel
import com.clonewhatsapp.feature.main.MainScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {
        composable(Routes.Splash) {
            val viewModel: SplashViewModel = hiltViewModel()
            val isLoggedIn by viewModel.isLoggedIn.collectAsState()

            SplashScreen(
                isLoggedIn = isLoggedIn,
                onNavigateToMain = {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.Login) {
            val viewModel: LoginViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(state.isLoginSuccess) {
                if (state.isLoginSuccess) {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                }
            }

            LoginScreen(
                phone = state.phone,
                password = state.password,
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                onPhoneChange = { viewModel.onEvent(LoginEvent.OnPhoneChange(it)) },
                onPasswordChange = { viewModel.onEvent(LoginEvent.OnPasswordChange(it)) },
                onLoginClick = { viewModel.onEvent(LoginEvent.OnLoginClick) },
                onNavigateToRegister = {
                    navController.navigate(Routes.Register)
                },
                onErrorDismiss = { viewModel.onEvent(LoginEvent.OnErrorDismiss) }
            )
        }

        composable(Routes.Register) {
            val viewModel: RegisterViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(state.isRegisterSuccess) {
                if (state.isRegisterSuccess) {
                    navController.navigate(Routes.Main) {
                        popUpTo(Routes.Register) { inclusive = true }
                    }
                }
            }

            RegisterScreen(
                name = state.name,
                phone = state.phone,
                password = state.password,
                confirmPassword = state.confirmPassword,
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                onNameChange = { viewModel.onEvent(RegisterEvent.OnNameChange(it)) },
                onPhoneChange = { viewModel.onEvent(RegisterEvent.OnPhoneChange(it)) },
                onPasswordChange = { viewModel.onEvent(RegisterEvent.OnPasswordChange(it)) },
                onConfirmPasswordChange = { viewModel.onEvent(RegisterEvent.OnConfirmPasswordChange(it)) },
                onRegisterClick = { viewModel.onEvent(RegisterEvent.OnRegisterClick) },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onErrorDismiss = { viewModel.onEvent(RegisterEvent.OnErrorDismiss) }
            )
        }

        composable(Routes.Main) {
            MainScreen(
                onChatClick = { chatId ->
                    navController.navigate(Routes.chatRoute(chatId))
                },
                onNewChatClick = {
                    // TODO: Navegacion a pantalla de contactos para nuevo chat
                }
            )
        }

        composable(
            route = Routes.Chat,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) {
            val viewModel: ChatWindowViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()

            ChatWindowScreen(
                chatName = state.chatName,
                chatPhotoUrl = state.chatPhotoUrl,
                isOnline = state.isOnline,
                messages = state.messages,
                currentUserId = state.currentUserId,
                messageText = state.messageText,
                isLoading = state.isLoading,
                onTextChange = { viewModel.onEvent(ChatWindowEvent.OnTextChange(it)) },
                onSendClick = { viewModel.onEvent(ChatWindowEvent.OnSendClick) },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
