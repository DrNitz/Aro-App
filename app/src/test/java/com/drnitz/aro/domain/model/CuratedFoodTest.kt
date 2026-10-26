package com.drnitz.aro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CuratedFoodTest {

    private val chicken = CuratedFoodItem(
        id = "curated-chicken-breast",
        name = "Chicken Breast",
        calories = 165.0,
        protein = 31.0,
        carbs = 0.0,
        fat = 3.6,
        fiber = 0.0,
        sugar = 0.0,
        sodium = 74.0,
        unreportedNutrients = setOf("POTASSIUM", "CALCIUM", "MAGNESIUM", "SATURATED_FAT"),
    )

    @Test
    fun `unreported nutrient stays null instead of pretending to be zero`() {
        val totals = chicken.per100gNutrients

        assertNull(totals[NutrientKey.POTASSIUM].value)
        assertEquals(0.0, totals[NutrientKey.POTASSIUM].orZero, 0.0)
        assertFalse(totals[NutrientKey.POTASSIUM].isReported)

        // A reported zero (chicken has no carbs) is still a known value.
        assertEquals(0.0, totals[NutrientKey.CARBS].value!!, 0.0)
        assertTrue(totals[NutrientKey.CARBS].isReported)
    }

    @Test
    fun `scaling a portion preserves unreported status`() {
        val entry = chicken.toFoodLogEntry(MealSlot.DINNER, grams = 172.0)
        val totals = entry.nutrients

        assertEquals(283.8, entry.calories, 0.01)
        assertEquals(53.32, entry.proteinG, 0.01)
        assertNull(totals[NutrientKey.POTASSIUM].value)
        assertNull(totals[NutrientKey.SATURATED_FAT].value)
        assertTrue(totals.hasAnyUnknown)
    }

    @Test
    fun `derived energy is marked estimated rather than unreported`() {
        val bar = CuratedFoodItem(
            id = "bar",
            name = "Protein Bar",
            calories = 330.0,
            protein = 20.0,
            carbs = 40.0,
            fat = 10.0,
            estimatedNutrients = setOf("CALORIES"),
            unreportedNutrients = setOf("FIBER", "SUGAR", "SODIUM", "POTASSIUM", "CALCIUM", "MAGNESIUM"),
        )

        val calories = bar.per100gNutrients[NutrientKey.CALORIES]
        assertTrue(calories.isReported)
        assertTrue(calories.isEstimated)
        assertEquals(330.0, bar.atwaterCalories, 0.01)
        assertEquals(0.0, bar.energyLabelDeltaPercent ?: 0.0, 0.01)
    }

    @Test
    fun `energy cross-check stays null when a macro is unreported`() {
        val partial = CuratedFoodItem(
            id = "partial",
            name = "Partial Label",
            calories = 200.0,
            protein = 10.0,
            carbs = 20.0,
            fat = 0.0,
            unreportedNutrients = setOf("FAT"),
        )

        assertNull(partial.energyLabelDeltaPercent)
    }

    @Test
    fun `default serving drives both the card preview and the log entry`() {
        val oats = CuratedFoodItem(
            id = "oats",
            name = "Oats",
            calories = 389.0,
            protein = 16.9,
            carbs = 66.3,
            fat = 6.9,
            commonServing = CommonServing("1/2 cup dry", 40.0),
        )

        assertEquals(40.0, oats.defaultServingGrams, 0.0)
        assertEquals(155.6, oats.valuePerServing(NutrientKey.CALORIES), 0.01)
        assertEquals(155.6, oats.toFoodLogEntry(MealSlot.BREAKFAST).calories, 0.01)
    }
}
