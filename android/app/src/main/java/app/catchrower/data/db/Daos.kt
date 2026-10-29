package app.catchrower.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY displayName COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles ORDER BY displayName COLLATE NOCASE ASC")
    suspend fun getAll(): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProfileEntity?

    @Insert
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM profiles")
    suspend fun deleteAll()
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts WHERE profileId = :profileId ORDER BY startedAtEpochMs DESC")
    fun observeForProfile(profileId: Long): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE profileId = :profileId ORDER BY startedAtEpochMs DESC")
    suspend fun getForProfile(profileId: Long): List<WorkoutEntity>

    @Query("SELECT * FROM workouts ORDER BY startedAtEpochMs DESC")
    suspend fun getAll(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts ORDER BY startedAtEpochMs DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WorkoutEntity?

    @Query(
        """
        SELECT COALESCE(SUM(distanceMeters), 0) FROM workouts
        WHERE profileId = :profileId AND startedAtEpochMs >= :sinceEpochMs
        """,
    )
    fun observeMetersSince(profileId: Long, sinceEpochMs: Long): Flow<Float>

    @Query(
        """
        SELECT COALESCE(SUM(distanceMeters), 0) FROM workouts
        WHERE profileId = :profileId AND startedAtEpochMs >= :sinceEpochMs
        """,
    )
    suspend fun metersSince(profileId: Long, sinceEpochMs: Long): Float

    @Query(
        """
        SELECT COALESCE(SUM(distanceMeters), 0) FROM workouts
        WHERE profileId = :profileId
        """,
    )
    suspend fun lifetimeMeters(profileId: Long): Float

    @Query(
        """
        SELECT COALESCE(SUM(distanceMeters), 0) FROM workouts
        WHERE startedAtEpochMs >= :sinceEpochMs
        """,
    )
    suspend fun familyMetersSince(sinceEpochMs: Long): Float

    @Insert
    suspend fun insert(workout: WorkoutEntity): Long

    @Update
    suspend fun update(workout: WorkoutEntity)

    @Query("DELETE FROM workouts")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun get(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettingsEntity)
}

@Dao
interface ProfileBtDeviceDao {
    @Query("SELECT * FROM profile_bt_devices WHERE profileId = :profileId ORDER BY lastConnectedEpochMs DESC")
    fun observeForProfile(profileId: Long): Flow<List<ProfileBtDeviceEntity>>

    @Query("SELECT * FROM profile_bt_devices WHERE profileId = :profileId ORDER BY lastConnectedEpochMs DESC")
    suspend fun getForProfile(profileId: Long): List<ProfileBtDeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: ProfileBtDeviceEntity)

    @Query("DELETE FROM profile_bt_devices WHERE profileId = :profileId AND macAddress = :mac")
    suspend fun delete(profileId: Long, mac: String)

    @Query("DELETE FROM profile_bt_devices")
    suspend fun deleteAll()
}

@Dao
interface BadgeAwardDao {
    @Query("SELECT * FROM badge_awards WHERE profileId = :profileId ORDER BY earnedAtEpochMs DESC")
    fun observeForProfile(profileId: Long): Flow<List<BadgeAwardEntity>>

    @Query("SELECT * FROM badge_awards WHERE profileId = :profileId")
    suspend fun getForProfile(profileId: Long): List<BadgeAwardEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(award: BadgeAwardEntity): Long

    @Query("DELETE FROM badge_awards")
    suspend fun deleteAll()
}

@Dao
interface ChallengeParticipationDao {
    @Query(
        """
        SELECT * FROM challenge_participations
        WHERE profileId = :profileId
        ORDER BY updatedAtEpochMs DESC
        """,
    )
    fun observeForProfile(profileId: Long): Flow<List<ChallengeParticipationEntity>>

    @Query(
        """
        SELECT * FROM challenge_participations
        WHERE profileId = :profileId AND status IN ('active', 'paused')
        ORDER BY updatedAtEpochMs DESC
        """,
    )
    suspend fun getOpenForProfile(profileId: Long): List<ChallengeParticipationEntity>

    @Query(
        """
        SELECT * FROM challenge_participations
        WHERE status = 'active' AND household = 1
        """,
    )
    suspend fun getActiveHousehold(): List<ChallengeParticipationEntity>

    @Query("SELECT * FROM challenge_participations WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ChallengeParticipationEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(row: ChallengeParticipationEntity): Long

    @Update
    suspend fun update(row: ChallengeParticipationEntity)

    @Query("DELETE FROM challenge_participations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM challenge_participations")
    suspend fun deleteAll()
}

@Dao
interface ChallengeWeekWinnerDao {
    @Query("SELECT * FROM challenge_week_winners ORDER BY weekStartEpochMs DESC LIMIT 1")
    fun observeLatest(): Flow<ChallengeWeekWinnerEntity?>

    @Query("SELECT * FROM challenge_week_winners WHERE weekStartEpochMs = :weekStart LIMIT 1")
    suspend fun get(weekStart: Long): ChallengeWeekWinnerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: ChallengeWeekWinnerEntity)

    @Query("DELETE FROM challenge_week_winners")
    suspend fun deleteAll()
}
