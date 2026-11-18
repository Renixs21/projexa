package com.projexa.app.domain.models

/** The four belt directions. [wireCode] is the letter used in phone->belt commands. */
enum class Direction(val label: String, val jsonKey: String, val wireCode: Char) {
    FRONT("Front", "front", 'F'),
    BACK("Back", "back", 'B'),
    LEFT("Left", "left", 'L'),
    RIGHT("Right", "right", 'R'),
}

/**
 * One complete scan of the belt.
 *
 * @property distancesCm null = the belt reported an invalid / no-echo reading. This is NOT "clear".
 * @property motorsOn actual motor output reported by the belt; null if the belt did not report it.
 * @property invalidStreak consecutive snapshots in which the sensor produced no valid reading.
 * @property receivedAtMs [android.os.SystemClock.elapsedRealtime] when the packet arrived.
 */
data class SensorSnapshot(
    val distancesCm: Map<Direction, Float?>,
    val motorsOn: Map<Direction, Boolean>?,
    val invalidStreak: Map<Direction, Int>,
    val receivedAtMs: Long,
)

enum class ProximityLevel(val label: String) {
    CLEAR("Clear"),
    CAUTION("Caution"),
    WARNING("Warning"),
    DANGER("Danger"),
    CRITICAL("Critical"),
    NO_READING("No reading"),
}

/** Initial UI thresholds from the spec. Display only: the belt keeps its own fixed 50 cm logic. */
data class ProximityThresholds(
    val cautionCm: Float = 50f,
    val warningCm: Float = 35f,
    val dangerCm: Float = 20f,
    val criticalCm: Float = 10f,
) {
    fun classify(distanceCm: Float?): ProximityLevel = when {
        distanceCm == null -> ProximityLevel.NO_READING
        distanceCm < criticalCm -> ProximityLevel.CRITICAL
        distanceCm < dangerCm -> ProximityLevel.DANGER
        distanceCm < warningCm -> ProximityLevel.WARNING
        distanceCm <= cautionCm -> ProximityLevel.CAUTION
        else -> ProximityLevel.CLEAR
    }
}

data class BondedDevice(val name: String, val address: String)

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val device: BondedDevice, val attempt: Int) : ConnectionState
    data class Connected(val device: BondedDevice) : ConnectionState
}

data class LinkStats(val packetsReceived: Long = 0, val malformedLines: Long = 0)

/** Phone -> belt commands. See firmware header for the wire protocol. */
sealed interface BeltCommand {
    fun toWire(): String

    /** Pulse one motor for 500 ms (hardware test). Obstacle alerts still take priority on the belt. */
    data class MotorTest(val direction: Direction) : BeltCommand {
        override fun toWire() = "T,${direction.wireCode}\n"
    }

    /** Pulsed navigation cue. Expires on the belt after ~3 s unless refreshed. */
    data class NavCue(val direction: Direction, val onMs: Int, val offMs: Int) : BeltCommand {
        init {
            require(onMs in 40..1000) { "onMs out of range" }
            require(offMs in 40..2000) { "offMs out of range" }
        }

        override fun toWire() = "N,${direction.wireCode},$onMs,$offMs\n"
    }

    data object NavClear : BeltCommand {
        override fun toWire() = "N,X\n"
    }
}

/**
 * Navigation haptic cues. Each uses ONE belt motor with a pulsed rhythm.
 * Obstacle alerts are steady (not pulsed) and always override these on the belt.
 */
enum class NavCueType(val label: String, val meaning: String, val direction: Direction, val onMs: Int, val offMs: Int) {
    STRAIGHT("Continue straight", "Slow pulses at the front", Direction.FRONT, 300, 700),
    TURN_LEFT("Turn left", "Slow pulses on the left", Direction.LEFT, 300, 700),
    TURN_RIGHT("Turn right", "Slow pulses on the right", Direction.RIGHT, 300, 700),
    STOP("Stop", "Fast pulses at the back", Direction.BACK, 100, 150),
    ARRIVED("Arrived", "Very fast pulses at the front", Direction.FRONT, 60, 60),
}
