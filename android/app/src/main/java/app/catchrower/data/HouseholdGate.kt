package app.catchrower.data

/**
 * One household PIN for opening Catch Settings.
 *
 * Switching profiles, opening another rower's profile, and adding a profile do
 * not ask for it. Household reset and the family goal live on the Catch Settings
 * screen, so they stay behind the same PIN. Android Settings is not this PIN.
 * [app.catchrower.util.AndroidSettingsPin] records why this tablet cannot share it.
 */
object HouseholdGate {
    const val PIN_LENGTH = 4

    enum class Action {
        OpenOwnProfile,
        OpenOtherProfile,
        AddProfile,
        OpenCatchSettings,
        OpenSystemSettings,
        ResetHouseholdMetrics,
        EditFamilyGoal,
    }

    fun isWellFormed(pin: String): Boolean =
        pin.length == PIN_LENGTH && pin.all { it.isDigit() }

    fun isConfigured(stored: String): Boolean = isWellFormed(stored)

    /** An empty or malformed stored PIN never matches. */
    fun matches(stored: String, entered: String): Boolean =
        isWellFormed(stored) && stored == entered

    /**
     * A null [ownProfileId] means nobody has rowed yet, so the first choice is
     * that person's own profile.
     */
    fun isOwn(profileId: Long, ownProfileId: Long?): Boolean =
        ownProfileId == null || profileId == ownProfileId

    /**
     * The PIN is asked only to open Catch Settings, and for the household actions
     * that live on that screen. Until a PIN is saved, Catch Settings stays open
     * so someone can set one.
     */
    fun allowed(action: Action, pinConfigured: Boolean, pinAccepted: Boolean): Boolean {
        if (!pinConfigured) return true
        return when (action) {
            Action.OpenCatchSettings,
            Action.ResetHouseholdMetrics,
            Action.EditFamilyGoal,
            -> pinAccepted
            Action.OpenOwnProfile,
            Action.OpenOtherProfile,
            Action.AddProfile,
            Action.OpenSystemSettings,
            -> true
        }
    }
}
