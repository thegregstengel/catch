package app.catchrower.update

/**
 * Holds back in-app installs until the Magisk module that allowlists
 * [PERMISSION] and an app that requests it are both on the tablet.
 *
 * This safety build does not request [PERMISSION]. The hold therefore stays
 * until that later app is sideloaded. Dismissing the message does not clear it.
 */
object ModuleSideload {
    const val PERMISSION = "android.permission.DEVICE_POWER"

    const val MESSAGE =
        "The next Catch update changes the Magisk module. Installing an APK from this screen " +
            "can stop the tablet from booting. Download the Magisk zip from the release page, " +
            "install it, reboot, then install the APK."

    /** The pre-release that contains the module zip and the APK that requests [PERMISSION]. */
    const val RELEASE_PAGE = "https://github.com/thegregstengel/catch/releases/tag/v2026.11.03"

    fun permissionsNamedIn(allowlistXml: String): Set<String> =
        Regex("""<permission\b[^>]*\bname="([^"]+)"""")
            .findAll(allowlistXml)
            .map { it.groupValues[1] }
            .toSet()

    /** True when Catch must not download or install an APK from Settings. */
    fun holdsBackInAppInstall(allowlisted: Set<String>, requestedByApp: Set<String>): Boolean {
        val moduleReady = PERMISSION in allowlisted
        val appReady = PERMISSION in requestedByApp
        return !(moduleReady && appReady)
    }

    /**
     * Release notes that mean the APK needs a module the tablet may not have.
     * Matches the changelog heading and a note that a privileged permission is new.
     */
    fun notesRequireModule(notes: String): Boolean {
        val text = notes.lowercase()
        return text.contains("module update required") || text.contains("privileged permission")
    }
}
