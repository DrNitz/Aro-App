package com.drnitz.aro.domain.model

import java.time.LocalDate

/**
 * The four eating occasions Aro tracks, mirroring the web diary.
 *
 * [label] is the English display name; Task 8 renders it, and a future localisation
 * pass can replace it with a string resource without touching the model.
 */
enum class MealSlot(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACKS("Snacks & Extras");

    companion object {
        val ORDERED = listOf(BREAKFAST, LUNCH, DINNER, SNACKS)
    }
}

/**
 * One food added to one meal.
 *
 * [nutrients] are **already scaled to [grams]** at construction time, so every total,
 * ratio and audit is a plain summation — no per-100 g factor leaks into the UI layer.
 *
 * Task 5 mirrors this as `FoodLogEntity` plus a `gramFactor` column if per-100 g values
 * need to be retained for re-scaling when the portion is edited.
 */
data class FoodLogEntry(
    val id: String,
    val name: String,
    val meal: MealSlot,
    val grams: Double,
    val nutrients: NutrientTotals,
    val brand: String? = null,
    /** Identifier in the curated or Open Food Facts database, when known. */
    val foodId: String? = null,
    /** Nutrients are user-entered rather than from a database. */
    val isCustom: Boolean = false,
) {
    val calories: Double get() = nutrients.calories
    val proteinG: Double get() = nutrients.proteinG

    /** Returns a copy logged at a different portion, re-scaling every nutrient. */
    fun withGrams(grams: Double): FoodLogEntry {
        val factor = if (this.grams > 0) grams / this.grams else 0.0
        return copy(grams = grams, nutrients = NutrientTotals.ofAmounts(
            NutrientKey.entries.associateWith { key ->
                nutrients[key] * factor
            }
        ))
    }
}

/**
 * One calendar day of the diary.
 *
 * Meals are keyed by slot so "nothing logged for lunch" is simply an absent or empty
 * list — the shape the Task 5 date-indexed Room schema wants.
 */
data class DiaryDay(
    val date: LocalDate,
    val meals: Map<MealSlot, List<FoodLogEntry>> = emptyMap(),
    val workouts: List<WorkoutLog> = emptyList(),
    val waterMl: Int = 0,
    val notes: String = "",
) {
    val isEmpty: Boolean
        get() = foodEntries.isEmpty() && workouts.isEmpty() && waterMl == 0 && notes.isBlank()

    val foodEntries: List<FoodLogEntry>
        get() = MealSlot.ORDERED.flatMap { meals[it].orEmpty() }

    val entriesFor: (MealSlot) -> List<FoodLogEntry> = { slot -> meals[slot].orEmpty() }

    /** Whole-day nutrient totals, summing across every meal. */
    val nutrientTotals: NutrientTotals
        get() = foodEntries.fold(NutrientTotals.EMPTY) { acc, entry -> acc + entry.nutrients }

    fun totalsFor(meal: MealSlot): NutrientTotals =
        meals[meal].orEmpty().fold(NutrientTotals.EMPTY) { acc, entry -> acc + entry.nutrients }

    /** Calories logged across every meal. */
    val caloriesEaten: Int get() = nutrientTotals.calories.toInt()

    /**
     * Calories burned and the resulting energy balance need calorie formulas, so they
     * live in `com.drnitz.aro.domain.calculation` (`DiaryDay.caloriesBurned` and
     * `DiaryDay.energyBalance`) — this model stays pure data.
     */

    /** Fraction of the water goal reached, for the hydration reservoir. */
    fun waterProgress(targetMl: Int): Double =
        if (targetMl <= 0) 0.0 else waterMl.toDouble() / targetMl

    fun withAddedFood(entry: FoodLogEntry): DiaryDay =
        copy(meals = meals + (entry.meal to (meals[entry.meal].orEmpty() + entry)))

    fun withUpdatedFood(entry: FoodLogEntry): DiaryDay = copy(
        meals = MealSlot.ORDERED.associateWith { slot ->
            meals[slot].orEmpty().map { if (it.id == entry.id) entry else it }
        }
    )

    fun withRemovedFood(entryId: String): DiaryDay = copy(
        meals = MealSlot.ORDERED.associateWith { slot ->
            meals[slot].orEmpty().filterNot { it.id == entryId }
        }
    )

    companion object {
        fun empty(date: LocalDate): DiaryDay = DiaryDay(date = date)
    }
}