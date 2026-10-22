package com.drnitz.aro.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.drnitz.aro.data.db.NutrientColumns

/**
 * One planned item series.
 *
 * A one-off plan has [repeatRule] = "NONE"; a repeating plan stores its recurrence rule
 * here and its occurrences are expanded in the domain layer. Type-specific fields are
 * nullable because a single row can describe food, exercise or a pill.
 *
 * Food nutrients are snapshotted at plan time, matching the meal-template behaviour:
 * a change to the food library must not silently alter a plan the user already made.
 */
@Entity(
    tableName = "plan_series",
    indices = [
        Index(value = ["start_epoch_day"]),
        Index(value = ["type"]),
    ],
)
data class PlanSeriesEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "start_epoch_day") val startEpochDay: Long,
    @ColumnInfo(name = "time_minute_of_day") val timeMinuteOfDay: Int,
    @ColumnInfo(name = "repeat_rule") val repeatRule: String,
    @ColumnInfo(name = "repeat_until_epoch_day") val repeatUntilEpochDay: Long?,
    @ColumnInfo(name = "reminder_enabled") val reminderEnabled: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,

    /** Remote food/exercise thumbnail captured when the plan was created. */
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,

    // ---- Food ----------------------------------------------------------
    @ColumnInfo(name = "meal_slot") val mealSlot: String? = null,
    @ColumnInfo(name = "food_id") val foodId: String? = null,
    @ColumnInfo(name = "brand") val brand: String? = null,
    @ColumnInfo(name = "grams") val grams: Double? = null,
    @ColumnInfo(name = "is_custom") val isCustom: Boolean? = null,

    // ---- Exercise ------------------------------------------------------
    @ColumnInfo(name = "exercise_id") val exerciseId: String? = null,
    @ColumnInfo(name = "is_strength") val isStrength: Boolean? = null,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int? = null,
    @ColumnInfo(name = "met") val met: Double? = null,
    @ColumnInfo(name = "category") val category: String? = null,
    @ColumnInfo(name = "muscle") val muscle: String? = null,
    @ColumnInfo(name = "equipment") val equipment: String? = null,

    // ---- Pill -----------------------------------------------------------
    @ColumnInfo(name = "dosage") val dosage: String? = null,
    @ColumnInfo(name = "saved_reminder_id") val savedReminderId: String? = null,

    // ---- Fasting --------------------------------------------------------
    @ColumnInfo(name = "fasting_target_hours") val fastingTargetHours: Int? = null,

    // ---- Water ----------------------------------------------------------
    @ColumnInfo(name = "water_ml") val waterMl: Int? = null,

    // ---- Food nutrients, frozen at plan time ---------------------------
    @ColumnInfo(name = "calories") override val calories: Double? = null,
    @ColumnInfo(name = "protein") override val protein: Double? = null,
    @ColumnInfo(name = "carbs") override val carbs: Double? = null,
    @ColumnInfo(name = "fat") override val fat: Double? = null,
    @ColumnInfo(name = "saturated_fat") override val saturatedFat: Double? = null,
    @ColumnInfo(name = "fiber") override val fiber: Double? = null,
    @ColumnInfo(name = "sugar") override val sugar: Double? = null,
    @ColumnInfo(name = "sodium") override val sodium: Double? = null,
    @ColumnInfo(name = "potassium") override val potassium: Double? = null,
    @ColumnInfo(name = "calcium") override val calcium: Double? = null,
    @ColumnInfo(name = "magnesium") override val magnesium: Double? = null,
) : NutrientColumns

/**
 * Completion is per occurrence, not per series: marking today's planned lunch done
 * must not mark tomorrow's occurrence done as well.
 */
@Entity(
    tableName = "plan_completions",
    primaryKeys = ["plan_id", "occurrence_epoch_day"],
    indices = [Index(value = ["occurrence_epoch_day"])],
    foreignKeys = [
        ForeignKey(
            entity = PlanSeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class PlanCompletionEntity(
    @ColumnInfo(name = "plan_id") val planId: String,
    @ColumnInfo(name = "occurrence_epoch_day") val occurrenceEpochDay: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long,
    @ColumnInfo(name = "actual_log_id") val actualLogId: String? = null,
    /** Non-null when the user deliberately skipped this occurrence. */
    @ColumnInfo(name = "skipped_at") val skippedAt: Long? = null,
)
