package app.catchrower.rower

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrakeProtocolTest {
    @Test
    fun parsesLiveDi2Floats() {
        val parsed = BrakeProtocol.parseLine("di2 0.000000 0.000 0.000 0.467 499")
        val instant = parsed as BrakeProtocol.ParsedPacket.Instant
        assertEquals(0f, instant.data.watts, 0.001f)
        assertEquals(499, instant.data.sequence)
        assertEquals(0.467f, instant.data.handleMeters!!, 0.001f)
    }

    @Test
    fun parsesDi2WithPower() {
        val parsed = BrakeProtocol.parseLine("di2 12.5 1.2 180.5 0.31 12")
        val instant = parsed as BrakeProtocol.ParsedPacket.Instant
        assertEquals(180.5f, instant.data.watts, 0.001f)
    }

    @Test
    fun parsesDs2CaseInsensitive() {
        val parsed = BrakeProtocol.parseLine("Ds2 0.1 0.2 8.5 140.0 0 0 3")
        val stroke = parsed as BrakeProtocol.ParsedPacket.Stroke
        assertEquals(8.5f, stroke.data.distanceMeters, 0.001f)
        assertEquals(140f, stroke.data.averagePowerWatts, 0.001f)
        assertEquals(3, stroke.data.sequence)
        assertTrue(!stroke.data.distanceIsAbsolute)
    }

    @Test
    fun parsesAbsoluteDs2Distance() {
        val parsed = BrakeProtocol.parseLine("ds2 0 0 125.5 160 0 0 9")
        val stroke = parsed as BrakeProtocol.ParsedPacket.Stroke
        assertTrue(stroke.data.distanceIsAbsolute)
        assertEquals(125.5f, stroke.data.distanceMeters, 0.001f)
    }

    @Test
    fun parsesResistance() {
        val parsed = BrakeProtocol.parseLine("rl 8")
        assertNotNull(parsed)
        assertEquals(8, (parsed as BrakeProtocol.ParsedPacket.Resistance).level)
    }
}
