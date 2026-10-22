package com.drnitz.aro.domain.calculation

import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.EnergyBalance
import com.drnitz.aro.domain.model.WorkoutLog
import kotlin.math.roundToInt

/**
 * A Metabolic Equivalent of Task from the **Compendium of Physical Activities**.
 *
 * One MET is the oxygen cost of sitting quietly; 1 MET x 1 kg x 1 hour = ~1 kcal.
 * The 26 entries below are the subset Aro ships, ported unchanged from the web app's
 * `metActivities.js`. Full compendium: sites.google.com/site/compendiumofphysicalactivities.
 */
data class MetActivity(
    val id: String,
    val name: String,
    val category: String,
    val met: Double,
)

object MetCatalog {

    val all: List<MetActivity> = listOf(
        MetActivity("running-6mph", "Running (6 mph / 10 km/h, ~10 min/mile)", "Running", 9.8),
        MetActivity("running-7mph", "Running (7 mph / 11.3 km/h, fast pace)", "Running", 11.0),
        MetActivity("running-5mph", "Jogging (5 mph / 8 km/h, casual jog)", "Running", 8.3),
        MetActivity("walking-brisk", "Walking (Brisk, 3.5 - 4.0 mph)", "Walking", 4.3),
        MetActivity("walking-casual", "Walking (Casual / Dog walk, 2.8 - 3.2 mph)", "Walking", 3.3),
        MetActivity("cycling-moderate", "Cycling (Outdoor, Moderate 12-14 mph)", "Cycling", 8.0),
        MetActivity("cycling-vigorous", "Cycling (Racing / Vigorous >16 mph)", "Cycling", 10.0),
        MetActivity("cycling-stationary", "Stationary Bike / Spin Class", "Cycling", 7.0),
        MetActivity("swimming-moderate", "Swimming (Freestyle, Moderate laps)", "Swimming", 7.0),
        MetActivity("swimming-vigorous", "Swimming (Vigorous / Butterfly)", "Swimming", 9.8),
        MetActivity("weight-training-vigorous", "Weight Lifting (Vigorous gym workout / bodybuilding)", "Gym & Strength", 6.0),
        MetActivity("weight-training-moderate", "Weight Lifting (Standard resistance sets & rest)", "Gym & Strength", 3.8),
        MetActivity("hiit-circuit", "HIIT / Crossfit / Circuit Training", "Conditioning", 8.5),
        MetActivity("jump-rope", "Jump Rope (Moderate to Fast pace)", "Conditioning", 11.8),
        MetActivity("rowing-machine", "Rowing Machine (Moderate effort 100W)", "Conditioning", 7.0),
        MetActivity("elliptical-trainer", "Elliptical Trainer (Moderate pace)", "Cardio Machines", 5.5),
        MetActivity("stair-climber", "Stairmaster / Stair Stepper", "Cardio Machines", 9.0),
        MetActivity("basketball-game", "Basketball (Full court game)", "Sports", 8.0),
        MetActivity("soccer-football", "Soccer / Football (Competitive match)", "Sports", 10.0),
        MetActivity("tennis-singles", "Tennis (Singles match)", "Sports", 7.3),
        MetActivity("badminton-singles", "Badminton (Competitive match)", "Sports", 5.5),
        MetActivity("boxing-sparring", "Boxing (Sparring / Bag work)", "Martial Arts", 9.0),
        MetActivity("yoga-vinyasa", "Yoga (Power / Vinyasa flow)", "Mind & Body", 4.0),
        MetActivity("yoga-hatha", "Yoga (Hatha / Restorative / Stretching)", "Mind & Body", 2.5),
        MetActivity("pilates-reformer", "Pilates (Mat / Reformer)", "Mind & Body", 3.5),
        MetActivity("hiking-crosscountry", "Hiking (Cross-country with daypack)", "Outdoors", 6.5),
    )

    val categories: List<String> = all.map { it.category }.distinct().sorted()

    fun byId(id: String): MetActivity? = all.firstOrNull { it.id == id }

    fun inCategory(category: String): List<MetActivity> = all.filter { it.category == category }
}

/**
 * Calories burned = MET x body weight (kg) x duration (hours).
 *
 * Returns 0 rather than throwing when weight is unknown, so an unconfigured profile
 * degrades to "0 burned" rather than crashing a dashboard.
 */
fun caloriesBurned(met: Double, bodyWeightKg: Double, durationMinutes: Int): Int {
    if (met <= 0.0 || bodyWeightKg <= 0.0 || durationMinutes <= 0) return 0
    return (met * bodyWeightKg * (durationMinutes / 60.0)).roundToInt()
}

/**
 * Calorie burn for a logged workout, resolved against the weight snapshotted at log
 * time and falling back to [fallbackBodyWeightKg] for entries logged before Aro stored
 * a snapshot. Strength sessions use the volume-based estimate instead of MET.
 */
val WorkoutLog.caloriesBurned: Int
    get() = when {
        isStrength -> estimatedCaloriesBurned()
        else -> caloriesBurned(
            met = met,
            bodyWeightKg = bodyWeightKg ?: 0.0,
            durationMinutes = durationMinutes,
        )
    }

/**
 * Total calories burned in a day.
 *
 * Each workout is resolved against the weight snapshotted at log time, falling back to
 * [fallbackBodyWeightKg] for entries logged before Aro stored a snapshot (and 0 when no
 * fallback is supplied, which renders as "0 burned" rather than a crash).
 */
fun DiaryDay.caloriesBurned(fallbackBodyWeightKg: Double = 0.0): Int =
    workouts.sumOf { log ->
        when {
            log.isStrength -> log.estimatedCaloriesBurned()
            log.bodyWeightKg != null -> caloriesBurned(log.met, log.bodyWeightKg, log.durationMinutes)
            else -> caloriesBurned(log.met, fallbackBodyWeightKg, log.durationMinutes)
        }
    }

/** Day-level energy balance: `target - logged + burned = remaining`. */
fun DiaryDay.energyBalance(targetCalories: Int): EnergyBalance =
    com.drnitz.aro.domain.calculation.energyBalance(
        targetCalories = targetCalories,
        caloriesEaten = caloriesEaten,
        caloriesBurned = caloriesBurned(),
    )