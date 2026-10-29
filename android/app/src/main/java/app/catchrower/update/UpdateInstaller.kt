package app.catchrower.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import app.catchrower.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads a [Release], verifies its checksum, and installs it over Catch.
 *
 * The Magisk module grants Catch INSTALL_PACKAGES, so the install is silent. When
 * Catch runs without the module, Android asks for confirmation instead. Android
 * stops Catch once the new version is in place; as the Home app it comes straight
 * back on the new version.
 */
object UpdateInstaller {
    private val _failure = MutableStateFlow<String?>(null)

    /** Set when Android rejects an install; cleared at the start of the next one. */
    val failure: StateFlow<String?> = _failure.asStateFlow()

    suspend fun download(context: Context, release: Release, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { deleteRecursively(); mkdirs() }
            val file = File(dir, "catch-${release.version}.apk")
            val digest = MessageDigest.getInstance("SHA-256")
            val conn = URL(release.apkUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("User-Agent", "Catch/${BuildConfig.VERSION_NAME}")
            try {
                if (conn.responseCode !in 200..299) throw IOException("Download failed: HTTP ${conn.responseCode}")
                val total = release.apkSizeBytes.takeIf { it > 0 } ?: conn.contentLengthLong
                var read = 0L
                conn.inputStream.use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            digest.update(buffer, 0, n)
                            read += n
                            if (total > 0) onProgress(read.toFloat() / total)
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            if (actual != release.sha256) {
                file.delete()
                throw IOException("The download is damaged (checksum mismatch). Try again.")
            }
            file
        }

    suspend fun install(context: Context, apk: File) = withContext(Dispatchers.IO) {
        _failure.value = null
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply { setAppPackageName(context.packageName) }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("catch.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val callback = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, InstallResultReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or mutable,
            )
            session.commit(callback.intentSender)
        }
    }

    class InstallResultReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                    @Suppress("DEPRECATION")
                    val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                    context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                PackageInstaller.STATUS_SUCCESS -> Unit
                else -> _failure.value =
                    intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "The update could not be installed."
            }
        }
    }
}
