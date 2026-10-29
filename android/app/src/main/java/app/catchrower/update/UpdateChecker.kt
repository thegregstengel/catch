package app.catchrower.update

import app.catchrower.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class UpdateChannel { Stable, Beta }

/** A Catch release on GitHub that this tablet can install. */
data class Release(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    /** Lowercase hex SHA-256 of the APK. */
    val sha256: String,
)

/**
 * Reads GitHub Releases. Every release carries `catch-<version>.apk`; its checksum
 * comes from the asset's `digest` field or, failing that, the release's
 * `SHA256SUMS` file. Releases without a verifiable APK are skipped.
 */
object UpdateChecker {
    /** The newest release on [channel] that is newer than [currentVersion], or null. */
    suspend fun latest(
        channel: UpdateChannel,
        currentVersion: String = BuildConfig.VERSION_NAME,
    ): Release? = withContext(Dispatchers.IO) {
        pick(get("${BuildConfig.RELEASES_API}?per_page=20"), channel, currentVersion)
    }

    internal fun pick(releasesJson: String, channel: UpdateChannel, currentVersion: String): Release? {
        val releases = JSONArray(releasesJson)
        val newest = (0 until releases.length())
            .map { releases.getJSONObject(it) }
            .filter { !it.optBoolean("draft") }
            .filter { channel == UpdateChannel.Beta || !it.optBoolean("prerelease") }
            .maxByOrNull { Version.code(it.optString("tag_name")) ?: -1 }
            ?: return null
        val version = newest.optString("tag_name").removePrefix("v")
        if (!Version.isNewer(version, currentVersion)) return null
        return toRelease(newest, version)
    }

    private fun toRelease(json: JSONObject, version: String): Release? {
        val assets = json.optJSONArray("assets") ?: return null
        val byName = (0 until assets.length()).associate {
            val asset = assets.getJSONObject(it)
            asset.optString("name") to asset
        }
        val apkName = "catch-$version.apk"
        val apk = byName[apkName] ?: return null
        val sha256 = apk.optString("digest").removePrefix("sha256:").takeIf { it.length == 64 }
            ?: byName["SHA256SUMS"]?.let { sums -> checksumFrom(get(sums.getString("browser_download_url")), apkName) }
            ?: return null
        return Release(
            version = version,
            notes = json.optString("body").trim(),
            apkUrl = apk.getString("browser_download_url"),
            apkSizeBytes = apk.optLong("size"),
            sha256 = sha256.lowercase(),
        )
    }

    internal fun checksumFrom(sums: String, fileName: String): String? =
        sums.lineSequence()
            .map { it.trim().split(Regex("\\s+\\*?")) }
            .firstOrNull { it.size == 2 && it[1] == fileName }
            ?.first()
            ?.takeIf { it.length == 64 }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("User-Agent", "Catch/${BuildConfig.VERSION_NAME}")
        try {
            if (conn.responseCode !in 200..299) throw IOException("GitHub returned HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
