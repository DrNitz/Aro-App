package com.drnitz.aro.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.drnitz.aro.data.db.entity.MedicationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationLogDao {

    @Query("SELECT * FROM medication_logs ORDER BY taken_at DESC")
    fun observeAll(): Flow<List<MedicationLogEntity>>

    @Query("SELECT * FROM medication_logs WHERE epoch_day >= :fromEpochDay ORDER BY taken_at DESC")
    fun observeSince(fromEpochDay: Long): Flow<List<MedicationLogEntity>>

    @Query(
        "SELECT * FROM medication_logs " +
            "WHERE epoch_day BETWEEN :startEpochDay AND :endEpochDay ORDER BY taken_at ASC",
    )
    suspend fun getInRange(startEpochDay: Long, endEpochDay: Long): List<MedicationLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MedicationLogEntity)

    @Query("DELETE FROM medication_logs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM medication_logs WHERE medication_id = :medicationId")
    suspend fun deleteForMedication(medicationId: String)
}
