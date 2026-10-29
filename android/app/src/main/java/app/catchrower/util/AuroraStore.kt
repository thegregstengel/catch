package app.catchrower.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Official path for installing and updating third-party apps (Netflix, Plex, etc.).
 * Aurora Store is a Play Store client that works without a full GMS stack.
 */
object AuroraStore {
    const val PACKAGE_ID = "com.aurora.store"

    /** Prefer the modern Aurora Store package; fall back to legacy id if present. */
    private val CANDIDATE_PACKAGES = listOf(
        "com.aurora.store",
        "com.aurora.store.nightly",
    )

    fun installedPackage(context: Context): String? {
        for (pkg in CANDIDATE_PACKAGES) {
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                return pkg
            } catch (_: PackageManager.NameNotFoundException) {
                // try next
            }
        }
        return null
    }

    fun isInstalled(context: Context): Boolean = installedPackage(context) != null

    fun open(context: Context): Boolean {
        val pkg = installedPackage(context) ?: return false
        val launch = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launch)
        return true
    }

    /** Opens Aurora if installed; otherwise shows a download page for first-time setup. */
    fun openOrInstallHelp(context: Context) {
        if (open(context)) return
        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://auroraoss.com/"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(web)
    }
}
