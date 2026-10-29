package app.catchrower.data

/**
 * Local catalog of always-on goals and awards. No join step: progress is derived
 * from workouts. Ids are stable badge keys.
 *
 * See docs/challenges-programs.md.
 */
object ChallengeCatalog {
    enum class Kind { Challenge, Program }

    enum class Scope { Profile, Household }

    enum class Metric {
        FamilyMetersWeek,
        ProfileMetersMonth,
        WorkoutDaysWeek,
        ScenicComplete,
        LifetimeMeters,
        DistinctWorkoutDays,
    }

    data class Tier(
        val badgeId: String,
        val title: String,
        val threshold: Float,
    )

    data class Def(
        val id: String,
        val kind: Kind,
        val title: String,
        val blurb: String,
        val scope: Scope,
        val metric: Metric,
        val tiers: List<Tier>,
        /** Suggested length hint for UI; windows are computed at join time. */
        val windowHint: String,
    )

    val ALL: List<Def> = listOf(
        Def(
            id = "week_meters_family",
            kind = Kind.Challenge,
            title = "Family weekly meters",
            blurb = "Hit the household weekly meters goal together.",
            scope = Scope.Household,
            metric = Metric.FamilyMetersWeek,
            tiers = listOf(
                Tier("chal_week_family", "Family week", 1f), // threshold = fraction of goal
            ),
            windowHint = "This week",
        ),
        Def(
            id = "month_meters_personal",
            kind = Kind.Challenge,
            title = "Monthly meters",
            blurb = "Stack meters this calendar month. Bronze, silver, gold.",
            scope = Scope.Profile,
            metric = Metric.ProfileMetersMonth,
            tiers = listOf(
                Tier("chal_month_bronze", "Bronze", 25_000f),
                Tier("chal_month_silver", "Silver", 50_000f),
                Tier("chal_month_gold", "Gold", 100_000f),
            ),
            windowHint = "This month",
        ),
        Def(
            id = "week_days",
            kind = Kind.Challenge,
            title = "Show-up week",
            blurb = "Row on as many days as you can this week.",
            scope = Scope.Profile,
            metric = Metric.WorkoutDaysWeek,
            tiers = listOf(
                Tier("chal_days_3", "3 days", 3f),
                Tier("chal_days_5", "5 days", 5f),
                Tier("chal_days_7", "7 days", 7f),
            ),
            windowHint = "This week",
        ),
        Def(
            id = "scenic_route",
            kind = Kind.Challenge,
            title = "Finish the scenic",
            blurb = "Complete one scenic route distance in a single session.",
            scope = Scope.Profile,
            metric = Metric.ScenicComplete,
            tiers = listOf(
                Tier("chal_scenic", "Scenic finisher", 1f),
            ),
            windowHint = "Until you finish or leave",
        ),
        Def(
            id = "first_10k",
            kind = Kind.Program,
            title = "First 10K",
            blurb = "Reach 10 km lifetime meters on Catch.",
            scope = Scope.Profile,
            metric = Metric.LifetimeMeters,
            tiers = listOf(
                Tier("prog_first_10k", "First 10K", 10_000f),
            ),
            windowHint = "Open-ended",
        ),
        Def(
            id = "habit_7",
            kind = Kind.Program,
            title = "Seven-day habit",
            blurb = "Row on seven distinct days. They do not need to be consecutive.",
            scope = Scope.Profile,
            metric = Metric.DistinctWorkoutDays,
            tiers = listOf(
                Tier("prog_habit_7", "Habit builder", 7f),
            ),
            windowHint = "Open-ended",
        ),
        Def(
            id = "scenic_sailor",
            kind = Kind.Program,
            title = "Scenic sailor",
            blurb = "Complete the scenic route goal once.",
            scope = Scope.Profile,
            metric = Metric.ScenicComplete,
            tiers = listOf(
                Tier("prog_scenic_sailor", "Scenic sailor", 1f),
            ),
            windowHint = "Open-ended",
        ),
    )

    fun byId(id: String): Def? = ALL.firstOrNull { it.id == id }

    fun challenges(): List<Def> = ALL.filter { it.kind == Kind.Challenge }

    fun programs(): List<Def> = ALL.filter { it.kind == Kind.Program }
}
