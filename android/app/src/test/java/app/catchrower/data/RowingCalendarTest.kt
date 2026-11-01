package app.catchrower.data

import app.catchrower.data.db.WorkoutEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

class RowingCalendarTest {
    private val zone = ZoneId.of("America/New_York")
    private val november = YearMonth.of(2026, 11)
    private val openingDay = LocalDate.of(2026, 11, 1)

    @Test
    fun november2026OpensOnSunday() {
        val weeks = RowingCalendar.weeks(november)
        assertEquals(LocalDate.of(2026, 11, 1), weeks.first().first().date)
        assertEquals(openingDay.dayOfWeek, java.time.DayOfWeek.SUNDAY)
        assertEquals(5, weeks.size)
        assertEquals(LocalDate.of(2026, 11, 30), weeks.last()[1].date)
        assertEquals(null, weeks.last()[2].date)
    }

    @Test
    fun december2026StartsOnTuesday() {
        val weeks = RowingCalendar.weeks(YearMonth.of(2026, 12))
        assertEquals(null, weeks.first()[0].date)
        assertEquals(null, weeks.first()[1].date)
        assertEquals(LocalDate.of(2026, 12, 1), weeks.first()[2].date)
    }

    @Test
    fun calendarCannotPageBeforeNovember2026() {
        assertFalse(RowingCalendar.canGoToPrevious(november))
        assertTrue(RowingCalendar.canGoToPrevious(YearMonth.of(2026, 12)))
        assertEquals(november, RowingCalendar.initialMonth(openingDay))
        assertEquals(november, RowingCalendar.initialMonth(LocalDate.of(2026, 10, 15)))
        assertEquals(YearMonth.of(2027, 3), RowingCalendar.initialMonth(LocalDate.of(2027, 3, 4)))
    }

    @Test
    fun daysBeforeTodayInNovemberStayEmpty() {
        val today = LocalDate.of(2026, 11, 15)
        val workouts = listOf(
            session(1, LocalDate.of(2026, 11, 10), 2_000f),
            session(1, LocalDate.of(2026, 11, 15), 1_500f),
            session(1, LocalDate.of(2026, 11, 20), 800f),
        )

        val marked = RowingCalendar.markedDays(1, workouts, november, today, zone)

        assertFalse(LocalDate.of(2026, 11, 10) in marked)
        assertTrue(LocalDate.of(2026, 11, 15) in marked)
        assertTrue(LocalDate.of(2026, 11, 20) in marked)
        assertEquals(2, marked.size)
    }

    @Test
    fun openingDayLightsWhenThatProfileRowed() {
        val workouts = listOf(
            session(1, openingDay, 2_000f),
            session(1, LocalDate.of(2026, 11, 3), 1_000f),
            session(2, LocalDate.of(2026, 11, 2), 5_000f),
            session(1, LocalDate.of(2026, 10, 31), 9_000f),
            session(1, openingDay, 0f),
        )
        val before = workouts.size

        val marked = RowingCalendar.markedDays(1, workouts, november, openingDay, zone)

        assertEquals(setOf(openingDay, LocalDate.of(2026, 11, 3)), marked)
        assertEquals(before, workouts.size)
        assertEquals(5, workouts.size)
    }

    @Test
    fun twoSessionsOnOneDayLightItOnce() {
        val day = LocalDate.of(2026, 11, 8)
        val workouts = listOf(
            session(1, day, 1_000f),
            session(1, day, 500f, hour = 18),
        )
        val marked = RowingCalendar.markedDays(1, workouts, november, openingDay, zone)
        assertEquals(setOf(day), marked)
        val stats = RowingCalendar.monthStats(1, workouts, november, openingDay, zone)
        assertEquals(1, stats.daysRowed)
        assertEquals(2, stats.sessions)
        assertEquals(1_500f, stats.distanceMeters)
    }

    @Test
    fun laterMonthsAndYearsKeepSavedDays() {
        val today = LocalDate.of(2026, 12, 5)
        val workouts = listOf(
            session(1, LocalDate.of(2026, 11, 10), 2_000f),
            session(1, LocalDate.of(2026, 12, 5), 3_000f),
            session(1, LocalDate.of(2027, 1, 2), 4_000f),
        )

        assertEquals(
            setOf(LocalDate.of(2026, 11, 10)),
            RowingCalendar.markedDays(1, workouts, november, today, zone),
        )
        assertEquals(
            setOf(LocalDate.of(2026, 12, 5)),
            RowingCalendar.markedDays(1, workouts, YearMonth.of(2026, 12), today, zone),
        )
        assertEquals(
            setOf(LocalDate.of(2027, 1, 2)),
            RowingCalendar.markedDays(1, workouts, YearMonth.of(2027, 1), today, zone),
        )
    }

    @Test
    fun daysBeforeTheCalendarStayEmptyButTheMetersRemain() {
        val october = session(1, LocalDate.of(2026, 10, 31), 4_200f, durationSec = 900)
        val novemberRow = session(1, openingDay, 1_000f, durationSec = 600)
        val workouts = listOf(october, novemberRow)

        assertTrue(
            RowingCalendar.markedDays(1, workouts, YearMonth.of(2026, 10), openingDay, zone).isEmpty(),
        )
        val lifetime = RowingCalendar.lifetimeStats(1, workouts, zone)
        assertEquals(2, lifetime.sessions)
        assertEquals(5_200f, lifetime.distanceMeters)
        assertEquals(1_500, lifetime.durationSec)

        val month = RowingCalendar.monthStats(1, workouts, november, openingDay, zone)
        assertEquals(1, month.sessions)
        assertEquals(1_000f, month.distanceMeters)
    }

    @Test
    fun localDateUsesNewYorkSoLateUtcStaysOnThePreviousDay() {
        val epoch = LocalDate.of(2026, 11, 2)
            .atTime(3, 30)
            .atZone(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        assertEquals(LocalDate.of(2026, 11, 1), RowingCalendar.localDate(epoch, zone))
    }

    private fun session(
        profileId: Long,
        day: LocalDate,
        meters: Float,
        hour: Int = 12,
        durationSec: Int = 600,
    ) = WorkoutEntity(
        profileId = profileId,
        startedAtEpochMs = day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli(),
        endedAtEpochMs = day.atTime(hour, 20).atZone(zone).toInstant().toEpochMilli(),
        distanceMeters = meters,
        durationSec = durationSec,
    )
}
