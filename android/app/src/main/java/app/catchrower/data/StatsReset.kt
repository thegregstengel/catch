package app.catchrower.data

/**
 * Profile and household stat resets. Nothing is erased unless [decide] is given
 * a confirmation. The warning text is what the dialog shows.
 */
object StatsReset {
    enum class Target { Profile, Household }

    data class Warning(val title: String, val body: String)

    sealed class Decision {
        data object Refused : Decision()
        data class EraseProfile(val profileId: Long) : Decision()
        data object EraseHousehold : Decision()
    }

    fun warning(target: Target, profileName: String): Warning = when (target) {
        Target.Profile -> Warning(
            title = "Reset $profileName's stats?",
            body = "This erases $profileName's saved sessions, distance, calendar days, awards, " +
                "and challenge progress on this tablet. The profile itself stays. This cannot be undone.",
        )
        Target.Household -> Warning(
            title = "Reset household metrics?",
            body = "This erases every rower's saved sessions, distance, calendar days, awards, " +
                "and challenge progress on this tablet. Profiles, the settings PIN, and the family goal stay. " +
                "This cannot be undone.",
        )
    }

    fun decide(target: Target, profileId: Long?, confirmed: Boolean): Decision {
        if (!confirmed) return Decision.Refused
        return when (target) {
            Target.Profile -> {
                val id = profileId ?: return Decision.Refused
                Decision.EraseProfile(id)
            }
            Target.Household -> Decision.EraseHousehold
        }
    }
}
