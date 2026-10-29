package app.catchrower.data

import android.content.Context
import app.catchrower.data.db.AppSettingsEntity
import app.catchrower.data.db.BadgeAwardEntity
import app.catchrower.data.db.CatchDatabase
import app.catchrower.data.db.ChallengeParticipationEntity
import app.catchrower.data.db.ChallengeWeekWinnerEntity
import app.catchrower.data.db.ProfileBtDeviceEntity
import app.catchrower.data.db.ProfileEntity
import app.catchrower.data.db.WorkoutEntity
import app.catchrower.util.WeekRollover
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class WeeklyBoardEntry(
    val profile: ProfileEntity,
    val meters: Float,
    val rank: Int,
)

data class FamilyBoardSnapshot(
    val entries: List<WeeklyBoardEntry>,
    val familyTotalMeters: Float,
    val weeklyGoalMeters: Int,
    val daysLeft: Int,
    val weekStartEpochMs: Long,
    val lastWeekWinnerName: String? = null,
)

data class ChallengeCard(
    val def: ChallengeCatalog.Def,
    val progressValue: Float,
    val targetValue: Float,
    val progressFraction: Float,
    /** Highest tier index earned (-1 if none). */
    val highestTierIndex: Int,
    val tierLabel: String?,
    val completed: Boolean,
)

class CatchRepository(context: Context) {
    private val db = CatchDatabase.get(context)
    private val profiles = db.profileDao()
    private val workouts = db.workoutDao()
    private val settings = db.settingsDao()
    private val btDevices = db.profileBtDeviceDao()
    private val badges = db.badgeAwardDao()
    private val challenges = db.challengeParticipationDao()
    private val weekWinners = db.challengeWeekWinnerDao()

    fun observeProfiles(): Flow<List<ProfileEntity>> = profiles.observeAll()
    fun observeSettings(): Flow<AppSettingsEntity?> = settings.observe()

    fun observeActiveProfile(): Flow<ProfileEntity?> =
        combine(settings.observe(), profiles.observeAll()) { s, list ->
            val id = s?.activeProfileId ?: return@combine null
            list.firstOrNull { it.id == id }
        }

    fun observeBtDevices(profileId: Long): Flow<List<ProfileBtDeviceEntity>> =
        btDevices.observeForProfile(profileId)

    fun observeBadges(profileId: Long): Flow<List<BadgeAwardEntity>> =
        badges.observeForProfile(profileId)

    fun observeChallengeParticipations(profileId: Long): Flow<List<ChallengeParticipationEntity>> =
        challenges.observeForProfile(profileId)

    fun observeLastWeekWinner(): Flow<ChallengeWeekWinnerEntity?> = weekWinners.observeLatest()

    fun observeFamilyBoard(): Flow<FamilyBoardSnapshot> =
        combine(
            profiles.observeAll(),
            settings.observe(),
            workouts.observeAll(),
            weekWinners.observeLatest(),
        ) { profileList, s, allWorkouts, lastWinner ->
            val zone = runCatching { ZoneId.of(s?.timezoneId ?: "America/New_York") }
                .getOrDefault(ZoneId.systemDefault())
            val weekStart = WeekRollover.weekStartEpochMs(zoneId = zone)
            val goal = s?.weeklyFamilyGoalMeters ?: 500_000
            val daysLeft = WeekRollover.daysLeftInWeek(zoneId = zone)
            val withMeters = profileList.map { p ->
                val meters = allWorkouts
                    .filter { it.profileId == p.id && it.startedAtEpochMs >= weekStart }
                    .sumOf { it.distanceMeters.toDouble() }
                    .toFloat()
                p to meters
            }.sortedByDescending { it.second }
            val entries = withMeters.mapIndexed { index, (p, m) ->
                WeeklyBoardEntry(p, m, index + 1)
            }
            val total = withMeters.sumOf { it.second.toDouble() }.toFloat()
            val winnerName = lastWinner
                ?.takeIf { it.weekStartEpochMs < weekStart }
                ?.profileName
            FamilyBoardSnapshot(entries, total, goal, daysLeft, weekStart, winnerName)
        }

    suspend fun ensureSettings(): AppSettingsEntity {
        val existing = settings.get()
        if (existing != null) return existing
        val fresh = AppSettingsEntity()
        settings.upsert(fresh)
        return fresh
    }

