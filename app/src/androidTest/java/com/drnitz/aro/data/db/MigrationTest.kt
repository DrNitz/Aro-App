package com.drnitz.aro.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val testDbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AroDatabase::class.java,
    )

    @Test
    fun migration1To2_createsVitalsAndConsultationsTables() {
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                """
                INSERT INTO days (epoch_day, water_ml, notes, created_at, updated_at)
                VALUES (19000, 2000, 'Baseline day', 1000, 1000)
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 2, true, AroDatabase.MIGRATION_1_2)

        // Verify existing data is preserved
        val dayCursor = db.query("SELECT epoch_day, water_ml, notes FROM days WHERE epoch_day = 19000")
        assertTrue(dayCursor.moveToFirst())
        assertEquals(19000L, dayCursor.getLong(0))
        assertEquals(2000, dayCursor.getInt(1))
        assertEquals("Baseline day", dayCursor.getString(2))
        dayCursor.close()

        // Verify new tables can be written to and read from
        db.execSQL(
            """
            INSERT INTO vitals_entries (
                id, epoch_day, timestamp, systolic, diastolic, pulse,
                glucose_mg_dl, glucose_context, ketones_mmol_l,
                body_temperature_c, spo2_percent, notes
            ) VALUES (
                'vitals-1', 19000, 1000, 120, 80, 72,
                95.0, 'FASTING', 0.5, 36.6, 98, 'Normal vitals'
            )
            """.trimIndent(),
        )
        val vitalsCursor = db.query("SELECT id, systolic, diastolic, spo2_percent FROM vitals_entries WHERE id = 'vitals-1'")
        assertTrue(vitalsCursor.moveToFirst())
        assertEquals("vitals-1", vitalsCursor.getString(0))
        assertEquals(120, vitalsCursor.getInt(1))
        assertEquals(80, vitalsCursor.getInt(2))
        assertEquals(98, vitalsCursor.getInt(3))
        vitalsCursor.close()

        db.execSQL(
            """
            INSERT INTO doctor_consultations (
                id, epoch_day, doctor_name, specialty, reason_for_visit,
                diagnosis_notes, prescribed_medications, recommended_tests,
                follow_up_plan, created_at
            ) VALUES (
                'doc-1', 19000, 'Dr. Smith', 'Cardiology', 'Annual checkup',
                'Healthy heart', 'None', 'Lipid panel', '1 year', 1000
            )
            """.trimIndent(),
        )
        val docCursor = db.query("SELECT id, doctor_name, specialty FROM doctor_consultations WHERE id = 'doc-1'")
        assertTrue(docCursor.moveToFirst())
        assertEquals("doc-1", docCursor.getString(0))
        assertEquals("Dr. Smith", docCursor.getString(1))
        assertEquals("Cardiology", docCursor.getString(2))
        docCursor.close()
    }

    @Test
    fun migration2To3_createsPlanSeriesAndCompletions() {
        helper.createDatabase(testDbName, 2).apply {
            execSQL(
                """
                INSERT INTO vitals_entries (
                    id, epoch_day, timestamp, systolic, diastolic, pulse,
                    glucose_mg_dl, glucose_context, ketones_mmol_l,
                    body_temperature_c, spo2_percent, notes
                ) VALUES (
                    'vitals-2', 19001, 2000, 118, 78, 68,
                    90.0, 'FASTING', 0.4, 36.5, 99, 'Great vitals'
                )
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 3, true, AroDatabase.MIGRATION_2_3)

        // Verify existing table data intact
        val vitalsCursor = db.query("SELECT id, systolic, diastolic FROM vitals_entries WHERE id = 'vitals-2'")
        assertTrue(vitalsCursor.moveToFirst())
        assertEquals("vitals-2", vitalsCursor.getString(0))
        assertEquals(118, vitalsCursor.getInt(1))
        vitalsCursor.close()

        // Insert into new plan_series table
        db.execSQL(
            """
            INSERT INTO plan_series (
                id, type, title, start_epoch_day, time_minute_of_day,
                repeat_rule, repeat_until_epoch_day, reminder_enabled, created_at
            ) VALUES (
                'plan-1', 'MEDICATION', 'Vitamin D', 19001, 480,
                'DAILY', NULL, 1, 2000
            )
            """.trimIndent(),
        )
        val planCursor = db.query("SELECT id, type, title, reminder_enabled FROM plan_series WHERE id = 'plan-1'")
        assertTrue(planCursor.moveToFirst())
        assertEquals("plan-1", planCursor.getString(0))
        assertEquals("MEDICATION", planCursor.getString(1))
        assertEquals("Vitamin D", planCursor.getString(2))
        assertEquals(1, planCursor.getInt(3))
        planCursor.close()

        // Insert into plan_completions referencing plan_series
        db.execSQL(
            """
            INSERT INTO plan_completions (plan_id, occurrence_epoch_day, completed_at, actual_log_id)
            VALUES ('plan-1', 19001, 2500, 'log-1')
            """.trimIndent(),
        )
        val compCursor = db.query("SELECT plan_id, occurrence_epoch_day, completed_at FROM plan_completions WHERE plan_id = 'plan-1'")
        assertTrue(compCursor.moveToFirst())
        assertEquals("plan-1", compCursor.getString(0))
        assertEquals(19001L, compCursor.getLong(1))
        assertEquals(2500L, compCursor.getLong(2))
        compCursor.close()
    }

    @Test
    fun migration3To4_addsFastingWaterAndSkippedAtColumns() {
        helper.createDatabase(testDbName, 3).apply {
            execSQL(
                """
                INSERT INTO plan_series (
                    id, type, title, start_epoch_day, time_minute_of_day,
                    repeat_rule, repeat_until_epoch_day, reminder_enabled, created_at
                ) VALUES (
                    'plan-v3', 'HABIT', 'Morning Walk', 19002, 420,
                    'DAILY', NULL, 0, 3000
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO plan_completions (plan_id, occurrence_epoch_day, completed_at, actual_log_id)
                VALUES ('plan-v3', 19002, 3500, NULL)
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 4, true, AroDatabase.MIGRATION_3_4)

        // Verify existing rows have nulls for the new columns
        val planCursor = db.query("SELECT id, fasting_target_hours, water_ml FROM plan_series WHERE id = 'plan-v3'")
        assertTrue(planCursor.moveToFirst())
        assertEquals("plan-v3", planCursor.getString(0))
        assertTrue(planCursor.isNull(1))
        assertTrue(planCursor.isNull(2))
        planCursor.close()

        val compCursor = db.query("SELECT plan_id, skipped_at FROM plan_completions WHERE plan_id = 'plan-v3'")
        assertTrue(compCursor.moveToFirst())
        assertEquals("plan-v3", compCursor.getString(0))
        assertTrue(compCursor.isNull(1))
        compCursor.close()

        // Verify columns can be updated
        db.execSQL("UPDATE plan_series SET fasting_target_hours = 16, water_ml = 500 WHERE id = 'plan-v3'")
        db.execSQL("UPDATE plan_completions SET skipped_at = 4000 WHERE plan_id = 'plan-v3'")

        val updatedPlanCursor = db.query("SELECT fasting_target_hours, water_ml FROM plan_series WHERE id = 'plan-v3'")
        assertTrue(updatedPlanCursor.moveToFirst())
        assertEquals(16, updatedPlanCursor.getInt(0))
        assertEquals(500, updatedPlanCursor.getInt(1))
        updatedPlanCursor.close()

        val updatedCompCursor = db.query("SELECT skipped_at FROM plan_completions WHERE plan_id = 'plan-v3'")
        assertTrue(updatedCompCursor.moveToFirst())
        assertEquals(4000L, updatedCompCursor.getLong(0))
        updatedCompCursor.close()
    }

    @Test
    fun migration4To5_addsImageUrlToPlanSeries() {
        helper.createDatabase(testDbName, 4).apply {
            execSQL(
                """
                INSERT INTO plan_series (
                    id, type, title, start_epoch_day, time_minute_of_day,
                    repeat_rule, repeat_until_epoch_day, reminder_enabled, created_at,
                    fasting_target_hours, water_ml
                ) VALUES (
                    'plan-v4', 'MEAL', 'Oatmeal', 19003, 500,
                    'DAILY', NULL, 0, 4000, NULL, NULL
                )
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 5, true, AroDatabase.MIGRATION_4_5)

        val planCursor = db.query("SELECT id, image_url FROM plan_series WHERE id = 'plan-v4'")
        assertTrue(planCursor.moveToFirst())
        assertEquals("plan-v4", planCursor.getString(0))
        assertTrue(planCursor.isNull(1))
        planCursor.close()

        db.execSQL("UPDATE plan_series SET image_url = 'https://example.com/oatmeal.jpg' WHERE id = 'plan-v4'")
        val updatedCursor = db.query("SELECT image_url FROM plan_series WHERE id = 'plan-v4'")
        assertTrue(updatedCursor.moveToFirst())
        assertEquals("https://example.com/oatmeal.jpg", updatedCursor.getString(0))
        updatedCursor.close()
    }

    @Test
    fun migration5To6_addsCardioColumnsToWorkouts() {
        helper.createDatabase(testDbName, 5).apply {
            execSQL(
                """
                INSERT INTO workouts (
                    id, epoch_day, type, exercise_name, category, met,
                    duration_minutes, body_weight_kg, muscle, equipment,
                    logged_at, created_at
                ) VALUES (
                    'workout-v5', 19004, 'CARDIO', 'Outdoor Running', 'Running', 9.8,
                    45, 75.0, 'Quadriceps', 'None', 5000, 5000
                )
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 6, true, AroDatabase.MIGRATION_5_6)

        val workoutCursor = db.query("SELECT id, distance_km, average_speed_kph, steps FROM workouts WHERE id = 'workout-v5'")
        assertTrue(workoutCursor.moveToFirst())
        assertEquals("workout-v5", workoutCursor.getString(0))
        assertTrue(workoutCursor.isNull(1))
        assertTrue(workoutCursor.isNull(2))
        assertTrue(workoutCursor.isNull(3))
        workoutCursor.close()

        db.execSQL("UPDATE workouts SET distance_km = 8.2, average_speed_kph = 10.9, steps = 8500 WHERE id = 'workout-v5'")
        val updatedCursor = db.query("SELECT distance_km, average_speed_kph, steps FROM workouts WHERE id = 'workout-v5'")
        assertTrue(updatedCursor.moveToFirst())
        assertEquals(8.2, updatedCursor.getDouble(0), 0.001)
        assertEquals(10.9, updatedCursor.getDouble(1), 0.001)
        assertEquals(8500, updatedCursor.getInt(2))
        updatedCursor.close()
    }

    @Test
    fun migration6To7_createsMedicationLogs() {
        helper.createDatabase(testDbName, 6).apply {
            execSQL(
                """
                INSERT INTO days (epoch_day, water_ml, notes, created_at, updated_at)
                VALUES (19006, 1500, 'Pre-adherence day', 1000, 1000)
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 7, true, AroDatabase.MIGRATION_6_7)

        // Existing data is untouched.
        val dayCursor = db.query("SELECT notes FROM days WHERE epoch_day = 19006")
        assertTrue(dayCursor.moveToFirst())
        assertEquals("Pre-adherence day", dayCursor.getString(0))
        dayCursor.close()

        db.execSQL(
            """
            INSERT INTO medication_logs (
                id, medication_id, medication_name, dosage, epoch_day,
                scheduled_hour, scheduled_minute, taken_at, source
            ) VALUES (
                'medlog-1', 'med-1', 'Vitamin D3', '5000 IU', 19006,
                8, 0, 5000, 'NOTIFICATION'
            )
            """.trimIndent(),
        )

        val logCursor = db.query(
            "SELECT id, medication_id, medication_name, dosage, source FROM medication_logs WHERE id = 'medlog-1'",
        )
        assertTrue(logCursor.moveToFirst())
        assertEquals("medlog-1", logCursor.getString(0))
        assertEquals("med-1", logCursor.getString(1))
        assertEquals("Vitamin D3", logCursor.getString(2))
        assertEquals("5000 IU", logCursor.getString(3))
        assertEquals("NOTIFICATION", logCursor.getString(4))
        logCursor.close()

        // The unique (medication_id, epoch_day) index prevents double-counting a dose
        // while allowing the later entry to replace the earlier one.
        db.execSQL(
            """
            INSERT OR REPLACE INTO medication_logs (
                id, medication_id, medication_name, dosage, epoch_day,
                scheduled_hour, scheduled_minute, taken_at, source
            ) VALUES (
                'medlog-2', 'med-1', 'Vitamin D3', '5000 IU', 19006,
                8, 0, 6000, 'MANUAL'
            )
            """.trimIndent(),
        )
        val countCursor = db.query(
            "SELECT COUNT(*) FROM medication_logs WHERE medication_id = 'med-1' AND epoch_day = 19006",
        )
        assertTrue(countCursor.moveToFirst())
        assertEquals(1, countCursor.getInt(0))
        countCursor.close()
    }

    @Test
    fun allMigrations_from1To7_preservesDataIntegrity() {
        helper.createDatabase(testDbName, 1).apply {
            execSQL(
                """
                INSERT INTO profile (
                    id, name, sex, age_years, weight_kg, target_weight_kg, height_cm,
                    activity_level, goal, macro_preset, active_protocol_id,
                    target_water_ml, onboarding_completed, created_at, updated_at
                ) VALUES (
                    1, 'Test User', 'MALE', 30, 80.0, 75.0, 180.0,
                    'MODERATE', 'WEIGHT_LOSS', 'BALANCED', '16:8',
                    2500, 1, 1000, 1000
                )
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO days (epoch_day, water_ml, notes, created_at, updated_at)
                VALUES (19005, 1800, 'All migration day', 1000, 1000)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workouts (
                    id, epoch_day, type, exercise_name, category, met,
                    duration_minutes, body_weight_kg, muscle, equipment,
                    logged_at, created_at
                ) VALUES (
                    'workout-all', 19005, 'CARDIO', 'Cycling', 'Cycling', 7.5,
                    30, 80.0, 'Quadriceps', 'Bicycle', 1000, 1000
                )
                """.trimIndent(),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            testDbName,
            7,
            true,
            AroDatabase.MIGRATION_1_2,
            AroDatabase.MIGRATION_2_3,
            AroDatabase.MIGRATION_3_4,
            AroDatabase.MIGRATION_4_5,
            AroDatabase.MIGRATION_5_6,
            AroDatabase.MIGRATION_6_7,
        )

        // Verify original profile
        val profileCursor = db.query("SELECT id, name, weight_kg FROM profile WHERE id = 1")
        assertTrue(profileCursor.moveToFirst())
        assertEquals(1L, profileCursor.getLong(0))
        assertEquals("Test User", profileCursor.getString(1))
        assertEquals(80.0, profileCursor.getDouble(2), 0.001)
        profileCursor.close()

        // Verify original day
        val dayCursor = db.query("SELECT epoch_day, water_ml, notes FROM days WHERE epoch_day = 19005")
        assertTrue(dayCursor.moveToFirst())
        assertEquals(19005L, dayCursor.getLong(0))
        assertEquals(1800, dayCursor.getInt(1))
        assertEquals("All migration day", dayCursor.getString(2))
        dayCursor.close()

        // Verify original workout and new columns present as null
        val workoutCursor = db.query("SELECT id, exercise_name, distance_km, steps FROM workouts WHERE id = 'workout-all'")
        assertTrue(workoutCursor.moveToFirst())
        assertEquals("workout-all", workoutCursor.getString(0))
        assertEquals("Cycling", workoutCursor.getString(1))
        assertTrue(workoutCursor.isNull(2))
        assertTrue(workoutCursor.isNull(3))
        workoutCursor.close()

        // Verify the adherence table created by the final migration is writable.
        db.execSQL(
            """
            INSERT INTO medication_logs (
                id, medication_id, medication_name, dosage, epoch_day,
                scheduled_hour, scheduled_minute, taken_at, source
            ) VALUES (
                'medlog-all', 'med-all', 'Creatine', '5 g', 19005,
                21, 0, 7000, 'MANUAL'
            )
            """.trimIndent(),
        )
        val medCursor = db.query("SELECT medication_name, source FROM medication_logs WHERE id = 'medlog-all'")
        assertTrue(medCursor.moveToFirst())
        assertEquals("Creatine", medCursor.getString(0))
        assertEquals("MANUAL", medCursor.getString(1))
        medCursor.close()
    }
}
