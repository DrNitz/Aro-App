package com.drnitz.aro.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateCalculatorTest {

    @Test
    fun `empty bar when target weight equals bar weight`() {
        val calc = PlateCalculator.calculate(targetWeightKg = 20.0, barWeightKg = 20.0)
        assertTrue(calc.isExact)
        assertEquals(0.0, calc.weightPerSideKg, 0.001)
        assertTrue(calc.platesPerSide.isEmpty())
        assertEquals("Empty bar", calc.sideSummary)
    }

    @Test
    fun `empty bar when target weight is less than bar weight`() {
        val calc = PlateCalculator.calculate(targetWeightKg = 15.0, barWeightKg = 20.0)
        assertTrue(calc.isExact)
        assertEquals(0.0, calc.weightPerSideKg, 0.001)
        assertTrue(calc.platesPerSide.isEmpty())
    }

    @Test
    fun `100kg with 20kg bar allocates 40kg per side (one 25kg, one 15kg)`() {
        // (100 - 20) / 2 = 40kg per side -> 25kg + 15kg
        val calc = PlateCalculator.calculate(targetWeightKg = 100.0, barWeightKg = 20.0)
        assertTrue(calc.isExact)
        assertEquals(40.0, calc.weightPerSideKg, 0.001)
        assertEquals(100.0, calc.achievedTotalWeightKg, 0.001)
        assertEquals(2, calc.platesPerSide.size)
        assertEquals(25.0, calc.platesPerSide[0].weightKg, 0.001)
        assertEquals(1, calc.platesPerSide[0].countPerSide)
        assertEquals(15.0, calc.platesPerSide[1].weightKg, 0.001)
        assertEquals(1, calc.platesPerSide[1].countPerSide)
        assertEquals("1 × 25kg, 1 × 15kg", calc.sideSummary)
    }

    @Test
    fun `60kg with 20kg bar allocates 20kg per side (one 20kg)`() {
        val calc = PlateCalculator.calculate(targetWeightKg = 60.0, barWeightKg = 20.0)
        assertTrue(calc.isExact)
        assertEquals(20.0, calc.weightPerSideKg, 0.001)
        assertEquals(1, calc.platesPerSide.size)
        assertEquals(20.0, calc.platesPerSide[0].weightKg, 0.001)
        assertEquals(1, calc.platesPerSide[0].countPerSide)
        assertEquals("1 × 20kg", calc.sideSummary)
    }

    @Test
    fun `142_5kg with 20kg bar calculates exact plates including 1_25kg plates`() {
        // (142.5 - 20) / 2 = 61.25kg per side -> 2x25kg (50kg) + 1x10kg (10kg) + 1x1.25kg (1.25kg)
        val calc = PlateCalculator.calculate(targetWeightKg = 142.5, barWeightKg = 20.0)
        assertTrue(calc.isExact)
        assertEquals(61.25, calc.weightPerSideKg, 0.001)
        assertEquals(142.5, calc.achievedTotalWeightKg, 0.001)
        assertEquals(0.0, calc.remainderKg, 0.001)

        val pairs = calc.platesPerSide
        assertEquals(3, pairs.size)
        assertEquals(25.0, pairs[0].weightKg, 0.001)
        assertEquals(2, pairs[0].countPerSide)
        assertEquals(10.0, pairs[1].weightKg, 0.001)
        assertEquals(1, pairs[1].countPerSide)
        assertEquals(1.25, pairs[2].weightKg, 0.001)
        assertEquals(1, pairs[2].countPerSide)
    }

    @Test
    fun `unmatched weight reports remainder correctly`() {
        // 61 kg with 20kg bar = 41kg total plates = 20.5kg per side.
        // Standard plates: 20kg + (0.5kg leftover per side = 1kg total remainder)
        val calc = PlateCalculator.calculate(targetWeightKg = 61.0, barWeightKg = 20.0)
        assertFalse(calc.isExact)
        assertEquals(60.0, calc.achievedTotalWeightKg, 0.001)
        assertEquals(1.0, calc.remainderKg, 0.001)
    }

    @Test
    fun `custom bar weight support (15kg women's bar)`() {
        // Target: 65kg, Bar: 15kg -> 50kg plates -> 25kg per side (one 25kg)
        val calc = PlateCalculator.calculate(targetWeightKg = 65.0, barWeightKg = PlateCalculator.WOMENS_BAR_KG)
        assertTrue(calc.isExact)
        assertEquals(25.0, calc.weightPerSideKg, 0.001)
        assertEquals(1, calc.platesPerSide.size)
        assertEquals(25.0, calc.platesPerSide[0].weightKg, 0.001)
    }
}
