package com.drnitz.aro.domain

import com.drnitz.aro.domain.model.ActivityLevel
import com.drnitz.aro.domain.model.ClinicalProtocol
import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.Goal
import com.drnitz.aro.domain.model.MacroPreset
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.Sex
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.domain.model.WorkoutLog
import com.drnitz.aro.domain.model.WorkoutType
import com.drnitz.aro.domain.model.SetEntry
import java.time.Instant
import java.time.LocalDate

/** Shared fixtures and assertion helpers for the domain test suite. */
object Fixtures {

    val DATE: LocalDate = LocalDate.of(2026, 10, 18)

    /** 26 y/o male, 75 kg, 178 cm, moderate activity, mild cut. BMR 1738, TDEE 2694. */
    val MALE = UserProfile(
        name = "Arjun",
        sex = Sex.MALE,
        ageYears = 26,
        weightKg = 75.0,
        targetWeightKg = 70.0,
        heightCm = 178.0,
        activityLevel = ActivityLevel.MODERATE,
        goal = Goal.MILD_CUT,
        macroPreset = MacroPreset.HIGH_PROTEIN,
    )

    /** 30 y/o female, 60 kg, 165 cm, light activity, maintenance. BMR 1320. */
    val FEMALE = UserProfile(
        name = "Meera",
        sex = Sex.FEMALE,
        ageYears = 30,
        weightKg = 60.0,
        targetWeightKg = 58.0,
        heightCm = 165.0,
        activityLevel = ActivityLevel.LIGHT,
        goal = Goal.MAINTAIN,
        macroPreset = MacroPreset.BALANCED,
    )

    fun totals(vararg pairs: Pair<NutrientKey, Double>): NutrientTotals = NutrientTotals.of(*pairs)

    /** Chicken breast, 150 g: protein-rich, but the source has no potassium entry. */
    fun chickenBreast(grams: Double = 150.0): FoodLogEntry = FoodLogEntry(
        id = "f-chicken",
        name = "Chicken Breast (grilled)",
        meal = MealSlot.LUNCH,
        grams = grams,
        nutrients = totals(
            NutrientKey.CALORIES to 248.0 * grams / 100,
            NutrientKey.PROTEIN to 46.5 * grams / 100,
            NutrientKey.CARBS to 0.0,
            NutrientKey.FAT to 5.4 * grams / 100,
            NutrientKey.SODIUM to 74.0 * grams / 100,
        ),
    )

    /** Cooked oatmeal, 100 g: contributes fiber, potassium and magnesium. */
    fun oatmeal(grams: Double = 100.0): FoodLogEntry = FoodLogEntry(
        id = "f-oats",
        name = "Oatmeal (cooked)",
        meal = MealSlot.BREAKFAST,
        grams = grams,
        nutrients = totals(
            NutrientKey.CALORIES to 71.0 * grams / 100,
            NutrientKey.PROTEIN to 2.5 * grams / 100,
            NutrientKey.CARBS to 12.0 * grams / 100,
            NutrientKey.FAT to 1.5 * grams / 100,
            NutrientKey.FIBER to 1.7 * grams / 100,
            NutrientKey.SODIUM to 4.0 * grams / 100,
            NutrientKey.POTASSIUM to 60.0 * grams / 100,
            NutrientKey.MAGNESIUM to 27.0 * grams / 100,
        ),
    )

    /** Butter: zero protein and carbs, saturated-fat heavy. */
    fun butter(grams: Double = 20.0): FoodLogEntry = FoodLogEntry(
        id = "f-butter",
        name = "Butter",
        meal = MealSlot.BREAKFAST,
        grams = grams,
        nutrients = totals(
            NutrientKey.CALORIES to 717.0 * grams / 100,
            NutrientKey.PROTEIN to 0.85 * grams / 100,
            NutrientKey.CARBS to 0.06 * grams / 100,
            NutrientKey.FAT to 81.1 * grams / 100,
            NutrientKey.SATURATED_FAT to 51.4 * grams / 100,
            NutrientKey.SODIUM to 11.0 * grams / 100,
        ),
    )

    fun cardio(
        met: Double = 8.0,
        minutes: Int = 45,
        bodyWeightKg: Double? = 80.0,
    ): WorkoutLog = WorkoutLog(
        id = "w-cardio",
        type = WorkoutType.CARDIO,
        exerciseName = "Running (6 mph)",
        loggedAt = Instant.parse("2026-10-18T07:30:00Z"),
        met = met,
        durationMinutes = minutes,
        bodyWeightKg = bodyWeightKg,
        category = "Running",
    )

    fun strength(vararg sets: Pair<Double, Int>, bodyWeightKg: Double? = 80.0): WorkoutLog = WorkoutLog(
        id = "w-strength",
        type = WorkoutType.STRENGTH,
        exerciseName = "Bench Press",
        loggedAt = Instant.parse("2026-10-18T18:00:00Z"),
        met = 6.0,
        durationMinutes = 45,
        bodyWeightKg = bodyWeightKg,
        sets = sets.map { SetEntry(it.first, it.second) },
        category = "Gym & Strength",
        muscle = "Chest",
        equipment = "Barbell",
    )

    fun day(vararg entries: FoodLogEntry): DiaryDay {
        val meals = entries.groupBy { it.meal }
        return DiaryDay(date = DATE, meals = meals)
    }

    val AHA: ClinicalProtocol = ClinicalCatalogById("aha_standard")
    val RENAL: ClinicalProtocol = ClinicalCatalogById("renal_restricted")

    private fun ClinicalCatalogById(id: String): ClinicalProtocol =
        com.drnitz.aro.domain.model.ClinicalCatalog.byId(id)
}

/** Unwraps a [DomainResult.Success] or fails the test with a readable message. */
fun <T> successOf(result: DomainResult<T>): T =
    (result as? DomainResult.Success)?.value ?: error("Expected Success but was $result")

/** Unwraps a [DomainResult.Failure] or fails the test. */
fun failureOf(result: DomainResult<*>): DomainResult.Failure =
    result as? DomainResult.Failure ?: error("Expected Failure but was $result")

/** Asserts the value sits within [tolerance] of [expected] — for floating point math. */
fun assertClose(expected: Double, actual: Double, tolerance: Double = 0.05) {
    if (kotlin.math.abs(expected - actual) > tolerance) {
        throw AssertionError("Expected $expected +/- $tolerance but was $actual")
    }
}