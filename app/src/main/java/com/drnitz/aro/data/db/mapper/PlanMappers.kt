package com.drnitz.aro.data.db.mapper

import com.drnitz.aro.data.db.NutrientColumns
import com.drnitz.aro.data.db.entity.PlanCompletionEntity
import com.drnitz.aro.data.db.entity.PlanSeriesEntity
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.PlanItem
import com.drnitz.aro.domain.model.PlanItemType
import com.drnitz.aro.domain.model.PlanOccurrence
import com.drnitz.aro.domain.model.PlanRepeat
import com.drnitz.aro.domain.model.PlannedExercise
import com.drnitz.aro.domain.model.PlannedFasting
import com.drnitz.aro.domain.model.PlannedFood
import com.drnitz.aro.domain.model.PlannedPill
import com.drnitz.aro.domain.model.PlannedWater
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Rebuilds a plan series, returning null for an unreadable legacy/corrupt row. */
fun PlanSeriesEntity.toDomain(): PlanItem? {
    val itemType = enumOrNull<PlanItemType>(type) ?: return null
    val repeat = enumOrNull<PlanRepeat>(repeatRule) ?: PlanRepeat.NONE
    val minuteOfDay = timeMinuteOfDay.coerceIn(0, 24 * 60 - 1)
    val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    val meal = enumOrNull<MealSlot>(mealSlot)

    val food = if (itemType == PlanItemType.FOOD && meal != null) {
        val gramsValue = grams ?: 0.0
        PlannedFood(
            meal = meal,
            grams = gramsValue,
            nutrients = toNutrientTotals(),
            foodId = foodId,
            brand = brand,
            isCustom = isCustom ?: false,
        )
    } else {
        null
    }

    val exercise = if (itemType == PlanItemType.EXERCISE) {
        PlannedExercise(
            exerciseId = exerciseId,
            isStrength = isStrength ?: true,
            durationMinutes = (durationMinutes ?: 30).coerceAtLeast(1),
            met = (met ?: 6.0).coerceAtLeast(0.1),
            category = category.orEmpty(),
            muscle = muscle,
            equipment = equipment,
        )
    } else {
        null
    }

    val pill = if (itemType == PlanItemType.PILL) {
        PlannedPill(
            dosage = dosage.orEmpty(),
            savedReminderId = savedReminderId,
        )
    } else {
        null
    }

    val fasting = if (itemType == PlanItemType.FASTING) {
        PlannedFasting(targetHours = (fastingTargetHours ?: 16).coerceIn(1, 72))
    } else {
        null
    }

    val water = if (itemType == PlanItemType.WATER) {
        PlannedWater(amountMl = (waterMl ?: 250).coerceAtLeast(1))
    } else {
        null
    }

    return PlanItem(
        id = id,
        title = title,
        type = itemType,
        startDate = LocalDate.ofEpochDay(startEpochDay),
        time = time,
        repeat = repeat,
        repeatUntil = repeatUntilEpochDay?.let(LocalDate::ofEpochDay),
        reminderEnabled = reminderEnabled,
        imageUrl = imageUrl,
        food = food,
        exercise = exercise,
        pill = pill,
        fasting = fasting,
        water = water,
        createdAt = createdAt,
    )
}

