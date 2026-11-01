package app.catchrower.data

import android.content.Context

/** Remembers that a profile already closed this year's January summary. */
class YearReviewPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("year_review", Context.MODE_PRIVATE)

    fun isDismissed(profileId: Long, year: Int): Boolean =
        prefs.getStringSet(KEY, emptySet())?.contains(key(profileId, year)) == true

    fun dismiss(profileId: Long, year: Int) {
        val next = prefs.getStringSet(KEY, emptySet()).orEmpty().toMutableSet()
        next.add(key(profileId, year))
        prefs.edit().putStringSet(KEY, next).apply()
    }

    private fun key(profileId: Long, year: Int) = "$profileId:$year"

    private companion object {
        const val KEY = "dismissed"
    }
}
