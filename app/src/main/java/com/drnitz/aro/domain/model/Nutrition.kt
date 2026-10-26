package com.drnitz.aro.domain.model

/** Standard Atwater energy factors, plus the IOM fiber guideline Aro plans against. */
object EnergyFactors {
    const val KCAL_PER_G_PROTEIN = 4.0
    const val KCAL_PER_G_CARBOHYDRATE = 4.0
    const val KCAL_PER_G_FAT = 9.0
    const val KCAL_PER_G_ALCOHOL = 7.0

    /** 14 g of dietary fiber per 1000 kcal — IOM DRI for adults. */
    const val FIBER_G_PER_1000_KCAL = 14.0

    /** Energy density of body fat, used to convert a daily kcal offset into kg per week. */
    const val KCAL_PER_KG_FAT = 7700.0
}

/** Unit a nutrient is reported in. Drives both storage and UI formatting. */
enum class NutrientUnit { KCAL, GRAMS, MILLIGRAMS }

/**
 * Every nutrient Aro tracks.
 *
 * Macro keys are measured in grams, electrolytes in milligrams, energy in kilocalories.
 * Adding a key here is the only change needed to support it end-to-end: totals,
 * audits and the missing-nutrient auditor are all map-driven.
 */
enum class NutrientKey(val label: String, val unit: NutrientUnit) {
    CALORIES("Calories", NutrientUnit.KCAL),
    PROTEIN("Protein", NutrientUnit.GRAMS),
    CARBS("Carbohydrates", NutrientUnit.GRAMS),
    FAT("Fat", NutrientUnit.GRAMS),
    SATURATED_FAT("Saturated fat", NutrientUnit.GRAMS),
    FIBER("Fiber", NutrientUnit.GRAMS),
    SUGAR("Sugars", NutrientUnit.GRAMS),
    SODIUM("Sodium", NutrientUnit.MILLIGRAMS),
    POTASSIUM("Potassium", NutrientUnit.MILLIGRAMS),
    CALCIUM("Calcium", NutrientUnit.MILLIGRAMS),
    MAGNESIUM("Magnesium", NutrientUnit.MILLIGRAMS);

    /** True when exceeding this nutrient is the clinically interesting direction. */
    val isUpperLimit: Boolean get() = this in UPPER_LIMIT_KEYS

    companion object {
        val MACRO_KEYS = listOf(PROTEIN, CARBS, FAT)
        val UPPER_LIMIT_KEYS = setOf(CALORIES, SODIUM, SATURATED_FAT, SUGAR)
    }
}

/**
 * A single nutrient measurement.
 *
 * `value == null` means **the source did not report this nutrient** — not that the food
 * contains none of it. Keeping that distinction is what makes Task 8's "missing
 * nutrient" alerts honest: an Open Food Facts product without a potassium entry shows
 * as *unreported* rather than silently reading 0 mg.
 */
data class NutrientAmount(val value: Double?, val isEstimated: Boolean = false) {

    val isReported: Boolean get() = value != null

    /** Value for arithmetic; unreported nutrients count as 0. Never use this for display. */
    val orZero: Double get() = value ?: 0.0

    operator fun plus(other: NutrientAmount): NutrientAmount =
        if (!isReported && !other.isReported) {
            // Nothing was reported on either side: the sum is unknown, not a known zero.
            UNKNOWN
        } else {
            of(
                orZero + other.orZero,
                isEstimated || other.isEstimated || !isReported || !other.isReported,
            )
        }

    /** Scales a reported amount — e.g. converting a per-100 g value to a 150 g portion. */
    operator fun times(factor: Double): NutrientAmount =
        NutrientAmount(value?.times(factor), isEstimated = isEstimated)

    /** Fraction of [target] consumed. Unreported intake reads as 0 progress. */
    fun progressToward(target: Double): Double =
        if (target <= 0.0) 0.0 else orZero / target

    fun format(): String = when (value) {
        null -> "—"
        else -> if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
    }

    companion object {
        val ZERO = NutrientAmount(0.0)
        val UNKNOWN = NutrientAmount(null)

        fun of(value: Double?, isEstimated: Boolean = false) = NutrientAmount(value, isEstimated)

        /**
         * Sums nutrient amounts without pretending missing data is real data.
         *
         * If nothing was reported the result is [UNKNOWN]; if only part was reported the
         * result is flagged `isEstimated = true` so the clinical audit can lower its
         * confidence rather than over-reporting a total.
         */
        fun sum(amounts: Iterable<NutrientAmount>): NutrientAmount {
            val list = amounts.toList()
            val reported = list.filter { it.isReported }
            if (reported.isEmpty()) return UNKNOWN
            val complete = reported.size == list.size && reported.none { it.isEstimated }
            return NutrientAmount(reported.sumOf { it.value!! }, isEstimated = !complete)
        }
    }
}

/**
 * A full nutrient profile for a food, a meal, or a whole day.
 *
 * Backed by a map keyed by [NutrientKey] so nutrients absent from a source are
 * `UNKNOWN` rather than zero, and so new nutrients need no model changes.
 */
data class NutrientTotals(private val values: Map<NutrientKey, NutrientAmount>) {

    operator fun get(key: NutrientKey): NutrientAmount = values[key] ?: NutrientAmount.UNKNOWN

    fun amountOf(key: NutrientKey): Double = get(key).orZero

