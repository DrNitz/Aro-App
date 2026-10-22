package com.drnitz.aro.ui.plan

import com.drnitz.aro.data.library.imageUrlAt
import com.drnitz.aro.domain.calculation.MetCatalog
import com.drnitz.aro.domain.model.Exercise
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.PlanItem
import com.drnitz.aro.domain.model.PlanItemType
import com.drnitz.aro.domain.model.PlannedExercise
import com.drnitz.aro.domain.model.PlannedFasting
import com.drnitz.aro.domain.model.PlannedFood
import com.drnitz.aro.domain.model.PlannedWater
import com.drnitz.aro.domain.model.WorkoutLog
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * Draft builders used when an add action targets a future day.
 *
 * These keep the future path separate from diary logging while reusing the exact same
 * plan model as the full planners. A quick-add is deliberately a one-off plan; the
 * user can change repeat and reminder settings from the Plan view afterwards.
 */
fun FoodLogEntry.toPlanItem(
    date: LocalDate,
    imageUrl: String? = null,
    time: LocalTime = defaultPlanTime(PlanItemType.FOOD, meal),
): PlanItem = PlanItem(
    id = UUID.randomUUID().toString(),
    title = name,
    type = PlanItemType.FOOD,
    startDate = date,
    time = time,
    reminderEnabled = true,
    imageUrl = imageUrl,
    food = PlannedFood(
        meal = meal,
        grams = grams,
        nutrients = nutrients,
        foodId = foodId,
        brand = brand,
        isCustom = isCustom,
    ),
    createdAt = System.currentTimeMillis(),
)

fun Exercise.toPlanItem(
    date: LocalDate,
    durationMinutes: Int = 30,
    time: LocalTime = defaultPlanTime(PlanItemType.EXERCISE),
): PlanItem {
    val cardio = category.equals("cardio", ignoreCase = true)
    val met = if (cardio) {
        MetCatalog.all.firstOrNull { it.name.equals(name, ignoreCase = true) }?.met ?: 6.0
    } else {
        6.0
    }
    return PlanItem(
        id = UUID.randomUUID().toString(),
        title = name,
        type = PlanItemType.EXERCISE,
        startDate = date,
        time = time,
        reminderEnabled = true,
        imageUrl = imageUrlAt(0),
        exercise = PlannedExercise(
            exerciseId = id,
            isStrength = !cardio,
            durationMinutes = durationMinutes.coerceAtLeast(1),
            met = met,
            category = category,
            muscle = primaryMuscles.firstOrNull(),
            equipment = equipment,
        ),
        createdAt = System.currentTimeMillis(),
    )
}

/**
 * Converts a quick cardio logger result into a one-off plan occurrence when the user is
 * viewing a future day. Distance and speed stay on the logged workout, not the plan.
 */
fun WorkoutLog.toPlanItem(
    date: LocalDate,
    time: LocalTime = defaultPlanTime(PlanItemType.EXERCISE),
): PlanItem = PlanItem(
    id = UUID.randomUUID().toString(),
    title = exerciseName,
    type = PlanItemType.EXERCISE,
    startDate = date,
    time = time,
    reminderEnabled = true,
    exercise = PlannedExercise(
        exerciseId = id,
        isStrength = isStrength,
        durationMinutes = durationMinutes.coerceAtLeast(1),
        met = met,
        category = category,
        muscle = muscle,
        equipment = equipment,
    ),
    createdAt = System.currentTimeMillis(),
)

fun waterPlanItem(
    date: LocalDate,
    amountMl: Int,
    time: LocalTime = defaultPlanTime(PlanItemType.WATER),
): PlanItem = PlanItem(
    id = UUID.randomUUID().toString(),
    title = "${amountMl.coerceAtLeast(1)} mL water",
    type = PlanItemType.WATER,
    startDate = date,
    time = time,
    reminderEnabled = true,
    water = PlannedWater(amountMl = amountMl.coerceAtLeast(1)),
    createdAt = System.currentTimeMillis(),
)

fun fastingPlanItem(
    date: LocalDate,
    targetHours: Int,
    time: LocalTime = defaultPlanTime(PlanItemType.FASTING),
): PlanItem {
    val hours = targetHours.coerceIn(1, 72)
    return PlanItem(
        id = UUID.randomUUID().toString(),
        title = "${hours}h fast",
        type = PlanItemType.FASTING,
        startDate = date,
        time = time,
        reminderEnabled = true,
        fasting = PlannedFasting(targetHours = hours),
        createdAt = System.currentTimeMillis(),
    )
}
