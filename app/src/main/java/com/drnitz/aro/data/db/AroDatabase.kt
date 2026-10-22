package com.drnitz.aro.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.drnitz.aro.data.db.dao.ConsultationDao
import com.drnitz.aro.data.db.dao.DiaryDao
import com.drnitz.aro.data.db.dao.FastingDao
import com.drnitz.aro.data.db.dao.FoodLibraryDao
import com.drnitz.aro.data.db.dao.PlanDao
import com.drnitz.aro.data.db.dao.ProfileDao
import com.drnitz.aro.data.db.dao.VitalsDao
import com.drnitz.aro.data.db.dao.WeightDao
import com.drnitz.aro.data.db.dao.WorkoutDao
import com.drnitz.aro.data.db.entity.ConsultationEntity
import com.drnitz.aro.data.db.entity.CustomFoodEntity
import com.drnitz.aro.data.db.entity.DayEntity
import com.drnitz.aro.data.db.entity.FastingSessionEntity
import com.drnitz.aro.data.db.entity.FavoriteFoodEntity
import com.drnitz.aro.data.db.entity.FoodLogEntity
import com.drnitz.aro.data.db.entity.MealTemplateEntity
import com.drnitz.aro.data.db.entity.MealTemplateItemEntity
import com.drnitz.aro.data.db.entity.PlanCompletionEntity
import com.drnitz.aro.data.db.entity.PlanSeriesEntity
import com.drnitz.aro.data.db.entity.ProfileEntity
import com.drnitz.aro.data.db.entity.RecentFoodEntity
import com.drnitz.aro.data.db.entity.VitalsEntryEntity
import com.drnitz.aro.data.db.entity.WeightEntryEntity
import com.drnitz.aro.data.db.entity.WorkoutEntity
import com.drnitz.aro.data.db.entity.WorkoutSetEntity

/**
 * Aro's local database.
 *
 * Design notes:
 *  - **No destructive fallback.** There is deliberately no
 *    `fallbackToDestructiveMigration()`. Adding one would silently delete a user's
 *    entire health history the first time a schema change shipped; schema versions are
 *    expected to ship a real `Migration` instead.
 *  - **Write-ahead logging** lets the UI keep reading while a write is in flight, which
 *    is what keeps the dashboard from stuttering while food is being logged.
 *  - **Foreign keys are enforced** so the `CASCADE` on workout sets and template items
 *    actually fires.
 */
