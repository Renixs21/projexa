package com.projexa.app

import android.app.Application
import com.projexa.app.core.haptics.NavHapticController
import com.projexa.app.data.bluetooth.BluetoothBeltRepository
import com.projexa.app.data.navigation.LocationSource
import com.projexa.app.data.navigation.NavigationCoordinator
import com.projexa.app.data.navigation.OsmClient
import com.projexa.app.data.preferences.SettingsStore
import com.projexa.app.domain.models.AppSettings
import com.projexa.app.domain.repositories.BeltRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Application-scoped manual dependency container (no DI framework needed yet). */
class ProjexaApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val beltRepository: BeltRepository by lazy { BluetoothBeltRepository(this, appScope) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }

    private val _settings = MutableStateFlow<AppSettings?>(null)

    /** Null until the saved settings have loaded. */
    val settings: StateFlow<AppSettings?> = _settings

    /** One controller shared by the haptic tutorial and route guidance so they never fight over the belt. */
    val navHaptics: NavHapticController by lazy {
        NavHapticController(beltRepository, appScope) { _settings.value?.pulseScale ?: 1f }
    }

    val navigationCoordinator: NavigationCoordinator by lazy {
        NavigationCoordinator(
            osm = OsmClient(),
            location = LocationSource(this),
            haptics = navHaptics,
            instructionDistanceM = { (_settings.value?.cueDistanceM ?: 30).toDouble() },
            scope = appScope,
            onDestinationStarted = { settingsStore.addHistory(it) },
        )
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch { settingsStore.appSettings.collect { _settings.value = it } }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value ?: AppSettings())
        _settings.value = next
        appScope.launch { settingsStore.saveAppSettings(next) }
    }
}
