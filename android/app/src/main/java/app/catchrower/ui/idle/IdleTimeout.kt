package app.catchrower.ui.idle

/**
 * Turns the Catch screen dark after the tablet sits unused, and wakes it on a tap.
 *
 * The Hydrow this ships on is Android 10, model foenix_h, MediaTek MT8167.
 * [WHY_NOT_A_PRIVILEGED_PERMISSION] is the constraint the implementation follows.
 */
object IdleTimeout {
    /**
     * Five minutes. A rest between pieces is often a minute or two, so the
     * screen stays up while someone catches their breath. Five minutes is
     * still short enough that an empty rower does not light the room all evening.
     */
    const val TIMEOUT_MINUTES = 5
    const val TIMEOUT_MS = TIMEOUT_MINUTES * 60 * 1000L

    const val WHY_NOT_A_PRIVILEGED_PERMISSION: String =
        "Catch darkens this Hydrow (Android 10, model foenix_h, MediaTek MT8167) without a new " +
            "privileged permission. The Magisk allowlist only includes INSTALL_PACKAGES. " +
            "PowerManager.goToSleep and PowerManager.wakeUp need android.permission.DEVICE_POWER, " +
            "which is signature|privileged and is not allowlisted. Requesting it would stop the " +
            "tablet from booting. Writing the system screen timeout needs WRITE_SECURE_SETTINGS, " +
            "which is also privileged and not allowlisted. A later module release can allowlist " +
            "DEVICE_POWER. The uid 1000 display host could then call goToSleep and wakeUp, which " +
            "cuts the backlight. After that real sleep, a tap reaches Catch only when the foenix_h " +
            "touch controller wakes the panel on its own. This build keeps the panel awake with " +
            "FLAG_KEEP_SCREEN_ON, sets the window brightness to 0 (the darkest an app can request " +
            "on API 29), and covers Catch in black. A tap removes the cover and restores system " +
            "brightness. The workout, the open screen, and a hosted show stay as they were. " +
            "ACTION_USER_PRESENT still opens Who is rowing and ends the workout, so the cover " +
            "does not use that path."

    /** Missing settings count as on, matching the database default. */
    fun isEnabled(saved: Boolean?): Boolean = saved ?: true

    /**
     * @param hostingMedia a streaming app is in the pane, which counts as using Catch
     * even when nobody taps.
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
     * Tap to wake. [session] is returned as the same value: waking leaves the
     * phase, the distance, and the stroke count alone, and it does not open
     * the profile picker.
     */
    fun <T> wake(session: T): T = session
}

/**
 * Clock for [IdleTimeout]. The activity owns one of these and never writes the
 * session it passes to [onTap] back into the rower.
 */
class ScreenIdle(
    private val timeoutMs: Long = IdleTimeout.TIMEOUT_MS,
) {
    var lastActivityElapsedMs: Long = 0L
        private set
    var coverVisible: Boolean = false
        private set
    private var strokeCount: Int = 0

    fun resetClock(nowElapsedMs: Long) {
        lastActivityElapsedMs = nowElapsedMs
        coverVisible = false
    }

    fun onUserInput(nowElapsedMs: Long) {
        lastActivityElapsedMs = nowElapsedMs
        coverVisible = false
    }

    fun <T> onTap(nowElapsedMs: Long, session: T): T {
        onUserInput(nowElapsedMs)
        return IdleTimeout.wake(session)
    }

    /**
     * @return true when this sample is a new stroke and the cover should come down.
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
            coverVisible = false
            return
        }
        if (!enabled) {
            coverVisible = false
            return
        }
        coverVisible = IdleTimeout.shouldTurnOff(
            enabled = true,
            hostingMedia = false,
            lastActivityElapsedMs = lastActivityElapsedMs,
            nowElapsedMs = nowElapsedMs,
            timeoutMs = timeoutMs,
        )
    }
}
