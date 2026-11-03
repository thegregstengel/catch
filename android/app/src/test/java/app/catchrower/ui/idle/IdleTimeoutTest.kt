package app.catchrower.ui.idle

import app.catchrower.data.db.AppSettingsEntity
import app.catchrower.rower.SessionPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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
        assertFalse(idle.readyToSleep)
        idle.tick(
            nowElapsedMs = IdleTimeout.TIMEOUT_MS,
            enabled = true,
            hostingMedia = false,
        )
        assertTrue(idle.readyToSleep)
    }

    @Test
    fun idleShorterThanTheTimeoutStaysAwake() {
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
    fun turnedOffOrWatchingAShowStaysAwake() {
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
    fun aNonPositiveTimeoutNeverSleeps() {
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
    fun turningTheSwitchOffCancelsAPendingSleep() {
        val idle = ScreenIdle(timeoutMs = 1_000L)
        idle.resetClock(0L)
        idle.tick(nowElapsedMs = 1_000L, enabled = true, hostingMedia = false)
        assertTrue(idle.readyToSleep)
        idle.tick(nowElapsedMs = 5_000L, enabled = false, hostingMedia = false)
        assertFalse(idle.readyToSleep)
    }

    @Test
    fun aShowKeepsTheScreenOnAndClosingItStartsAFreshTimeout() {
        val idle = ScreenIdle(timeoutMs = 1_000L)
        idle.resetClock(0L)
        idle.tick(nowElapsedMs = 60_000L, enabled = true, hostingMedia = true)
        assertFalse(idle.readyToSleep)
        assertEquals(60_000L, idle.lastActivityElapsedMs)
        idle.tick(nowElapsedMs = 60_999L, enabled = true, hostingMedia = false)
        assertFalse(idle.readyToSleep)
        idle.tick(nowElapsedMs = 61_999L, enabled = true, hostingMedia = false)
        assertTrue(idle.readyToSleep)
    }

    @Test
    fun aNewStrokeRestartsTheIdleClock() {
        assertTrue(IdleTimeout.strokeCountsAsUse(previousStrokeCount = 0, strokeCount = 1))
        assertFalse(IdleTimeout.strokeCountsAsUse(previousStrokeCount = 5, strokeCount = 5))
        assertFalse(IdleTimeout.strokeCountsAsUse(previousStrokeCount = 5, strokeCount = 0))

        val idle = ScreenIdle(timeoutMs = 1_000L)
        idle.resetClock(0L)
        idle.tick(nowElapsedMs = 1_000L, enabled = true, hostingMedia = false)
        assertTrue(idle.readyToSleep)

        assertTrue(idle.onTelemetry(strokeCount = 1, nowElapsedMs = 1_500L))
        assertFalse(idle.readyToSleep)
        idle.tick(nowElapsedMs = 1_500L, enabled = true, hostingMedia = false)
        assertFalse(idle.readyToSleep)

        assertFalse(idle.onTelemetry(strokeCount = 1, nowElapsedMs = 1_800L))
        assertEquals(1_500L, idle.lastActivityElapsedMs)
        idle.tick(nowElapsedMs = 2_500L, enabled = true, hostingMedia = false)
        assertTrue(idle.readyToSleep)
    }

    @Test
    fun strokesAlreadyOnTheClockCountAsUseOnTheFirstSample() {
        val idle = ScreenIdle(timeoutMs = 5_000L)
        idle.resetClock(0L)
        assertTrue(idle.onTelemetry(strokeCount = 40, nowElapsedMs = 10_000L))
        idle.tick(nowElapsedMs = 10_000L, enabled = true, hostingMedia = false)
        assertFalse(idle.readyToSleep)
        assertEquals(10_000L, idle.lastActivityElapsedMs)
    }

    @Test
    fun tapToWakeLeavesTheSessionAsItWas() {
        val idle = ScreenIdle(timeoutMs = 1_000L)
        idle.resetClock(0L)
        idle.tick(nowElapsedMs = 1_000L, enabled = true, hostingMedia = false)
        assertTrue(idle.readyToSleep)

        val session = Session(SessionPhase.Running, distanceMeters = 1_840f, strokeCount = 62)
        assertSame(session, IdleTimeout.wake(session))
        val returned = idle.onWake(nowElapsedMs = 4_000L, session = session)
        assertSame(session, returned)
        assertEquals(SessionPhase.Running, returned.phase)
        assertEquals(1_840f, returned.distanceMeters)
        assertEquals(62, returned.strokeCount)
        assertFalse(idle.readyToSleep)
        assertEquals(4_000L, idle.lastActivityElapsedMs)

        assertSame(SessionPhase.Paused, idle.onWake(5_000L, SessionPhase.Paused))
        assertSame(SessionPhase.Idle, IdleTimeout.wake(SessionPhase.Idle))
    }

    @Test
    fun idleSleepWakeKeepsTheSessionAndAnyOtherWakeDoesNot() {
        val stored = HashMap<String, Boolean>()
        val flag = IdleSleepFlag(
            read = { stored["asleep"] == true },
            write = { stored["asleep"] = it },
        )
        assertFalse(flag.consumeKeepSession(1_000L))

        flag.markSleep()
        assertTrue(flag.consumeKeepSession(2_000L))
        assertTrue(flag.consumeKeepSession(4_000L))
        assertFalse(flag.consumeKeepSession(6_000L))

        flag.markSleep()
        flag.clear()
        assertFalse(flag.consumeKeepSession(7_000L))
    }

    @Test
    fun sleepUsesTheAllowlistedPowerPath() {
        val reason = IdleTimeout.HOW_THE_PANEL_SLEEPS
        assertTrue(reason.contains("goToSleep"))
        assertTrue(reason.contains("DEVICE_POWER"))
        assertTrue(reason.contains("INSTALL_PACKAGES"))
        assertTrue(reason.contains("foenix_h"))
        assertTrue(reason.contains("ACTION_USER_PRESENT"))
        assertTrue(reason.contains("touchscreen"))
        assertEquals(PanelPower.PERMISSION, "android.permission.DEVICE_POWER")

        val root = repoRoot()
        val allowlist = File(root, "magisk/system/etc/permissions/privapp-permissions-catch.xml").readText()
        val manifest = File(root, "android/app/src/main/AndroidManifest.xml").readText()
        val hidden = File(root, "magisk/system/etc/sysconfig/catch-hidden-api.xml").readText()
        val service = File(root, "magisk/service.sh").readText()
        assertTrue(allowlist.contains("android.permission.DEVICE_POWER"))
        assertTrue(allowlist.contains("android.permission.INSTALL_PACKAGES"))
        assertFalse(
            "This safety build must not request DEVICE_POWER. Tablets install it with the old module.",
            manifest.contains("""uses-permission android:name="android.permission.DEVICE_POWER""""),
        )
        assertTrue(hidden.contains("hidden-api-whitelisted-app"))
        assertTrue(hidden.contains("app.catchrower.catch"))
        assertTrue(service.contains("power/wakeup"))
        assertTrue(service.contains("double_tap_to_wake"))
    }

    private fun repoRoot(): File {
        val start = System.getProperty("user.dir") ?: error("user.dir is unset")
        var dir = File(start)
        repeat(6) {
            if (File(dir, "magisk/module.prop").isFile && File(dir, "android").isDirectory) return dir
            dir = dir.parentFile ?: return dir
        }
        error("Could not find the Catch repo from $start")
    }

    private data class Session(
        val phase: SessionPhase,
        val distanceMeters: Float,
        val strokeCount: Int,
    )
}
