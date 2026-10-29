package app.catchrower.ui

import java.util.Locale

object Formatters {
    fun elapsed(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return String.format(Locale.US, "%d:%02d", m, s)
    }

    fun split(secPer500: Float?): String {
        if (secPer500 == null || secPer500 <= 0f || !secPer500.isFinite()) return "--:--"
        val total = secPer500.toInt().coerceAtLeast(0)
        val m = total / 60
        val s = total % 60
        return String.format(Locale.US, "%d:%02d", m, s)
    }

    fun meters(value: Float): String =
        String.format(Locale.US, "%,.0f", value)

    fun spm(value: Float?): String =
        if (value == null) "--" else String.format(Locale.US, "%.0f", value)

    fun watts(value: Float?): String =
        if (value == null) "--" else String.format(Locale.US, "%.0f", value)

    fun kcal(value: Float?): String =
        if (value == null) "--" else String.format(Locale.US, "%.0f", value)

    fun milesFromMeters(meters: Float): String =
        String.format(Locale.US, "%.1f", meters / 1609.344f)

    fun zoneLabel(zone: Int?): String = when (zone) {
        1 -> "1 Easy"
        2 -> "2 Light"
        3 -> "3 Moderate"
        4 -> "4 Hard"
        5 -> "5 Max"
        else -> "--"
    }
}
