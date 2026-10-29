package app.catchrower.update

/**
 * Catch versions are dates. A release is `2026.10.29`, a second release that day is
 * `2026.10.29.2`, a beta is `2026.10.29-beta.1`, and a local build after
 * `2026.10.29` is `2026.10.29.2-dev`.
 *
 * [code] matches the version code `app/build.gradle.kts` derives, so ordering here
 * and on the device agree: 2026.10.29-beta.1 < 2026.10.29 < 2026.10.29.2-dev <
 * 2026.10.29.2-beta.1 < 2026.10.29.2.
 */
object Version {
    private val PATTERN =
        Regex("""^v?(\d{4})\.(\d{1,2})\.(\d{1,2})(?:\.(\d))?(?:-(dev|(?:alpha|beta|rc)\.(\d{1,2})))?$""")

    /** Returns null for names that are not Catch versions. */
    fun code(name: String): Int? {
        val match = PATTERN.matchEntire(name.trim()) ?: return null
        val (year, month, day, sameDay, suffix, pre) = match.destructured
        val preRelease = when {
            suffix == "dev" -> 0
            suffix.isEmpty() -> 99
            else -> pre.toInt().coerceIn(1, 98)
        }
        val date = (year.toInt() - 2000) * 10_000 + month.toInt() * 100 + day.toInt()
        return date * 1_000 + (sameDay.toIntOrNull() ?: 1) * 100 + preRelease
    }

    fun isNewer(candidate: String, current: String): Boolean {
        val a = code(candidate) ?: return false
        val b = code(current) ?: return true
        return a > b
    }
}
