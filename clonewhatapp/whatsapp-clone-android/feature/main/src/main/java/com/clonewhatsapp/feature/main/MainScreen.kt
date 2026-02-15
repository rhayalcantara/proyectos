package com.clonewhatsapp.feature.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clonewhatsapp.core.ui.theme.WhatsAppTealGreen
import com.clonewhatsapp.feature.chat.list.ChatListScreen
import com.clonewhatsapp.feature.chat.list.ChatListViewModel
import com.clonewhatsapp.feature.status.list.StatusListEvent
import com.clonewhatsapp.feature.status.list.StatusListScreen
import com.clonewhatsapp.feature.status.list.StatusListViewModel

/**
 * Tabs de la pantalla principal
 */
enum class MainTab(
    val label: String,
    val icon: ImageVector
) {
    CHATS("Chats", Icons.Default.Chat),
    ESTADOS("Estados", Icons.Outlined.Circle),
    LLAMADAS("Llamadas", Icons.Default.Call)
}

/**
 * Pantalla principal con BottomNavigation (T-032)
 * 3 tabs: Chats, Estados, Llamadas
 *
 * @param onChatClick Callback al hacer clic en un chat
 * @param onNewChatClick Callback al presionar nuevo chat
 * @param modifier Modifier opcional
 */
@Composable
fun MainScreen(
    onChatClick: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onViewMyStatus: () -> Unit = {},
    onViewContactStatus: (String) -> Unit = {},
    onCreateTextStatus: () -> Unit = {},
    onCreateImageStatus: () -> Unit = {},
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel(),
    chatListViewModel: ChatListViewModel = hiltViewModel(),
    statusListViewModel: StatusListViewModel = hiltViewModel()
) {
    val mainState by mainViewModel.state.collectAsStateWithLifecycle()
    val chatListState by chatListViewModel.state.collectAsStateWithLifecycle()
    val statusListState by statusListViewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = mainState.selectedTab == tab,
                        onClick = { mainViewModel.onEvent(MainEvent.OnTabSelected(tab)) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(text = tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WhatsAppTealGreen,
                            selectedTextColor = WhatsAppTealGreen,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (mainState.selectedTab) {
                MainTab.CHATS -> {
                    ChatListScreen(
                        chats = chatListState.filteredChats,
                        isLoading = chatListState.isLoading,
                        isRefreshing = chatListState.isRefreshing,
                        onChatClick = onChatClick,
                        onNewChatClick = onNewChatClick,
                        onSearchClick = {
                            chatListViewModel.onEvent(
                                com.clonewhatsapp.feature.chat.list.ChatListEvent.OnSearchClick
                            )
                        },
                        onRefresh = {
                            chatListViewModel.onEvent(
                                com.clonewhatsapp.feature.chat.list.ChatListEvent.OnRefresh
                            )
                        }
                    )
                }

                MainTab.ESTADOS -> {
                    StatusListScreen(
                        misEstados = statusListState.misEstados,
                        estadosContactos = statusListState.estadosContactos,
                        isLoading = statusListState.isLoading,
                        isRefreshing = statusListState.isRefreshing,
                        onMyStatusClick = onViewMyStatus,
                        onContactStatusClick = onViewContactStatus,
                        onCreateTextStatus = onCreateTextStatus,
                        onCreateImageStatus = onCreateImageStatus,
                        onRefresh = {
                            statusListViewModel.onEvent(StatusListEvent.OnRefresh)
                        }
                    )
                }

                MainTab.LLAMADAS -> {
                    PlaceholderScreen(title = "Llamadas")
                }
            }
        }
    }
}

/**
 * Pantalla placeholder para tabs no implementados aún
 */
@Composable
private fun PlaceholderScreen(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$title\n(Próximamente)",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
