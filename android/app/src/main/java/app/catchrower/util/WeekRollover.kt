package app.catchrower.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Sunday-based week and calendar-month windows for goals and challenges.
 *
 * The tablet timezone is America/New_York, the same zone as the rowing calendar.
 * A week starts at Sunday 00:00 and ends at the next Sunday 00:00. A month starts
 * on the first at 00:00 and ends on the next first at 00:00. The end instant is
 * exclusive, so it falls on the first day of the following week or month.
 */
object WeekRollover {
    val TABLET_ZONE: ZoneId = ZoneId.of("America/New_York")
    val WEEK_START: DayOfWeek = DayOfWeek.SUNDAY

    fun weekStartEpochMs(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = TABLET_ZONE,
    ): Long {
        val date = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate()
        val start = date.with(TemporalAdjusters.previousOrSame(WEEK_START))
        return start.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun weekEndExclusiveEpochMs(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = TABLET_ZONE,
    ): Long {
        val start = Instant.ofEpochMilli(weekStartEpochMs(nowEpochMs, zoneId))
            .atZone(zoneId)
            .toLocalDate()
        return start.plusWeeks(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun monthStartEpochMs(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = TABLET_ZONE,
    ): Long {
        val date = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate()
        return date.withDayOfMonth(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun monthEndExclusiveEpochMs(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = TABLET_ZONE,
    ): Long {
        val start = Instant.ofEpochMilli(monthStartEpochMs(nowEpochMs, zoneId))
            .atZone(zoneId)
            .toLocalDate()
        return start.plusMonths(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun daysLeftInWeek(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = TABLET_ZONE,
    ): Int = daysUntil(weekEndExclusiveEpochMs(nowEpochMs, zoneId), nowEpochMs, zoneId)

    fun daysLeftInMonth(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = TABLET_ZONE,
    ): Int = daysUntil(monthEndExclusiveEpochMs(nowEpochMs, zoneId), nowEpochMs, zoneId)

    fun isNewWeekRelativeTo(
        previousWeekStartEpochMs: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = TABLET_ZONE,
    ): Boolean = weekStartEpochMs(nowEpochMs, zoneId) > previousWeekStartEpochMs

    fun currentWeekLabel(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = TABLET_ZONE,
    ): String {
        val start: LocalDate = Instant.ofEpochMilli(weekStartEpochMs(nowEpochMs, zoneId))
            .atZone(zoneId)
            .toLocalDate()
        return "Week of $start"
    }

    private fun daysUntil(endEpochMs: Long, nowEpochMs: Long, zoneId: ZoneId): Int {
        val end = Instant.ofEpochMilli(endEpochMs).atZone(zoneId).toLocalDate()
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate()
        return (end.toEpochDay() - today.toEpochDay()).toInt().coerceAtLeast(0)
    }
}
