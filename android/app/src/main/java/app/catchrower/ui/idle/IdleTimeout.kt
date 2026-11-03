package app.catchrower.ui.idle

/**
 * Decides when the Hydrow panel should sleep, and which wake leaves the workout alone.
 *
 * The tablet is Android 10, model foenix_h, MediaTek MT8167. [HOW_THE_PANEL_SLEEPS]
 * is the path this build uses.
 */
object IdleTimeout {
    /**
     * Five minutes. A rest between pieces is often a minute or two, so the
     * screen stays up while someone catches their breath. Five minutes is
     * still short enough that an empty rower does not light the room all evening.
     */
    const val TIMEOUT_MINUTES = 5
    const val TIMEOUT_MS = TIMEOUT_MINUTES * 60 * 1000L

    const val HOW_THE_PANEL_SLEEPS: String =
        "On this Hydrow (Android 10, model foenix_h, MediaTek MT8167), the idle timeout calls " +
            "PowerManager.goToSleep. That needs android.permission.DEVICE_POWER, which is " +
            "signature|privileged. The Magisk module allowlists DEVICE_POWER beside " +
            "INSTALL_PACKAGES, and whitelists Catch for that hidden API, before the app " +
            "requests the permission. The module enables the touchscreen as a kernel wakeup " +
            "source and turns on double_tap_to_wake, so a tap brings the panel back through " +
            "Android's normal power path. ACTION_USER_PRESENT after this idle sleep leaves the " +
            "workout and the open screen as they were. A wake Catch did not start, such as the " +
            "power button, still opens Who is rowing. FLAG_KEEP_SCREEN_ON only runs the 5 minute " +
            "timer; goToSleep is what turns the panel off."

    /** Missing settings count as on, matching the database default. */
    fun isEnabled(saved: Boolean?): Boolean = saved ?: true

    /**
     * @param hostingMedia a streaming app is in the pane. Watching counts as using Catch.
     */
    fun shouldTurnOff(
        enabled: Boolean,
        hostingMedia: Boolean,
        lastActivityElapsedMs: Long,
        nowElapsedMs: Long,
        timeoutMs: Long = TIMEOUT_MS,
    ): Boolean {
        if (!enabled || hostingMedia) return false
        if (timeoutMs <= 0L) return false
        return nowElapsedMs - lastActivityElapsedMs >= timeoutMs
    }

    /** A new stroke means someone is rowing, even when they never touch the glass. */
    fun strokeCountsAsUse(previousStrokeCount: Int, strokeCount: Int): Boolean =
        strokeCount > previousStrokeCount

    /**
     * Wake from idle sleep. [session] is returned as the same value: the phase,
     * the distance, and the stroke count stay, and the profile picker stays closed.
     */
    fun <T> wake(session: T): T = session
}

/**
 * Clock for [IdleTimeout]. The activity owns one and does not write [onWake]'s
 * session back into the rower.
 */
class ScreenIdle(
    private val timeoutMs: Long = IdleTimeout.TIMEOUT_MS,
) {
    var lastActivityElapsedMs: Long = 0L
        private set
    var readyToSleep: Boolean = false
        private set
    private var strokeCount: Int = 0

    fun resetClock(nowElapsedMs: Long) {
        lastActivityElapsedMs = nowElapsedMs
        readyToSleep = false
    }

    fun onUserInput(nowElapsedMs: Long) {
        lastActivityElapsedMs = nowElapsedMs
        readyToSleep = false
    }

    fun <T> onWake(nowElapsedMs: Long, session: T): T {
        onUserInput(nowElapsedMs)
        return IdleTimeout.wake(session)
    }

    /**
     * @return true when this sample is a new stroke and the idle clock restarts.
     */
    fun onTelemetry(strokeCount: Int, nowElapsedMs: Long): Boolean {
        val rowing = IdleTimeout.strokeCountsAsUse(this.strokeCount, strokeCount)
        this.strokeCount = strokeCount
        if (rowing) onUserInput(nowElapsedMs)
        return rowing
    }

    fun tick(nowElapsedMs: Long, enabled: Boolean, hostingMedia: Boolean) {
        if (hostingMedia) {
            lastActivityElapsedMs = nowElapsedMs
            readyToSleep = false
            return
        }
        if (!enabled) {
            readyToSleep = false
            return
        }
        readyToSleep = IdleTimeout.shouldTurnOff(
            enabled = true,
            hostingMedia = false,
            lastActivityElapsedMs = lastActivityElapsedMs,
            nowElapsedMs = nowElapsedMs,
            timeoutMs = timeoutMs,
        )
    }
}

/**
 * Remembers that Catch itself called [android.os.PowerManager.goToSleep].
 * The following wake, including a duplicate [android.content.Intent.ACTION_USER_PRESENT],
 * leaves the workout in place. Any other wake still opens "Who is rowing?".
 */
class IdleSleepFlag(
    private val read: () -> Boolean,
    private val write: (Boolean) -> Unit,
    private val duplicateWindowMs: Long = 3_000L,
) {
    private var ignoreUntilMs: Long = 0L

    fun markSleep() = write(true)

    fun clear() {
        write(false)
        ignoreUntilMs = 0L
    }

    /** True when this wake should leave the workout and the open screen alone. */
    fun consumeKeepSession(nowElapsedMs: Long): Boolean {
        val marked = read()
        val duplicate = nowElapsedMs < ignoreUntilMs
        if (marked || duplicate) {
            if (marked) {
                write(false)
                ignoreUntilMs = nowElapsedMs + duplicateWindowMs
            }
            return true
        }
        return false
    }
}
