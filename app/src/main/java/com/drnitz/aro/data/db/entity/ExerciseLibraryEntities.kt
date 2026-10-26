package com.drnitz.aro.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.drnitz.aro.domain.model.Exercise

/**
 * A user-created exercise (e.g. gym-specific machine, club sport, custom movement).
 */
@Entity(
    tableName = "custom_exercises",
    indices = [Index(value = ["name"])],
)
data class CustomExerciseEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "force") val force: String? = null,
    @ColumnInfo(name = "level") val level: String = "beginner",
    @ColumnInfo(name = "mechanic") val mechanic: String? = null,
    @ColumnInfo(name = "equipment") val equipment: String? = null,
    @ColumnInfo(name = "primary_muscles") val primaryMuscles: String = "",
    @ColumnInfo(name = "secondary_muscles") val secondaryMuscles: String = "",
    @ColumnInfo(name = "instructions") val instructions: String = "",
    @ColumnInfo(name = "category") val category: String = "strength",
    @ColumnInfo(name = "images") val images: String = "",
    @ColumnInfo(name = "tips") val tips: String = "",
    @ColumnInfo(name = "met") val met: Double = 6.0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

/**
 * A starred favorite exercise for instant 1-tap routine filtering.
 */
@Entity(
    tableName = "favorite_exercises",
    indices = [Index(value = ["exercise_id"], unique = true)],
)
data class FavoriteExerciseEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "exercise_id") val exerciseId: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "category") val category: String = "strength",
    @ColumnInfo(name = "muscle") val muscle: String? = null,
    @ColumnInfo(name = "equipment") val equipment: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
)

fun CustomExerciseEntity.toExercise(): Exercise = Exercise(
    id = id,
    name = name,
    force = force,
    level = level,
    mechanic = mechanic,
    equipment = equipment,
    primaryMuscles = primaryMuscles.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    secondaryMuscles = secondaryMuscles.split(",").map { it.trim() }.filter { it.isNotEmpty() },
    instructions = instructions.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
    category = category,
    images = images.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
    tips = tips.split("\n").map { it.trim() }.filter { it.isNotEmpty() },
    met = met,
    isCustom = true,
)

fun Exercise.toCustomEntity(): CustomExerciseEntity = CustomExerciseEntity(
    id = id,
    name = name,
    force = force,
    level = level,
    mechanic = mechanic,
    equipment = equipment,
    primaryMuscles = primaryMuscles.joinToString(", "),
    secondaryMuscles = secondaryMuscles.joinToString(", "),
    instructions = instructions.joinToString("\n"),
    category = category,
    images = images.joinToString("\n"),
    tips = tips.joinToString("\n"),
    met = met ?: 6.0,
    createdAt = System.currentTimeMillis(),
)

fun Exercise.toFavoriteEntity(): FavoriteExerciseEntity = FavoriteExerciseEntity(
    id = "fav-ex-$id",
    exerciseId = id,
    name = name,
    category = category,
    muscle = primaryMuscles.firstOrNull(),
    equipment = equipment,
    createdAt = System.currentTimeMillis(),
)
