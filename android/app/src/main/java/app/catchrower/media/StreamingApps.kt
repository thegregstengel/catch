package app.catchrower.media

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import app.catchrower.util.AuroraStore

data class StreamingApp(
    val id: String,
    val label: String,
    val packageIds: List<String>,
)

object StreamingApps {
    val DAY_ONE = listOf(
        StreamingApp("netflix", "Netflix", listOf("com.netflix.mediaclient")),
        StreamingApp("plex", "Plex", listOf("com.plexapp.android")),
        StreamingApp(
            "paramount",
            "Paramount+",
            listOf(
                "com.cbs.app",
                "com.cbs.ott",
                "com.paramount.android.pplus",
                "com.paramountplus.androidtv",
            ),
        ),
        StreamingApp("disney", "Disney+", listOf("com.disney.disneyplus")),
    )

    /** Snapshot of which day-one apps are installed (by StreamingApp.id). */
    fun installedIds(context: Context): Set<String> =
        DAY_ONE.mapNotNull { app ->
            if (resolveInstalledPackage(context, app) != null) app.id else null
        }.toSet()

    fun resolveInstalledPackage(context: Context, app: StreamingApp): String? {
        for (pkg in app.packageIds) {
            if (isInstalled(context, pkg)) return pkg
        }
        return null
    }

    fun isInstalled(context: Context, packageId: String): Boolean =
        try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageId, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    /** Launch intent for [app] without starting it (to check that it can be hosted in the pane). */
    fun launchIntent(context: Context, app: StreamingApp): Intent? {
        val pkg = resolveInstalledPackage(context, app) ?: return null
        return context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun launchOrAurora(context: Context, app: StreamingApp): LaunchResult {
        val launch = launchIntent(context, app)
        if (launch != null) {
            context.startActivity(launch)
            return LaunchResult.Launched(
                launch.component?.packageName ?: app.packageIds.first(),
            )
        }
        AuroraStore.openOrInstallHelp(context)
        return LaunchResult.OpenedAurora
    }

    sealed class LaunchResult {
        data class Launched(val packageId: String) : LaunchResult()
        data object OpenedAurora : LaunchResult()
    }
}
