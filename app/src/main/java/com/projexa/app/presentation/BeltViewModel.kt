package com.projexa.app.presentation

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projexa.app.data.preferences.SettingsStore
import com.projexa.app.domain.models.BeltCommand
import com.projexa.app.domain.models.BondedDevice
import com.projexa.app.domain.models.ConnectionState
import com.projexa.app.domain.models.Direction
import com.projexa.app.core.haptics.NavHapticController
import com.projexa.app.domain.models.LinkStats
import com.projexa.app.domain.models.NavCueType
import com.projexa.app.domain.models.ProximityThresholds
import com.projexa.app.domain.models.SensorSnapshot
import com.projexa.app.domain.repositories.BeltRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BeltUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val snapshot: SensorSnapshot? = null,
    /** True when a snapshot exists but no new packet has arrived recently. */
    val isStale: Boolean = false,
    val lastUpdateAgeMs: Long? = null,
    val stats: LinkStats = LinkStats(),
    val wallClockMs: Long = System.currentTimeMillis(),
    val bluetoothSupported: Boolean = true,
    val bluetoothEnabled: Boolean = true,
    val pickerDevices: List<BondedDevice>? = null, // non-null => show picker
    val message: String? = null,
    val testRunning: Boolean = false,
    val thresholds: ProximityThresholds = ProximityThresholds(),
    val activeCue: NavCueType? = null,
) {
    val isConnected get() = connection is ConnectionState.Connected
    val isLive get() = isConnected && snapshot != null && !isStale
}

private data class Local(
    val pickerDevices: List<BondedDevice>? = null,
    val message: String? = null,
    val testRunning: Boolean = false,
)

private data class Core(
    val connection: ConnectionState,
    val snapshot: SensorSnapshot?,
    val stats: LinkStats,
    val elapsedNowMs: Long,
    val wallNowMs: Long,
)

class BeltViewModel(
    private val repo: BeltRepository,
    private val settings: SettingsStore,
    private val navHaptics: NavHapticController,
) : ViewModel() {

    private val local = MutableStateFlow(Local())

    private val ticker = flow {
        while (true) {
            emit(SystemClock.elapsedRealtime() to System.currentTimeMillis())
            delay(500)
        }
    }

    private val core = combine(repo.connection, repo.snapshot, repo.linkStats, ticker) { c, s, st, t ->
        Core(c, s, st, t.first, t.second)
    }

    val uiState: StateFlow<BeltUiState> = combine(core, local, navHaptics.active) { c, l, cue ->
        val age = c.snapshot?.let { c.elapsedNowMs - it.receivedAtMs }
        BeltUiState(
            connection = c.connection,
            snapshot = c.snapshot,
            isStale = age != null && age > STALE_AFTER_MS,
            lastUpdateAgeMs = age,
            stats = c.stats,
            wallClockMs = c.wallNowMs,
            bluetoothSupported = repo.isBluetoothSupported(),
            bluetoothEnabled = repo.isBluetoothEnabled(),
            pickerDevices = l.pickerDevices,
            message = l.message,
            testRunning = l.testRunning,
            activeCue = cue,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BeltUiState())

    /** Call only after BLUETOOTH_CONNECT is granted (Android 12+). */
    fun onConnectToggle() {
        val state = uiState.value
        if (state.connection !is ConnectionState.Disconnected) {
            repo.disconnect()
            return
        }
        if (!repo.isBluetoothSupported()) {
            postMessage("This phone has no Bluetooth.")
            return
        }
        if (!repo.isBluetoothEnabled()) {
            postMessage("Bluetooth is off. Turn it on, then press Connect again.")
            return
        }
        val devices = repo.bondedDevices()
        if (devices.isEmpty()) {
            postMessage("No paired devices. Pair \"PROJEXA\" in Android Bluetooth settings first, then press Connect.")
            return
        }
        viewModelScope.launch {
            val last = settings.lastBelt.first()
            val preferred = devices.firstOrNull { it.name.equals(BELT_NAME, ignoreCase = true) }
                ?: last?.let { (_, addr) -> devices.firstOrNull { it.address == addr } }
            if (preferred != null) connectTo(preferred) else local.update { it.copy(pickerDevices = devices) }
        }
    }

    fun connectTo(device: BondedDevice) {
        local.update { it.copy(pickerDevices = null, message = null) }
        repo.connect(device)
        viewModelScope.launch { settings.saveLastBelt(device.name, device.address) }
    }

    fun dismissPicker() = local.update { it.copy(pickerDevices = null) }
    fun dismissMessage() = local.update { it.copy(message = null) }
    fun postMessage(text: String) = local.update { it.copy(message = text) }

    fun playCue(cue: NavCueType) {
        if (!uiState.value.isConnected) {
            postMessage("Connect to the belt first.")
            return
        }
        navHaptics.play(cue)
    }

    fun stopCue() = navHaptics.stop()

    fun testMotor(direction: Direction) {
        if (!uiState.value.isConnected) {
            postMessage("Connect to the belt first.")
            return
        }
        repo.send(BeltCommand.MotorTest(direction))
    }

    /** Pulses each motor in turn. If an obstacle is present the belt keeps that motor on regardless. */
    fun runSensorAndMotorTest() {
        if (!uiState.value.isConnected) {
            postMessage("Connect to the belt first.")
            return
        }
        if (uiState.value.testRunning) return
        viewModelScope.launch {
            local.update { it.copy(testRunning = true) }
            for (d in Direction.entries) {
                repo.send(BeltCommand.MotorTest(d))
                delay(900)
            }
            local.update { it.copy(testRunning = false) }
        }
    }

    companion object {
        const val BELT_NAME = "PROJEXA"
        const val STALE_AFTER_MS = 2500L
    }
}
