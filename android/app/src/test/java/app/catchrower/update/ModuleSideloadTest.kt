package app.catchrower.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ModuleSideloadTest {
    private val oldAllowlist = """
        <permissions>
            <privapp-permissions package="app.catchrower.catch">
                <permission name="android.permission.INSTALL_PACKAGES" />
            </privapp-permissions>
        </permissions>
    """.trimIndent()

    private val newAllowlist = """
        <permissions>
            <privapp-permissions package="app.catchrower.catch">
                <permission name="android.permission.INSTALL_PACKAGES" />
                <permission name="android.permission.DEVICE_POWER" />
            </privapp-permissions>
        </permissions>
    """.trimIndent()

    @Test
    fun messageStaysUntilTheModuleAndTheAppAreInstalled() {
        val thisApp = setOf("android.permission.INSTALL_PACKAGES")
        val appThatNeedsTheModule = thisApp + ModuleSideload.PERMISSION
        assertTrue(ModuleSideload.holdsBackInAppInstall(ModuleSideload.permissionsNamedIn(oldAllowlist), thisApp))
        assertTrue(ModuleSideload.holdsBackInAppInstall(ModuleSideload.permissionsNamedIn(newAllowlist), thisApp))
        assertTrue(ModuleSideload.holdsBackInAppInstall(emptySet(), appThatNeedsTheModule))
        assertFalse(
            ModuleSideload.holdsBackInAppInstall(
                ModuleSideload.permissionsNamedIn(newAllowlist),
                appThatNeedsTheModule,
            ),
        )
    }

    @Test
    fun releaseNotesThatNeedAModuleBlockInstall() {
        assertTrue(ModuleSideload.notesRequireModule("**Module update required.** Install the Magisk zip."))
        assertTrue(ModuleSideload.notesRequireModule("This build requests a new privileged permission."))
        assertFalse(ModuleSideload.notesRequireModule("Catch Settings checks for updates and does not install them."))
    }

    @Test
    fun theMessagePointsAtTheModuleRelease() {
        assertEquals(
            "The next Catch update changes the Magisk module. Installing an APK from this screen " +
                "can stop the tablet from booting. Download the Magisk zip from the release page, " +
                "install it, reboot, then install the APK.",
            ModuleSideload.MESSAGE,
        )
        assertEquals(
            "https://github.com/thegregstengel/catch/releases/tag/v2026.11.03",
            ModuleSideload.RELEASE_PAGE,
        )
    }

    @Test
    fun releaseWorkflowRejectsVersionsTheOldUpdaterWouldPrefer() {
        val script = File(repoRoot(), "scripts/reject-newer-than-safety.sh")
        assertTrue(script.canExecute() || script.setExecutable(true))
        assertEquals(0, runScript(script, "2026.11.03.2"))
        assertEquals(0, runScript(script, "v2026.11.03.2"))
        assertEquals(0, runScript(script, "2026.11.03"))
        assertEquals(0, runScript(script, "2026.11.02"))
        assertEquals(0, runScript(script, "2026.11.03-beta.1"))
        assertEquals(0, runScript(script, "not-a-version"))
        assertEquals(1, runScript(script, "2026.11.04"))
        assertEquals(1, runScript(script, "2026.11.04-beta.1"))
        assertEquals(1, runScript(script, "2026.11.03.3"))
        assertEquals(1, runScript(script, "v2027.1.1"))
    }

    private fun runScript(script: File, version: String): Int =
        ProcessBuilder("sh", script.absolutePath, version)
            .redirectErrorStream(true)
            .start()
            .waitFor()

    private fun repoRoot(): File {
        var dir = File(System.getProperty("user.dir") ?: error("user.dir is unset"))
        repeat(6) {
            if (File(dir, "scripts/reject-newer-than-safety.sh").isFile) return dir
            dir = dir.parentFile ?: return dir
        }
        error("Could not find the Catch repo from ${System.getProperty("user.dir")}")
    }
}
