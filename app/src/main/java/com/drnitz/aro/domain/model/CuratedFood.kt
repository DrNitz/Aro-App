package com.drnitz.aro.domain.model

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.roundToInt

@Serializable
data class CommonServing(
    val name: String,
    val grams: Double,
)

/**
 * A food Aro can log.
 *
 * `null` nutrient reporting is represented by [unreportedNutrients]: the numeric fields
 * stay non-null (and read as zero) so arithmetic and UI code remain simple, while the key
 * set records that the source never supplied a value. `per100gNutrients` turns those keys
 * into [NutrientAmount.UNKNOWN], which is how the clinical audit distinguishes
 * "this food has no potassium" from "nobody told us the potassium value".
 */
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

    // ---- Honest reporting -------------------------------------------------
    /** Core nutrient keys ([NutrientKey.name]) the source did not report. */
    val unreportedNutrients: Set<String> = emptySet(),
    /** Core nutrient keys whose value was derived (e.g. kcal from macros, sodium from salt). */
    val estimatedNutrients: Set<String> = emptySet(),

    // ---- Extra label detail (Open Food Facts) -----------------------------
    /** Extra nutrients in the display units declared by [MicronutrientCatalog]. */
    val micronutrients: Map<String, Double> = emptyMap(),
    /** [MicronutrientCatalog] keys the source did not report (label-only nutrients). */
    val unreportedMicronutrients: Set<String> = emptySet(),
    val quantity: String? = null,
    val servingSizeText: String? = null,
    val ingredientsText: String? = null,
    val allergens: List<String> = emptyList(),
    val traces: List<String> = emptyList(),
    val additives: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val packaging: List<String> = emptyList(),
    val ecoscore: String? = null,
    val completeness: Double? = null,
    val lastModified: Long? = null,
    val dataSource: String = "Aro curated whole-food database",
) {
    /** True when the source supplied a value for [key]. */
    fun isReported(key: NutrientKey): Boolean = key.name !in unreportedNutrients

    /** Base nutrient profile per 100g, with unreported values kept null. */
    val per100gNutrients: NutrientTotals
        get() {
            fun amount(key: NutrientKey, value: Double): NutrientAmount =
                if (!isReported(key)) {
                    NutrientAmount.UNKNOWN
                } else {
                    NutrientAmount(value, isEstimated = key.name in estimatedNutrients)
                }

            return NutrientTotals.ofAmounts(
                mapOf(
                    NutrientKey.CALORIES to amount(NutrientKey.CALORIES, calories),
                    NutrientKey.PROTEIN to amount(NutrientKey.PROTEIN, protein),
                    NutrientKey.CARBS to amount(NutrientKey.CARBS, carbs),
                    NutrientKey.FAT to amount(NutrientKey.FAT, fat),
                    NutrientKey.SATURATED_FAT to amount(NutrientKey.SATURATED_FAT, saturatedFat),
                    NutrientKey.FIBER to amount(NutrientKey.FIBER, fiber),
                    NutrientKey.SUGAR to amount(NutrientKey.SUGAR, sugar),
                    NutrientKey.SODIUM to amount(NutrientKey.SODIUM, sodium),
                    NutrientKey.POTASSIUM to amount(NutrientKey.POTASSIUM, potassium),
                    NutrientKey.CALCIUM to amount(NutrientKey.CALCIUM, calcium),
                    NutrientKey.MAGNESIUM to amount(NutrientKey.MAGNESIUM, magnesium),
                )
            )
        }

    /** Grams in the default serving; 100 g when a product has no serving information. */
    val defaultServingGrams: Double
        get() = commonServing?.grams?.takeIf { it > 0.0 } ?: servingSize.takeIf { it > 0.0 } ?: 100.0

    /** Human label for the default serving. */
    val defaultServingLabel: String
        get() = commonServing?.name ?: "${defaultServingGrams.roundToInt()} g"

    /**
     * True when the item carries enough information to log meaningfully: at least one of
     * energy or the three macros was reported.
     */
    val hasUsableNutrition: Boolean
        get() = isReported(NutrientKey.CALORIES) ||
            isReported(NutrientKey.PROTEIN) ||
            isReported(NutrientKey.CARBS) ||
            isReported(NutrientKey.FAT)

    /** Energy implied by the reported macros, using Atwater factors. */
    val atwaterCalories: Double
        get() {
            var total = 0.0
            if (isReported(NutrientKey.PROTEIN)) total += protein * EnergyFactors.KCAL_PER_G_PROTEIN
            if (isReported(NutrientKey.CARBS)) total += carbs * EnergyFactors.KCAL_PER_G_CARBOHYDRATE
            if (isReported(NutrientKey.FAT)) total += fat * EnergyFactors.KCAL_PER_G_FAT
            return total
        }

    /**
     * Percentage difference between the label energy and the Atwater estimate.
     * `null` when energy or macros are missing, or the label reports zero energy.
     */
    val energyLabelDeltaPercent: Double?
        get() {
            if (!isReported(NutrientKey.CALORIES) || calories <= 0.0) return null
            // The 4/4/9 check is only meaningful when every energy-yielding macro was
            // reported; comparing against a partial sum would manufacture a false gap.
            if (!isReported(NutrientKey.PROTEIN) ||
                !isReported(NutrientKey.CARBS) ||
                !isReported(NutrientKey.FAT)
            ) {
                return null
            }
            val implied = atwaterCalories
            if (implied <= 0.0) return null
            return ((calories - implied) / implied) * 100.0
        }

    /** Builds a FoodLogEntry scaled to portion grams, preserving unreported nutrients. */
    fun toFoodLogEntry(
        meal: MealSlot,
        grams: Double = defaultServingGrams,
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

    /**
     * Scaled macro/nutrient value for the default serving, used by food cards and the
     * quick-log button so both agree on what one tap will actually record.
     */
    fun valuePerServing(key: NutrientKey): Double = per100gNutrients[key].orZero * (defaultServingGrams / 100.0)

    fun matchesQuery(query: String): Boolean {
        if (query.isBlank()) return true
        val clean = query.trim().lowercase()
        return name.lowercase().contains(clean) ||
            brand?.lowercase()?.contains(clean) == true ||
            category?.lowercase()?.contains(clean) == true ||
            code?.lowercase()?.contains(clean) == true
    }

    /** True when [category] is unset or matches this food's category. */
    fun matchesCategory(category: String?): Boolean {
        val clean = category?.trim()?.takeIf { it.isNotEmpty() && !it.equals("all", ignoreCase = true) }
            ?: return true
        return this.category?.equals(clean, ignoreCase = true) == true
    }
}
