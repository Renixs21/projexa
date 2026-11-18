package com.projexa.app.domain.repositories

import com.projexa.app.domain.models.BeltCommand
import com.projexa.app.domain.models.BondedDevice
import com.projexa.app.domain.models.ConnectionState
import com.projexa.app.domain.models.LinkStats
import com.projexa.app.domain.models.SensorSnapshot
import kotlinx.coroutines.flow.StateFlow

interface BeltRepository {
    val connection: StateFlow<ConnectionState>

    /** Latest real snapshot from the belt; null whenever there is no live link. */
    val snapshot: StateFlow<SensorSnapshot?>
    val linkStats: StateFlow<LinkStats>

    fun isBluetoothSupported(): Boolean
    fun isBluetoothEnabled(): Boolean

    /** Paired devices. Empty if the Bluetooth permission has not been granted. */
    fun bondedDevices(): List<BondedDevice>

    /** Connects and keeps reconnecting until [disconnect] is called. */
    fun connect(device: BondedDevice)
    fun disconnect()
    fun send(command: BeltCommand)
}
