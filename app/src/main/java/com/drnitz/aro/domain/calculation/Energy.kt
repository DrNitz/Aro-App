package com.drnitz.aro.domain.calculation

import com.drnitz.aro.domain.DomainResult
import com.drnitz.aro.domain.ValidationFailure
import com.drnitz.aro.domain.model.ActivityLevel
import com.drnitz.aro.domain.model.EnergyBalance
import com.drnitz.aro.domain.model.EnergyFactors
import com.drnitz.aro.domain.model.Goal
import com.drnitz.aro.domain.model.MacroPreset
import com.drnitz.aro.domain.model.MacroTargets
import com.drnitz.aro.domain.model.UserProfile
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Basal Metabolic Rate via the **Mifflin-St Jeor** equation:
 *
 * ```
 * male:   BMR = (10 x weightKg) + (6.25 x heightCm) - (5 x age) + 5
 * female: BMR = (10 x weightKg) + (6.25 x heightCm) - (5 x age) - 161
 * ```
 *
 * Reference: Mifflin MD, St Jeor ST (1990), *Am J Clin Nutr* 51(2):329-333.
 *
 * Unlike the web implementation, missing or out-of-range inputs return
 * [DomainResult.Failure] instead of the placeholder `1800` kcal, which previously let
 * an incomplete profile produce a confident-looking but meaningless target.
 */
fun basalMetabolicRate(profile: UserProfile): DomainResult<Int> {
    val errors = profile.validationErrors()
    if (errors.isNotEmpty()) {
        return DomainResult.Failure(ValidationFailure.OUT_OF_RANGE, errors[0])
    }
    val base = 10 * profile.weightKg + 6.25 * profile.heightCm - 5 * profile.ageYears
    return DomainResult.Success((base + profile.sex.mifflinStJeorOffset).roundToInt())
}

/**
 * Total Daily Energy Expenditure = BMR x physical activity factor.
 *
 * Deliberately takes an already-rounded BMR [Int] so Kotlin and the web app agree to
 * the calorie; feeding the unrounded value shifts the result by up to 2 kcal.
 */
fun totalDailyEnergyExpenditure(bmr: Int, activityLevel: ActivityLevel): Int {
    require(bmr > 0) { "BMR must be positive, was $bmr" }
    return (bmr * activityLevel.tdeeMultiplier).roundToInt()
}

/**
 * Daily calorie target = TDEE + the goal's energy offset, floored at
 * [MIN_SAFE_DAILY_CALORIES] so an aggressive cut on a small TDEE can never plan below
 * a safe intake.
 */
fun dailyCalorieTarget(tdee: Int, goal: Goal): Int {
    require(tdee > 0) { "TDEE must be positive, was $tdee" }
    return (tdee + goal.calorieAdjustment).coerceAtLeast(MIN_SAFE_DAILY_CALORIES)
}

/**
 * Splits a calorie target into gram targets using Atwater factors (4/4/9 kcal per g).
 *
 * Each macro is rounded to the nearest whole gram independently, so the resulting
 * energy can differ from [calories] by at most `0.5*4 + 0.5*4 + 0.5*9 = 8.5` kcal and by
 * less than 5 kcal for any realistic target — `macroTargets` keeps the exact preset
 * ratio rather than nudging a gram around to shave a calorie of rounding drift.
 *
 * Fiber follows the IOM DRI of 14 g per 1000 kcal.
 */
fun macroTargets(calories: Int, preset: MacroPreset = MacroPreset.BALANCED): MacroTargets {
    require(calories > 0) { "Calories must be positive, was $calories" }

    val proteinG = (calories * preset.proteinRatio / EnergyFactors.KCAL_PER_G_PROTEIN).roundToInt()
    val carbsG = (calories * preset.carbohydrateRatio / EnergyFactors.KCAL_PER_G_CARBOHYDRATE).roundToInt()
    val fatG = (calories * preset.fatRatio / EnergyFactors.KCAL_PER_G_FAT).roundToInt()

    val fiberG = ((calories / 1000.0) * EnergyFactors.FIBER_G_PER_1000_KCAL)
        .roundToInt()
        .coerceAtLeast(0)

    return MacroTargets(
        calories = calories,
        proteinG = proteinG.coerceAtLeast(0),
        carbsG = carbsG.coerceAtLeast(0),
        fatG = fatG.coerceAtLeast(0),
        fiberG = fiberG,
    )
}