fun PlanItem.toEntity(): PlanSeriesEntity {
    val n = food?.nutrients
    fun v(key: NutrientKey): Double? = n?.let { NutrientColumns.valueOf(it, key) }

    return PlanSeriesEntity(
        id = id,
        type = type.name,
        title = title,
        startEpochDay = startDate.toEpochDay(),
        timeMinuteOfDay = time.hour * 60 + time.minute,
        repeatRule = repeat.name,
        repeatUntilEpochDay = repeatUntil?.toEpochDay(),
        reminderEnabled = reminderEnabled,
        createdAt = createdAt,
        imageUrl = imageUrl,
        mealSlot = food?.meal?.name,
        foodId = food?.foodId,
        brand = food?.brand,
        grams = food?.grams,
        isCustom = food?.isCustom,
        exerciseId = exercise?.exerciseId,
        isStrength = exercise?.isStrength,
        durationMinutes = exercise?.durationMinutes,
        met = exercise?.met,
        category = exercise?.category,
        muscle = exercise?.muscle,
        equipment = exercise?.equipment,
        dosage = pill?.dosage,
        savedReminderId = pill?.savedReminderId,
        fastingTargetHours = fasting?.targetHours,
        waterMl = water?.amountMl,
        calories = v(NutrientKey.CALORIES),
        protein = v(NutrientKey.PROTEIN),
        carbs = v(NutrientKey.CARBS),
        fat = v(NutrientKey.FAT),
        saturatedFat = v(NutrientKey.SATURATED_FAT),
        fiber = v(NutrientKey.FIBER),
        sugar = v(NutrientKey.SUGAR),
        sodium = v(NutrientKey.SODIUM),
        potassium = v(NutrientKey.POTASSIUM),
        calcium = v(NutrientKey.CALCIUM),
        magnesium = v(NutrientKey.MAGNESIUM),
    )
}

fun PlanOccurrence.toCompletionEntity(): PlanCompletionEntity = PlanCompletionEntity(
    planId = item.id,
    occurrenceEpochDay = date.toEpochDay(),
    completedAt = completedAt?.toEpochMilli() ?: System.currentTimeMillis(),
    actualLogId = actualLogId,
    skippedAt = skippedAt?.toEpochMilli(),
)

fun PlanOccurrence.withCompletion(completion: PlanCompletionEntity?): PlanOccurrence =
    if (completion == null) {
        this
    } else {
        val skipped = completion.skippedAt?.let(Instant::ofEpochMilli)
        copy(
            // A skipped occurrence contains a legacy completion timestamp because the
            // column is non-null; do not expose it as a completed plan.
            completedAt = if (skipped == null) Instant.ofEpochMilli(completion.completedAt) else null,
            actualLogId = completion.actualLogId,
            skippedAt = skipped,
        )
    }

/** Converts a completion row to a lightweight map keyed by its occurrence day. */
fun List<PlanCompletionEntity>.byOccurrenceDay(): Map<Long, PlanCompletionEntity> =
    associateBy { it.occurrenceEpochDay }

/** Builds a domain occurrence from a series and an optional completion. */
fun PlanSeriesEntity.toOccurrence(
    date: LocalDate,
    completion: PlanCompletionEntity?,
): PlanOccurrence? {
    val item = toDomain() ?: return null
    return PlanOccurrence(item = item, date = date).withCompletion(completion)
}

/** Expands all series for one date and attaches the completion belonging to each plan. */
fun planOccurrencesForDay(
    series: List<PlanSeriesEntity>,
    completions: List<PlanCompletionEntity>,
    date: LocalDate,
): List<PlanOccurrence> {
    val byPlanId = completions.associateBy { it.planId }
    return series.mapNotNull { entity ->
        val item = entity.toDomain() ?: return@mapNotNull null
        if (!item.occursOn(date)) {
            null
        } else {
            PlanOccurrence(item = item, date = date)
                .withCompletion(byPlanId[item.id])
        }
    }.sortedWith(compareBy({ it.item.time }, { it.item.title }))
}

/** Expands all series in an inclusive range and attaches per-occurrence completions. */
fun planOccurrencesInRange(
    series: List<PlanSeriesEntity>,
    completions: List<PlanCompletionEntity>,
    from: LocalDate,
    toInclusive: LocalDate,
): List<PlanOccurrence> {
    if (toInclusive.isBefore(from)) return emptyList()
    val byKey = completions.associateBy { it.planId to it.occurrenceEpochDay }
    return series.flatMap { entity ->
        val item = entity.toDomain() ?: return@flatMap emptyList()
        item.occurrencesInRange(from, toInclusive).map { date ->
            PlanOccurrence(item = item, date = date)
                .withCompletion(byKey[item.id to date.toEpochDay()])
        }
    }.sortedWith(compareBy({ it.date }, { it.item.time }, { it.item.title }))
}

private inline fun <reified T : Enum<T>> enumOrNull(value: String?): T? {
    if (value == null) return null
    return enumValues<T>().firstOrNull { it.name == value }
}
