package com.drnitz.aro.data.library

import android.content.Context
import com.drnitz.aro.domain.model.Exercise
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream

/**
 * 100% Offline Free Exercise DB Library (870+ exercises).
 * Lazily parsed once from assets and indexed in memory for sub-millisecond searches.
 */
class OfflineExerciseLibrary(
    private val assetStreamProvider: () -> InputStream,
) {
    constructor(context: Context) : this({ context.assets.open("exercises.json") })

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Volatile
    private var cachedExercises: List<Exercise>? = null

    /**
     * Loads and caches exercises in memory.
     */
    suspend fun getExercises(): List<Exercise> = cachedExercises ?: withContext(Dispatchers.IO) {
        cachedExercises ?: run {
            val list = assetStreamProvider().use { stream ->
                val content = stream.bufferedReader().readText()
                json.decodeFromString<List<Exercise>>(content)
            }
            cachedExercises = list
            list
        }
    }

    /**
     * Synchronous access if already loaded, or parses on current thread.
     */
    fun getExercisesBlocking(): List<Exercise> = cachedExercises ?: synchronized(this) {
        cachedExercises ?: run {
            val list = assetStreamProvider().use { stream ->
                val content = stream.bufferedReader().readText()
                json.decodeFromString<List<Exercise>>(content)
            }
            cachedExercises = list
            list
        }
    }

    suspend fun search(
        query: String = "",
        muscle: String? = null,
        equipment: String? = null,
        level: String? = null,
        category: String? = null,
    ): List<Exercise> {
        val all = getExercises()
        val cleanQ = query.trim().lowercase()
        val cleanMuscle = muscle?.trim()?.lowercase()?.takeIf { it != "all" }
        val cleanEq = equipment?.trim()?.lowercase()?.takeIf { it != "all" }
        val cleanLevel = level?.trim()?.lowercase()?.takeIf { it != "all" }
        val cleanCat = category?.trim()?.lowercase()?.takeIf { it != "all" }

        return all.filter { exercise ->
            val matchesQuery = cleanQ.isEmpty() || exercise.matchesQuery(cleanQ)
            val matchesMuscle = cleanMuscle == null ||
                exercise.primaryMuscles.any { it.lowercase() == cleanMuscle } ||
                exercise.secondaryMuscles.any { it.lowercase() == cleanMuscle }
            val matchesEquipment = cleanEq == null || exercise.equipment?.lowercase() == cleanEq
            val matchesLevel = cleanLevel == null || exercise.level.lowercase() == cleanLevel
            val matchesCategory = cleanCat == null || exercise.category.lowercase() == cleanCat

            matchesQuery && matchesMuscle && matchesEquipment && matchesLevel && matchesCategory
        }
    }

    suspend fun getById(id: String): Exercise? {
        return getExercises().find { it.id == id }
    }

    suspend fun getMuscles(): List<String> {
        return getExercises()
            .flatMap { it.primaryMuscles }
            .distinct()
            .sorted()
    }

    suspend fun getEquipments(): List<String> {
        return getExercises()
            .mapNotNull { it.equipment }
            .distinct()
            .sorted()
    }

    suspend fun getCategories(): List<String> {
        return getExercises()
            .map { it.category }
            .distinct()
            .sorted()
    }
}
