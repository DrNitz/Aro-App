package com.drnitz.aro.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CommonServing(
    val name: String,
    val grams: Double,
)

@Serializable
data class CuratedFoodItem(
    val id: String,
    val code: String? = null,
    val name: String,
    val brand: String? = "Whole Food / Fresh",
    val category: String? = "Whole Foods",
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val saturatedFat: Double = 0.0,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val sodium: Double = 0.0,
    val potassium: Double = 0.0,
    val calcium: Double = 0.0,
    val magnesium: Double = 0.0,
    val servingSize: Double = 100.0,
    val servingUnit: String = "g",
    val commonServing: CommonServing? = null,
    val nutriscore: String? = null,
    val nova: Int? = null,
    val image: String? = null,
) {
    /** Base nutrient profile per 100g */
    val per100gNutrients: NutrientTotals
        get() = NutrientTotals.of(
            NutrientKey.CALORIES to calories,
            NutrientKey.PROTEIN to protein,
            NutrientKey.CARBS to carbs,
            NutrientKey.FAT to fat,
            NutrientKey.SATURATED_FAT to saturatedFat,
            NutrientKey.FIBER to fiber,
            NutrientKey.SUGAR to sugar,
            NutrientKey.SODIUM to sodium,
            NutrientKey.POTASSIUM to potassium,
            NutrientKey.CALCIUM to calcium,
            NutrientKey.MAGNESIUM to magnesium,
        )

    /** Builds a FoodLogEntry scaled to portion grams */
    fun toFoodLogEntry(
        meal: MealSlot,
        grams: Double = commonServing?.grams ?: servingSize,
        logId: String = UUID.randomUUID().toString(),
    ): FoodLogEntry {
        val scaled = per100gNutrients.scaledToGrams(grams)
        return FoodLogEntry(
            id = logId,
            name = name,
            meal = meal,
            grams = grams,
            nutrients = scaled,
            brand = brand,
            foodId = id,
            isCustom = false,
        )
    }

    fun matchesQuery(query: String): Boolean {
        if (query.isBlank()) return true
        val clean = query.trim().lowercase()
        return name.lowercase().contains(clean) ||
            brand?.lowercase()?.contains(clean) == true ||
            category?.lowercase()?.contains(clean) == true ||
            code?.lowercase()?.contains(clean) == true
    }
}
