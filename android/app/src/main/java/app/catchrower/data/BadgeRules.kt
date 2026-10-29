package app.catchrower.data

/**
 * Distance milestone badge catalog. Titles are generic (no household names).
 */
object BadgeRules {
    data class BadgeDef(
        val id: String,
        val title: String,
        val metersRequired: Float,
        val lockedHint: String = "Locked",
    )

    val DISTANCE_MILESTONES = listOf(
        BadgeDef("dist_5k", "5K Row", 5_000f),
        BadgeDef("dist_10k", "10K Row", 10_000f),
        BadgeDef("dist_25k", "25K Row", 25_000f),
        BadgeDef("dist_50k", "50K Row", 50_000f),
        BadgeDef("dist_100k", "100K Row", 100_000f),
        BadgeDef("family_row", "Family Row", 0f), // awarded when weekly family goal met once
        BadgeDef("streak_3", "3-Day Streak", 0f),
        BadgeDef("streak_7", "7-Day Streak", 0f),
        BadgeDef("new_location", "New Location", 0f), // scenic session complete
    )

    fun earnedFromLifetimeMeters(lifetimeMeters: Float): List<BadgeDef> =
        DISTANCE_MILESTONES.filter { it.metersRequired > 0f && lifetimeMeters >= it.metersRequired }

    fun streakBadges(consecutiveDays: Int): List<BadgeDef> = buildList {
        if (consecutiveDays >= 3) add(DISTANCE_MILESTONES.first { it.id == "streak_3" })
        if (consecutiveDays >= 7) add(DISTANCE_MILESTONES.first { it.id == "streak_7" })
    }

    fun titleFor(badgeId: String): String {
        DISTANCE_MILESTONES.firstOrNull { it.id == badgeId }?.title?.let { return it }
        ChallengeCatalog.ALL.asSequence()
            .flatMap { it.tiers.asSequence() }
            .firstOrNull { it.badgeId == badgeId }
            ?.title
            ?.let { return it }
        return badgeId
    }
}
