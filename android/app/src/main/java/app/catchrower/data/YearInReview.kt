package app.catchrower.data

import app.catchrower.data.db.WorkoutEntity
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.util.Locale

/**
 * A short look back at the previous calendar year.
 *
 * It appears only on 1-7 January, and only when that profile has saved
 * sessions in the year that just ended. An empty January stays quiet.
 */
object YearInReview {
    const val WINDOW_LAST_DAY = 7

    /** The year to summarize, or null when today is outside the January window. */
    fun reviewYear(today: LocalDate): Int? {
        if (today.month != Month.JANUARY) return null
        if (today.dayOfMonth !in 1..WINDOW_LAST_DAY) return null
        return today.year - 1
    }

    fun shouldShow(today: LocalDate, summary: Summary?, dismissed: Boolean): Boolean {
        if (dismissed) return false
        val year = reviewYear(today) ?: return false
        return summary != null && summary.year == year && summary.sessions > 0
    }

    data class Summary(
        val year: Int,
        val daysRowed: Int,
        val sessions: Int,
        val distanceMeters: Float,
        val durationSec: Int,
        val calories: Float,
        val strokeCount: Int,
    ) {
        fun title(): String = "$year in review"

        fun body(): String {
            val days = if (daysRowed == 1) "1 day" else "$daysRowed days"
            val sessionsLabel = if (sessions == 1) "1 session" else "$sessions sessions"
            val parts = mutableListOf("$days on the water, $sessionsLabel", "${metersLabel()} m")
            if (durationSec > 0) parts += timeLabel(durationSec)
            return parts.joinToString(". ") + ". Every stroke counted."
        }

        /** Strokes and calories, when the year actually recorded them. */
        fun detailLine(): String? {
            val bits = mutableListOf<String>()
            if (strokeCount > 0) {
                bits += "${String.format(Locale.US, "%,d", strokeCount)} strokes"
            }
            if (calories > 0f) bits += "${caloriesLabel()} kcal"
            if (bits.isEmpty()) return null
            return bits.joinToString(". ") + "."
        }

        fun metersLabel(): String = String.format(Locale.US, "%,.0f", distanceMeters)

        fun caloriesLabel(): String = String.format(Locale.US, "%,.0f", calories)
    }

    fun timeLabel(durationSec: Int): String {
        val hours = durationSec / 3600
        val minutes = (durationSec % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "$hours h $minutes min"
            hours > 0 -> "$hours h"
            minutes > 0 -> "$minutes min"
            else -> "0 min"
        }
    }

    /**
     * Totals for [year] from stored workouts. Returns null when this profile
     * has no saved sessions that year, so January does not open an empty popup.
     */
    fun summarize(
        profileId: Long,
        workouts: List<WorkoutEntity>,
        year: Int,
        zone: ZoneId = RowingCalendar.DEFAULT_ZONE,
    ): Summary? {
        val rows = workouts.filter { workout ->
            workout.profileId == profileId &&
                RowingCalendar.isRowedSession(workout) &&
                RowingCalendar.localDate(workout.startedAtEpochMs, zone).year == year
        }
        if (rows.isEmpty()) return null
        return Summary(
            year = year,
            daysRowed = rows
                .map { RowingCalendar.localDate(it.startedAtEpochMs, zone) }
                .toSet()
                .size,
            sessions = rows.size,
            distanceMeters = rows.sumOf { it.distanceMeters.toDouble() }.toFloat(),
            durationSec = rows.sumOf { it.durationSec },
            calories = rows.sumOf { (it.calories ?: 0f).toDouble() }.toFloat(),
            strokeCount = rows.sumOf { it.strokeCount },
        )
    }
}
