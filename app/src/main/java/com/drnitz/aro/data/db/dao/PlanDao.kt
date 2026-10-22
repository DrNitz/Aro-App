package com.drnitz.aro.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.drnitz.aro.data.db.entity.PlanCompletionEntity
import com.drnitz.aro.data.db.entity.PlanSeriesEntity
import kotlinx.coroutines.flow.Flow

/**
 * Plan series and their per-occurrence completion markers.
 *
 * Reads intentionally load series whose start is on or before the requested range end;
 * recurrence itself is expanded in the pure domain layer. That keeps Room queries
 * simple and makes daily/weekly/monthly/yearly expansion unit-testable without a
 * database.
 */
@Dao
interface PlanDao {

    @Query(
        "SELECT * FROM plan_series " +
            "WHERE start_epoch_day <= :toEpochDay " +
            "ORDER BY start_epoch_day ASC, time_minute_of_day ASC",
    )
    fun observeSeriesStartingBy(toEpochDay: Long): Flow<List<PlanSeriesEntity>>

    @Query(
        "SELECT * FROM plan_series " +
            "ORDER BY start_epoch_day ASC, time_minute_of_day ASC",
    )
    fun observeAllSeries(): Flow<List<PlanSeriesEntity>>

    @Query(
        "SELECT * FROM plan_series " +
            "ORDER BY start_epoch_day ASC, time_minute_of_day ASC",
    )
    suspend fun getAllSeries(): List<PlanSeriesEntity>

    @Query("SELECT * FROM plan_series WHERE id = :id LIMIT 1")
    suspend fun getSeries(id: String): PlanSeriesEntity?

    @Upsert
    suspend fun upsertSeries(series: PlanSeriesEntity)

    @Query("DELETE FROM plan_series WHERE id = :id")
    suspend fun deleteSeries(id: String): Int

    @Query(
        "SELECT * FROM plan_completions " +
            "WHERE occurrence_epoch_day = :epochDay",
    )
    fun observeCompletionsForDay(epochDay: Long): Flow<List<PlanCompletionEntity>>

    @Query(
        "SELECT * FROM plan_completions " +
            "WHERE occurrence_epoch_day BETWEEN :fromEpochDay AND :toEpochDay",
    )
    fun observeCompletionsInRange(
        fromEpochDay: Long,
        toEpochDay: Long,
    ): Flow<List<PlanCompletionEntity>>

    @Query(
        "SELECT * FROM plan_completions " +
            "WHERE plan_id = :planId AND occurrence_epoch_day = :epochDay LIMIT 1",
    )
    suspend fun getCompletion(planId: String, epochDay: Long): PlanCompletionEntity?

    @Query(
        "SELECT occurrence_epoch_day FROM plan_completions " +
            "WHERE plan_id = :planId " +
            "AND occurrence_epoch_day BETWEEN :fromEpochDay AND :toEpochDay",
    )
    suspend fun getCompletionDays(
        planId: String,
        fromEpochDay: Long,
        toEpochDay: Long,
    ): List<Long>

    @Upsert
    suspend fun upsertCompletion(completion: PlanCompletionEntity)

    @Query(
        "DELETE FROM plan_completions " +
            "WHERE plan_id = :planId AND occurrence_epoch_day = :epochDay",
    )
    suspend fun deleteCompletion(planId: String, epochDay: Long): Int
}
