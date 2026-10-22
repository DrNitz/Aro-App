package com.drnitz.aro.data.db

import com.drnitz.aro.domain.model.NutrientAmount
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals

/**
 * The eleven nutrient columns shared by every table that stores a food's nutrition:
 * food log entries, custom foods and meal template items.
 *
 * Every column is **nullable**, and that nullability carries meaning: `null` means the
 * source never reported the nutrient, which is not the same as `0.0`. Keeping that
 * distinction in the database is what lets
 * `com.drnitz.aro.domain.clinical.MissingMacroAuditor` tell "Aro does not know how much
 * potassium this product has" apart from "this product has no potassium".
 *
 * Reading is centralised here so the three entity types cannot drift apart; a unit test
 * round-trips every [NutrientKey] to prove the mapping is complete.
 */
interface NutrientColumns {

    val calories: Double?
    val protein: Double?
    val carbs: Double?
    val fat: Double?
    val saturatedFat: Double?
    val fiber: Double?
    val sugar: Double?
    val sodium: Double?
    val potassium: Double?
    val calcium: Double?
    val magnesium: Double?

    /** Reads one nutrient; `null` when the source did not report it. */
    fun read(key: NutrientKey): Double? = when (key) {
        NutrientKey.CALORIES -> calories
        NutrientKey.PROTEIN -> protein
        NutrientKey.CARBS -> carbs
        NutrientKey.FAT -> fat
        NutrientKey.SATURATED_FAT -> saturatedFat
        NutrientKey.FIBER -> fiber
        NutrientKey.SUGAR -> sugar
        NutrientKey.SODIUM -> sodium
        NutrientKey.POTASSIUM -> potassium
        NutrientKey.CALCIUM -> calcium
        NutrientKey.MAGNESIUM -> magnesium
    }

    /** Rebuilds the domain totals, preserving both value and reported-ness. */
    fun toNutrientTotals(): NutrientTotals =
        NutrientTotals.ofAmounts(NutrientKey.entries.associateWith { NutrientAmount(read(it)) })

    companion object {
        /** Reads a totals object back out, for the entity `copy(...)` mappers. */
        fun valueOf(totals: NutrientTotals, key: NutrientKey): Double? = totals[key].value
    }
}

/**
 * SQL projection for a whole day's nutrient sums.
 *
 * This is the "zero bottleneck" dashboard query: one indexed scan with eleven `SUM`s
 * returns everything the rings, badges and clinical audit need, so the UI never loads
 * rows to sum them in Kotlin.
 */
data class DayNutrientTotals(
    override val calories: Double?,
    override val protein: Double?,
    override val carbs: Double?,
    override val fat: Double?,
    override val saturatedFat: Double?,
    override val fiber: Double?,
    override val sugar: Double?,
    override val sodium: Double?,
    override val potassium: Double?,
    override val calcium: Double?,
    override val magnesium: Double?,
) : NutrientColumns {

    /**
     * An empty day has no rows, so every SUM is null. That is the honest reading of
     * "nothing logged" and stays distinct from a day of food that genuinely reports zero.
     */
    val isEmptyDay: Boolean get() = calories == null
}

/**
 * Per-day totals for a range query, used by the Task 12 trend charts.
 *
 * Identical to [DayNutrientTotals] plus the day it belongs to, so a multi-day scan
 * returns one row per day that actually has food — days with no entries are absent
 * rather than present-with-nulls, which is what a `GROUP BY` produces.
 */
data class DayTotalsRow(
    val epochDay: Long,
    override val calories: Double?,
    override val protein: Double?,
    override val carbs: Double?,
    override val fat: Double?,
    override val saturatedFat: Double?,
    override val fiber: Double?,
    override val sugar: Double?,
    override val sodium: Double?,
    override val potassium: Double?,
    override val calcium: Double?,
    override val magnesium: Double?,
) : NutrientColumns {

    val date: java.time.LocalDate get() = java.time.LocalDate.ofEpochDay(epochDay)
}