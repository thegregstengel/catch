package app.catchrower.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    private val sha = "a".repeat(64)

    private fun release(tag: String, prerelease: Boolean = false, draft: Boolean = false, digest: Boolean = true) = """
        {"tag_name": "$tag", "prerelease": $prerelease, "draft": $draft, "body": "Notes for $tag",
         "assets": [{"name": "catch-${tag.removePrefix("v")}.apk", "size": 1234,
                     "browser_download_url": "https://example.com/catch-${tag.removePrefix("v")}.apk"
                     ${if (digest) ""","digest": "sha256:$sha"""" else ""}}]}
    """.trimIndent()

    private fun releases(vararg items: String) = items.joinToString(",", "[", "]")

    @Test
    fun versionsOrderBetasBeforeReleasesAndDevAfter() {
        assertTrue(Version.isNewer("2026.10.29", "2026.10.29-beta.2"))
        assertTrue(Version.isNewer("2026.10.29-beta.2", "2026.10.29-beta.1"))
        assertTrue(Version.isNewer("2026.10.29.2-dev", "2026.10.29"))
        assertTrue(Version.isNewer("2026.10.29.2-beta.1", "2026.10.29.2-dev"))
        assertTrue(Version.isNewer("2026.10.29.2", "2026.10.29.2-beta.1"))
        assertTrue(Version.isNewer("2026.11.1", "2026.10.31.9"))
        assertTrue(Version.isNewer("2027.1.1", "2026.12.31"))
        assertFalse(Version.isNewer("2026.10.29", "2026.10.29"))
        assertFalse(Version.isNewer("0.8.0", "2026.10.29"))
    }

    @Test
    fun versionCodesFitAndMatchTheBuildScheme() {
        assertEquals(261029101, Version.code("2026.10.29-beta.1"))
        assertEquals(261029199, Version.code("v2026.10.29"))
        assertEquals(261029200, Version.code("2026.10.29.2-dev"))
        assertTrue(Version.code("2099.12.31.9")!! < Int.MAX_VALUE)
    }

    @Test
    fun stableChannelIgnoresBetas() {
        val json = releases(release("v2026.11.2-beta.1", prerelease = true), release("v2026.10.29"))
        assertEquals("2026.10.29", UpdateChecker.pick(json, UpdateChannel.Stable, "2026.10.1")?.version)
        assertEquals("2026.11.2-beta.1", UpdateChecker.pick(json, UpdateChannel.Beta, "2026.10.1")?.version)
    }

    @Test
    fun skipsDraftsAndOlderReleases() {
        val json = releases(release("v2027.1.1", draft = true), release("v2026.10.29"))
        assertNull(UpdateChecker.pick(json, UpdateChannel.Stable, "2026.10.29"))
        assertNull(UpdateChecker.pick(json, UpdateChannel.Stable, "2026.10.30"))
    }

    @Test
    fun readsApkAndChecksum() {
        val picked = UpdateChecker.pick(releases(release("v2026.10.29")), UpdateChannel.Stable, "2026.10.1")!!
        assertEquals("https://example.com/catch-2026.10.29.apk", picked.apkUrl)
        assertEquals(sha, picked.sha256)
        assertEquals(1234L, picked.apkSizeBytes)
    }

    @Test
    fun parsesSha256SumsFile() {
        val sums = "$sha  catch-2026.10.29.apk\n${"b".repeat(64)} *catch-magisk-2026.10.29.zip\n"
        assertEquals(sha, UpdateChecker.checksumFrom(sums, "catch-2026.10.29.apk"))
        assertEquals("b".repeat(64), UpdateChecker.checksumFrom(sums, "catch-magisk-2026.10.29.zip"))
        assertNull(UpdateChecker.checksumFrom(sums, "catch-2026.10.30.apk"))
    }
}
