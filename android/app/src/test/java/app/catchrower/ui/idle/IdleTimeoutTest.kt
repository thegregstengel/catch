package app.catchrower.ui.idle

import app.catchrower.data.db.AppSettingsEntity
import app.catchrower.rower.SessionPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class IdleTimeoutTest {
    @Test
    fun defaultTimeoutIsFiveMinutesAndTheSettingStartsOn() {
        assertEquals(5, IdleTimeout.TIMEOUT_MINUTES)
        assertEquals(5 * 60 * 1000L, IdleTimeout.TIMEOUT_MS)
        assertTrue(IdleTimeout.isEnabled(null))
        assertTrue(IdleTimeout.isEnabled(true))
        assertFalse(IdleTimeout.isEnabled(false))
        assertTrue(AppSettingsEntity().screenOffWhenIdle)

        val idle = ScreenIdle()
        idle.resetClock(0L)
        idle.tick(
            nowElapsedMs = IdleTimeout.TIMEOUT_MS - 1,
            enabled = true,
            hostingMedia = false,
        )
        assertFalse(idle.coverVisible)
        idle.tick(
            nowElapsedMs = IdleTimeout.TIMEOUT_MS,
            enabled = true,
            hostingMedia = false,
        )
        assertTrue(idle.coverVisible)
    }

    @Test
    fun idleShorterThanTheTimeoutStaysLit() {
        assertFalse(
            IdleTimeout.shouldTurnOff(
                enabled = true,
                hostingMedia = false,
                lastActivityElapsedMs = 1_000L,
                nowElapsedMs = 1_000L + IdleTimeout.TIMEOUT_MS - 1,
            ),
        )
    }

    @Test
    fun turnedOffOrWatchingAShowStaysLit() {
        val longIdle = IdleTimeout.TIMEOUT_MS * 4
        assertFalse(
            IdleTimeout.shouldTurnOff(
                enabled = false,
                hostingMedia = false,
                lastActivityElapsedMs = 0L,
                nowElapsedMs = longIdle,
            ),
        )
        assertFalse(
            IdleTimeout.shouldTurnOff(
                enabled = true,
                hostingMedia = true,
                lastActivityElapsedMs = 0L,
                nowElapsedMs = longIdle,
            ),
        )
    }

    @Test
    fun aNonPositiveTimeoutNeverBlanks() {
        assertFalse(
            IdleTimeout.shouldTurnOff(
                enabled = true,
                hostingMedia = false,
                lastActivityElapsedMs = 0L,
                nowElapsedMs = 60_000L,
                timeoutMs = 0L,
            ),
        )
        assertFalse(
            IdleTimeout.shouldTurnOff(
                enabled = true,
                hostingMedia = false,
                lastActivityElapsedMs = 5_000L,
                nowElapsedMs = 1_000L,
                timeoutMs = 1_000L,
            ),
        )
    }

    @Test
    fun turningTheSwitchOffClearsACoverThatIsAlreadyUp() {
        val idle = ScreenIdle(timeoutMs = 1_000L)
        idle.resetClock(0L)
        idle.tick(nowElapsedMs = 1_000L, enabled = true, hostingMedia = false)
        assertTrue(idle.coverVisible)
        idle.tick(nowElapsedMs = 5_000L, enabled = false, hostingMedia = false)
        assertFalse(idle.coverVisible)
    }

    @Test
    fun aShowKeepsTheScreenOnAndClosingItStartsAFreshTimeout() {
        val idle = ScreenIdle(timeoutMs = 1_000L)
        idle.resetClock(0L)
        idle.tick(nowElapsedMs = 60_000L, enabled = true, hostingMedia = true)
        assertFalse(idle.coverVisible)
        assertEquals(60_000L, idle.lastActivityElapsedMs)
        idle.tick(nowElapsedMs = 60_999L, enabled = true, hostingMedia = false)
        assertFalse(idle.coverVisible)
        idle.tick(nowElapsedMs = 61_999L, enabled = true, hostingMedia = false)
        assertTrue(idle.coverVisible)
    }

    @Test
    fun aNewStrokeWakesTheScreenAndARepeatDoesNotRefreshTheClock() {
        assertTrue(IdleTimeout.strokeCountsAsUse(previousStrokeCount = 0, strokeCount = 1))
        assertFalse(IdleTimeout.strokeCountsAsUse(previousStrokeCount = 5, strokeCount = 5))
        assertFalse(IdleTimeout.strokeCountsAsUse(previousStrokeCount = 5, strokeCount = 0))

        val idle = ScreenIdle(timeoutMs = 1_000L)
        idle.resetClock(0L)
        idle.tick(nowElapsedMs = 1_000L, enabled = true, hostingMedia = false)
        assertTrue(idle.coverVisible)

        assertTrue(idle.onTelemetry(strokeCount = 1, nowElapsedMs = 1_500L))
        assertFalse(idle.coverVisible)
        idle.tick(nowElapsedMs = 1_500L, enabled = true, hostingMedia = false)
        assertFalse(idle.coverVisible)

        assertFalse(idle.onTelemetry(strokeCount = 1, nowElapsedMs = 1_800L))
        assertEquals(1_500L, idle.lastActivityElapsedMs)
        idle.tick(nowElapsedMs = 2_500L, enabled = true, hostingMedia = false)
        assertTrue(idle.coverVisible)
    }

    @Test
    fun strokesAlreadyOnTheClockCountAsUseOnTheFirstSample() {
        val idle = ScreenIdle(timeoutMs = 5_000L)
        idle.resetClock(0L)
        assertTrue(idle.onTelemetry(strokeCount = 40, nowElapsedMs = 10_000L))
        idle.tick(nowElapsedMs = 10_000L, enabled = true, hostingMedia = false)
        assertFalse(idle.coverVisible)
        assertEquals(10_000L, idle.lastActivityElapsedMs)
    }

    @Test
    fun tapToWakeLeavesTheSessionAsItWas() {
        val idle = ScreenIdle(timeoutMs = 1_000L)
        idle.resetClock(0L)
        idle.tick(nowElapsedMs = 1_000L, enabled = true, hostingMedia = false)
        assertTrue(idle.coverVisible)

        val session = Session(SessionPhase.Running, distanceMeters = 1_840f, strokeCount = 62)
        assertSame(session, IdleTimeout.wake(session))
        val returned = idle.onTap(nowElapsedMs = 4_000L, session = session)
        assertSame(session, returned)
        assertEquals(SessionPhase.Running, returned.phase)
        assertEquals(1_840f, returned.distanceMeters)
        assertEquals(62, returned.strokeCount)
        assertFalse(idle.coverVisible)
        assertEquals(4_000L, idle.lastActivityElapsedMs)

        assertSame(SessionPhase.Paused, idle.onTap(5_000L, SessionPhase.Paused))
        assertSame(SessionPhase.Idle, IdleTimeout.wake(SessionPhase.Idle))
    }

    @Test
    fun darkeningDoesNotNeedAPrivilegedPermission() {
        val reason = IdleTimeout.WHY_NOT_A_PRIVILEGED_PERMISSION
        assertTrue(reason.contains("INSTALL_PACKAGES"))
        assertTrue(reason.contains("DEVICE_POWER"))
        assertTrue(reason.contains("WRITE_SECURE_SETTINGS"))
        assertTrue(reason.contains("FLAG_KEEP_SCREEN_ON"))
        assertTrue(reason.contains("ACTION_USER_PRESENT"))
        assertTrue(reason.contains("foenix_h"))
        assertTrue(reason.contains("brightness to 0"))
    }

    private data class Session(
        val phase: SessionPhase,
        val distanceMeters: Float,
        val strokeCount: Int,
    )
}
