package app.catchrower.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Monday-based weekly window helpers for the family meters board.
 */
object WeekRollover {
    fun weekStartEpochMs(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val date = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate()
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun weekEndExclusiveEpochMs(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val start = Instant.ofEpochMilli(weekStartEpochMs(nowEpochMs, zoneId))
            .atZone(zoneId)
            .toLocalDate()
        return start.plusWeeks(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun daysLeftInWeek(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Int {
        val end = Instant.ofEpochMilli(weekEndExclusiveEpochMs(nowEpochMs, zoneId))
            .atZone(zoneId)
            .toLocalDate()
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate()
        return (end.toEpochDay() - today.toEpochDay()).toInt().coerceAtLeast(0)
    }

    fun isNewWeekRelativeTo(
        previousWeekStartEpochMs: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean = weekStartEpochMs(nowEpochMs, zoneId) > previousWeekStartEpochMs

    fun currentWeekLabel(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        val start: LocalDate = Instant.ofEpochMilli(weekStartEpochMs(nowEpochMs, zoneId))
            .atZone(zoneId)
            .toLocalDate()
        return "Week of $start"
    }
}
