package com.drnitz.aro.data.db

import com.drnitz.aro.data.db.entity.CustomFoodEntity
import com.drnitz.aro.data.db.entity.FoodLogEntity
import com.drnitz.aro.domain.model.NutrientAmount
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The nutrient columns are the hinge between the database and the clinical audit, so
 * this proves the mapping is **complete** (every [NutrientKey] has a column) and
 * **lossless** (a null column stays null).
 */
class NutrientColumnsMappingTest {

    private fun foodEntity(
        calories: Double? = 248.0,
        protein: Double? = 46.5,
        carbs: Double? = 0.0,
        fat: Double? = 5.4,
        saturatedFat: Double? = 1.5,
        fiber: Double? = null,
        sugar: Double? = null,
        sodium: Double? = 74.0,
        potassium: Double? = null,
        calcium: Double? = 15.0,
        magnesium: Double? = 36.0,
    ) = FoodLogEntity(
        id = "f1",
        epochDay = LocalDate.of(2026, 10, 18).toEpochDay(),
        mealSlot = com.drnitz.aro.domain.model.MealSlot.LUNCH,
        name = "Chicken Breast",
        brand = null,
        foodId = "off-1234",
        grams = 150.0,
        isCustom = false,
        createdAt = 1_000L,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        saturatedFat = saturatedFat,
        fiber = fiber,
        sugar = sugar,
        sodium = sodium,
        potassium = potassium,
        calcium = calcium,
        magnesium = magnesium,
    )

    @Test
    fun `every nutrient key maps to a column`() {
        val totals = NutrientTotals.ofAmounts(
            NutrientKey.entries.associateWith { NutrientAmount(it.ordinal * 1.0 + 0.5) },
        )
        val entity = FoodLogEntity(
            id = "f",
            epochDay = 0,
            mealSlot = com.drnitz.aro.domain.model.MealSlot.DINNER,
            name = "n",
            brand = null,
            foodId = null,
            grams = 100.0,
            isCustom = false,
            createdAt = 0,
            calories = totals[NutrientKey.CALORIES].value,
            protein = totals[NutrientKey.PROTEIN].value,
            carbs = totals[NutrientKey.CARBS].value,
            fat = totals[NutrientKey.FAT].value,
            saturatedFat = totals[NutrientKey.SATURATED_FAT].value,
            fiber = totals[NutrientKey.FIBER].value,
            sugar = totals[NutrientKey.SUGAR].value,
            sodium = totals[NutrientKey.SODIUM].value,
            potassium = totals[NutrientKey.POTASSIUM].value,
            calcium = totals[NutrientKey.CALCIUM].value,
            magnesium = totals[NutrientKey.MAGNESIUM].value,
        )
        for (key in NutrientKey.entries) {
            assertEquals("${key} did not round-trip", totals[key].value!!, entity.read(key)!!, 0.0001)
        }
    }

    @Test
    fun `null columns stay unreported`() {
        val totals = foodEntity().toNutrientTotals()
        assertFalse(totals[NutrientKey.FIBER].isReported)
        assertFalse(totals[NutrientKey.SUGAR].isReported)
        assertFalse(totals[NutrientKey.POTASSIUM].isReported)
        assertNull(totals[NutrientKey.FIBER].value)
        assertNull(totals[NutrientKey.POTASSIUM].value)
    }

    @Test
    fun `a reported zero is not the same as unreported`() {
        val totals = foodEntity().toNutrientTotals()
        assertTrue(totals[NutrientKey.CARBS].isReported)
        assertEquals(0.0, totals[NutrientKey.CARBS].value!!, 0.0001)
    }

    @Test
    fun `read and toNutrientTotals agree for reported values`() {
        val entity = foodEntity()
        val totals = entity.toNutrientTotals()
        assertEquals(248.0, totals[NutrientKey.CALORIES].value!!, 0.0001)
        assertEquals(46.5, totals[NutrientKey.PROTEIN].value!!, 0.0001)
        assertEquals(74.0, totals[NutrientKey.SODIUM].value!!, 0.0001)
        assertEquals(NutrientKey.entries.size, totals.reportedKeys().size + 3)
    }

    @Test
    fun `custom foods share the same column contract`() {
        val custom = CustomFoodEntity(
            id = "c1",
            name = "Homemade granola",
            brand = null,
            defaultGrams = 50.0,
            createdAt = 0,
            calories = 250.0,
            protein = 8.0,
            carbs = 30.0,
            fat = 10.0,
            saturatedFat = null,
            fiber = 4.0,
            sugar = null,
            sodium = 120.0,
            potassium = null,
            calcium = null,
            magnesium = null,
        )
        val totals = custom.toNutrientTotals()
        assertEquals(250.0, totals.calories, 0.0001)
        assertFalse(totals[NutrientKey.SATURATED_FAT].isReported)
        assertEquals(setOf(NutrientKey.CALORIES, NutrientKey.PROTEIN, NutrientKey.CARBS, NutrientKey.FAT, NutrientKey.FIBER, NutrientKey.SODIUM), totals.reportedKeys())
    }

    @Test
    fun `an empty day projection reads as fully unreported`() {
        val empty = DayNutrientTotals(null, null, null, null, null, null, null, null, null, null, null)
        assertTrue(empty.isEmptyDay)
        assertEquals(emptySet<NutrientKey>(), empty.toNutrientTotals().reportedKeys())
    }

    @Test
    fun `a populated day projection carries its day`() {
        val row = DayTotalsRow(
            epochDay = LocalDate.of(2026, 10, 18).toEpochDay(),
            calories = 2000.0,
            protein = 150.0,
            carbs = 200.0,
            fat = 65.0,
            saturatedFat = null,
            fiber = 28.0,
            sugar = null,
            sodium = 2200.0,
            potassium = 3000.0,
            calcium = null,
            magnesium = null,
        )
        assertEquals(LocalDate.of(2026, 10, 18), row.date)
        assertFalse(row.toNutrientTotals()[NutrientKey.SUGAR].isReported)
        assertEquals(2000.0, row.calories!!, 0.0001)
    }
}