@Database(
    entities = [
        ProfileEntity::class,
        WeightEntryEntity::class,
        FastingSessionEntity::class,
        DayEntity::class,
        FoodLogEntity::class,
        WorkoutEntity::class,
        WorkoutSetEntity::class,
        CustomFoodEntity::class,
        FavoriteFoodEntity::class,
        RecentFoodEntity::class,
        MealTemplateEntity::class,
        MealTemplateItemEntity::class,
        VitalsEntryEntity::class,
        ConsultationEntity::class,
        PlanSeriesEntity::class,
        PlanCompletionEntity::class,
    ],
    version = AroDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AroDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun diaryDao(): DiaryDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun weightDao(): WeightDao
    abstract fun fastingDao(): FastingDao
    abstract fun foodLibraryDao(): FoodLibraryDao
    abstract fun vitalsDao(): VitalsDao
    abstract fun consultationDao(): ConsultationDao
    abstract fun planDao(): PlanDao

    companion object {
        const val VERSION = 6
        const val NAME = "aro.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `vitals_entries` (
                        `id` TEXT NOT NULL,
                        `epoch_day` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `systolic` INTEGER,
                        `diastolic` INTEGER,
                        `pulse` INTEGER,
                        `glucose_mg_dl` REAL,
                        `glucose_context` TEXT,
                        `ketones_mmol_l` REAL,
                        `body_temperature_c` REAL,
                        `spo2_percent` INTEGER,
                        `notes` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vitals_entries_epoch_day` ON `vitals_entries` (`epoch_day`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vitals_entries_timestamp` ON `vitals_entries` (`timestamp`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `doctor_consultations` (
                        `id` TEXT NOT NULL,
                        `epoch_day` INTEGER NOT NULL,
                        `doctor_name` TEXT NOT NULL,
                        `specialty` TEXT NOT NULL,
                        `reason_for_visit` TEXT NOT NULL,
                        `diagnosis_notes` TEXT NOT NULL,
                        `prescribed_medications` TEXT NOT NULL,
                        `recommended_tests` TEXT NOT NULL,
                        `follow_up_plan` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_doctor_consultations_epoch_day` ON `doctor_consultations` (`epoch_day`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_doctor_consultations_created_at` ON `doctor_consultations` (`created_at`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `plan_series` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `start_epoch_day` INTEGER NOT NULL,
                        `time_minute_of_day` INTEGER NOT NULL,
                        `repeat_rule` TEXT NOT NULL,
                        `repeat_until_epoch_day` INTEGER,
                        `reminder_enabled` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        `meal_slot` TEXT,
                        `food_id` TEXT,
                        `brand` TEXT,
                        `grams` REAL,
                        `is_custom` INTEGER,
                        `exercise_id` TEXT,
                        `is_strength` INTEGER,
                        `duration_minutes` INTEGER,
                        `met` REAL,
                        `category` TEXT,
                        `muscle` TEXT,
                        `equipment` TEXT,
                        `dosage` TEXT,
                        `saved_reminder_id` TEXT,
                        `calories` REAL,
                        `protein` REAL,
                        `carbs` REAL,
                        `fat` REAL,
                        `saturated_fat` REAL,
                        `fiber` REAL,
                        `sugar` REAL,
                        `sodium` REAL,
                        `potassium` REAL,
                        `calcium` REAL,
                        `magnesium` REAL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_series_start_epoch_day` ON `plan_series` (`start_epoch_day`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_plan_series_type` ON `plan_series` (`type`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `plan_completions` (
                        `plan_id` TEXT NOT NULL,
                        `occurrence_epoch_day` INTEGER NOT NULL,
                        `completed_at` INTEGER NOT NULL,
                        `actual_log_id` TEXT,
                        PRIMARY KEY(`plan_id`, `occurrence_epoch_day`),
                        FOREIGN KEY(`plan_id`) REFERENCES `plan_series`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_plan_completions_occurrence_epoch_day` " +
                        "ON `plan_completions` (`occurrence_epoch_day`)",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Existing plan types remain valid; the new columns are nullable and
                // only populated for fasting and water plans.
                db.execSQL("ALTER TABLE `plan_series` ADD COLUMN `fasting_target_hours` INTEGER")
                db.execSQL("ALTER TABLE `plan_series` ADD COLUMN `water_ml` INTEGER")
                db.execSQL("ALTER TABLE `plan_completions` ADD COLUMN `skipped_at` INTEGER")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Capture the source thumbnail so planned food and workouts keep their
                // images after a restart instead of falling back to a generic icon.
                db.execSQL("ALTER TABLE `plan_series` ADD COLUMN `image_url` TEXT")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Cardio extras are optional: older workouts stay valid and simply have
                // no distance, speed or step count.
                db.execSQL("ALTER TABLE `workouts` ADD COLUMN `distance_km` REAL")
                db.execSQL("ALTER TABLE `workouts` ADD COLUMN `average_speed_kph` REAL")
                db.execSQL("ALTER TABLE `workouts` ADD COLUMN `steps` INTEGER")
            }
        }

        @Volatile
        private var instance: AroDatabase? = null

        fun get(context: Context): AroDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }

        private fun build(context: Context): AroDatabase =
            Room.databaseBuilder(context, AroDatabase::class.java, NAME)
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys = ON")
                    }
                })
                .build()

        /** Replaces the singleton — instrumented tests inject an in-memory database. */
        internal fun overrideForTesting(database: AroDatabase?) {
            instance = database
        }
    }
}