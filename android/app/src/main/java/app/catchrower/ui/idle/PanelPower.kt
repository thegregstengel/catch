package app.catchrower.ui.idle

import android.content.Context
import android.os.PowerManager
import android.os.SystemClock
import android.view.Window
import android.view.WindowManager

/**
 * Turns the Hydrow panel off with [PowerManager.goToSleep].
 *
 * `goToSleep` is a hidden API and needs [PERMISSION]. This safety build does
 * not request it, so the call fails closed and the panel stays up. The Magisk
 * module has to allowlist the permission before a later app requests it, or
 * the next boot stops.
 *
 * [keepAwakeWhileCounting] holds the panel during the idle countdown so
 * Android's own screen timeout does not sleep first. That earlier sleep would
 * arrive as a wake Catch did not start. `goToSleep` still powers the panel
 * down when the countdown ends. This flag does not keep it lit after that.
 */
object PanelPower {
    const val PERMISSION = "android.permission.DEVICE_POWER"

    fun isInteractive(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isInteractive
    }

    fun keepAwakeWhileCounting(window: Window, enabled: Boolean) {
        val flag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        if (enabled) window.addFlags(flag) else window.clearFlags(flag)
    }

    /**
     * Asks the system to power the panel down. Already-asleep counts as success.
     * A missing permission or a hidden-API block returns false and leaves the panel up.
     */
    fun goToSleep(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isInteractive) return true
        return try {
            val method = pm.javaClass.getMethod("goToSleep", java.lang.Long.TYPE)
            method.invoke(pm, SystemClock.uptimeMillis())
            true
        } catch (_: Exception) {
            false
        }
    }
}
