package com.drnitz.aro.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drnitz.aro.data.db.entity.CustomExerciseEntity
import com.drnitz.aro.data.db.entity.toCustomEntity
import com.drnitz.aro.data.db.entity.toExercise
import com.drnitz.aro.data.db.entity.toFavoriteEntity
import com.drnitz.aro.domain.model.Exercise
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExerciseLibraryDaoTest : DatabaseTestBase() {

    private val dao get() = db.exerciseLibraryDao()

    @Test
    fun customExercises_upsertRetrieveAndDelete() = runBlocking {
        val exercise = Exercise(
            id = "custom-1",
            name = "Belt Squat Machine",
            force = "push",
            level = "intermediate",
            mechanic = "compound",
            equipment = "machine",
            primaryMuscles = listOf("quadriceps", "glutes"),
            secondaryMuscles = listOf("calves"),
            instructions = listOf("Step on platform", "Squat down with belt loaded"),
            category = "strength",
            tips = listOf("Keep torso upright"),
            met = 6.0,
            isCustom = true,
        )

        dao.upsertCustomExercise(exercise.toCustomEntity())

        val stored = dao.findCustomExercise("custom-1")
        assertNotNull(stored)
        val domain = stored!!.toExercise()
        assertEquals("Belt Squat Machine", domain.name)
        assertEquals(listOf("quadriceps", "glutes"), domain.primaryMuscles)
        assertEquals(listOf("calves"), domain.secondaryMuscles)
        assertEquals(listOf("Step on platform", "Squat down with belt loaded"), domain.instructions)
        assertEquals(listOf("Keep torso upright"), domain.tips)
        assertEquals(6.0, domain.met ?: 0.0, 0.001)
        assertTrue(domain.isCustom)

        val list = dao.observeCustomExercises().first()
        assertEquals(1, list.size)

        dao.deleteCustomExercise("custom-1")
        assertNull(dao.findCustomExercise("custom-1"))
        assertTrue(dao.observeCustomExercises().first().isEmpty())
    }

    @Test
    fun favoriteExercises_toggleAndObserve() = runBlocking {
        val bench = Exercise(
            id = "bench-press",
            name = "Barbell Bench Press",
            category = "strength",
            primaryMuscles = listOf("chest"),
            equipment = "barbell",
        )

        assertFalse(dao.isFavoriteExercise("bench-press"))

        // Toggle on
        dao.toggleFavoriteExercise(bench.toFavoriteEntity())
        assertTrue(dao.isFavoriteExercise("bench-press"))

        val favIds = dao.observeFavoriteExerciseIds().first()
        assertTrue(favIds.contains("bench-press"))

        // Toggle off
        dao.toggleFavoriteExercise(bench.toFavoriteEntity())
        assertFalse(dao.isFavoriteExercise("bench-press"))
        assertFalse(dao.observeFavoriteExerciseIds().first().contains("bench-press"))
    }
}
