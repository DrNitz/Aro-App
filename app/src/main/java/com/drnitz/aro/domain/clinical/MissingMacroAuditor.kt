package com.drnitz.aro.domain.clinical

import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.MacroTargets
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals

/**
 * Whether a nutrient is present in the log, absent from it, or simply unknown.
 *
 * The distinction matters: "0 mg potassium logged" and "potassium was never tracked by
 * the food database" look identical once a total is collapsed into a number, and only
 * one of them is actionable.
 */
enum class CoverageStatus(val label: String) {
    ON_TARGET("On target"),
    ABOVE_TARGET("Above target"),
    BELOW_TARGET("Below target"),
    ABSENT("Not logged at all"),
    UNREPORTED("Not tracked by source"),
    NO_LOG_ENTRIES("Nothing logged yet"),
}

/** One nutrient's coverage against its target. */
data class NutrientCoverage(
    val key: NutrientKey,
    val status: CoverageStatus,
    val actual: Double?,
    val target: Double?,
    val unitLabel: String,
) {
    val isMissing: Boolean
        get() = status == CoverageStatus.ABSENT ||
            status == CoverageStatus.UNREPORTED ||
            status == CoverageStatus.NO_LOG_ENTRIES

    val fractionOfTarget: Double
        get() {
            val t = target ?: return 0.0
            if (t <= 0.0) return 0.0
            return (actual ?: 0.0) / t
        }
}

/**
 * Coverage for a single meal.
 *
 * [missingMacros] is what Task 8 renders as "Breakfast has no protein source" — a macro
 * at exactly zero. [unreportedMacros] is the softer case where the database simply has
 * no value, which Aro never treats as zero.
 */
data class MealCoverage(
    val meal: MealSlot,
    val entryCount: Int,
    val totals: NutrientTotals,
    val missingMacros: List<NutrientKey>,
    val unreportedMacros: List<NutrientKey>,
) {
    val isEmpty: Boolean get() = entryCount == 0
}

/** Whole-day coverage, plus per-meal detail. */
data class DailyCoverage(
    val meals: List<MealCoverage>,
    val macros: List<NutrientCoverage>,
    val fiber: NutrientCoverage,
    val totalEntries: Int,
    /** Day totals, carrying per-nutrient `isEstimated` flags for partially logged data. */
    val totalTotals: NutrientTotals,
) {
    val emptyMeals: List<MealSlot> get() = meals.filter { it.isEmpty }.map { it.meal }

    val mealsMissingMacros: List<MealCoverage> get() = meals.filter { it.missingMacros.isNotEmpty() }

    /** Nutrients no logged food reported a value for — the audit's blind spot. */
    val unreportedKeys: Set<NutrientKey>
        get() = NutrientKey.entries.filterNot { totalTotals[it].isReported }.toSet()

    val hasNothingLogged: Boolean get() = totalEntries == 0
}

/**
 * "Missing Macro Audit" — finds what a day's log has not accounted for.
 *
 * Answers three questions Task 8 and Task 12 need:
 *  - which macros have **no** logged source at all (a day of only fruit has no protein),
 *  - which nutrients the food database **never reported** (so Aro knows its own blind spot),
 *  - which meals are still empty.
 *
 * Implemented as a pure function over [DiaryDay] and [MacroTargets] — no Android types,
 * fully unit tested.
 */
object MissingMacroAuditor {

    fun audit(day: DiaryDay, targets: MacroTargets): DailyCoverage {
        val meals = MealSlot.ORDERED.map { slot -> auditMeal(slot, day.entriesFor(slot)) }
        val totals = day.nutrientTotals
        val totalEntries = day.foodEntries.size

        val macros = NutrientKey.MACRO_KEYS.map { key ->
            coverageFor(key, totals, targets.targetFor(key), hasEntries = totalEntries > 0)
        }

        return DailyCoverage(
            meals = meals,
            macros = macros,
            fiber = coverageFor(
                NutrientKey.FIBER,
                totals,
                targets.fiberG.toDouble(),
                hasEntries = totalEntries > 0,
            ),
            totalEntries = totalEntries,
            totalTotals = totals,
        )
    }

    fun auditMeal(meal: MealSlot, entries: List<FoodLogEntry>): MealCoverage {
        val totals = entries.fold(NutrientTotals.EMPTY) { acc, entry -> acc + entry.nutrients }
        return MealCoverage(
            meal = meal,
            entryCount = entries.size,
            totals = totals,
            missingMacros = NutrientKey.MACRO_KEYS.filter { isAbsent(it, totals) },
            unreportedMacros = NutrientKey.MACRO_KEYS.filter { isUnreported(it, totals) },
        )
    }

    private fun coverageFor(
        key: NutrientKey,
        totals: NutrientTotals,
        target: Double?,
        hasEntries: Boolean,
    ): NutrientCoverage {
        val amount = totals[key]
        val status = when {
            !hasEntries -> CoverageStatus.NO_LOG_ENTRIES
            !amount.isReported -> CoverageStatus.UNREPORTED
            amount.orZero < MEANINGFUL_SOURCE_THRESHOLD_G -> CoverageStatus.ABSENT
            target == null || target <= 0.0 -> CoverageStatus.ON_TARGET
            amount.orZero < target -> CoverageStatus.BELOW_TARGET
            else -> CoverageStatus.ABOVE_TARGET
        }
        return NutrientCoverage(
            key = key,
            status = status,
            actual = amount.value,
            target = target,
            unitLabel = key.unit.name.lowercase(),
        )
    }

    /**
     * Reported, but too little to count as a real source.
     *
     * A strict `== 0` test is useless in practice: 20 g of butter reports 0.012 g of
     * carbohydrate, which a label technically supplies but which is not a carbohydrate
     * source by any nutritional reading. Below [MEANINGFUL_SOURCE_THRESHOLD_G] a macro
     * is treated as having no logged source.
     */
    private fun isAbsent(key: NutrientKey, totals: NutrientTotals): Boolean {
        val amount = totals[key]
        return amount.isReported && amount.orZero < MEANINGFUL_SOURCE_THRESHOLD_G
    }

    /** The source never provided a value. Not the same as zero. */
    private fun isUnreported(key: NutrientKey, totals: NutrientTotals): Boolean =
        !totals[key].isReported

    /** Grams below which a macro does not count as a logged source. */
    const val MEANINGFUL_SOURCE_THRESHOLD_G = 1.0
}