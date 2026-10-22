package com.drnitz.aro.domain.calculation

import com.drnitz.aro.domain.model.PlanDaySummary
import com.drnitz.aro.domain.model.PlanOccurrence
import kotlin.math.roundToInt

/** Estimated food calories for one planned occurrence. */
val PlanOccurrence.plannedIntakeCalories: Int
    get() = item.food?.nutrients?.calories?.roundToInt() ?: 0

/** Estimated processed burn for one planned exercise occurrence. */
fun PlanOccurrence.plannedBurnCalories(bodyWeightKg: Double): Int {
    val exercise = item.exercise ?: return 0
    return caloriesBurned(
        met = exercise.met,
        bodyWeightKg = bodyWeightKg,
        durationMinutes = exercise.durationMinutes,
    )
}

/**
 * Planned intake, macros, burn, water and fasting for a day.
 *
 * Completed and skipped occurrences are excluded by default because completing a plan
 * creates a real diary/workout log, while a skipped one will not happen. Including both
 * would double-count or overstate what is still planned.
 */
fun planDaySummary(
    occurrences: List<PlanOccurrence>,
    bodyWeightKg: Double,
    includeCompleted: Boolean = false,
): PlanDaySummary {
    val relevant = occurrences.filter { includeCompleted || it.isPending }
    val foods = relevant.mapNotNull { it.item.food?.nutrients }
    val calorieTotal = foods.sumOf { it.calories }
    val protein = foods.sumOf { it.proteinG }
    val carbs = foods.sumOf { it.carbsG }
    val fat = foods.sumOf { it.fatG }
    val burn = relevant.sumOf { it.plannedBurnCalories(bodyWeightKg) }
    val waterMl = relevant.sumOf { it.item.water?.amountMl ?: 0 }
    val fastingHours = relevant.sumOf { it.item.fasting?.targetHours ?: 0 }

    return PlanDaySummary(
        plannedCalories = calorieTotal.roundToInt(),
        plannedProteinG = protein,
        plannedCarbsG = carbs,
        plannedFatG = fat,
        plannedBurnCalories = burn,
        plannedWaterMl = waterMl,
        plannedFastingHours = fastingHours,
    )
}
