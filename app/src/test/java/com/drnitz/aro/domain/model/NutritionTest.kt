package com.drnitz.aro.domain.model

import com.drnitz.aro.domain.assertClose
import com.drnitz.aro.domain.calculation.energyBalance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionTest {

    // ---- NutrientAmount ---------------------------------------------------

    @Test
    fun `unknown is not zero`() {
        assertFalse(NutrientAmount.UNKNOWN.isReported)
        assertEquals(0.0, NutrientAmount.UNKNOWN.orZero, 0.0001)
        assertEquals("—", NutrientAmount.UNKNOWN.format())
    }

    @Test
    fun `formatting keeps whole numbers clean and rounds fractions`() {
        assertEquals("42", NutrientAmount(42.0).format())
        assertEquals("42.5", NutrientAmount(42.5).format())
    }

    @Test
    fun `summing only unknowns stays unknown`() {
        val sum = NutrientAmount.sum(listOf(NutrientAmount.UNKNOWN, NutrientAmount.UNKNOWN))
        assertFalse(sum.isReported)
    }

    @Test
    fun `summing with a gap is flagged estimated rather than pretending complete`() {
        val sum = NutrientAmount.sum(listOf(NutrientAmount(10.0), NutrientAmount.UNKNOWN, NutrientAmount(5.0)))
        assertEquals(15.0, sum.value!!, 0.0001)
        assertTrue(sum.isEstimated)
    }

    @Test
    fun `summing complete reported values is exact and not estimated`() {
        val sum = NutrientAmount.sum(listOf(NutrientAmount(10.0), NutrientAmount(5.0)))
        assertEquals(15.0, sum.value!!, 0.0001)
        assertFalse(sum.isEstimated)
    }

    @Test
    fun `empty sum is unknown not zero`() {
        assertFalse(NutrientAmount.sum(emptyList()).isReported)
    }

    @Test
    fun `adding an unreported amount marks the result estimated`() {
        val sum = NutrientAmount(10.0) + NutrientAmount.UNKNOWN
        assertEquals(10.0, sum.value!!, 0.0001)
        assertTrue(sum.isEstimated)
    }

    @Test
    fun `progress toward zero target is zero not infinity`() {
        assertEquals(0.0, NutrientAmount(100.0).progressToward(0.0), 0.0001)
        assertClose(0.5, NutrientAmount(50.0).progressToward(100.0))
    }

    // ---- NutrientTotals ---------------------------------------------------

    @Test
    fun `an absent nutrient reads as unknown`() {
        val totals = NutrientTotals.of(NutrientKey.PROTEIN to 30.0)
        assertFalse(totals[NutrientKey.POTASSIUM].isReported)
        assertEquals(0.0, totals.potassiumMg, 0.0001)
    }

    @Test
    fun `totals merge across the union of keys`() {
        val chicken = NutrientTotals.of(NutrientKey.PROTEIN to 30.0, NutrientKey.SODIUM to 60.0)
        val oats = NutrientTotals.of(NutrientKey.PROTEIN to 5.0, NutrientKey.POTASSIUM to 60.0)
        val sum = chicken + oats
        assertEquals(35.0, sum.proteinG, 0.0001)
        assertEquals(60.0, sum.sodiumMg, 0.0001)
        assertEquals(60.0, sum.potassiumMg, 0.0001)
        assertEquals(
            setOf(NutrientKey.PROTEIN, NutrientKey.SODIUM, NutrientKey.POTASSIUM),
            sum.reportedKeys(),
        )
        // Sodium was only reported by one of the two foods, so the total is an under-count.
        assertTrue(sum[NutrientKey.SODIUM].isEstimated)
    }

    @Test
    fun `scaling rescales every nutrient for a portion change`() {
        val per100g = NutrientTotals.of(NutrientKey.CALORIES to 71.0, NutrientKey.PROTEIN to 2.5)
        val scaled = per100g.scaledToGrams(150.0)
        assertClose(106.5, scaled.calories)
        assertClose(3.75, scaled.proteinG)
    }

    @Test
    fun `energy from macros follows atwater factors`() {
        val totals = NutrientTotals.of(
            NutrientKey.PROTEIN to 30.0,   // 120 kcal
            NutrientKey.CARBS to 40.0,     // 160 kcal
            NutrientKey.FAT to 10.0,       // 90 kcal
        )
        assertClose(370.0, totals.energyFromMacros)
    }

    @Test
    fun `derived calories fill in only when the source omitted them`() {
        val macrosOnly = NutrientTotals.of(NutrientKey.PROTEIN to 25.0, NutrientKey.CARBS to 25.0)
        val derived = NutrientTotals.withDerivedCalories(macrosOnly)
        assertTrue(derived[NutrientKey.CALORIES].isReported)
        assertTrue(derived[NutrientKey.CALORIES].isEstimated)
        assertClose(200.0, derived.calories)

        val labeled = NutrientTotals.of(NutrientKey.CALORIES to 250.0, NutrientKey.PROTEIN to 25.0)
        assertClose(250.0, NutrientTotals.withDerivedCalories(labeled).calories)
    }

    @Test
    fun `multiplying totals keeps unreported nutrients unreported`() {
        val totals = NutrientTotals.of(NutrientKey.PROTEIN to 10.0)
        val scaled = totals * 2.0
        assertEquals(20.0, scaled.proteinG, 0.0001)
        assertFalse(scaled[NutrientKey.SODIUM].isReported)
    }

    // ---- MacroTargets -----------------------------------------------------

    @Test
    fun `progress fractions feed the concentric rings`() {
        val targets = MacroTargets(2200, 160, 220, 70, 31)
        val totals = NutrientTotals.of(
            NutrientKey.CALORIES to 1100.0,
            NutrientKey.PROTEIN to 80.0,
            NutrientKey.CARBS to 330.0,
            NutrientKey.FAT to 35.0,
        )
        val progress = targets.progress(totals)
        assertClose(0.5, progress.calories)
        assertClose(0.5, progress.protein)
        assertClose(1.5, progress.carbs) // over target is allowed through
        assertClose(0.5, progress.fat)
    }

    @Test
    fun `progress against a zero target degrades to zero`() {
        val progress = MacroTargets.EMPTY.progress(NutrientTotals.of(NutrientKey.PROTEIN to 100.0))
        assertEquals(0.0, progress.protein, 0.0001)
    }

    @Test
    fun `macro energy is reported for display`() {
        assertEquals(2201, MacroTargets(2200, 220, 193, 61, 31).energyFromMacros)
    }

    // ---- EnergyBalance ----------------------------------------------------

    @Test
    fun `energy balance edge cases`() {
        assertEquals(1100, energyBalance(2200, 1420, 320).remaining)
        assertEquals(0, energyBalance(2000, 2500, 100).remaining)
        // net consumed 2400 against a 2000 target -> 400 kcal over
        assertEquals(400, energyBalance(2000, 2500, 100).surplus)
        assertEquals(0, energyBalance(-100, 0, 0).targetCalories)
    }

    // ---- Enums and tokens -------------------------------------------------

    @Test
    fun `upper limit nutrients are the ones where more is worse`() {
        assertTrue(NutrientKey.SODIUM.isUpperLimit)
        assertTrue(NutrientKey.SATURATED_FAT.isUpperLimit)
        assertFalse(NutrientKey.POTASSIUM.isUpperLimit)
        assertEquals(listOf(NutrientKey.PROTEIN, NutrientKey.CARBS, NutrientKey.FAT), NutrientKey.MACRO_KEYS)
    }

    @Test
    fun `atwater factors are the textbook values`() {
        assertEquals(4.0, EnergyFactors.KCAL_PER_G_PROTEIN, 0.0001)
        assertEquals(4.0, EnergyFactors.KCAL_PER_G_CARBOHYDRATE, 0.0001)
        assertEquals(9.0, EnergyFactors.KCAL_PER_G_FAT, 0.0001)
        assertEquals(14.0, EnergyFactors.FIBER_G_PER_1000_KCAL, 0.0001)
    }

    @Test
    fun `meal slots are ordered breakfast to snacks`() {
        assertEquals(
            listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER, MealSlot.SNACKS),
            MealSlot.ORDERED,
        )
    }
}