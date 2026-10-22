package com.drnitz.aro.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.drnitz.aro.data.db.entity.WeightEntryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Weight history for the Task 12 planner.
 *
 * Two details matter here:
 *  - reads are [Flow]s, so the trend chart re-renders on write without polling;
 *  - the newest entry is `ORDER BY epoch_day DESC LIMIT 1`, served directly by the
 *    unique index on `epoch_day`.
 */
@Dao
interface WeightDao {

    @Query("SELECT * FROM weight_entries ORDER BY epoch_day DESC")
    fun observeAll(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries WHERE epoch_day BETWEEN :from AND :to ORDER BY epoch_day ASC")
    fun observeBetween(from: Long, to: Long): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY epoch_day DESC LIMIT 1")
    suspend fun latest(): WeightEntryEntity?

    @Query("SELECT * FROM weight_entries ORDER BY epoch_day DESC LIMIT 1")
    fun observeLatest(): Flow<WeightEntryEntity?>

    /**
     * Records a weigh-in, replacing any existing entry for that day.
     *
     * `@Upsert` alone is **not** enough here. Room's upsert resolves conflicts on the
     * primary key only; a unique index on a *different* column (`epoch_day`) makes the
     * insert silently do nothing, so re-weighing kept the stale weight. An instrumented
     * test caught that. Deleting the day's row first makes the constraint mean what it
     * looks like it means, in one transaction.
     */
    @Transaction
    suspend fun recordWeight(entry: WeightEntryEntity) {
        deleteForDay(entry.epochDay)
        upsert(entry)
    }

    @Upsert
    suspend fun upsert(entry: WeightEntryEntity)

    @Query("DELETE FROM weight_entries WHERE epoch_day = :epochDay")
    suspend fun deleteForDay(epochDay: Long): Int
}