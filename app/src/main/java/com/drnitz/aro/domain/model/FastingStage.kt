package com.drnitz.aro.domain.model

/**
 * Physiological stages of intermittent fasting.
 * Pure Kotlin clinical domain model.
 */
enum class FastingStage(
    val title: String,
    val description: String,
    val minHours: Double,
) {
    DIGESTIVE(
        title = "Digestive Phase",
        description = "Blood glucose and insulin normalize",
        minHours = 0.0,
    ),
    FAT_BURNING_PREP(
        title = "Fat Burning Prep",
        description = "Glycogen depleted, insulin levels drop",
        minHours = 4.0,
    ),
    KETOSIS(
        title = "Ketosis & Fat Oxidation",
        description = "Accelerated fat burning and ketone production",
        minHours = 12.0,
    ),
    AUTOPHAGY(
        title = "Autophagy & Cellular Renewal",
        description = "Cellular cleanup and damaged protein clearance",
        minHours = 16.0,
    );

    companion object {
        fun forElapsedHours(hours: Double): FastingStage = when {
            hours >= 16.0 -> AUTOPHAGY
            hours >= 12.0 -> KETOSIS
            hours >= 4.0 -> FAT_BURNING_PREP
            else -> DIGESTIVE
        }
    }
}
