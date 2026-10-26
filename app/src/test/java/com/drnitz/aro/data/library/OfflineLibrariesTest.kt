package com.drnitz.aro.data.library

import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientKey
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.InputStream

class OfflineLibrariesTest {

    private fun resolveAsset(fileName: String): InputStream {
        val candidates = listOf(
            "src/main/assets/$fileName",
            "app/src/main/assets/$fileName",
            "android/app/src/main/assets/$fileName",
        )
        var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (dir != null) {
            for (candidate in candidates) {
                val f = File(dir, candidate)
                if (f.exists()) return f.inputStream()
            }
            dir = dir.parentFile
        }
        throw AssertionError("Could not find asset $fileName from ${System.getProperty("user.dir")}")
    }

    @Test
    fun exerciseLibrary_loadsAllExercisesAndPerformsSearch() = runTest {
        val library = OfflineExerciseLibrary { resolveAsset("exercises.json") }
        val exercises = library.getExercises()

        assertTrue("Expected 800+ exercises", exercises.size >= 870)

        // Find Bench Press
        val benchPress = library.search(query = "Bench Press", equipment = "barbell")
        assertTrue(benchPress.isNotEmpty())
        assertTrue(benchPress.any { it.name.contains("Bench Press", ignoreCase = true) })

        // Muscle filter
        val chestExercises = library.search(muscle = "chest")
        assertTrue(chestExercises.isNotEmpty())
        assertTrue(chestExercises.all { ex ->
            ex.primaryMuscles.any { it.equals("chest", ignoreCase = true) } ||
                ex.secondaryMuscles.any { it.equals("chest", ignoreCase = true) }
        })

        // Distinct muscles and categories
        val muscles = library.getMuscles()
        assertTrue(muscles.contains("chest"))
        assertTrue(muscles.contains("biceps"))

        val categories = library.getCategories()
        assertTrue(categories.contains("strength"))
    }

    @Test
    fun foodLibrary_loadsCuratedFoodsAndScalesPortions() = runTest {
        val library = OfflineFoodLibrary { resolveAsset("curated_foods.json") }
        val foods = library.getCuratedFoods()

        assertTrue("Expected 30+ curated staple foods", foods.size >= 35)

        // Find Chicken Breast
        val chicken = library.search(query = "Chicken Breast")
        assertTrue(chicken.isNotEmpty())
        val item = chicken.first()
        assertEquals("curated-chicken-breast", item.id)
        assertEquals(165.0, item.calories, 0.01)
        assertEquals(31.0, item.protein, 0.01)

        // Portion scaling: 200g of chicken should yield 330 kcal and 62g protein
        val logEntry = item.toFoodLogEntry(meal = MealSlot.LUNCH, grams = 200.0)
        assertEquals(200.0, logEntry.grams, 0.01)
        assertEquals(330.0, logEntry.calories, 0.01)
        assertEquals(62.0, logEntry.proteinG, 0.01)

        // Chicken has no potassium entry in the bundled library. That must stay
        // "unreported" rather than silently becoming a known zero.
        assertFalse(item.isReported(NutrientKey.POTASSIUM))
        assertNull(logEntry.nutrients[NutrientKey.POTASSIUM].value)
        assertEquals(0.0, logEntry.nutrients[NutrientKey.POTASSIUM].orZero, 0.0)

        // Barcode lookup
        val byBarcode = library.findByBarcode("raw-chicken-breast")
        assertNotNull(byBarcode)
        assertEquals("curated-chicken-breast", byBarcode?.id)
    }
}
