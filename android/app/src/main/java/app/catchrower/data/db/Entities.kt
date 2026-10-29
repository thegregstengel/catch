package app.catchrower.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val photoUri: String? = null,
    val weightKg: Float? = null,
    val ageYears: Int? = null,
    val sex: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("profileId"), Index("startedAtEpochMs")],
)
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long? = null,
    val distanceMeters: Float = 0f,
    val durationSec: Int = 0,
    val avgSplitSecPer500: Float? = null,
    val avgSpm: Float? = null,
    val avgWatts: Float? = null,
    val calories: Float? = null,
    val strokeCount: Int = 0,
    val maxHeartRate: Int? = null,
    val avgHeartRate: Int? = null,
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val activeProfileId: Long? = null,
    val timezoneId: String = "America/New_York",
    val weeklyFamilyGoalMeters: Int = 500_000,
    val monthlyFamilyGoalMeters: Int = 2_000_000,
    val scenicGoalMeters: Int = 12_875, // ~8.0 mi
    val scenicRouteName: String = "Lake Winnipesaukee, NH",
    val onboardingComplete: Boolean = false,
    /** Comma-separated package ids that should be hidden from the app grid. */
    val disabledAppPackages: String = "",
)

@Entity(
    tableName = "profile_bt_devices",
    primaryKeys = ["profileId", "macAddress"],
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("profileId")],
)
data class ProfileBtDeviceEntity(
    val profileId: Long,
    val macAddress: String,
    val displayName: String,
    val kind: String = "unknown", // hr | audio | other
    val lastConnectedEpochMs: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "badge_awards",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("profileId"), Index(value = ["profileId", "badgeId"], unique = true)],
)
data class BadgeAwardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val badgeId: String,
    val earnedAtEpochMs: Long = System.currentTimeMillis(),
)

/**
 * Opt-in challenge or program participation for a profile (or household-scoped
 * rows keyed by the joining profile with [household] true).
 */
@Entity(
    tableName = "challenge_participations",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("profileId"),
        Index(value = ["profileId", "catalogId", "windowStartEpochMs"], unique = true),
    ],
)
data class ChallengeParticipationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val catalogId: String,
    /** active | paused | completed | abandoned */
    val status: String = "active",
    val windowStartEpochMs: Long,
    val windowEndEpochMs: Long? = null,
    val progressValue: Float = 0f,
    val targetValue: Float = 0f,
    val highestTierIndex: Int = -1,
    val household: Boolean = false,
    val joinedAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val completedAtEpochMs: Long? = null,
)

@Entity(tableName = "challenge_week_winners")
data class ChallengeWeekWinnerEntity(
    @PrimaryKey val weekStartEpochMs: Long,
    val profileId: Long,
    val profileName: String,
    val meters: Float,
)