    fun reportedKeys(): Set<NutrientKey> = values.filterValues { it.isReported }.keys

    operator fun plus(other: NutrientTotals): NutrientTotals {
        if (other.values.isEmpty()) return this
        if (values.isEmpty()) return other
        val merged = values.keys.union(other.values.keys).associateWith { key ->
            this[key] + other[key]
        }
        return NutrientTotals(merged)
    }

    operator fun times(factor: Double): NutrientTotals =
        NutrientTotals(values.mapValues { (_, amount) -> amount * factor })

    /** Scales a per-100 g profile to a logged portion, e.g. 150 g of oats. */
    fun scaledToGrams(grams: Double): NutrientTotals = this * (grams / 100.0)

    val calories: Double get() = amountOf(NutrientKey.CALORIES)
    val proteinG: Double get() = amountOf(NutrientKey.PROTEIN)
    val carbsG: Double get() = amountOf(NutrientKey.CARBS)
    val fatG: Double get() = amountOf(NutrientKey.FAT)
    val fiberG: Double get() = amountOf(NutrientKey.FIBER)
    val sodiumMg: Double get() = amountOf(NutrientKey.SODIUM)
    val potassiumMg: Double get() = amountOf(NutrientKey.POTASSIUM)

    /** Energy implied by the macro grams, used to sanity-check a label's calorie figure. */
    val energyFromMacros: Double
        get() = proteinG * EnergyFactors.KCAL_PER_G_PROTEIN +
            carbsG * EnergyFactors.KCAL_PER_G_CARBOHYDRATE +
            fatG * EnergyFactors.KCAL_PER_G_FAT

    val hasAnyUnknown: Boolean get() = values.values.any { !it.isReported }

    companion object {
        val EMPTY = NutrientTotals(emptyMap())

        fun of(vararg pairs: Pair<NutrientKey, Double>): NutrientTotals =
            of(pairs.toMap())

        fun of(values: Map<NutrientKey, Double>): NutrientTotals =
            NutrientTotals(values.mapValues { (_, value) -> NutrientAmount(value) })

        fun ofAmounts(values: Map<NutrientKey, NutrientAmount>): NutrientTotals =
            NutrientTotals(values)

        /**
         * Derives calories from the macro grams when a source reports macros but no
         * energy value (very common for Open Food Facts entries).
         */
        fun withDerivedCalories(totals: NutrientTotals): NutrientTotals =
            if (totals.get(NutrientKey.CALORIES).isReported) {
                totals
            } else {
                totals + NutrientTotals(mapOf(NutrientKey.CALORIES to NutrientAmount(totals.energyFromMacros, isEstimated = true)))
            }
    }
}

/** Daily macro and fiber goals, derived from the profile by `MacroTargetCalculator`. */
data class MacroTargets(
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val fiberG: Int,
) {
    /** Energy actually implied by the rounded gram targets. */
    val energyFromMacros: Int
        get() = (proteinG * EnergyFactors.KCAL_PER_G_PROTEIN +
            carbsG * EnergyFactors.KCAL_PER_G_CARBOHYDRATE +
            fatG * EnergyFactors.KCAL_PER_G_FAT).toInt()

    /** Maps a macro key to its gram target; non-macro keys have no target. */
    fun targetFor(key: NutrientKey): Double? = when (key) {
        NutrientKey.PROTEIN -> proteinG.toDouble()
        NutrientKey.CARBS -> carbsG.toDouble()
        NutrientKey.FAT -> fatG.toDouble()
        NutrientKey.FIBER -> fiberG.toDouble()
        NutrientKey.CALORIES -> calories.toDouble()
        else -> null
    }

    /** Progress fractions for the concentric rings; may exceed 1.0 when over target. */
    fun progress(totals: NutrientTotals): MacroProgress = MacroProgress(
        calories = fractionOf(totals.calories, calories.toDouble()),
        protein = fractionOf(totals.proteinG, proteinG.toDouble()),
        carbs = fractionOf(totals.carbsG, carbsG.toDouble()),
        fat = fractionOf(totals.fatG, fatG.toDouble()),
    )

    private fun fractionOf(actual: Double, target: Double): Double =
        if (target <= 0.0) 0.0 else actual / target

    companion object {
        val EMPTY = MacroTargets(calories = 0, proteinG = 0, carbsG = 0, fatG = 0, fiberG = 0)
    }
}

/** Fraction-of-target values consumed by [com.drnitz.aro.ui.components.ConcentricMacroRings]. */
data class MacroProgress(
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

/**
 * Aro's headline equation: `target − logged + burned = remaining`.
 *
 * [remaining] is floored at zero so the ring can never display a negative centre value;
 * [surplus] carries the overshoot instead.
 */
data class EnergyBalance(
    val targetCalories: Int,
    val caloriesEaten: Int,
    val caloriesBurned: Int,
) {
    val netConsumed: Int get() = caloriesEaten - caloriesBurned

    val remaining: Int get() = (targetCalories - netConsumed).coerceAtLeast(0)

    val isOverTarget: Boolean get() = netConsumed > targetCalories

    /** Calories eaten beyond target; always >= 0. */
    val surplus: Int get() = (netConsumed - targetCalories).coerceAtLeast(0)

    companion object {
        val EMPTY = EnergyBalance(0, 0, 0)
    }
}