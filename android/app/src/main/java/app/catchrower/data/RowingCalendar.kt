package app.catchrower.data

import app.catchrower.data.db.WorkoutEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * One rower's month, Sunday through Saturday.
 *
 * The calendar opens on 1 November 2026 in America/New_York. Days before that
 * stay empty. While today is still in November 2026, earlier days of that
 * month stay empty too. From today forward, a day lights up when this profile
 * has a saved session with distance on that local date. After November, those
 * saved days light when you look back.
 *
 * Nothing here deletes workouts. Later months and years read the same table.
 */
object RowingCalendar {
    val START: LocalDate = LocalDate.of(2026, 11, 1)
    val DEFAULT_ZONE: ZoneId = ZoneId.of("America/New_York")
    private val WEEK_START: DayOfWeek = DayOfWeek.SUNDAY

    fun zoneOf(timezoneId: String?): ZoneId =
        runCatching { ZoneId.of(timezoneId ?: DEFAULT_ZONE.id) }.getOrDefault(DEFAULT_ZONE)

    fun localDate(epochMs: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    /**
     * Opening Catch writes a workout row before anyone rows. A saved session
     * is one that actually recorded distance.
     */
    fun isRowedSession(workout: WorkoutEntity): Boolean = workout.distanceMeters > 0f

    /**
     * Whether [day] may light, before checking for a session.
     * November 2026 days before [today] stay empty while today is still in that month.
     */
    fun canLight(day: LocalDate, today: LocalDate): Boolean {
        if (day < START) return false
        val openingMonth = today.year == START.year && today.monthValue == START.monthValue &&
            day.year == START.year && day.monthValue == START.monthValue
        if (openingMonth && day < today) return false
        return true
    }

    fun markedDays(
        profileId: Long,
        workouts: List<WorkoutEntity>,
        month: YearMonth,
        today: LocalDate,
        zone: ZoneId = DEFAULT_ZONE,
    ): Set<LocalDate> {
        return rowedBy(profileId, workouts)
            .map { localDate(it.startedAtEpochMs, zone) }
            .filter { YearMonth.from(it) == month && canLight(it, today) }
            .toSet()
    }

    fun earliestMonth(): YearMonth = YearMonth.from(START)

    fun initialMonth(today: LocalDate): YearMonth {
        val current = YearMonth.from(today)
        val earliest = earliestMonth()
        return if (current < earliest) earliest else current
    }

    fun canGoToPrevious(month: YearMonth): Boolean = month > earliestMonth()

    fun monthTitle(month: YearMonth): String {
        val name = month.month.getDisplayName(TextStyle.FULL, Locale.US)
        return "$name ${month.year}"
    }

    fun weekdayLabels(): List<String> = listOf(
        DayOfWeek.SUNDAY,
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
        DayOfWeek.SATURDAY,
    ).map { it.getDisplayName(TextStyle.NARROW, Locale.US) }

    data class Cell(val date: LocalDate?)

    /** Sunday-first weeks. Cells outside [month] are null. */
    fun weeks(month: YearMonth): List<List<Cell>> {
        val first = month.atDay(1)
        val lead = (first.dayOfWeek.value - WEEK_START.value + 7) % 7
        val cells = ArrayList<Cell>()
        repeat(lead) { cells += Cell(null) }
        for (day in 1..month.lengthOfMonth()) {
            cells += Cell(month.atDay(day))
        }
        while (cells.size % 7 != 0) cells += Cell(null)
        return cells.chunked(7)
    }

    data class Stats(
        val daysRowed: Int,
        val sessions: Int,
        val distanceMeters: Float,
        val durationSec: Int,
    )

    /** Numbers for the days that are allowed to light in [month]. */
    fun monthStats(
        profileId: Long,
        workouts: List<WorkoutEntity>,
        month: YearMonth,
        today: LocalDate,
        zone: ZoneId = DEFAULT_ZONE,
    ): Stats = stats(profileId, workouts, zone) { day ->
        YearMonth.from(day) == month && canLight(day, today)
    }

    /** Every saved session for this profile, including days the calendar leaves empty. */
    fun lifetimeStats(
        profileId: Long,
        workouts: List<WorkoutEntity>,
        zone: ZoneId = DEFAULT_ZONE,
    ): Stats = stats(profileId, workouts, zone) { true }

    private fun stats(
        profileId: Long,
        workouts: List<WorkoutEntity>,
        zone: ZoneId,
        include: (LocalDate) -> Boolean,
    ): Stats {
        val rows = rowedBy(profileId, workouts).filter {
            include(localDate(it.startedAtEpochMs, zone))
        }
        return Stats(
            daysRowed = rows.map { localDate(it.startedAtEpochMs, zone) }.toSet().size,
            sessions = rows.size,
            distanceMeters = rows.sumOf { it.distanceMeters.toDouble() }.toFloat(),
            durationSec = rows.sumOf { it.durationSec },
        )
    }

    private fun rowedBy(profileId: Long, workouts: List<WorkoutEntity>): List<WorkoutEntity> =
        workouts.filter { it.profileId == profileId && isRowedSession(it) }
}
