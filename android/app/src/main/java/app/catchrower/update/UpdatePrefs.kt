package app.catchrower.update

import android.content.Context

/** Update settings that live outside the Room database. */
class UpdatePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)

    var channel: UpdateChannel
        get() = if (prefs.getString(KEY_CHANNEL, null) == "beta") UpdateChannel.Beta else UpdateChannel.Stable
        set(value) = prefs.edit().putString(KEY_CHANNEL, value.name.lowercase()).apply()

    var lastCheckEpochMs: Long
        get() = prefs.getLong(KEY_LAST_CHECK, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_CHECK, value).apply()

    /** The version that last ran, so Catch can say "Updated to x" once after an update. */
    var lastRunVersion: String?
        get() = prefs.getString(KEY_LAST_RUN, null)
        set(value) = prefs.edit().putString(KEY_LAST_RUN, value).apply()

    private companion object {
        const val KEY_CHANNEL = "channel"
        const val KEY_LAST_CHECK = "last_check_ms"
        const val KEY_LAST_RUN = "last_run_version"
    }
}
