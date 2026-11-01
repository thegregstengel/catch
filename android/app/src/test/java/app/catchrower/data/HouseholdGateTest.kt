package app.catchrower.data

import app.catchrower.util.AndroidSettingsPin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HouseholdGateTest {
    @Test
    fun ownProfileDoesNotNeedThePin() {
        assertTrue(
            HouseholdGate.allowed(
                HouseholdGate.Action.OpenOwnProfile,
                pinConfigured = true,
                pinAccepted = false,
            ),
        )
        assertTrue(HouseholdGate.isOwn(profileId = 3L, ownProfileId = 3L))
        assertFalse(HouseholdGate.isOwn(profileId = 4L, ownProfileId = 3L))
        assertTrue(HouseholdGate.isOwn(profileId = 4L, ownProfileId = null))
    }

    @Test
    fun householdControlsNeedThePinOnceItIsSet() {
        val gated = listOf(
            HouseholdGate.Action.OpenOtherProfile,
            HouseholdGate.Action.AddProfile,
            HouseholdGate.Action.OpenCatchSettings,
            HouseholdGate.Action.OpenSystemSettings,
            HouseholdGate.Action.ResetHouseholdMetrics,
            HouseholdGate.Action.EditFamilyGoal,
        )
        gated.forEach { action ->
            assertFalse(HouseholdGate.allowed(action, pinConfigured = true, pinAccepted = false))
            assertTrue(HouseholdGate.allowed(action, pinConfigured = true, pinAccepted = true))
        }
    }

    @Test
    fun unsetPinLeavesControlsOpenSoItCanBeCreated() {
        HouseholdGate.Action.entries.forEach { action ->
            assertTrue(HouseholdGate.allowed(action, pinConfigured = false, pinAccepted = false))
        }
    }

    @Test
    fun pinIsFourDigits() {
        assertTrue(HouseholdGate.isWellFormed("1234"))
        assertFalse(HouseholdGate.isWellFormed(""))
        assertFalse(HouseholdGate.isWellFormed("123"))
        assertFalse(HouseholdGate.isWellFormed("12345"))
        assertFalse(HouseholdGate.isWellFormed("12a4"))
        assertTrue(HouseholdGate.matches("1234", "1234"))
        assertFalse(HouseholdGate.matches("1234", "0000"))
        assertFalse(HouseholdGate.matches("", "1234"))
        assertFalse(HouseholdGate.matches("12", "12"))
    }

    @Test
    fun androidSettingsCannotShareThePin() {
        assertFalse(AndroidSettingsPin.canShareWithAndroidSettings())
        assertTrue(AndroidSettingsPin.REASON.contains("Android 10"))
        assertTrue(AndroidSettingsPin.REASON.contains("device owner"))
        assertTrue(AndroidSettingsPin.REASON.contains("INSTALL_PACKAGES"))
        assertTrue(AndroidSettingsPin.REASON.contains("not protected"))
    }
}
