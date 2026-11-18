package com.projexa.app.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projexa.app.data.navigation.NavUiState
import com.projexa.app.data.navigation.NavigationCoordinator
import com.projexa.app.data.preferences.SettingsStore
import com.projexa.app.domain.models.Place
import com.projexa.app.domain.models.SavedPlaces
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NavViewModel(
    private val coordinator: NavigationCoordinator,
    private val settings: SettingsStore,
) : ViewModel() {
    val state: StateFlow<NavUiState> = coordinator.state

    val saved: StateFlow<SavedPlaces> =
        settings.savedPlaces.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SavedPlaces())

    fun search(query: String) = coordinator.search(query)
    fun start(place: Place) = coordinator.start(place)
    fun stop() = coordinator.stop()
    fun clearError() = coordinator.clearError()
    fun setHome(place: Place) { viewModelScope.launch { settings.setHome(place) } }
    fun setWork(place: Place) { viewModelScope.launch { settings.setWork(place) } }
}
