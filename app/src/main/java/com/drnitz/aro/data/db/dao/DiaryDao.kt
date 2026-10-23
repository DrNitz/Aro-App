package com.drnitz.aro.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.drnitz.aro.data.db.DayNutrientTotals
import com.drnitz.aro.data.db.DayTotalsRow
import com.drnitz.aro.data.db.entity.DayEntity
import com.drnitz.aro.data.db.entity.FoodLogEntity
import com.drnitz.aro.data.db.entity.WaterLogEntity
import com.drnitz.aro.domain.model.MealSlot
import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes for a day's food log.
 *
 * Every read filters on an indexed `epoch_day`; every write is a single upsert or one
 * batch, so opening the diary costs one indexed scan no matter how much history has
 * accumulated. Nothing loads rows in order to sum them in Kotlin — the dashboard totals
 * come from [observeDayTotals].
 */
@Dao
interface DiaryDao {

    // ---- Food log entries -------------------------------------------------

    @Query("SELECT * FROM food_logs WHERE epoch_day = :epochDay ORDER BY created_at ASC")
    fun observeFoodsForDay(epochDay: Long): Flow<List<FoodLogEntity>>

    @Query(
        "SELECT * FROM food_logs WHERE epoch_day = :epochDay AND meal_slot = :meal " +
            "ORDER BY created_at ASC",
    )
    fun observeFoodsForMeal(epochDay: Long, meal: MealSlot): Flow<List<FoodLogEntity>>

    @Query("SELECT * FROM food_logs WHERE id = :id")
    suspend fun findFood(id: String): FoodLogEntity?

    @Upsert
    suspend fun upsertFood(entry: FoodLogEntity)

    /** One call for a whole meal's worth of rows, not one per food. */
    @Upsert
    suspend fun upsertFoods(entries: List<FoodLogEntity>)

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteFood(id: String): Int

    @Query("DELETE FROM food_logs WHERE id IN (:ids)")
    suspend fun deleteFoods(ids: List<String>): Int

    /** Moves an entry to another meal or another day without losing its id. */
    @Query("UPDATE food_logs SET meal_slot = :meal, epoch_day = :epochDay WHERE id = :id")
    suspend fun moveFood(id: String, epochDay: Long, meal: MealSlot): Int

    /**
     * Replaces a day's entries wholesale in one transaction: the delete and the batch
     * insert either both land or neither does, so a crash mid-write can never leave a
     * day half-logged.
     */
    @Transaction
    suspend fun replaceDayFoods(epochDay: Long, entries: List<FoodLogEntity>) {
        deleteAllFoodsForDay(epochDay)
        if (entries.isNotEmpty()) upsertFoods(entries)
    }

    @Query("DELETE FROM food_logs WHERE epoch_day = :epochDay")
    suspend fun deleteAllFoodsForDay(epochDay: Long): Int

    // ---- Dashboard aggregate ---------------------------------------------

    /**
     * A whole day's nutrition in one indexed scan with eleven SUMs.
     *
     * A day with no food returns a single row of nulls rather than an empty list, so the
     * caller always gets a projection and can tell "nothing logged" from "logged zero".
     */
    @Query(
        """
        SELECT
            SUM(calories) AS calories,
            SUM(protein) AS protein,
            SUM(carbs) AS carbs,
            SUM(fat) AS fat,
            SUM(saturated_fat) AS saturatedFat,
            SUM(fiber) AS fiber,
            SUM(sugar) AS sugar,
            SUM(sodium) AS sodium,
            SUM(potassium) AS potassium,
            SUM(calcium) AS calcium,
            SUM(magnesium) AS magnesium
        FROM food_logs
        WHERE epoch_day = :epochDay
        """,
    )
    fun observeDayTotals(epochDay: Long): Flow<DayNutrientTotals>

    /** Totals for a multi-day window grouped by day — powers the Task 12 trend charts. */
    @Query(
        """
        SELECT
            epoch_day AS epochDay,
            SUM(calories) AS calories,
            SUM(protein) AS protein,
            SUM(carbs) AS carbs,
            SUM(fat) AS fat,
            SUM(saturated_fat) AS saturatedFat,
            SUM(fiber) AS fiber,
            SUM(sugar) AS sugar,
            SUM(sodium) AS sodium,
            SUM(potassium) AS potassium,
            SUM(calcium) AS calcium,
            SUM(magnesium) AS magnesium
        FROM food_logs
        WHERE epoch_day BETWEEN :fromEpochDay AND :toEpochDay
        GROUP BY epoch_day
        ORDER BY epoch_day ASC
        """,
    )
    fun observeTotalsBetween(fromEpochDay: Long, toEpochDay: Long): Flow<List<DayTotalsRow>>

    // ---- Day state (water, notes) ----------------------------------------

    @Query("SELECT * FROM days WHERE epoch_day = :epochDay")
    fun observeDay(epochDay: Long): Flow<DayEntity?>

    /** Point read for writes that must read-modify-write, e.g. adding 250 mL of water. */
    @Query("SELECT * FROM days WHERE epoch_day = :epochDay LIMIT 1")
    suspend fun getDay(epochDay: Long): DayEntity?

    @Upsert
    suspend fun upsertDay(day: DayEntity)

    // ---- Water log -------------------------------------------------------

    /** Every drink, newest first — the history screen's single query. */
    @Query("SELECT * FROM water_logs ORDER BY logged_at DESC")
    fun observeAllWaterLogs(): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs WHERE epoch_day = :epochDay ORDER BY logged_at DESC")
    fun observeWaterLogsForDay(epochDay: Long): Flow<List<WaterLogEntity>>

    @Query("SELECT * FROM water_logs WHERE id = :id")
    suspend fun findWaterLog(id: String): WaterLogEntity?

    @Insert
    suspend fun insertWaterLog(log: WaterLogEntity)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun deleteWaterLog(id: String): Int

    @Query("DELETE FROM water_logs WHERE epoch_day = :epochDay")
    suspend fun deleteWaterLogsForDay(epochDay: Long): Int

    // ---- History ----------------------------------------------------------

    /** Days that actually have entries, newest first — drives the date strip. */
    @Query(
        """
        SELECT DISTINCT epoch_day FROM (
            SELECT epoch_day FROM food_logs
            UNION SELECT epoch_day FROM workouts
            UNION SELECT epoch_day FROM days
        )
        ORDER BY epoch_day DESC
        LIMIT :limit
        """,
    )
    fun observeRecentDays(limit: Int): Flow<List<Long>>

    @Query("SELECT COUNT(*) FROM food_logs WHERE epoch_day = :epochDay")
    suspend fun countFoodsForDay(epochDay: Long): Int
}