/** The complete energy plan derived from a profile — what Task 12's planner screen renders. */
data class EnergyPlan(
    val bmr: Int,
    val tdee: Int,
    val goal: Goal,
    val goalAdjustment: Int,
    val targetCalories: Int,
    val macros: MacroTargets,
) {
    /** True when the goal's offset was clipped by [MIN_SAFE_DAILY_CALORIES]. */
    val hitSafetyFloor: Boolean get() = tdee + goal.calorieAdjustment < MIN_SAFE_DAILY_CALORIES
}

/**
 * Chains BMR -> TDEE -> calorie target -> macro split for a whole profile.
 * Returns the first failure encountered rather than a partially-filled plan.
 */
object EnergyPlanner {
    fun plan(profile: UserProfile): DomainResult<EnergyPlan> {
        val bmr = when (val result = basalMetabolicRate(profile)) {
            is DomainResult.Failure -> return result
            is DomainResult.Success -> result.value
        }
        val tdee = totalDailyEnergyExpenditure(bmr, profile.activityLevel)
        val targetCalories = dailyCalorieTarget(tdee, profile.goal)
        return DomainResult.Success(
            EnergyPlan(
                bmr = bmr,
                tdee = tdee,
                goal = profile.goal,
                goalAdjustment = profile.goal.calorieAdjustment,
                targetCalories = targetCalories,
                macros = macroTargets(targetCalories, profile.macroPreset),
            )
        )
    }
}

/** WHO body-mass-index bands. */
enum class BmiCategory(val label: String) {
    UNDERWEIGHT("Underweight"),
    NORMAL("Healthy / Normal"),
    OVERWEIGHT("Overweight"),
    OBESE("Obese"),
}

data class BmiResult(val value: Double, val category: BmiCategory)

/** BMI = weightKg / heightM^2, reported to one decimal place as clinical convention dictates. */
fun bodyMassIndex(weightKg: Double, heightCm: Double): DomainResult<BmiResult> {
    if (weightKg <= 0.0) {
        return DomainResult.Failure(ValidationFailure.MISSING_VALUE, "Enter a weight to calculate BMI")
    }
    if (heightCm !in UserProfile.HEIGHT_RANGE_CM) {
        return DomainResult.Failure(
            ValidationFailure.OUT_OF_RANGE,
            "Height must be ${UserProfile.HEIGHT_RANGE_CM.start}-" +
                "${UserProfile.HEIGHT_RANGE_CM.endInclusive} cm",
        )
    }
    val heightM = heightCm / 100.0
    val raw = weightKg / heightM.pow(2)
    val value = (raw * 10).roundToInt() / 10.0 // one decimal, rounded as clinical convention dictates
    val category = when {
        raw < 18.5 -> BmiCategory.UNDERWEIGHT
        raw < 25.0 -> BmiCategory.NORMAL
        raw < 30.0 -> BmiCategory.OVERWEIGHT
        else -> BmiCategory.OBESE
    }
    return DomainResult.Success(BmiResult(value, category))
}

/**
 * Aro's headline equation, `target - logged + burned = remaining`.
 * [remaining] is floored at zero; the overshoot is preserved in [EnergyBalance.surplus].
 */
fun energyBalance(targetCalories: Int, caloriesEaten: Int, caloriesBurned: Int = 0): EnergyBalance =
    EnergyBalance(
        targetCalories = targetCalories.coerceAtLeast(0),
        caloriesEaten = caloriesEaten.coerceAtLeast(0),
        caloriesBurned = caloriesBurned.coerceAtLeast(0),
    )

/**
 * Never plan below this intake, whatever the goal says. For smaller TDEEs the goal's
 * offset is clipped (see [EnergyPlan.hitSafetyFloor]) and the UI is expected to say so
 * rather than silently presenting the clipped number.
 */
const val MIN_SAFE_DAILY_CALORIES = 1200