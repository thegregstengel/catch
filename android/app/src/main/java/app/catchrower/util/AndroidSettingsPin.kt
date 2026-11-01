package app.catchrower.util

/**
 * The Catch Settings PIN is not applied to Android Settings.
 *
 * This Hydrow build is Android 10 (API 29). On that version,
 * `DevicePolicyManager.resetPassword` and `resetPasswordWithToken` throw
 * SecurityException unless the caller is the device owner or a profile owner.
 * Catch is neither. The build has no device-admin receiver, no device-owner
 * provisioning, no screen lock integration, and no Settings guard.
 *
 * The only privileged permission on the Magisk allowlist is INSTALL_PACKAGES.
 * Setting a lockscreen credential would also need
 * SET_AND_VERIFY_LOCKSCREEN_CREDENTIALS. That permission is not allowlisted,
 * and requesting a privileged permission that is missing from the allowlist
 * stops the tablet from booting. The allowlist has to ship in a module release
 * before an app release can request it. This change does not publish a release.
 *
 * A lockscreen credential would lock the whole tablet, not the Settings app.
 * Catch asks for its own PIN before it opens Android Settings. That does not
 * protect Settings opened any other way.
 */
object AndroidSettingsPin {
    const val REASON: String =
        "Catch cannot share its PIN with Android Settings on this Hydrow tablet. " +
            "The tablet runs Android 10 (API 29). On that version, DevicePolicyManager.resetPassword " +
            "and resetPasswordWithToken throw SecurityException unless the caller is the device owner " +
            "or a profile owner. Catch is neither, and this build has no device-admin receiver, " +
            "device-owner provisioning, screen lock, or Settings guard. The only privileged permission " +
            "allowlisted for Catch is INSTALL_PACKAGES. Setting a lockscreen credential needs " +
            "SET_AND_VERIFY_LOCKSCREEN_CREDENTIALS, which is not allowlisted. Requesting it would stop " +
            "the tablet from booting until a Magisk module release shipped the allowlist first, and this " +
            "change does not publish a release. A lockscreen credential would also lock the whole tablet, " +
            "not the Settings app. Catch asks for its PIN before it opens Android Settings. The Android " +
            "Settings app itself is not protected by that PIN."

    fun canShareWithAndroidSettings(): Boolean = false
}
