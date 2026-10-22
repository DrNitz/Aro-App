package com.drnitz.aro.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.drnitz.aro.data.db.entity.ConsultationEntity
import com.drnitz.aro.data.db.entity.VitalsEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VitalsDao {

    @Query("SELECT * FROM vitals_entries ORDER BY timestamp DESC")
    fun observeAllVitals(): Flow<List<VitalsEntryEntity>>

    @Query("SELECT * FROM vitals_entries WHERE epoch_day = :epochDay ORDER BY timestamp DESC")
    fun observeVitalsForDay(epochDay: Long): Flow<List<VitalsEntryEntity>>

    @Query("SELECT * FROM vitals_entries ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentVitals(limit: Int): Flow<List<VitalsEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVitals(entry: VitalsEntryEntity)

    @Query("DELETE FROM vitals_entries WHERE id = :id")
    suspend fun deleteVitals(id: String)

    @Query("SELECT * FROM vitals_entries WHERE epoch_day BETWEEN :startEpochDay AND :endEpochDay ORDER BY timestamp ASC")
    suspend fun getVitalsInRange(startEpochDay: Long, endEpochDay: Long): List<VitalsEntryEntity>
}

@Dao
interface ConsultationDao {

    @Query("SELECT * FROM doctor_consultations ORDER BY epoch_day DESC, created_at DESC")
    fun observeAllConsultations(): Flow<List<ConsultationEntity>>

    @Query("SELECT * FROM doctor_consultations WHERE id = :id")
    fun observeConsultationById(id: String): Flow<ConsultationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsultation(entry: ConsultationEntity)

    @Query("DELETE FROM doctor_consultations WHERE id = :id")
    suspend fun deleteConsultation(id: String)

    @Query("SELECT * FROM doctor_consultations WHERE epoch_day BETWEEN :startEpochDay AND :endEpochDay ORDER BY epoch_day ASC")
    suspend fun getConsultationsInRange(startEpochDay: Long, endEpochDay: Long): List<ConsultationEntity>
}
