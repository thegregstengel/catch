package app.catchrower.data

import app.catchrower.data.db.WorkoutEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class YearInReviewTest {
    private val zone = ZoneId.of("America/New_York")

    @Test
    fun windowIsTheFirstSevenDaysOfJanuary() {
        assertEquals(2026, YearInReview.reviewYear(LocalDate.of(2027, 1, 1)))
        assertEquals(2026, YearInReview.reviewYear(LocalDate.of(2027, 1, 7)))
        assertNull(YearInReview.reviewYear(LocalDate.of(2027, 1, 8)))
        assertNull(YearInReview.reviewYear(LocalDate.of(2026, 12, 31)))
        assertNull(YearInReview.reviewYear(LocalDate.of(2026, 11, 1)))
        assertEquals(2025, YearInReview.reviewYear(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun januaryWithNoPriorYearDataStaysQuiet() {
        val today = LocalDate.of(2027, 1, 3)
        val onlyOlder = listOf(session(1, LocalDate.of(2025, 6, 1), 2_000f))
        val otherProfile = listOf(session(2, LocalDate.of(2026, 11, 2), 3_000f))
        val placeholders = listOf(session(1, LocalDate.of(2026, 12, 1), 0f))

        assertNull(YearInReview.summarize(1, onlyOlder, 2026, zone))
        assertNull(YearInReview.summarize(1, otherProfile, 2026, zone))
        assertNull(YearInReview.summarize(1, placeholders, 2026, zone))
        assertFalse(YearInReview.shouldShow(today, summary = null, dismissed = false))
    }

    @Test
    fun summaryUsesStoredSessionsFromThePreviousYear() {
        val lateUtc = LocalDate.of(2027, 1, 1)
            .atTime(4, 30)
            .atZone(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val workouts = listOf(
            session(1, LocalDate.of(2026, 3, 2), 5_000f, durationSec = 3_600, calories = 400f, strokes = 800),
            session(1, LocalDate.of(2026, 3, 2), 1_000f, hour = 18, durationSec = 1_200, calories = 80f, strokes = 200),
            session(1, LocalDate.of(2026, 11, 1), 2_000f, durationSec = 900, calories = 150f, strokes = 300),
            WorkoutEntity(
                profileId = 1,
                startedAtEpochMs = lateUtc,
                endedAtEpochMs = lateUtc + 60_000,
                distanceMeters = 500f,
                durationSec = 300,
                calories = 40f,
                strokeCount = 50,
            ),
            session(1, LocalDate.of(2027, 1, 1), 9_000f),
            session(2, LocalDate.of(2026, 5, 5), 8_000f),
        )
        val before = workouts.toList()

        val summary = YearInReview.summarize(1, workouts, 2026, zone)!!

        assertEquals(before, workouts)
        assertEquals(2026, summary.year)
        assertEquals(3, summary.daysRowed)
        assertEquals(4, summary.sessions)
        assertEquals(8_500f, summary.distanceMeters)
        assertEquals(3_600 + 1_200 + 900 + 300, summary.durationSec)
        assertEquals(670f, summary.calories)
        assertEquals(1_350, summary.strokeCount)
        assertEquals(
            "3 days on the water, 4 sessions. 8,500 m. 1 h 40 min. Every stroke counted.",
            summary.body(),
        )
        assertEquals("1,350 strokes. 670 kcal.", summary.detailLine())
        assertEquals("2026 in review", summary.title())
    }

    @Test
    fun popupShowsDuringTheWindowUntilDismissed() {
        val summary = YearInReview.Summary(
            year = 2026,
            daysRowed = 1,
            sessions = 1,
            distanceMeters = 2_000f,
            durationSec = 1_200,
            calories = 0f,
            strokeCount = 0,
        )
        val inside = LocalDate.of(2027, 1, 7)
        assertTrue(YearInReview.shouldShow(inside, summary, dismissed = false))
        assertFalse(YearInReview.shouldShow(inside, summary, dismissed = true))
        assertFalse(YearInReview.shouldShow(LocalDate.of(2027, 1, 8), summary, dismissed = false))
        assertFalse(
            YearInReview.shouldShow(
                inside,
                summary.copy(year = 2025),
                dismissed = false,
            ),
        )
        assertEquals(
            "1 day on the water, 1 session. 2,000 m. 20 min. Every stroke counted.",
            summary.body(),
        )
        assertNull(summary.detailLine())
    }

    @Test
    fun timeLabelReadsInHoursAndMinutes() {
        assertEquals("20 min", YearInReview.timeLabel(20 * 60))
        assertEquals("2 h", YearInReview.timeLabel(2 * 3600))
        assertEquals("2 h 5 min", YearInReview.timeLabel(2 * 3600 + 5 * 60))
    }

    private fun session(
        profileId: Long,
        day: LocalDate,
        meters: Float,
        hour: Int = 12,
        durationSec: Int = 600,
        calories: Float? = null,
        strokes: Int = 0,
    ) = WorkoutEntity(
        profileId = profileId,
        startedAtEpochMs = day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli(),
        endedAtEpochMs = day.atTime(hour, 30).atZone(zone).toInstant().toEpochMilli(),
        distanceMeters = meters,
        durationSec = durationSec,
        calories = calories,
        strokeCount = strokes,
    )
}
