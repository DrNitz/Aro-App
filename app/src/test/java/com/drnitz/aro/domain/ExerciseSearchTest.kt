package com.drnitz.aro.domain

import com.drnitz.aro.data.library.OfflineExerciseLibrary
import com.drnitz.aro.domain.model.Exercise
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class ExerciseSearchTest {

    @Test
    fun `matchesQuery supports direct and alias matches`() {
        val bench = Exercise(
            id = "bb-bench",
            name = "Barbell Bench Press",
            category = "strength",
            equipment = "barbell",
            primaryMuscles = listOf("chest"),
            aliases = listOf("Bench Press", "Flat Bench", "BB Bench"),
            met = 6.0,
        )

        assertTrue(bench.matchesQuery("bench"))
        assertTrue(bench.matchesQuery("bb bench"))
        assertTrue(bench.matchesQuery("flat bench"))
        assertTrue(bench.matchesQuery("barbell"))
        assertTrue(bench.matchesQuery("chest"))
        assertTrue(bench.matchesQuery("pecs")) // Acronym synonym
        assertFalse(bench.matchesQuery("squat"))
    }

    @Test
    fun `matchesQuery expands common acronyms like rdl and ohp`() {
        val rdl = Exercise(
            id = "rdl",
            name = "Romanian Deadlift",
            category = "strength",
            equipment = "barbell",
            primaryMuscles = listOf("hamstrings"),
            secondaryMuscles = listOf("glutes"),
            met = 6.0,
        )
        assertTrue(rdl.matchesQuery("rdl"))
        assertTrue(rdl.matchesQuery("deadlift"))
        assertTrue(rdl.matchesQuery("hamstrings"))
        assertTrue(rdl.matchesQuery("hams"))

        val ohp = Exercise(
            id = "ohp",
            name = "Standing Military Press",
            category = "strength",
            equipment = "barbell",
            primaryMuscles = listOf("shoulders"),
            aliases = listOf("Overhead Press", "OHP"),
            met = 6.0,
        )
        assertTrue(ohp.matchesQuery("ohp"))
        assertTrue(ohp.matchesQuery("military press"))
        assertTrue(ohp.matchesQuery("delts"))
    }

    @Test
    fun `matchesQuery supports multi-token unordered queries`() {
        val dbBench = Exercise(
            id = "db-bench",
            name = "Incline Dumbbell Bench Press",
            category = "strength",
            equipment = "dumbbell",
            primaryMuscles = listOf("chest"),
            met = 6.0,
        )

        // Tokens in reversed order
        assertTrue(dbBench.matchesQuery("bench dumbbell"))
        assertTrue(dbBench.matchesQuery("db incline"))
        assertTrue(dbBench.matchesQuery("chest dumbbell press"))
        assertFalse(dbBench.matchesQuery("barbell squat"))
    }

    @Test
    fun `bundled exercises json parses cleanly with expanded library`() = runBlocking {
        // Load the actual assets file used in the APK
        val assetFile = File("src/main/assets/exercises.json").takeIf { it.exists() }
            ?: File("app/src/main/assets/exercises.json")

        assertTrue("Asset file must exist", assetFile.exists())

        val library = OfflineExerciseLibrary { FileInputStream(assetFile) }
        val all = library.getExercises()

        assertTrue("Library must contain over 1300 exercises", all.size >= 1300)

        // Ensure every exercise has non-empty ID, name, and valid MET rating
        all.forEach { ex ->
            assertTrue("Exercise ID must not be blank: ${ex.name}", ex.id.isNotBlank())
            assertTrue("Exercise name must not be blank", ex.name.isNotBlank())
            assertNotNull("MET must be defined for ${ex.name}", ex.met)
            assertTrue("MET must be positive for ${ex.name}", (ex.met ?: 0.0) > 0.0)
        }

        // Verify key movements exist
        assertNotNull(all.find { it.name.contains("Bench Press", ignoreCase = true) })
        assertNotNull(all.find { it.name.contains("Squat", ignoreCase = true) })
        assertNotNull(all.find { it.name.contains("Deadlift", ignoreCase = true) })
        assertNotNull(all.find { it.name.contains("Ab Wheel Rollout", ignoreCase = true) })
        assertNotNull(all.find { it.name.contains("Arnold Press", ignoreCase = true) })

        // Verify search functions correctly
        val benchResults = library.search(query = "bench")
        assertTrue(benchResults.isNotEmpty())

        val rdlResults = library.search(query = "rdl")
        assertTrue(rdlResults.isNotEmpty())

        val chestBarbell = library.search(muscle = "chest", equipment = "barbell")
        assertTrue(chestBarbell.isNotEmpty())
        assertTrue(chestBarbell.all { it.equipment.equals("barbell", ignoreCase = true) })

        // Test custom exercises inclusion in library search
        val custom = Exercise(
            id = "custom-special-press",
            name = "Special Custom Press",
            category = "strength",
            equipment = "machine",
            primaryMuscles = listOf("chest"),
            met = 6.0,
            isCustom = true,
        )
        val customResults = library.search(query = "special custom", customExercises = listOf(custom))
        assertEquals(1, customResults.size)
        assertEquals("Special Custom Press", customResults.first().name)
        assertTrue(customResults.first().isCustom)
    }
}
