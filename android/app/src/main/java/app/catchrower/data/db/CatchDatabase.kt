package app.catchrower.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

/**
 * Every schema change needs a migration (auto or manual) and a new exported schema
 * in `app/schemas`. In-app updates install over existing data, so a missing
 * migration crashes on launch rather than silently wiping the family's history.
 */
@Database(
    entities = [
        ProfileEntity::class,
        WorkoutEntity::class,
        AppSettingsEntity::class,
        ProfileBtDeviceEntity::class,
        BadgeAwardEntity::class,
        ChallengeParticipationEntity::class,
        ChallengeWeekWinnerEntity::class,
    ],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 4, to = 5, spec = CatchDatabase.DropChromeMode::class),
    ],
    exportSchema = true,
)
abstract class CatchDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun settingsDao(): SettingsDao
    abstract fun profileBtDeviceDao(): ProfileBtDeviceDao
    abstract fun badgeAwardDao(): BadgeAwardDao
    abstract fun challengeParticipationDao(): ChallengeParticipationDao
    abstract fun challengeWeekWinnerDao(): ChallengeWeekWinnerDao

    @DeleteColumn(tableName = "app_settings", columnName = "chromeMode")
    class DropChromeMode : AutoMigrationSpec

    companion object {
        @Volatile private var instance: CatchDatabase? = null

        fun get(context: Context): CatchDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CatchDatabase::class.java,
                    "catch.db",
                ).build()
                    .also { instance = it }
            }
        }
    }
}
