package com.drnitz.aro.domain.calculation

import com.drnitz.aro.domain.Fixtures
import com.drnitz.aro.domain.DomainResult
import com.drnitz.aro.domain.ValidationFailure
import com.drnitz.aro.domain.assertClose
import com.drnitz.aro.domain.failureOf
import com.drnitz.aro.domain.model.ActivityLevel
import com.drnitz.aro.domain.model.Goal
import com.drnitz.aro.domain.model.MacroPreset
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.domain.successOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnergyTest {

    // ---- BMR: Mifflin-St Jeor -------------------------------------------

    @Test
    fun `bmr male 26y 178cm 75kg is 1738`() {
        assertEquals(1738, successOf(basalMetabolicRate(Fixtures.MALE)))
    }

    @Test
    fun `bmr female 30y 165cm 60kg is 1320`() {
        assertEquals(1320, successOf(basalMetabolicRate(Fixtures.FEMALE)))
    }

    @Test
    fun `bmr uses the sex specific offset`() {
        val male = successOf(basalMetabolicRate(Fixtures.MALE))
        val female = successOf(basalMetabolicRate(Fixtures.MALE.copy(sex = com.drnitz.aro.domain.model.Sex.FEMALE)))
        assertEquals(166.0, (male - female).toDouble(), 1.0) // +5 vs -161
    }

    @Test
    fun `bmr rejects a zero weight instead of returning a placeholder`() {
        val result = basalMetabolicRate(Fixtures.MALE.copy(weightKg = 0.0))
        val failure = failureOf(result)
        assertEquals(ValidationFailure.OUT_OF_RANGE, failure.reason)
        assertTrue(failure.message.contains("Weight"))
    }

    @Test
    fun `bmr rejects an impossible age`() {
        assertEquals(
            ValidationFailure.OUT_OF_RANGE,
            failureOf(basalMetabolicRate(Fixtures.MALE.copy(ageYears = 0))).reason,
        )
    }

    @Test
    fun `bmr rejects a goal implying unsafe loss before doing any math`() {
        val failure = failureOf(
            basalMetabolicRate(Fixtures.MALE.copy(weightKg = 60.0, targetWeightKg = 20.0)),
        )
        assertTrue(failure.message.contains("Target"))
    }

    // ---- TDEE -------------------------------------------------------------

    @Test
    fun `tdee applies the moderate activity factor`() {
        assertEquals(2694, totalDailyEnergyExpenditure(1738, ActivityLevel.MODERATE))
    }

    @Test
    fun `tdee sedentary from 1738 is 2086`() {
        assertEquals(2086, totalDailyEnergyExpenditure(1738, ActivityLevel.SEDENTARY))
    }

    @Test
    fun `tdee extra active from 1738 is 3302`() {
        assertEquals(3302, totalDailyEnergyExpenditure(1738, ActivityLevel.EXTRA_ACTIVE))
    }

    @Test
    fun `tdee increases monotonically with activity`() {
        val order = ActivityLevel.entries
        val values = order.map { totalDailyEnergyExpenditure(1738, it) }
        assertEquals(values.sorted(), values)
        assertEquals(5, values.distinct().size)
    }

    // ---- Calorie target ---------------------------------------------------

    @Test
    fun `calorie target applies the goal offset`() {
        assertEquals(2444, dailyCalorieTarget(2694, Goal.MILD_CUT))
        assertEquals(2194, dailyCalorieTarget(2694, Goal.AGGRESSIVE_CUT))
        assertEquals(2694, dailyCalorieTarget(2694, Goal.MAINTAIN))
        assertEquals(3194, dailyCalorieTarget(2694, Goal.HYPERTROPHY_BULK))
    }

    @Test
    fun `calorie target never plans below the safe floor`() {
        assertEquals(MIN_SAFE_DAILY_CALORIES, dailyCalorieTarget(1300, Goal.AGGRESSIVE_CUT))
    }

    @Test
    fun `goal weekly change matches 7700 kcal per kg of fat`() {
        // A 500 kcal/day deficit is ~3500 kcal/week -> ~0.45 kg of fat per week, lost.
        assertClose(-0.45, Goal.AGGRESSIVE_CUT.weeklyChangeKg, 0.01)
        assertClose(0.45, Goal.HYPERTROPHY_BULK.weeklyChangeKg, 0.01)
        assertEquals(0.0, Goal.MAINTAIN.weeklyChangeKg, 0.001)
    }

    // ---- Macro targets ----------------------------------------------------

    @Test
    fun `high protein 2200 kcal gives 220 193 61`() {
        val targets = macroTargets(2200, MacroPreset.HIGH_PROTEIN)
        assertEquals(220, targets.proteinG)
        assertEquals(193, targets.carbsG)
        assertEquals(61, targets.fatG)
        assertEquals(2200, targets.calories)
    }

    @Test
    fun `balanced 2000 kcal gives 150 200 67`() {
        val targets = macroTargets(2000, MacroPreset.BALANCED)
        assertEquals(150, targets.proteinG)
        assertEquals(200, targets.carbsG)
        assertEquals(67, targets.fatG)
    }

    @Test
    fun `keto 2200 kcal gives 110 28 183`() {
        val targets = macroTargets(2200, MacroPreset.KETO)
        assertEquals(110, targets.proteinG)
        assertEquals(28, targets.carbsG)
        assertEquals(183, targets.fatG)
    }

    @Test
    fun `low carb 2000 kcal gives 150 100 111`() {
        val targets = macroTargets(2000, MacroPreset.LOW_CARB)
        assertEquals(150, targets.proteinG)
        assertEquals(100, targets.carbsG)
        assertEquals(111, targets.fatG)
    }

    @Test
    fun `macro energy stays within 5 kcal of the calorie target for every preset and size`() {
        val calories = listOf(1400, 1800, 2000, 2200, 2600, 3000, 3400)
        for (kcal in calories) {
            for (preset in MacroPreset.entries) {
                val targets = macroTargets(kcal, preset)
                val drift = kotlin.math.abs(targets.energyFromMacros - kcal)
                assertTrue(
                    "$preset at $kcal kcal drifted $drift kcal (${targets.energyFromMacros})",
                    drift <= 5,
                )
            }
        }
    }

    @Test
    fun `every preset ratio sums to one`() {
        for (preset in MacroPreset.entries) {
            val sum = preset.proteinRatio + preset.carbohydrateRatio + preset.fatRatio
            assertEquals(1.0, sum, 0.0001)
        }
    }

    @Test
    fun `fiber target follows the 14g per 1000 kcal IOM guideline`() {
        assertEquals(31, macroTargets(2200, MacroPreset.HIGH_PROTEIN).fiberG)
        assertEquals(28, macroTargets(2000, MacroPreset.BALANCED).fiberG)
        assertEquals(14, macroTargets(1000, MacroPreset.BALANCED).fiberG)
    }

    @Test
    fun `macro targets never go negative`() {
        val tiny = macroTargets(MIN_SAFE_DAILY_CALORIES, MacroPreset.KETO)
        assertTrue(tiny.proteinG >= 0 && tiny.carbsG >= 0 && tiny.fatG >= 0 && tiny.fiberG >= 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `macro targets rejects a zero calorie target`() {
        macroTargets(0, MacroPreset.BALANCED)
    }

    // ---- Planner chain ----------------------------------------------------

    @Test
    fun `planner chains bmr tdee target and macros for the default profile`() {
        val plan = successOf(EnergyPlanner.plan(Fixtures.MALE))
        assertEquals(1738, plan.bmr)
        assertEquals(2694, plan.tdee)
        assertEquals(-250, plan.goalAdjustment)
        assertEquals(2444, plan.targetCalories)
        assertEquals(244, plan.macros.proteinG)
        assertEquals(214, plan.macros.carbsG)
        assertEquals(68, plan.macros.fatG)
        assertEquals(34, plan.macros.fiberG)
        assertFalse(plan.hitSafetyFloor)
    }

    @Test
    fun `planner flags when the safety floor clips the goal`() {
        val small = Fixtures.FEMALE.copy(
            weightKg = 45.0,
            heightCm = 150.0,
            activityLevel = ActivityLevel.SEDENTARY,
            goal = Goal.AGGRESSIVE_CUT,
        )
        val plan = successOf(EnergyPlanner.plan(small))
        assertTrue(plan.hitSafetyFloor)
        assertEquals(MIN_SAFE_DAILY_CALORIES, plan.targetCalories)
    }

    @Test
    fun `planner surfaces profile validation failures instead of inventing a plan`() {
        assertTrue(EnergyPlanner.plan(Fixtures.MALE.copy(heightCm = 0.0)) is DomainResult.Failure)
    }

    // ---- BMI --------------------------------------------------------------

    @Test
    fun `bmi 75kg at 178cm is 23_7 and normal`() {
        val result = successOf(bodyMassIndex(75.0, 178.0))
        assertEquals(23.7, result.value, 0.001)
        assertEquals(BmiCategory.NORMAL, result.category)
    }

    @Test
    fun `bmi 100kg at 170cm is obese`() {
        val result = successOf(bodyMassIndex(100.0, 170.0))
        assertEquals(34.6, result.value, 0.001)
        assertEquals(BmiCategory.OBESE, result.category)
    }

    @Test
    fun `bmi boundaries follow the WHO bands`() {
        val cases = listOf(
            Triple(50.0, 170.0, BmiCategory.UNDERWEIGHT), // 17.3
            Triple(70.0, 170.0, BmiCategory.NORMAL),      // 24.2
            Triple(85.0, 170.0, BmiCategory.OVERWEIGHT),  // 29.4
            Triple(90.0, 170.0, BmiCategory.OBESE),       // 31.1
        )
        for ((weight, height, expected) in cases) {
            assertEquals(expected, successOf(bodyMassIndex(weight, height)).category)
        }
    }

    @Test
    fun `bmi reports missing weight rather than dividing by zero`() {
        val failure = failureOf(bodyMassIndex(0.0, 178.0))
        assertEquals(ValidationFailure.MISSING_VALUE, failure.reason)
    }

    @Test
    fun `bmi rejects an out of range height`() {
        assertEquals(
            ValidationFailure.OUT_OF_RANGE,
            failureOf(bodyMassIndex(75.0, 30.0)).reason,
        )
    }

    // ---- Energy balance ---------------------------------------------------

    @Test
    fun `energy balance computes target minus eaten plus burned`() {
        val balance = energyBalance(targetCalories = 2200, caloriesEaten = 1420, caloriesBurned = 320)
        assertEquals(1100, balance.netConsumed)   // 1420 - 320
        assertEquals(1100, balance.remaining)     // 2200 - 1100
        assertFalse(balance.isOverTarget)
        assertEquals(0, balance.surplus)
    }

    @Test
    fun `energy balance floors remaining at zero and keeps the surplus`() {
        val balance = energyBalance(targetCalories = 2000, caloriesEaten = 2400, caloriesBurned = 100)
        assertEquals(0, balance.remaining)
        assertEquals(300, balance.surplus)
        assertTrue(balance.isOverTarget)
    }

    @Test
    fun `energy balance of a fasted day leaves the full target remaining`() {
        assertEquals(2200, energyBalance(2200, 0, 0).remaining)
    }

    @Test
    fun `profile validation passes for a realistic profile`() {
        assertTrue(Fixtures.MALE.isValid)
        assertTrue(UserProfile.DEFAULT.isValid)
        assertEquals(5.0, Fixtures.MALE.weightDeltaKg, 0.001) // 75 kg targeting 70 kg
    }
}