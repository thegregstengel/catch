package app.catchrower.ui.idle

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Applies the idle-screen window flags Catch can use without a privileged permission.
 *
 * [BRIGHTNESS_DARKEST] is 0, the lowest [WindowManager.LayoutParams.screenBrightness]
 * an app can set on API 29. The panel maps that to its minimum backlight. On this
 * LCD that can still be a dim glow, so the black cover supplies the pixels.
 * [BRIGHTNESS_SYSTEM] is -1, which on API 29 means "use the system brightness".
 */
object IdleWindow {
    const val BRIGHTNESS_DARKEST = 0f
    const val BRIGHTNESS_SYSTEM = -1f

    fun apply(window: Window, blanked: Boolean, keepAwake: Boolean) {
        val attrs = window.attributes
        attrs.screenBrightness = if (blanked) BRIGHTNESS_DARKEST else BRIGHTNESS_SYSTEM
        val keepOn = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        attrs.flags = if (keepAwake) {
            attrs.flags or keepOn
        } else {
            attrs.flags and keepOn.inv()
        }
        window.attributes = attrs
    }

    /** Full-screen black dialog window. Its brightness wins while it is focused. */
    fun applyCover(window: Window) {
        window.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        window.setDimAmount(0f)
        window.setWindowAnimations(0)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )
        val attrs = window.attributes
        attrs.screenBrightness = BRIGHTNESS_DARKEST
        attrs.flags = attrs.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        window.attributes = attrs
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