    suspend fun getSettings(): AppSettingsEntity = ensureSettings()

    suspend fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        val current = ensureSettings()
        settings.upsert(transform(current))
    }

    suspend fun addProfile(
        name: String,
        photoUri: String? = null,
        weightKg: Float? = null,
        ageYears: Int? = null,
        sex: String? = null,
    ): Long {
        val id = profiles.insert(
            ProfileEntity(
                displayName = name.trim(),
                photoUri = photoUri,
                weightKg = weightKg,
                ageYears = ageYears,
                sex = sex,
            ),
        )
        val s = ensureSettings()
        if (s.activeProfileId == null) {
            settings.upsert(s.copy(activeProfileId = id, onboardingComplete = true))
        }
        return id
    }

    suspend fun updateProfile(profile: ProfileEntity) = profiles.update(profile)

    suspend fun deleteProfile(id: Long) {
        profiles.deleteById(id)
        val s = ensureSettings()
        if (s.activeProfileId == id) {
            val next = profiles.getAll().firstOrNull()?.id
            settings.upsert(s.copy(activeProfileId = next))
        }
    }

    suspend fun setActiveProfile(id: Long) {
        val s = ensureSettings()
        settings.upsert(s.copy(activeProfileId = id, onboardingComplete = true))
    }

    suspend fun clearActiveProfile() {
        val s = ensureSettings()
        settings.upsert(s.copy(activeProfileId = null))
    }

    suspend fun rememberBtDevice(
        profileId: Long,
        mac: String,
        name: String,
        kind: String = "other",
    ) {
        btDevices.upsert(
            ProfileBtDeviceEntity(
                profileId = profileId,
                macAddress = mac.uppercase(),
                displayName = name,
                kind = kind,
            ),
        )
    }

    suspend fun preferredHrMac(profileId: Long): String? =
        btDevices.getForProfile(profileId).firstOrNull { it.kind == "hr" }?.macAddress

    suspend fun startWorkout(profileId: Long): Long =
        workouts.insert(
            WorkoutEntity(
                profileId = profileId,
                startedAtEpochMs = System.currentTimeMillis(),
            ),
        )

    suspend fun updateLiveWorkout(
        workoutId: Long,
        distanceMeters: Float,
        durationSec: Int,
        avgSplitSecPer500: Float?,
        avgSpm: Float?,
        avgWatts: Float?,
        calories: Float?,
        strokeCount: Int,
        maxHeartRate: Int? = null,
        avgHeartRate: Int? = null,
    ) {
        val existing = workouts.getById(workoutId) ?: return
        workouts.update(
            existing.copy(
                distanceMeters = distanceMeters,
                durationSec = durationSec,
                avgSplitSecPer500 = avgSplitSecPer500,
                avgSpm = avgSpm,
                avgWatts = avgWatts,
                calories = calories,
                strokeCount = strokeCount,
                maxHeartRate = maxHeartRate,
                avgHeartRate = avgHeartRate,
            ),
        )
    }

    suspend fun endWorkout(workoutId: Long): WorkoutEntity? {
        val existing = workouts.getById(workoutId) ?: return null
        val ended = existing.copy(endedAtEpochMs = System.currentTimeMillis())
        workouts.update(ended)
        evaluateBadges(ended.profileId)
        refreshChallengeProgress(ended.profileId)
        return ended
    }

    /** What a finished session newly earned, for the end-of-session summary. */
    data class FinishOutcome(
        val badgeTitles: List<String>,
        val challengeTiers: List<String>,
        val familyGoalReached: Boolean,
    )

    suspend fun finishWorkout(workoutId: Long): FinishOutcome {
        val none = FinishOutcome(emptyList(), emptyList(), familyGoalReached = false)
        val profileId = workouts.getById(workoutId)?.profileId ?: return none
        val badgesBefore = badges.getForProfile(profileId).map { it.badgeId }.toSet()
        val ended = endWorkout(workoutId) ?: return none
        val earned = badges.getForProfile(profileId)
            .map { it.badgeId }
            .filterNot { it in badgesBefore }
            .distinct()

        val tierTitles = mutableListOf<String>()
        val badgeTitles = mutableListOf<String>()
        for (id in earned) {
            val def = ChallengeCatalog.ALL.firstOrNull { d -> d.tiers.any { it.badgeId == id } }
            val tier = def?.tiers?.first { it.badgeId == id }
            if (def != null && tier != null) {
                tierTitles += "${def.title}: ${tier.title}"
            } else {
                badgeTitles += BadgeRules.titleFor(id)
            }
        }

        val s = ensureSettings()
        val zone = runCatching { ZoneId.of(s.timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val familyAfter = workouts.familyMetersSince(WeekRollover.weekStartEpochMs(zoneId = zone))
        val familyBefore = familyAfter - ended.distanceMeters
        val goal = s.weeklyFamilyGoalMeters
        return FinishOutcome(
            badgeTitles = badgeTitles,
            challengeTiers = tierTitles,
            familyGoalReached = ended.distanceMeters > 0f && familyBefore < goal && familyAfter >= goal,
        )
    }

    suspend fun evaluateBadges(profileId: Long) {
        val lifetime = workouts.lifetimeMeters(profileId)
        for (def in BadgeRules.earnedFromLifetimeMeters(lifetime)) {
            badges.insertIgnore(
                BadgeAwardEntity(profileId = profileId, badgeId = def.id),
            )
        }
        val s = ensureSettings()
        val zone = runCatching { ZoneId.of(s.timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val weekStart = WeekRollover.weekStartEpochMs(zoneId = zone)
        val familyTotal = workouts.familyMetersSince(weekStart)
        if (familyTotal >= s.weeklyFamilyGoalMeters) {
            badges.insertIgnore(
                BadgeAwardEntity(profileId = profileId, badgeId = "family_row"),
            )
        }
        val streak = consecutiveWorkoutDays(profileId, zone)
        for (def in BadgeRules.streakBadges(streak)) {
            badges.insertIgnore(
                BadgeAwardEntity(profileId = profileId, badgeId = def.id),
            )
        }
        val scenicGoal = s.scenicGoalMeters.toFloat()
        val finishedScenic = workouts.getForProfile(profileId)
            .any { it.distanceMeters >= scenicGoal }
        if (finishedScenic) {
            badges.insertIgnore(
                BadgeAwardEntity(profileId = profileId, badgeId = "new_location"),
            )
        }
    }

    /**
     * Always-on progress cards. Catch does not require joining; windows and
     * lifetime totals are derived from workouts automatically.
     */
    suspend fun challengeCardsFor(profileId: Long): List<ChallengeCard> {
        return ChallengeCatalog.ALL.map { def ->
            val (progress, target) = computeProgress(def, profileId)
            val fraction = if (target > 0f) (progress / target).coerceIn(0f, 1f) else 0f
            var highest = -1
            for ((index, tier) in def.tiers.withIndex()) {
                if (tierHit(def, progress, target, tier)) highest = index
            }
            val completed = highest >= def.tiers.lastIndex && def.tiers.isNotEmpty()
            ChallengeCard(
                def = def,
                progressValue = progress,
                targetValue = target,
                progressFraction = fraction,
                highestTierIndex = highest,
                tierLabel = def.tiers.getOrNull(highest)?.title,
                completed = completed,
            )
        }
    }

    /** Award tier badges for every catalog item from current workout history. */
    suspend fun refreshChallengeProgress(profileId: Long) {
        for (def in ChallengeCatalog.ALL) {
            val (progress, target) = computeProgress(def, profileId)
            for (tier in def.tiers) {
                if (!tierHit(def, progress, target, tier)) continue
                badges.insertIgnore(
                    BadgeAwardEntity(profileId = profileId, badgeId = tier.badgeId),
                )
                if (def.scope == ChallengeCatalog.Scope.Household) {
                    // Household finish: badge the rower who just logged meters too.
                    // Other profiles pick it up next time they refresh.
                }
            }
        }
    }

    private fun tierHit(
        def: ChallengeCatalog.Def,
        progress: Float,
        target: Float,
        tier: ChallengeCatalog.Tier,
    ): Boolean {
        return when (def.metric) {
            ChallengeCatalog.Metric.FamilyMetersWeek ->
                progress >= target && tier.threshold >= 1f
            else -> progress >= tier.threshold
        }
    }

    private suspend fun computeProgress(
        def: ChallengeCatalog.Def,
        profileId: Long,
    ): Pair<Float, Float> {
        val s = ensureSettings()
        val zone = runCatching { ZoneId.of(s.timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val windowStart = windowFor(def, zone, s).first
        return when (def.metric) {
            ChallengeCatalog.Metric.FamilyMetersWeek -> {
                val meters = workouts.familyMetersSince(windowStart)
                meters to s.weeklyFamilyGoalMeters.toFloat()
            }
            ChallengeCatalog.Metric.ProfileMetersMonth -> {
                val meters = workouts.metersSince(profileId, windowStart)
                val gold = def.tiers.lastOrNull()?.threshold ?: 100_000f
                meters to gold
            }
            ChallengeCatalog.Metric.WorkoutDaysWeek -> {
                val days = distinctWorkoutDaysSince(profileId, windowStart, zone).toFloat()
                days to 7f
            }
            ChallengeCatalog.Metric.ScenicComplete -> {
                val goal = s.scenicGoalMeters.toFloat()
                val done = workouts.getForProfile(profileId)
                    .any {
                        it.startedAtEpochMs >= windowStart && it.distanceMeters >= goal
                    }
                (if (done) 1f else 0f) to 1f
            }
            ChallengeCatalog.Metric.LifetimeMeters -> {
                val meters = workouts.lifetimeMeters(profileId)
                val target = def.tiers.lastOrNull()?.threshold ?: 10_000f
                meters to target
            }
            ChallengeCatalog.Metric.DistinctWorkoutDays -> {
                val days = distinctWorkoutDaysSince(profileId, 0L, zone).toFloat()
                val target = def.tiers.lastOrNull()?.threshold ?: 7f
                days to target
            }
        }
    }

    private fun windowFor(
        def: ChallengeCatalog.Def,
        zone: ZoneId,
        settings: AppSettingsEntity,
    ): Triple<Long, Long?, Float> {
        val now = System.currentTimeMillis()
        return when (def.metric) {
            ChallengeCatalog.Metric.FamilyMetersWeek,
            ChallengeCatalog.Metric.WorkoutDaysWeek,
            -> {
                val start = WeekRollover.weekStartEpochMs(now, zone)
                val end = WeekRollover.weekEndExclusiveEpochMs(now, zone)
                val target = when (def.metric) {
                    ChallengeCatalog.Metric.FamilyMetersWeek ->
                        settings.weeklyFamilyGoalMeters.toFloat()
                    else -> 7f
                }
                Triple(start, end, target)
            }
            ChallengeCatalog.Metric.ProfileMetersMonth -> {
                val ym = YearMonth.now(zone)
                val start = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                Triple(start, end, def.tiers.last().threshold)
            }
            ChallengeCatalog.Metric.ScenicComplete,
            ChallengeCatalog.Metric.LifetimeMeters,
            ChallengeCatalog.Metric.DistinctWorkoutDays,
            -> Triple(0L, null, def.tiers.last().threshold)
        }
    }

    private suspend fun distinctWorkoutDaysSince(
        profileId: Long,
        sinceEpochMs: Long,
        zone: ZoneId,
    ): Int {
        return workouts.getForProfile(profileId)
            .filter { it.startedAtEpochMs >= sinceEpochMs }
            .map {
                Instant.ofEpochMilli(it.startedAtEpochMs).atZone(zone).toLocalDate()
            }
            .toSet()
            .size
    }

    suspend fun archiveLastWeekWinnerIfNeeded() {
        val s = ensureSettings()
        val zone = runCatching { ZoneId.of(s.timezoneId) }.getOrDefault(ZoneId.systemDefault())
        val currentWeekStart = WeekRollover.weekStartEpochMs(zoneId = zone)
        val previousStart = WeekRollover.weekStartEpochMs(
            nowEpochMs = currentWeekStart - 1,
            zoneId = zone,
        )
        if (weekWinners.get(previousStart) != null) return
        val profileList = profiles.getAll()
        val top = profileList.map { p ->
            p to workouts.metersSince(p.id, previousStart).let { meters ->
                // metersSince is cumulative from previousStart; subtract current week.
                val current = workouts.metersSince(p.id, currentWeekStart)
                (meters - current).coerceAtLeast(0f)
            }
        }.maxByOrNull { it.second } ?: return
        if (top.second <= 0f) return
        weekWinners.upsert(
            ChallengeWeekWinnerEntity(
                weekStartEpochMs = previousStart,
                profileId = top.first.id,
                profileName = top.first.displayName,
                meters = top.second,
            ),
        )
    }

    private suspend fun consecutiveWorkoutDays(profileId: Long, zone: ZoneId): Int {
        val days = workouts.getForProfile(profileId)
            .map {
                java.time.Instant.ofEpochMilli(it.startedAtEpochMs)
                    .atZone(zone)
                    .toLocalDate()
            }
            .distinct()
            .sortedDescending()
        if (days.isEmpty()) return 0
        var streak = 1
        var cursor = days.first()
        for (d in days.drop(1)) {
            if (d == cursor.minusDays(1)) {
                streak++
                cursor = d
            } else {
                break
            }
        }
        return streak
    }

    suspend fun setWeeklyFamilyGoal(meters: Int) {
        updateSettings { it.copy(weeklyFamilyGoalMeters = meters.coerceAtLeast(1_000)) }
    }

    suspend fun setMonthlyFamilyGoal(meters: Int) {
        updateSettings { it.copy(monthlyFamilyGoalMeters = meters.coerceAtLeast(1_000)) }
    }

    suspend fun setDisabledApps(packages: Set<String>) {
        updateSettings { it.copy(disabledAppPackages = packages.joinToString(",")) }
    }

    fun disabledAppSet(settings: AppSettingsEntity): Set<String> =
        settings.disabledAppPackages
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

    suspend fun exportWorkoutsJson(): String {
        val all = workouts.getAll()
        val profileMap = profiles.getAll().associateBy { it.id }
        return buildString {
            append("[\n")
            all.forEachIndexed { i, w ->
                val name = profileMap[w.profileId]?.displayName ?: ""
                append("  {")
                append("\"id\":${w.id},")
                append("\"profileId\":${w.profileId},")
                append("\"profileName\":${jsonStr(name)},")
                append("\"startedAtEpochMs\":${w.startedAtEpochMs},")
                append("\"endedAtEpochMs\":${w.endedAtEpochMs},")
                append("\"distanceMeters\":${w.distanceMeters},")
                append("\"durationSec\":${w.durationSec},")
                append("\"avgSplitSecPer500\":${w.avgSplitSecPer500},")
                append("\"avgSpm\":${w.avgSpm},")
                append("\"avgWatts\":${w.avgWatts},")
                append("\"calories\":${w.calories},")
                append("\"strokeCount\":${w.strokeCount}")
                append("}")
                if (i < all.lastIndex) append(",")
                append("\n")
            }
            append("]\n")
        }
    }

    suspend fun exportWorkoutsCsv(): String {
        val all = workouts.getAll()
        val profileMap = profiles.getAll().associateBy { it.id }
        return buildString {
            appendLine(
                "id,profileId,profileName,startedAtEpochMs,endedAtEpochMs," +
                    "distanceMeters,durationSec,avgSplitSecPer500,avgSpm,avgWatts,calories,strokeCount",
            )
            for (w in all) {
                val name = (profileMap[w.profileId]?.displayName ?: "").replace(",", " ")
                appendLine(
                    listOf(
                        w.id,
                        w.profileId,
                        name,
                        w.startedAtEpochMs,
                        w.endedAtEpochMs ?: "",
                        w.distanceMeters,
                        w.durationSec,
                        w.avgSplitSecPer500 ?: "",
                        w.avgSpm ?: "",
                        w.avgWatts ?: "",
                        w.calories ?: "",
                        w.strokeCount,
                    ).joinToString(","),
                )
            }
        }
    }

    /** Wipes all personal Catch data on device. */
    suspend fun privacyWipe() {
        workouts.deleteAll()
        badges.deleteAll()
        challenges.deleteAll()
        weekWinners.deleteAll()
        btDevices.deleteAll()
        profiles.deleteAll()
        settings.upsert(AppSettingsEntity())
    }

    private fun jsonStr(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}
