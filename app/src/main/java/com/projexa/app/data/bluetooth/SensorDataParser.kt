package com.projexa.app.data.bluetooth

import com.projexa.app.domain.models.Direction
import com.projexa.app.domain.models.SensorSnapshot
import org.json.JSONException
import org.json.JSONObject

/** Parses one newline-delimited JSON packet from the belt firmware. */
object SensorDataParser {
    private const val MAX_VALID_CM = 400.0

    /**
     * @param previousStreak invalid-reading streaks of the previous snapshot (empty on first packet)
     * @return null if the line is not a valid sensor packet
     */
    fun parse(
        line: String,
        receivedAtMs: Long,
        previousStreak: Map<Direction, Int> = emptyMap(),
    ): SensorSnapshot? {
        val json = try {
            JSONObject(line.trim())
        } catch (_: JSONException) {
            return null
        }
        // A sensor packet must contain all four keys (value may be null).
        if (Direction.entries.any { !json.has(it.jsonKey) }) return null

        val distances = Direction.entries.associateWith { dir ->
            if (json.isNull(dir.jsonKey)) {
                null
            } else {
                val v = json.optDouble(dir.jsonKey, Double.NaN)
                if (v.isNaN() || v < 0.0 || v > MAX_VALID_CM) null else v.toFloat()
            }
        }

        val motors = json.optJSONObject("motors")?.let { m ->
            Direction.entries.associateWith { m.optBoolean(it.jsonKey, false) }
        }

        val streak = Direction.entries.associateWith { dir ->
            if (distances[dir] == null) (previousStreak[dir] ?: 0) + 1 else 0
        }
        return SensorSnapshot(distances, motors, streak, receivedAtMs)
    }
}
