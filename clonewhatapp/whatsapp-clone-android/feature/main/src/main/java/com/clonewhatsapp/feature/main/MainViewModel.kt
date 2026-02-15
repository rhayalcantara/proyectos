package com.clonewhatsapp.feature.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Estado de la pantalla principal
 */
data class MainState(
    val selectedTab: MainTab = MainTab.CHATS
)

/**
 * Eventos de la pantalla principal (MVI)
 */
sealed interface MainEvent {
    data class OnTabSelected(val tab: MainTab) : MainEvent
}

/**
 * ViewModel para la pantalla principal
 * Maneja la selección de tabs
 */
@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    fun onEvent(event: MainEvent) {
        when (event) {
            is MainEvent.OnTabSelected -> {
                _state.update { it.copy(selectedTab = event.tab) }
            }
        }
    }
}
