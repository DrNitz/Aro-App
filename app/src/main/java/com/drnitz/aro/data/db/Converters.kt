package com.drnitz.aro.data.db

import androidx.room.TypeConverter
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.WorkoutType
import java.time.Instant

/**
 * Room type converters.
 *
 * Enums are stored by **name**, not ordinal: reordering an enum would silently
 * reinterpret existing rows. Names are readable in `sqlite3`, which matters when
 * debugging a user's diary by hand.
 */
class Converters {

    @TypeConverter
    fun mealSlotToString(value: MealSlot): String = value.name

    @TypeConverter
    fun stringToMealSlot(value: String): MealSlot =
        MealSlot.entries.firstOrNull { it.name == value } ?: MealSlot.SNACKS

    @TypeConverter
    fun workoutTypeToString(value: WorkoutType): String = value.name

    @TypeConverter
    fun stringToWorkoutType(value: String): WorkoutType =
        WorkoutType.entries.firstOrNull { it.name == value } ?: WorkoutType.CARDIO

    /** Instants are stored as epoch milliseconds. */
    @TypeConverter
    fun instantToEpochMillis(value: Instant): Long = value.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long): Instant = Instant.ofEpochMilli(value)
}