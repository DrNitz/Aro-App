package com.drnitz.aro.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.drnitz.aro.data.db.entity.CustomExerciseEntity
import com.drnitz.aro.data.db.entity.FavoriteExerciseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Custom exercises and favorite bookmarks.
 */
@Dao
interface ExerciseLibraryDao {

    // ---- Custom exercises --------------------------------------------------

    @Query("SELECT * FROM custom_exercises ORDER BY name COLLATE NOCASE ASC")
    fun observeCustomExercises(): Flow<List<CustomExerciseEntity>>

    @Query("SELECT * FROM custom_exercises ORDER BY name COLLATE NOCASE ASC")
    suspend fun getCustomExercises(): List<CustomExerciseEntity>

    @Query("SELECT * FROM custom_exercises WHERE id = :id LIMIT 1")
    suspend fun findCustomExercise(id: String): CustomExerciseEntity?

    @Upsert
    suspend fun upsertCustomExercise(exercise: CustomExerciseEntity)

    @Query("DELETE FROM custom_exercises WHERE id = :id")
    suspend fun deleteCustomExercise(id: String): Int

    // ---- Favorite exercises ------------------------------------------------

    @Query("SELECT * FROM favorite_exercises ORDER BY created_at DESC")
    fun observeFavoriteExercises(): Flow<List<FavoriteExerciseEntity>>

    @Query("SELECT exercise_id FROM favorite_exercises ORDER BY created_at DESC")
    fun observeFavoriteExerciseIds(): Flow<List<String>>

    @Query("SELECT exercise_id FROM favorite_exercises ORDER BY created_at DESC")
    suspend fun getFavoriteExerciseIds(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_exercises WHERE exercise_id = :exerciseId)")
    suspend fun isFavoriteExercise(exerciseId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_exercises WHERE exercise_id = :exerciseId)")
    fun observeIsFavorite(exerciseId: String): Flow<Boolean>

    @Upsert
    suspend fun upsertFavoriteExercise(favorite: FavoriteExerciseEntity)

    @Query("DELETE FROM favorite_exercises WHERE exercise_id = :exerciseId")
    suspend fun deleteFavoriteExercise(exerciseId: String): Int

    @Transaction
    suspend fun toggleFavoriteExercise(favorite: FavoriteExerciseEntity) {
        val deleted = deleteFavoriteExercise(favorite.exerciseId)
        if (deleted == 0) {
            upsertFavoriteExercise(favorite)
        }
    }
}
