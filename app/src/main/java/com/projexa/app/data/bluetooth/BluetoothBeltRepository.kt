package com.projexa.app.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.SystemClock
import com.projexa.app.domain.models.BeltCommand
import com.projexa.app.domain.models.BondedDevice
import com.projexa.app.domain.models.ConnectionState
import com.projexa.app.domain.models.Direction
import com.projexa.app.domain.models.LinkStats
import com.projexa.app.domain.models.SensorSnapshot
import com.projexa.app.domain.repositories.BeltRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/**
 * Real Bluetooth Classic (RFCOMM / SPP) link to the PROJEXA belt.
 * No simulated data: [snapshot] is null whenever there is no live link.
 */
@SuppressLint("MissingPermission") // Callers gate on BLUETOOTH_CONNECT; SecurityException is handled below.
class BluetoothBeltRepository(
    context: Context,
    private val scope: CoroutineScope,
) : BeltRepository {

    private val adapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    private val _connection = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connection: StateFlow<ConnectionState> = _connection

    private val _snapshot = MutableStateFlow<SensorSnapshot?>(null)
    override val snapshot: StateFlow<SensorSnapshot?> = _snapshot

    private val _stats = MutableStateFlow(LinkStats())
    override val linkStats: StateFlow<LinkStats> = _stats

    private var job: Job? = null
    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var output: OutputStream? = null
    private val writeLock = Any()

    override fun isBluetoothSupported() = adapter != null
    override fun isBluetoothEnabled() = adapter?.isEnabled == true

    override fun bondedDevices(): List<BondedDevice> = try {
        adapter?.bondedDevices.orEmpty()
            .map { BondedDevice(it.name ?: it.address, it.address) }
            .sortedBy { it.name }
    } catch (_: SecurityException) {
        emptyList()
    }

    override fun connect(device: BondedDevice) {
        job?.cancel()
        closeSocket()
        _stats.value = LinkStats()
        job = scope.launch(Dispatchers.IO) { connectionLoop(device) }
    }

    override fun disconnect() {
        job?.cancel()
        job = null
        closeSocket()
        _snapshot.value = null
        _connection.value = ConnectionState.Disconnected
    }

    override fun send(command: BeltCommand) {
        val bytes = command.toWire().toByteArray(Charsets.US_ASCII)
        scope.launch(Dispatchers.IO) {
            try {
                synchronized(writeLock) {
                    output?.let {
                        it.write(bytes)
                        it.flush()
                    }
                }
            } catch (_: IOException) {
                // The read loop notices the broken link and reconnects.
            }
        }
    }

    private suspend fun connectionLoop(device: BondedDevice) {
        var attempt = 0
        while (scope.isActive && job?.isActive != false) {
            attempt++
            _connection.value = ConnectionState.Connecting(device, attempt)
            try {
                val ad = adapter ?: return
                if (!ad.isEnabled) throw IOException("Bluetooth is off")
                val remote = ad.getRemoteDevice(device.address)
                try {
                    ad.cancelDiscovery() // needs BLUETOOTH_SCAN on Android 12+; not required to connect
                } catch (_: SecurityException) {
                }
                val s = remote.createRfcommSocketToServiceRecord(SPP_UUID)
                socket = s
                s.connect()
                output = s.outputStream
                attempt = 0
                _connection.value = ConnectionState.Connected(device)
                readLoop(s)
            } catch (_: IOException) {
                // fall through to backoff + retry
            } catch (_: SecurityException) {
                _connection.value = ConnectionState.Disconnected
                return
            } finally {
                closeSocket()
                _snapshot.value = null
            }
            if (!scope.isActive) return
            _connection.value = ConnectionState.Connecting(device, attempt)
            delay((1000L * attempt).coerceAtMost(5000L))
        }
    }

    private fun readLoop(s: BluetoothSocket) {
        val reader = s.inputStream.bufferedReader(Charsets.US_ASCII)
        while (true) {
            val line = reader.readLine() ?: throw IOException("Link closed")
            if (line.isBlank()) continue
            val parsed = SensorDataParser.parse(
                line = line,
                receivedAtMs = SystemClock.elapsedRealtime(),
                previousStreak = _snapshot.value?.invalidStreak ?: emptyMap<Direction, Int>(),
            )
            _stats.value = if (parsed != null) {
                _stats.value.copy(packetsReceived = _stats.value.packetsReceived + 1)
            } else {
                _stats.value.copy(malformedLines = _stats.value.malformedLines + 1)
            }
            if (parsed != null) _snapshot.value = parsed
        }
    }

    private fun closeSocket() {
        synchronized(writeLock) { output = null }
        try {
            socket?.close()
        } catch (_: IOException) {
        }
        socket = null
    }

    private companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
