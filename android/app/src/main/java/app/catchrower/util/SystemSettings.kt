package app.catchrower.util

import android.content.Context
import android.content.Intent
import android.provider.Settings

object SystemSettings {
    /** Opens the real Android system Settings app. */
    fun open(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    /** Opens the system Wi‑Fi panel for first-run networking. */
    fun openWifi(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    /** Triggers the Android Home-app picker so the user can choose Catch. */
    fun openHomePicker(context: Context) {
        context.startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
        )
    }
}
