package com.drnitz.aro.ui.mascot

import com.drnitz.aro.ui.main.MainScreenUiState
import java.time.LocalTime

/**
 * Expressive emotional and physiological states for Aro Bot.
 *
 * Each mood changes the mascot's facial geometry, eye curves,
 * ambient aura color, and antenna pulsation on the native canvas.
 */
enum class MascotMood(val label: String) {
    /** Default calm state with natural breathing and blinking. */
    IDLE("Ready & Focused"),

    /** Cheerful state when meals or workouts are logged. */
    HAPPY("Feeling Energetic"),

    /** Celebratory state when daily calorie, protein, or workout goals are met. */
    CELEBRATING("Goal Crushed!"),

    /** Meditative state during active fasting with a calm aura. */
    FASTING_ZEN("Fasting in Flow"),

    /** Thirsty state when water intake is behind target in the afternoon. */
    THIRSTY("Time to Hydrate"),

    /** Relaxed state late at night (past 10 PM) emphasizing rest. */
    SLEEPY("Rest & Recovery"),

    /** Inquisitive state when analyzing metrics or being tapped. */
    THINKING("Analyzing Vitals"),
}

/**
 * Proactive coaching diagnosis generated from live diary metrics.
 */
data class MascotCoaching(
    val title: String,
    val message: String,
    val actionableTip: String,
    val mood: MascotMood,
)

/**
 * Resolves the real-time mood of Aro Bot from the user's daily vitals.
 */
fun resolveMascotMood(
    state: MainScreenUiState.Success,
    currentHour: Int = LocalTime.now().hour,
): MascotMood {
    // 1. Active fasting takes primary physiological focus
    if (state.activeFast != null) {
        return MascotMood.FASTING_ZEN
    }

    // 2. Goal completion celebration
    val proteinGoal = state.energyPlan.macros.proteinG
    val proteinLogged = state.dayTotals.proteinG
    val targetCalories = state.energyPlan.targetCalories
    val consumedCalories = state.energyBalance.caloriesEaten
    val hasMetProtein = proteinGoal > 0 && proteinLogged >= proteinGoal
    val hasMetCalories = targetCalories > 0 && consumedCalories in (targetCalories - 120)..(targetCalories + 120)
    val hasCompletedWorkouts = state.diaryDay.workouts.isNotEmpty()

    if (hasMetProtein || (hasMetCalories && hasCompletedWorkouts)) {
        return MascotMood.CELEBRATING
    }

    // 3. Late night recovery mode
    if (currentHour >= 22 || currentHour < 5) {
        return MascotMood.SLEEPY
    }

    // 4. Hydration deficit check in the afternoon / evening
    val waterLogged = state.diaryDay.waterMl
    val waterTarget = state.targetWaterMl
    if (currentHour >= 13 && waterTarget > 0 && waterLogged < (waterTarget * 0.45f)) {
        return MascotMood.THIRSTY
    }

    // 5. Active progress today
    if (state.diaryDay.meals.isNotEmpty() || hasCompletedWorkouts) {
        return MascotMood.HAPPY
    }

    return MascotMood.IDLE
}

/**
 * Generates proactive contextual coaching from Dr. Nitz's clinical intelligence engine.
 */
fun getMascotCoaching(
    state: MainScreenUiState.Success,
    currentHour: Int = LocalTime.now().hour,
): MascotCoaching {
    val mood = resolveMascotMood(state, currentHour)
    val remainingCalories = state.energyBalance.remaining
    val proteinLogged = state.dayTotals.proteinG.toInt()
    val proteinTarget = state.energyPlan.macros.proteinG
    val waterLogged = state.diaryDay.waterMl
    val waterTarget = state.targetWaterMl

    return when (mood) {
        MascotMood.FASTING_ZEN -> {
            val fast = state.activeFast
            val elapsedHours = fast?.let {
                val start = it.startedAt.toEpochMilli()
                val now = System.currentTimeMillis()
                ((now - start) / 3_600_000L).coerceAtLeast(0)
            } ?: 0

            MascotCoaching(
                title = "Autophagy & Ketosis Flow",
                message = "You're $elapsedHours hours into your fast. Your insulin is low and metabolic renewal is actively running.",
                actionableTip = "Keep up the water and electrolytes. Black coffee or herbal tea won't break your metabolic fast.",
                mood = mood,
            )
        }
        MascotMood.CELEBRATING -> {
            MascotCoaching(
                title = "Fantastic Milestone!",
                message = "You hit your primary targets today! Protein is locked in at ${proteinLogged}g and energy balance is on point.",
                actionableTip = "Consistent days like this compound into remarkable long-term metabolic health and body composition.",
                mood = mood,
            )
        }
        MascotMood.THIRSTY -> {
            val neededMl = (waterTarget - waterLogged).coerceAtLeast(250)
            MascotCoaching(
                title = "Hydration Alert",
                message = "You've logged ${waterLogged}ml of your ${waterTarget}ml goal. Dehydration drops cognitive performance and muscle pump.",
                actionableTip = "Tap below to log a quick 250ml glass of water to bring your hydration curve back on track.",
                mood = mood,
            )
        }
        MascotMood.SLEEPY -> {
            MascotCoaching(
                title = "Rest & Growth Hormone Window",
                message = "Deep sleep triggers up to 70% of daily human growth hormone (HGH) release and neural recovery.",
                actionableTip = "Aim for 7 to 8 hours of uninterrupted sleep in a cool, dark room to recover for tomorrow.",
                mood = mood,
            )
        }
        MascotMood.HAPPY -> {
            val proteinLeft = (proteinTarget - proteinLogged).coerceAtLeast(0)
            MascotCoaching(
                title = "Steady Progress",
                message = "You have ${remainingCalories} kcal left for today with ${proteinLeft}g of protein remaining.",
                actionableTip = "Pick lean proteins and colorful micronutrient-dense vegetables for your next meal.",
                mood = mood,
            )
        }
        MascotMood.IDLE -> {
            MascotCoaching(
                title = "Hello! I'm Aro Bot",
                message = "I'm your native health companion. Today's budget is ${state.energyPlan.targetCalories} kcal with ${proteinTarget}g target protein.",
                actionableTip = "Log your first meal, workout, or glass of water to activate your daily metabolic streak!",
                mood = mood,
            )
        }
        MascotMood.THINKING -> {
            MascotCoaching(
                title = "Analyzing Vitals",
                message = "Calculating your macronutrient ratios, active energy expenditure, and adherence pace...",
                actionableTip = "Every log sharpens the clinical intelligence model to guide your fitness journey.",
                mood = mood,
            )
        }
    }
}
