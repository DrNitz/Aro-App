package com.drnitz.aro.ui.mascot

import com.drnitz.aro.ui.main.MainScreenUiState
import java.time.LocalTime

/**
 * Expressive emotional, physiological, and activity states for Aro, the cartoon health companion.
 *
 * Each mood changes Aro's cartoon anatomy, dynamic outfit, animated accessories
 * (dumbbells, water glass, crisp apple, sweatband, nightcap, confetti), and body physics.
 */
enum class MascotMood(val label: String, val shortGreeting: String) {
    /** Hanging playfully from the top bar, peeking down with curious eyes. */
    HANGING_CURIOUS("Hanging Around", "Ready to explore!"),

    /** Welcoming user on app launch or morning greeting with an energetic waving paw. */
    WELCOMING("Welcome Back!", "Good to see you!"),

    /** Pumping mini dumbbell after logging workouts or active exercise. */
    WORKOUT_BEAST("Beast Mode!", "Crushing fitness!"),

    /** Nibbling on a healthy crisp apple after logging meals. */
    EATING_HEALTHY("Fueling Up!", "Nutrition on point!"),

    /** Sipping refreshing water from a glass after hydrating. */
    DRINKING_WATER("Hydrated & Fresh", "Hydration level up!"),

    /** Thirsty state when water intake is behind target in the afternoon. */
    THIRSTY("Time to Hydrate", "Need some water!"),

    /** Celebratory state with confetti and victory poses when daily goals are crushed. */
    CELEBRATING("Goal Crushed!", "You are a champion!"),

    /** Meditative floating state with a zen halo during active fasting. */
    FASTING_ZEN("Fasting in Flow", "Autophagy active!"),

    /** Relaxed state late at night with a slouching nightcap emphasizing recovery sleep. */
    SLEEPY("Rest & Recovery", "Time to recharge Zzz");

    companion object {
        /** Backwards-compatibility alias for tests and previous references. */
        val IDLE: MascotMood get() = HANGING_CURIOUS
        val HAPPY: MascotMood get() = EATING_HEALTHY
        val THINKING: MascotMood get() = HANGING_CURIOUS
    }
}

/**
 * Proactive clinical coaching diagnosis generated from live diary metrics by Dr. Nitz.
 */
data class MascotCoaching(
    val title: String,
    val message: String,
    val actionableTip: String,
    val mood: MascotMood,
)

/**
 * Resolves the real-time mood of Aro from the user's daily vitals and live activities.
 */
fun resolveMascotMood(
    state: MainScreenUiState.Success,
    currentHour: Int = LocalTime.now().hour,
): MascotMood {
    // 1. Active fasting takes primary physiological focus
    if (state.activeFast != null) {
        return MascotMood.FASTING_ZEN
    }

    // 2. Goal completion celebration (protein or calorie budget met)
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

    // 3. Late night recovery mode (past 10 PM or before 5 AM)
    if (currentHour >= 22 || currentHour < 5) {
        return MascotMood.SLEEPY
    }

    // 4. Workout logged today -> Athletic Beast Mode
    if (hasCompletedWorkouts) {
        return MascotMood.WORKOUT_BEAST
    }

    // 5. Hydration deficit check in the afternoon / evening
    val waterLogged = state.diaryDay.waterMl
    val waterTarget = state.targetWaterMl
    if (currentHour >= 13 && waterTarget > 0 && waterLogged < (waterTarget * 0.45f)) {
        return MascotMood.THIRSTY
    }

    // 6. Good water logged today
    if (waterLogged >= 500 && state.diaryDay.meals.isEmpty()) {
        return MascotMood.DRINKING_WATER
    }

    // 7. Active nutrition progress today (meals logged)
    if (state.diaryDay.meals.isNotEmpty()) {
        return MascotMood.EATING_HEALTHY
    }

    // 8. Morning / App opening welcome (5 AM to 11 AM)
    if (currentHour in 5..11 && state.diaryDay.isEmpty) {
        return MascotMood.WELCOMING
    }

    return MascotMood.HANGING_CURIOUS
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
        MascotMood.WORKOUT_BEAST -> {
            val workoutCount = state.diaryDay.workouts.size
            MascotCoaching(
                title = "Beast Mode Activated!",
                message = "You've logged $workoutCount workout session today! Muscle protein synthesis is stimulated and insulin sensitivity is peak.",
                actionableTip = "Consume 25-40g of protein within a few hours to optimize muscle recovery and glycogen replenishment.",
                mood = mood,
            )
        }
        MascotMood.EATING_HEALTHY -> {
            val proteinLeft = (proteinTarget - proteinLogged).coerceAtLeast(0)
            MascotCoaching(
                title = "Steady Fueling Progress",
                message = "Meals are logged and metabolic tracking is active! You have ${remainingCalories} kcal left today with ${proteinLeft}g of protein remaining.",
                actionableTip = "Prioritize whole-food micronutrients, high-satiety fiber, and lean protein for your upcoming meal.",
                mood = mood,
            )
        }
        MascotMood.DRINKING_WATER -> {
            MascotCoaching(
                title = "Hydration Leveling Up",
                message = "Logged ${waterLogged}ml of water! Optimal blood volume supports joint lubrication, muscle pumps, and mental clarity.",
                actionableTip = "Sip steadily across the day rather than chugging in bulk for superior cellular retention.",
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
        MascotMood.WELCOMING -> {
            MascotCoaching(
                title = "Welcome! I'm Aro",
                message = "Good morning! Today's budget is ${state.energyPlan.targetCalories} kcal with ${proteinTarget}g target protein. Let's make today count!",
                actionableTip = "Start your morning with 300ml of water and log your breakfast to ignite your daily streak.",
                mood = mood,
            )
        }
        MascotMood.HANGING_CURIOUS -> {
            MascotCoaching(
                title = "Aro is Here to Help",
                message = "I'm your pocket health buddy hanging out on your dashboard! Tap my quick actions below to log water, meals, or workouts in one touch.",
                actionableTip = "Consistent small daily logs lead to monumental health transformations.",
                mood = mood,
            )
        }
    }
}
