package com.drnitz.aro.domain.model

/**
 * The user's anthropometrics, activity level, goal and macro strategy.
 *
 * This is the single source of truth for every energy calculation in Aro. It is a pure
 * Kotlin value object — no Room, no DataStore, no Compose — so it can be validated and
 * tested on the JVM (see `UserProfileValidationTest`) before it is ever persisted.
 *
 * Task 5 will mirror these fields 1:1 into a Room `UserProfileEntity`.
 */
data class UserProfile(
    val name: String = "Health Champion",
    val sex: Sex = Sex.MALE,
    val ageYears: Int = 26,
    val weightKg: Double = 75.0,
    val targetWeightKg: Double = 70.0,
    val heightCm: Double = 178.0,
    val activityLevel: ActivityLevel = ActivityLevel.MODERATE,
    val goal: Goal = Goal.MILD_CUT,
    val macroPreset: MacroPreset = MacroPreset.HIGH_PROTEIN,
) {

    /** Height in metres — the unit BMI requires. */
    val heightMeters: Double get() = heightCm / 100.0

    /**
     * How much body mass still needs to come off. Positive means weight to lose,
     * negative means weight to gain. Drives the planner's projected timeline.
     */
    val weightDeltaKg: Double get() = weightKg - targetWeightKg

    /** Roughly how many kg of body mass changes per week at this goal's energy offset. */
    fun estimatedWeeklyChangeKg(): Double = goal.weeklyChangeKg

    /** Physiologically sane bounds used to reject nonsense input before any formula runs. */
    fun validationErrors(): List<String> {
        val errors = mutableListOf<String>()
        if (ageYears !in AGE_RANGE) errors += "Age must be ${AGE_RANGE.start}-${AGE_RANGE.endInclusive} years"
        if (heightCm !in HEIGHT_RANGE_CM) {
            errors += "Height must be ${HEIGHT_RANGE_CM.start}-${HEIGHT_RANGE_CM.endInclusive} cm"
        }
        if (weightKg !in WEIGHT_RANGE_KG) {
            errors += "Weight must be ${WEIGHT_RANGE_KG.start}-${WEIGHT_RANGE_KG.endInclusive} kg"
        }
        if (targetWeightKg !in WEIGHT_RANGE_KG) {
            errors += "Target weight must be ${WEIGHT_RANGE_KG.start}-${WEIGHT_RANGE_KG.endInclusive} kg"
        }
        if (weightDeltaKg > MAX_PLAUSIBLE_LOSS_KG) errors += "Target is more than $MAX_PLAUSIBLE_LOSS_KG kg below current weight"
        return errors
    }

    val isValid: Boolean get() = validationErrors().isEmpty()

    companion object {
        val AGE_RANGE = 13..100
        val HEIGHT_RANGE_CM = 100.0..250.0
        val WEIGHT_RANGE_KG = 25.0..400.0

        /** Beyond this, a goal implies an unsafe rate of loss; we refuse to plan for it. */
        const val MAX_PLAUSIBLE_LOSS_KG = 60.0

        /** Mirrors the web app's `DEFAULT_PROFILE` so both clients behave identically. */
        val DEFAULT = UserProfile()
    }
}

/**
 * Sex is recorded only because the Mifflin-St Jeor equation requires it.
 * Aro stores it locally, never transmits it, and it drives nothing else.
 */
enum class Sex(val mifflinStJeorOffset: Double) {
    MALE(5.0),
    FEMALE(-161.0),
}

/**
 * Physical Activity Level multipliers applied to BMR to obtain TDEE.
 *
 * Source: Mifflin & St Jeor (1990); activity factors as used by the NIH/Academy
 * Nutrition and Dietetics reference set, matching the web app's factors exactly.
 */
enum class ActivityLevel(val tdeeMultiplier: Double, val label: String) {
    SEDENTARY(1.2, "Sedentary — little or no exercise"),
    LIGHT(1.375, "Light — exercise 1-3 days/week"),
    MODERATE(1.55, "Moderate — exercise 3-5 days/week"),
    VERY_ACTIVE(1.725, "Very active — exercise 6-7 days/week"),
    EXTRA_ACTIVE(1.9, "Extra active — intense daily training or physical job"),
}

/**
 * Energy offset applied to TDEE, plus the implied rate of body-mass change.
 * 1 kg of body fat ~= 7700 kcal, so 500 kcal/day ~= 0.45 kg/week.
 */
enum class Goal(val calorieAdjustment: Int, val label: String) {
    AGGRESSIVE_CUT(-500, "Aggressive cut (-500 kcal)"),
    MILD_CUT(-250, "Mild recomposition cut (-250 kcal)"),
    MAINTAIN(0, "Maintenance"),
    LEAN_BULK(250, "Lean bulk (+250 kcal)"),
    HYPERTROPHY_BULK(500, "Hypertrophy bulk (+500 kcal)");

    /** Implied weekly change in body mass: negative while cutting, positive while bulking. */
val weeklyChangeKg: Double get() = (calorieAdjustment * 7) / EnergyFactors.KCAL_PER_KG_FAT
}

/**
 * Macro splits as a fraction of total energy. Fractions sum to 1.0 for every preset.
 * Protein and carbohydrate yield 4 kcal/g, fat yields 9 kcal/g.
 */
enum class MacroPreset(
    val proteinRatio: Double,
    val carbohydrateRatio: Double,
    val fatRatio: Double,
    val label: String,
) {
    BALANCED(0.30, 0.40, 0.30, "Balanced (30/40/30)"),
    HIGH_PROTEIN(0.40, 0.35, 0.25, "High protein (40/35/25)"),
    LOW_CARB(0.30, 0.20, 0.50, "Low carb (30/20/50)"),
    KETO(0.20, 0.05, 0.75, "Keto (20/5/75)")
}