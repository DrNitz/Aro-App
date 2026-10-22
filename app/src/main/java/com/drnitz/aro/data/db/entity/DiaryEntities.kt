package com.drnitz.aro.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.drnitz.aro.data.db.NutrientColumns
import com.drnitz.aro.domain.model.MealSlot

/**
 * Day-level diary state: hydration and the free-text note.
 *
 * **Only a row exists for a day the user actually touched.** Aro does not pre-create
 * today's row at launch — "no row" is the honest representation of "nothing logged", and
 * it keeps the table proportional to real usage rather than to how many times the app
 * has been opened.
 */
@Entity(
    tableName = "days",
    indices = [Index(value = ["epoch_day"], unique = true)],
)
data class DayEntity(
    @PrimaryKey
    @ColumnInfo(name = "epoch_day")
    val epochDay: Long,

    @ColumnInfo(name = "water_ml") val waterMl: Int = 0,
    @ColumnInfo(name = "notes") val notes: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

/**
 * One food added to one meal on one day.
 *
 * ### Why `epochDay` is a `Long`
 * The calendar day is stored as [java.time.LocalDate.toEpochDay], not as an ISO string or
 * a timestamp. Three reasons:
 *  1. **Isolation** — equality on a day is integer equality, so "yesterday" can never
 *     leak into "today" through a formatting or timezone mismatch.
 *  2. **Range queries** — the Task 12 trend queries are plain `BETWEEN` integer scans
 *     that use the index directly, with no string collation or date parsing.
 *  3. **Stability** — the value is the user's local calendar day at the moment of
 *     logging. It never shifts because a later query runs in a different timezone.
 *
 * ### Indices
 * `epoch_day` serves every per-day read (dashboard, diary, totals); `meal_slot` serves
 * per-meal reads; `food_id` serves the "logged this before" lookups and re-logging.
 * All three are declared, because an unindexed day filter on the diary table is the
 * single most likely query to degrade as history accumulates.
 *
 * The eleven nutrient columns are nullable on purpose — see [NutrientColumns].
 */
@Entity(
    tableName = "food_logs",
    indices = [
        Index(value = ["epoch_day"]),
        Index(value = ["meal_slot"]),
        Index(value = ["food_id"]),
    ],
)
data class FoodLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "meal_slot") val mealSlot: MealSlot,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "brand") val brand: String?,
    @ColumnInfo(name = "food_id") val foodId: String?,
    @ColumnInfo(name = "grams") val grams: Double,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,

    @ColumnInfo(name = "calories") override val calories: Double?,
    @ColumnInfo(name = "protein") override val protein: Double?,
    @ColumnInfo(name = "carbs") override val carbs: Double?,
    @ColumnInfo(name = "fat") override val fat: Double?,
    @ColumnInfo(name = "saturated_fat") override val saturatedFat: Double?,
    @ColumnInfo(name = "fiber") override val fiber: Double?,
    @ColumnInfo(name = "sugar") override val sugar: Double?,
    @ColumnInfo(name = "sodium") override val sodium: Double?,
    @ColumnInfo(name = "potassium") override val potassium: Double?,
    @ColumnInfo(name = "calcium") override val calcium: Double?,
    @ColumnInfo(name = "magnesium") override val magnesium: Double?,
) : NutrientColumns

/**
 * A logged workout. Sets live in a child table so a session's rows are never rewritten
 * to add a set.
 *
 * [bodyWeightKg] is snapshotted at log time: recomputing a past day's burn against
 * today's weight silently rewrites history, which the web app did.
 */
@Entity(
    tableName = "workouts",
    indices = [
        Index(value = ["epoch_day"]),
        Index(value = ["logged_at"]),
    ],
)
data class WorkoutEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "exercise_name") val exerciseName: String,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "met") val met: Double,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,
    @ColumnInfo(name = "distance_km") val distanceKm: Double? = null,
    @ColumnInfo(name = "average_speed_kph") val averageSpeedKph: Double? = null,
    @ColumnInfo(name = "steps") val steps: Int? = null,
    @ColumnInfo(name = "body_weight_kg") val bodyWeightKg: Double?,
    @ColumnInfo(name = "muscle") val muscle: String?,
    @ColumnInfo(name = "equipment") val equipment: String?,
    @ColumnInfo(name = "logged_at") val loggedAt: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/**
 * One working set.
 *
 * `onDelete = CASCADE` means deleting a workout cleans up its sets automatically —
 * there is no second statement to forget, so orphaned rows are impossible rather than
 * merely unlikely.
 */
@Entity(
    tableName = "workout_sets",
    indices = [Index(value = ["workout_id"])],
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["workout_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "workout_id") val workoutId: String,

    /** Preserves the order the sets were entered in; 1-based like the UI. */
    @ColumnInfo(name = "set_index") val setIndex: Int,
    @ColumnInfo(name = "weight_kg") val weightKg: Double,
    @ColumnInfo(name = "reps") val reps: Int,
)

/** A workout with its sets, assembled by Room in one query (no N+1). */
data class WorkoutWithSets(
    @androidx.room.Embedded val workout: WorkoutEntity,
    @androidx.room.Relation(
        parentColumn = "id",
        entityColumn = "workout_id",
    )
    val sets: List<WorkoutSetEntity>,
)