package app.catchrower.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FormattersTest {
    @Test
    fun distanceIsMetersWithoutAMilesUnit() {
        assertEquals("2,000 m", Formatters.metersLabel(2_000f))
        assertEquals("1,609 m", Formatters.metersLabel(1_609.344f))
        assertEquals("500 m", Formatters.metersLabel(500f))
        assertEquals("2,000,000 m", Formatters.metersLabel(2_000_000f))
        listOf(
            Formatters.metersLabel(2_000f),
            Formatters.metersLabel(1_609.344f),
            "Monthly family goal (m)",
            "12,875 m",
        ).forEach { label ->
            assertFalse(label, Formatters.containsMilesUnit(label))
        }
    }

    @Test
    fun milesWordsAreDetected() {
        assertEquals(true, Formatters.containsMilesUnit("311 mi"))
        assertEquals(true, Formatters.containsMilesUnit("1.0 miles"))
        assertEquals(true, Formatters.containsMilesUnit("Weekly goal (mi)"))
        assertFalse(Formatters.containsMilesUnit("2,000 m"))
        assertFalse(Formatters.containsMilesUnit("family"))
        assertFalse(Formatters.containsMilesUnit("12 min"))
    }
}
