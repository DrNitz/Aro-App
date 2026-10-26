package com.drnitz.aro.domain.model

/** Visual grouping used by the "Know more" nutrition panel. */
enum class MicronutrientGroup(val label: String) {
    MINERALS("Minerals"),
    VITAMINS("Vitamins"),
    OTHER("Other nutrients"),
}

/**
 * A micronutrient tracked for display but not (yet) part of the eleven daily-total columns.
 *
 * Values are stored in the unit named by [unit], already converted from Open Food Facts'
 * canonical grams. Keeping the conversion here means the UI never has to know whether a
 * vitamin is reported in milligrams or micrograms.
 */
data class Micronutrient(
    val key: String,
    val label: String,
    val unit: String,
    val group: MicronutrientGroup,
    val offKeys: List<String> = listOf(key),
    val scaleFromGrams: Double = when (unit) {
        "mg" -> 1_000.0
        "µg" -> 1_000_000.0
        else -> 1.0
    },
)

/**
 * Every extra nutrient Aro can show when a label reports it.
 *
 * The eleven [NutrientKey] values (calories, protein, carbs, fat, saturated fat, fiber,
 * sugar, sodium, potassium, calcium, magnesium) are the ones that participate in daily
 * totals and clinical audits. The catalogue below covers the long tail of a nutrition
 * label — the detail a packaged-food entry can carry that the daily rings do not use.
 */
object MicronutrientCatalog {
    val ALL: List<Micronutrient> = listOf(
        // Minerals
        Micronutrient("iron", "Iron", "mg", MicronutrientGroup.MINERALS),
        Micronutrient("zinc", "Zinc", "mg", MicronutrientGroup.MINERALS),
        Micronutrient("phosphorus", "Phosphorus", "mg", MicronutrientGroup.MINERALS),
        Micronutrient("copper", "Copper", "mg", MicronutrientGroup.MINERALS),
        Micronutrient("manganese", "Manganese", "mg", MicronutrientGroup.MINERALS),
        Micronutrient("selenium", "Selenium", "µg", MicronutrientGroup.MINERALS),
        Micronutrient("iodine", "Iodine", "µg", MicronutrientGroup.MINERALS),

        // Vitamins
        Micronutrient("vitamin-a", "Vitamin A", "µg", MicronutrientGroup.VITAMINS),
        Micronutrient("vitamin-c", "Vitamin C", "mg", MicronutrientGroup.VITAMINS),
        Micronutrient("vitamin-d", "Vitamin D", "µg", MicronutrientGroup.VITAMINS),
        Micronutrient("vitamin-e", "Vitamin E", "mg", MicronutrientGroup.VITAMINS),
        Micronutrient("vitamin-k", "Vitamin K", "µg", MicronutrientGroup.VITAMINS),
        Micronutrient("vitamin-b1", "Thiamin (B1)", "mg", MicronutrientGroup.VITAMINS),
        Micronutrient("vitamin-b2", "Riboflavin (B2)", "mg", MicronutrientGroup.VITAMINS),
        Micronutrient(
            "vitamin-pp",
            "Niacin (B3)",
            "mg",
            MicronutrientGroup.VITAMINS,
            offKeys = listOf("vitamin-pp", "vitamin-b3", "niacin"),
        ),
        Micronutrient("pantothenic-acid", "Pantothenic acid (B5)", "mg", MicronutrientGroup.VITAMINS),
        Micronutrient("vitamin-b6", "Vitamin B6", "mg", MicronutrientGroup.VITAMINS),
        Micronutrient(
            "vitamin-b9",
            "Folate (B9)",
            "µg",
            MicronutrientGroup.VITAMINS,
            offKeys = listOf("vitamin-b9", "folate"),
        ),
        Micronutrient(
            "vitamin-b12",
            "Vitamin B12",
            "µg",
            MicronutrientGroup.VITAMINS,
            offKeys = listOf("vitamin-b12", "cobalamin"),
        ),

        // Fats, sugars and other label fields
        Micronutrient("cholesterol", "Cholesterol", "mg", MicronutrientGroup.OTHER),
        Micronutrient("trans-fat", "Trans fat", "g", MicronutrientGroup.OTHER),
        Micronutrient(
            "omega-3-fat",
            "Omega-3",
            "g",
            MicronutrientGroup.OTHER,
            offKeys = listOf("omega-3-fat", "omega-3"),
        ),
        Micronutrient("added-sugars", "Added sugars", "g", MicronutrientGroup.OTHER),
        Micronutrient("caffeine", "Caffeine", "mg", MicronutrientGroup.OTHER),
        Micronutrient("alcohol", "Alcohol", "g", MicronutrientGroup.OTHER),
        Micronutrient("water", "Water", "g", MicronutrientGroup.OTHER),
    )

    val byKey: Map<String, Micronutrient> = ALL.associateBy { it.key }

    /** Formats a catalogue value with a sensible number of decimals for its magnitude. */
    fun format(key: String, value: Double): String {
        val def = byKey[key] ?: return value.toString()
        val rounded = when {
            value >= 100.0 -> "%.0f".format(value)
            value >= 10.0 -> "%.1f".format(value)
            value >= 1.0 -> "%.1f".format(value)
            else -> "%.2f".format(value)
        }
        return "$rounded ${def.unit}"
    }
}
