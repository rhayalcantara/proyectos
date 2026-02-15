package com.clonewhatsapp.feature.auth.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SplashScreen(
    isLoggedIn: Boolean?,
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Navigate based on login status
    LaunchedEffect(isLoggedIn) {
        when (isLoggedIn) {
            true -> onNavigateToMain()
            false -> onNavigateToLogin()
            null -> { /* Loading, do nothing */ }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // WhatsApp Icon placeholder (you can replace with actual icon/logo)
            Text(
                text = "💬",
                fontSize = 96.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // App name
            Text(
                text = "WhatsApp Clone",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                ),
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Loading indicator
            if (isLoggedIn == null) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}
