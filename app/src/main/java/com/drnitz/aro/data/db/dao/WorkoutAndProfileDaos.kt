package com.drnitz.aro.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.drnitz.aro.data.db.entity.FastingSessionEntity
import com.drnitz.aro.data.db.entity.ProfileEntity
import com.drnitz.aro.data.db.entity.WorkoutEntity
import com.drnitz.aro.data.db.entity.WorkoutSetEntity
import com.drnitz.aro.data.db.entity.WorkoutWithSets
import kotlinx.coroutines.flow.Flow

/**
 * The single-row profile table.
 *
 * Reads are [Flow]s so the rings re-render the moment the target changes; writes are a
 * single [Upsert] against the pinned [ProfileEntity.SINGLETON_ID].
 */
@Dao
interface ProfileDao {

    @Query("SELECT * FROM profile WHERE id = :id LIMIT 1")
    fun observeProfile(id: Int = ProfileEntity.SINGLETON_ID): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: Int = ProfileEntity.SINGLETON_ID): ProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("DELETE FROM profile")
    suspend fun clear()
}

/**
 * Workouts and their working sets.
 *
 * Sets are written with the parent in one transaction, so a partially-saved session is
 * impossible; and reads use Room's `@Relation`, so a day's sessions and all their sets
 * arrive in one observed stream instead of a query per workout.
 */
@Dao
interface WorkoutDao {

    @Transaction
    @Query("SELECT * FROM workouts WHERE epoch_day = :epochDay ORDER BY logged_at ASC")
    fun observeWorkoutsForDay(epochDay: Long): Flow<List<WorkoutWithSets>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE epoch_day = :epochDay ORDER BY logged_at ASC")
    suspend fun workoutsForDay(epochDay: Long): List<WorkoutWithSets>

    @Query("SELECT * FROM workouts WHERE epoch_day BETWEEN :from AND :to ORDER BY logged_at ASC")
    fun observeWorkoutsBetween(from: Long, to: Long): Flow<List<WorkoutEntity>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE epoch_day BETWEEN :from AND :to ORDER BY logged_at ASC")
    fun observeWorkoutsWithSetsBetween(from: Long, to: Long): Flow<List<WorkoutWithSets>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE epoch_day BETWEEN :from AND :to ORDER BY logged_at ASC")
    suspend fun workoutsWithSetsBetween(from: Long, to: Long): List<WorkoutWithSets>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id LIMIT 1")
    suspend fun findWorkout(id: String): WorkoutWithSets?

    @Transaction
    @Query("SELECT * FROM workouts WHERE exercise_name = :exerciseName ORDER BY logged_at DESC LIMIT 1")
    suspend fun findLastWorkoutForExercise(exerciseName: String): WorkoutWithSets?

    /**
     * Saves a session and its sets atomically, stamping the new sets with the workout's
     * id so the caller never has to manage that foreign key itself.
     */
    @Transaction
    suspend fun insertWorkoutWithSets(workout: WorkoutEntity, sets: List<WorkoutSetEntity>) {
        upsertWorkout(workout)
        if (sets.isNotEmpty()) {
            deleteSetsForWorkout(workout.id)
            insertSets(sets.map { it.copy(workoutId = workout.id) })
        }
    }

    @Upsert
    suspend fun upsertWorkout(workout: WorkoutEntity)

    @Upsert
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Query("DELETE FROM workout_sets WHERE workout_id = :workoutId")
    suspend fun deleteSetsForWorkout(workoutId: String): Int

    /** Sets are removed by the CASCADE on `workout_sets.workout_id`. */
    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: String): Int

    @Query("SELECT COUNT(*) FROM workout_sets WHERE workout_id = :workoutId")
    suspend fun countSetsForWorkout(workoutId: String): Int
}

/**
 * Intermittent fasting sessions.
 *
 * The active session is identified by `ended_at IS NULL` rather than by an in-memory
 * flag, so the Task 9 timer recovers correctly after the process is killed mid-fast.
 */
@Dao
interface FastingDao {

    @Query("SELECT * FROM fasting_sessions WHERE epoch_day = :epochDay ORDER BY started_at DESC")
    fun observeSessionsForDay(epochDay: Long): Flow<List<FastingSessionEntity>>

    @Query("SELECT * FROM fasting_sessions WHERE ended_at IS NULL ORDER BY started_at DESC LIMIT 1")
    fun observeActiveSession(): Flow<FastingSessionEntity?>

    @Query("SELECT * FROM fasting_sessions WHERE ended_at IS NULL ORDER BY started_at DESC LIMIT 1")
    suspend fun activeSession(): FastingSessionEntity?

    @Upsert
    suspend fun upsertSession(session: FastingSessionEntity)

    /** Undo path: keeps the original row only when it is genuinely missing. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessionIfAbsent(session: FastingSessionEntity): Long

    /** Edits a completed session; the running session is intentionally excluded. */
    @Query(
        "UPDATE fasting_sessions SET epoch_day = :epochDay, target_hours = :targetHours, " +
            "started_at = :startedAt, ended_at = :endedAt, note = :note " +
            "WHERE id = :id AND ended_at IS NOT NULL",
    )
    suspend fun updateCompletedSession(
        id: String,
        epochDay: Long,
        targetHours: Int,
        startedAt: Long,
        endedAt: Long,
        note: String?,
    ): Int

    @Query("UPDATE fasting_sessions SET ended_at = :endedAt WHERE id = :id")
    suspend fun endSession(id: String, endedAt: Long): Int

    /**
     * Ends any session still open, then opens a new one — in one transaction, so two
     * fasts can never be open at once after a crash between the two statements.
     */
    @Transaction
    suspend fun startSession(session: FastingSessionEntity) {
        closeAllOpenSessions(session.startedAt)
        upsertSession(session)
    }

    @Query("UPDATE fasting_sessions SET ended_at = :endedAt WHERE ended_at IS NULL")
    suspend fun closeAllOpenSessions(endedAt: Long): Int

    /** History rows can be removed; the running session cannot. */
    @Query("DELETE FROM fasting_sessions WHERE id = :id AND ended_at IS NOT NULL")
    suspend fun deleteCompletedSession(id: String): Int

    @Query("SELECT * FROM fasting_sessions ORDER BY started_at DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int): Flow<List<FastingSessionEntity>>
}