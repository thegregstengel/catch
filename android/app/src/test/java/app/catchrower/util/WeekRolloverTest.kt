package app.catchrower.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class WeekRolloverTest {
    private val zone = WeekRollover.TABLET_ZONE

    @Test
    fun defaultZoneIsTheTabletTimezone() {
        assertEquals(ZoneId.of("America/New_York"), WeekRollover.TABLET_ZONE)
        assertEquals(DayOfWeek.SUNDAY, WeekRollover.WEEK_START)
        val wednesday = at(2026, 11, 4, 12, 0)
        assertEquals(
            WeekRollover.weekStartEpochMs(wednesday, zone),
            WeekRollover.weekStartEpochMs(wednesday),
        )
    }

    @Test
    fun weekStartsSundayAndEndsTheNextSunday() {
        val wednesday = at(2026, 11, 4, 12, 0)
        val start = localDate(WeekRollover.weekStartEpochMs(wednesday, zone))
        val end = localDate(WeekRollover.weekEndExclusiveEpochMs(wednesday, zone))
        assertEquals(LocalDate.of(2026, 11, 1), start)
        assertEquals(DayOfWeek.SUNDAY, start.dayOfWeek)
        assertEquals(LocalDate.of(2026, 11, 8), end)
        assertEquals(DayOfWeek.SUNDAY, end.dayOfWeek)
        assertNotEquals(LocalDate.of(2026, 11, 2), start)
    }

    @Test
    fun saturdayIsInsideTheWeekAndTheNextSundayIsNot() {
        val saturday = at(2026, 11, 7, 23, 30)
        val nextSunday = at(2026, 11, 8, 0, 0)
        val start = WeekRollover.weekStartEpochMs(saturday, zone)
        val end = WeekRollover.weekEndExclusiveEpochMs(saturday, zone)
        assertTrue(saturday >= start)
        assertTrue(saturday < end)
        assertEquals(nextSunday, end)
        assertEquals(end, WeekRollover.weekStartEpochMs(nextSunday, zone))
    }

    @Test
    fun sundayMorningBelongsToTheNewWeek() {
        val sunday = at(2026, 11, 8, 0, 1)
        assertEquals(LocalDate.of(2026, 11, 8), localDate(WeekRollover.weekStartEpochMs(sunday, zone)))
    }

    @Test
    fun weekLengthFollowsNewYorkDaylightSaving() {
        val spring = at(2026, 3, 10, 12, 0)
        val springHours = hoursBetween(
            WeekRollover.weekStartEpochMs(spring, zone),
            WeekRollover.weekEndExclusiveEpochMs(spring, zone),
        )
        assertEquals(167, springHours)
        assertEquals(LocalDate.of(2026, 3, 8), localDate(WeekRollover.weekStartEpochMs(spring, zone)))
        assertEquals(LocalDate.of(2026, 3, 15), localDate(WeekRollover.weekEndExclusiveEpochMs(spring, zone)))

        val fall = at(2026, 11, 3, 12, 0)
        val fallHours = hoursBetween(
            WeekRollover.weekStartEpochMs(fall, zone),
            WeekRollover.weekEndExclusiveEpochMs(fall, zone),
        )
        assertEquals(169, fallHours)
    }

    @Test
    fun monthStartsAndEndsOnTheFirst() {
        val mid = at(2026, 11, 15, 18, 0)
        val start = WeekRollover.monthStartEpochMs(mid, zone)
        val end = WeekRollover.monthEndExclusiveEpochMs(mid, zone)
        assertEquals(LocalDate.of(2026, 11, 1), localDate(start))
        assertEquals(1, localDate(start).dayOfMonth)
        assertEquals(LocalDate.of(2026, 12, 1), localDate(end))
        assertEquals(1, localDate(end).dayOfMonth)
        assertEquals(DayOfWeek.SUNDAY, localDate(start).dayOfWeek)

        val lastEvening = at(2026, 11, 30, 23, 30)
        assertTrue(lastEvening >= start)
        assertTrue(lastEvening < end)
        assertEquals(end, WeekRollover.monthStartEpochMs(at(2026, 12, 1, 0, 0), zone))
    }

    @Test
    fun februaryEndsOnTheFirstOfMarch() {
        val feb = at(2026, 2, 28, 23, 0)
        assertEquals(LocalDate.of(2026, 2, 1), localDate(WeekRollover.monthStartEpochMs(feb, zone)))
        assertEquals(LocalDate.of(2026, 3, 1), localDate(WeekRollover.monthEndExclusiveEpochMs(feb, zone)))
        val leap = at(2028, 2, 29, 12, 0)
        assertEquals(LocalDate.of(2028, 3, 1), localDate(WeekRollover.monthEndExclusiveEpochMs(leap, zone)))
    }

    @Test
    fun daysLeftUseTheTabletCalendar() {
        assertEquals(7, WeekRollover.daysLeftInWeek(at(2026, 11, 1, 9, 0), zone))
        assertEquals(1, WeekRollover.daysLeftInWeek(at(2026, 11, 7, 21, 0), zone))
        assertEquals(30, WeekRollover.daysLeftInMonth(at(2026, 11, 1, 9, 0), zone))
        assertEquals(1, WeekRollover.daysLeftInMonth(at(2026, 11, 30, 21, 0), zone))
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private fun localDate(epochMs: Long): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    private fun hoursBetween(start: Long, end: Long): Long =
        Duration.ofMillis(end - start).toHours()
}
