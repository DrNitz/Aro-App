package com.drnitz.aro.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** The kinds of item a future day can contain. */
enum class PlanItemType(val label: String) {
    FOOD("Food"),
    EXERCISE("Exercise"),
    PILL("Pill"),
    FASTING("Fasting"),
    WATER("Water"),
}

/** How often a plan series repeats. A one-off plan is [NONE]. */
enum class PlanRepeat(val label: String) {
    NONE("Once"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly");

    /**
     * Sensible default end date when the user first switches a plan to repeat.
     * The UI lets the user choose a different date before saving.
     */
    fun defaultEndDate(startDate: LocalDate): LocalDate? = when (this) {
        NONE -> null
        DAILY -> startDate.plusDays(30)
        WEEKLY -> startDate.plusWeeks(12)
        MONTHLY -> startDate.plusMonths(12)
        YEARLY -> startDate.plusYears(5)
    }
}

/**
 * Food planned for a future day.
 *
 * Nutrients are frozen at plan time, exactly like a saved meal template: editing the
 * food library later must not silently change what the user planned to eat.
 */
data class PlannedFood(
    val meal: MealSlot,
    val grams: Double,
    val nutrients: NutrientTotals,
    val foodId: String? = null,
    val brand: String? = null,
    val isCustom: Boolean = false,
)

/**
 * Exercise planned for a future day.
 *
 * Strength exercises are selected from the exercise database; cardio is selected from
 * the MET activity catalog. Burn is estimated from [met], [durationMinutes] and the
 * profile's current body weight until the user logs the actual workout.
 */
data class PlannedExercise(
    val exerciseId: String? = null,
    val isStrength: Boolean = true,
    val durationMinutes: Int = 30,
    val met: Double = 6.0,
    val category: String = "",
    val muscle: String? = null,
    val equipment: String? = null,
)

/** A medication or supplement planned for a specific time. */
data class PlannedPill(
    val dosage: String = "",
    val savedReminderId: String? = null,
)

/**
 * A fasting window planned for a future day.
 *
 * This is only an intention. Marking it done starts a live fast at that moment; Aro
 * never invents a fast that the user did not actually start.
 */
data class PlannedFasting(
    val targetHours: Int = 16,
)

/**
 * A water target planned for a future day.
 *
 * Planned water is never added to the diary automatically. Marking it done opens a
 * prefilled amount that the user confirms as actually consumed.
 */
data class PlannedWater(
    val amountMl: Int = 250,
)

/**
 * One plan series. A one-off plan is a series with [repeat] = [PlanRepeat.NONE].
 *
 * The selected future date is [startDate]. Repeating occurrences are expanded from
 * this series at read time rather than materialised into the database, so editing a
 * series changes its future occurrences without rewriting rows.
 */
data class PlanItem(
    val id: String,
    val title: String,
    val type: PlanItemType,
    val startDate: LocalDate,
    val time: LocalTime,
    val repeat: PlanRepeat = PlanRepeat.NONE,
    val repeatUntil: LocalDate? = null,
    val reminderEnabled: Boolean = true,
    /** Remote thumbnail for the planned food or exercise, when the source supplied one. */
    val imageUrl: String? = null,
    val food: PlannedFood? = null,
    val exercise: PlannedExercise? = null,
    val pill: PlannedPill? = null,
    val fasting: PlannedFasting? = null,
    val water: PlannedWater? = null,
    val createdAt: Long = 0L,
) {
    /** True when this series has an occurrence on [date]. */
    fun occursOn(date: LocalDate): Boolean {
        if (date.isBefore(startDate)) return false
        val lastDate = repeatUntil
        if (lastDate != null && date.isAfter(lastDate)) return false

        return when (repeat) {
            PlanRepeat.NONE -> date == startDate
            PlanRepeat.DAILY -> true
            PlanRepeat.WEEKLY -> date.dayOfWeek == startDate.dayOfWeek
            PlanRepeat.MONTHLY -> date.dayOfMonth == startDate.dayOfMonthOn(date)
            PlanRepeat.YEARLY ->
                date.month == startDate.month &&
                    date.dayOfMonth == startDate.dayOfMonthOn(date)
        }
    }

    /**
     * Every occurrence in the inclusive range. Month-end plans clamp to the last day
     * of a shorter month but return to the original day in longer months (Jan 31 →
     * Feb 28 → Mar 31), which is the least surprising behaviour for a calendar plan.
     */
    fun occurrencesInRange(from: LocalDate, toInclusive: LocalDate): List<LocalDate> {
        if (toInclusive.isBefore(from)) return emptyList()
        val first = maxOf(from, startDate)
        val last = minOf(toInclusive, repeatUntil ?: toInclusive)
        if (last.isBefore(first)) return emptyList()

        val dates = mutableListOf<LocalDate>()
        var date = first
        while (!date.isAfter(last)) {
            if (occursOn(date)) dates += date
            date = date.plusDays(1)
        }
        return dates
    }

    /** The next occurrence on or after [date], or null when the series has ended. */
    fun nextOccurrenceOnOrAfter(date: LocalDate): LocalDate? {
        if (repeat == PlanRepeat.NONE) {
            return if (startDate >= date) startDate else null
        }
        val limit = repeatUntil ?: date.plusYears(5)
        var candidate = maxOf(date, startDate)
        // A yearly Feb-29 plan can need up to four years to recur; five years covers it.
        while (!candidate.isAfter(limit)) {
            if (occursOn(candidate)) return candidate
            candidate = candidate.plusDays(1)
        }
        return null
    }

    private fun LocalDate.dayOfMonthOn(reference: LocalDate): Int =
        minOf(startDate.dayOfMonth, reference.lengthOfMonth())
}

/** A future occurrence that should produce one local notification. */
data class PlanReminderOccurrence(
    val date: LocalDate,
    val triggerAt: LocalDateTime,
)

/**
 * The next reminder for this series, or null when reminders are disabled, the series
 * has ended, or every reachable future occurrence is already completed.
 *
 * Reminders are deliberately limited to a five-year look-ahead: it matches the
 * open-ended recurrence search horizon and avoids walking an unbounded series for a
 * notification the user will never receive.
 */
fun PlanItem.nextReminderOccurrence(
    now: LocalDateTime,
    completedDates: Set<LocalDate> = emptySet(),
): PlanReminderOccurrence? {
    if (!reminderEnabled) return null

    val lastSearchDate = now.toLocalDate().plusYears(MAX_REMINDER_SEARCH_YEARS)
    var searchFrom = maxOf(now.toLocalDate(), startDate)

    while (!searchFrom.isAfter(lastSearchDate)) {
        val occurrenceDate = nextOccurrenceOnOrAfter(searchFrom) ?: return null
        if (occurrenceDate.isAfter(lastSearchDate)) return null

        val triggerAt = LocalDateTime.of(occurrenceDate, time)
        if (triggerAt.isAfter(now) && occurrenceDate !in completedDates) {
            return PlanReminderOccurrence(date = occurrenceDate, triggerAt = triggerAt)
        }
        searchFrom = occurrenceDate.plusDays(1)
    }
    return null
}

private const val MAX_REMINDER_SEARCH_YEARS = 5L

/** One occurrence of a series, with completion state for that specific date. */
data class PlanOccurrence(
    val item: PlanItem,
    val date: LocalDate,
    val completedAt: Instant? = null,
    val actualLogId: String? = null,
    val skippedAt: Instant? = null,
) {
    val isCompleted: Boolean get() = completedAt != null && skippedAt == null
    val isSkipped: Boolean get() = skippedAt != null
    val isPending: Boolean get() = completedAt == null && skippedAt == null
}

/**
 * Planned intake, burn, water and fasting for one day.
 *
 * Completed and skipped occurrences are excluded from estimates. Planned water and
 * fasting are shown for context only; they never become diary totals until the user
 * confirms them through a completion dialog.
 */
data class PlanDaySummary(
    val plannedCalories: Int = 0,
    val plannedProteinG: Double = 0.0,
    val plannedCarbsG: Double = 0.0,
    val plannedFatG: Double = 0.0,
    val plannedBurnCalories: Int = 0,
    val plannedWaterMl: Int = 0,
    val plannedFastingHours: Int = 0,
) {
    val plannedNetCalories: Int get() = plannedCalories - plannedBurnCalories

    companion object {
        val EMPTY = PlanDaySummary()
    }
}
