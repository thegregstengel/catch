package app.catchrower.update

/**
 * Tablets on 2026.11.02 and earlier install the single highest stable version
 * their updater can parse. [VERSION] is that release. A higher parsed version
 * would replace it as the offer and skip the build that stops in-app installs.
 *
 * The release workflow rejects any tag [outranks] reports. v2026.11.03 stays a
 * pre-release, so the stable channel does not offer it either.
 */
object SafetyRelease {
    const val VERSION = "2026.11.03.2"

    /** True when the updater shipped through 2026.11.02 would prefer [version] over [VERSION]. */
    fun outranks(version: String): Boolean {
        val candidate = Version.code(version) ?: return false
        val safety = Version.code(VERSION) ?: return false
        return candidate > safety
    }
}
