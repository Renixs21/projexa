package com.projexa.app.data.bluetooth

import com.projexa.app.domain.models.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SensorDataParserTest {
    @Test
    fun parsesFullPacketWithNullsAndMotors() {
        val line = """{"front":35.2,"back":null,"left":18.5,"right":75.0,"motors":{"front":true,"back":false,"left":true,"right":false}}"""
        val s = SensorDataParser.parse(line, 1000)
        assertNotNull(s)
        assertEquals(35.2f, s!!.distancesCm[Direction.FRONT]!!, 0.001f)
        assertNull(s.distancesCm[Direction.BACK])
        assertEquals(true, s.motorsOn!![Direction.LEFT])
        assertEquals(false, s.motorsOn!![Direction.BACK])
        assertEquals(1, s.invalidStreak[Direction.BACK])
        assertEquals(0, s.invalidStreak[Direction.FRONT])
    }

    @Test
    fun invalidStreakAccumulates() {
        val line = """{"front":null,"back":null,"left":null,"right":null}"""
        val first = SensorDataParser.parse(line, 1)!!
        val second = SensorDataParser.parse(line, 2, first.invalidStreak)!!
        assertEquals(2, second.invalidStreak[Direction.LEFT])
        assertNull(second.motorsOn)
    }

    @Test
    fun rejectsGarbageAndIncompletePackets() {
        assertNull(SensorDataParser.parse("not json", 0))
        assertNull(SensorDataParser.parse("""{"front":10.0}""", 0))
        assertNull(SensorDataParser.parse("", 0))
    }

    @Test
    fun outOfRangeValuesBecomeNull() {
        val s = SensorDataParser.parse("""{"front":-5,"back":9999,"left":10,"right":0}""", 0)!!
        assertNull(s.distancesCm[Direction.FRONT])
        assertNull(s.distancesCm[Direction.BACK])
        assertEquals(10f, s.distancesCm[Direction.LEFT]!!, 0.001f)
        assertEquals(0f, s.distancesCm[Direction.RIGHT]!!, 0.001f)
    }
}
