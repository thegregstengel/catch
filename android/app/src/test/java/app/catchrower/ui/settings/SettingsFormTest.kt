package app.catchrower.ui.settings

import app.catchrower.ui.Formatters
import app.catchrower.update.UpdateChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsFormTest {
    @Test
    fun goalIsStoredAsMetersNotMiles() {
        val parsed = ok("1609", pinText = "", existingPin = "")
        assertEquals(1609, parsed.monthlyGoalMeters)
        assertFalse(Formatters.containsMilesUnit(parsed.monthlyGoalMeters.toString()))
        assertFalse(Formatters.containsMilesUnit("Enter the monthly family goal in meters, at least 1,000."))
    }

    @Test
    fun blankPinKeepsTheExistingOneAndANewPinReplacesIt() {
        assertEquals("1234", ok("5000", pinText = "", existingPin = "1234").settingsPin)
        assertEquals("9876", ok("5000", pinText = "9876", existingPin = "1234").settingsPin)
    }

    @Test
    fun invalidGoalOrPinIsRejected() {
        assertTrue(SettingsForm.parse("999", emptySet(), UpdateChannel.Stable, "", "", true) is SettingsForm.ParseResult.Invalid)
        assertTrue(SettingsForm.parse("miles", emptySet(), UpdateChannel.Stable, "", "", true) is SettingsForm.ParseResult.Invalid)
        assertTrue(SettingsForm.parse("5000", emptySet(), UpdateChannel.Stable, "12", "", true) is SettingsForm.ParseResult.Invalid)
        assertTrue(SettingsForm.parse("5000", emptySet(), UpdateChannel.Beta, "12ab", "1234", false) is SettingsForm.ParseResult.Invalid)
    }

    @Test
    fun screenOffWhenIdleIsSavedWithTheForm() {
        assertTrue(ok("5000", pinText = "", existingPin = "").screenOffWhenIdle)
        assertFalse(ok("5000", pinText = "", existingPin = "", screenOffWhenIdle = false).screenOffWhenIdle)
    }

    private fun ok(
        goal: String,
        pinText: String,
        existingPin: String,
        screenOffWhenIdle: Boolean = true,
    ): SettingsForm.Parsed {
        val result = SettingsForm.parse(
            monthlyGoalText = goal,
            disabledAppPackages = setOf("com.example"),
            updateChannel = UpdateChannel.Stable,
            pinText = pinText,
            existingPin = existingPin,
            screenOffWhenIdle = screenOffWhenIdle,
        )
        assertTrue(result is SettingsForm.ParseResult.Ok)
        return (result as SettingsForm.ParseResult.Ok).parsed
    }
}
