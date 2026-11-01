package app.catchrower.data

/**
 * One household PIN for Catch Settings and the controls around other rowers.
 * Opening the rower's own profile does not ask for it.
 *
 * The PIN is not shared with Android Settings. [app.catchrower.util.AndroidSettingsPin]
 * records why this tablet cannot do that.
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
     * Until a PIN is saved, household controls stay open so someone can set one.
     * After that, only [Action.OpenOwnProfile] works before the PIN is accepted.
     */
    fun allowed(action: Action, pinConfigured: Boolean, pinAccepted: Boolean): Boolean {
        if (!pinConfigured) return true
        if (action == Action.OpenOwnProfile) return true
        return pinAccepted
    }
}
