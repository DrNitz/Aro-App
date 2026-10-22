package com.drnitz.aro.domain

import com.drnitz.aro.domain.calculation.planDaySummary
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.PlanDaySummary
import com.drnitz.aro.domain.model.PlanItem
import com.drnitz.aro.domain.model.PlanItemType
import com.drnitz.aro.domain.model.PlanOccurrence
import com.drnitz.aro.domain.model.PlanRepeat
import com.drnitz.aro.domain.model.PlannedExercise
import com.drnitz.aro.domain.model.PlannedFasting
import com.drnitz.aro.domain.model.PlannedFood
import com.drnitz.aro.domain.model.PlannedPill
import com.drnitz.aro.domain.model.PlannedWater
import com.drnitz.aro.domain.model.nextReminderOccurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PlanTest {

    private val start = LocalDate.of(2026, 1, 31)

    private fun plan(
        repeat: PlanRepeat = PlanRepeat.NONE,
        end: LocalDate? = null,
        date: LocalDate = start,
    ) = PlanItem(
        id = "plan-1",
        title = "Plan",
        type = PlanItemType.PILL,
        startDate = date,
        time = LocalTime.of(8, 0),
        repeat = repeat,
        repeatUntil = end,
        pill = PlannedPill(dosage = "1 tablet"),
    )

    @Test
    fun oneOffOccursOnlyOnSelectedDate() {
        val item = plan()

        assertTrue(item.occursOn(start))
        assertFalse(item.occursOn(start.plusDays(1)))
        assertEquals(listOf(start), item.occurrencesInRange(start.minusDays(3), start.plusDays(3)))
        assertNull(item.nextOccurrenceOnOrAfter(start.plusDays(1)))
    }

    @Test
    fun dailyOccurrenceStopsAtEndDate() {
        val item = plan(PlanRepeat.DAILY, start.plusDays(2))

        assertEquals(
            listOf(start, start.plusDays(1), start.plusDays(2)),
            item.occurrencesInRange(start, start.plusDays(5)),
        )
        assertEquals(start.plusDays(1), item.nextOccurrenceOnOrAfter(start.plusDays(1)))
    }

    @Test
    fun weeklyOccurrenceUsesStartDayOfWeek() {
        val item = plan(PlanRepeat.WEEKLY, start.plusWeeks(3))

        assertEquals(
            listOf(start, start.plusWeeks(1), start.plusWeeks(2), start.plusWeeks(3)),
            item.occurrencesInRange(start, start.plusWeeks(6)),
        )
        assertFalse(item.occursOn(start.plusDays(2)))
    }

    @Test
    fun monthlyOccurrenceClampsToLastDayThenReturnsToOriginalDay() {
        val item = plan(PlanRepeat.MONTHLY, LocalDate.of(2026, 4, 30))

        val dates = item.occurrencesInRange(start, LocalDate.of(2026, 4, 30))

        assertEquals(
            listOf(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30),
            ),
            dates,
        )
    }

    @Test
    fun yearlyLeapDayOccurrenceClampsInNonLeapYears() {
        val leapDay = LocalDate.of(2024, 2, 29)
        val item = plan(PlanRepeat.YEARLY, LocalDate.of(2028, 2, 29), leapDay)

        assertEquals(
            listOf(
                LocalDate.of(2024, 2, 29),
                LocalDate.of(2025, 2, 28),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2028, 2, 29),
            ),
            item.occurrencesInRange(leapDay, LocalDate.of(2028, 2, 29)),
        )
    }

    @Test
    fun occurrencesInRangeIncludesOnlyRequestedWindow() {
        val item = plan(PlanRepeat.DAILY, start.plusDays(10))

        assertEquals(
            listOf(start.plusDays(4), start.plusDays(5)),
            item.occurrencesInRange(start.plusDays(4), start.plusDays(5)),
        )
    }

    @Test
    fun reminderUsesFutureOneOffOccurrence() {
        val item = plan()
        val now = LocalDateTime.of(start, LocalTime.of(7, 30))

        val reminder = item.nextReminderOccurrence(now)

        assertEquals(start, reminder?.date)
        assertEquals(LocalDateTime.of(start, LocalTime.of(8, 0)), reminder?.triggerAt)
    }

    @Test
    fun reminderIgnoresPastAndExactlyDueOneOffOccurrence() {
        val item = plan()

        assertNull(item.nextReminderOccurrence(LocalDateTime.of(start, LocalTime.of(8, 0))))
        assertNull(
            item.nextReminderOccurrence(
                LocalDateTime.of(start.plusDays(1), LocalTime.of(7, 0)),
            ),
        )
    }

    @Test
    fun dailyReminderAdvancesToTomorrowAfterTodaysTimeHasPassed() {
        val item = plan(PlanRepeat.DAILY, start.plusDays(3))
        val now = LocalDateTime.of(start, LocalTime.of(9, 0))

        val reminder = item.nextReminderOccurrence(now)

        assertEquals(start.plusDays(1), reminder?.date)
        assertEquals(LocalDateTime.of(start.plusDays(1), LocalTime.of(8, 0)), reminder?.triggerAt)
    }

    @Test
    fun reminderSkipsCompletedOccurrences() {
        val item = plan(PlanRepeat.DAILY, start.plusDays(3))
        val now = LocalDateTime.of(start, LocalTime.of(7, 0))

        val reminder = item.nextReminderOccurrence(
            now = now,
            completedDates = setOf(start, start.plusDays(1)),
        )

        assertEquals(start.plusDays(2), reminder?.date)
    }

    @Test
    fun reminderIsNullWhenDisabledOrSeriesEnded() {
        val disabled = plan(PlanRepeat.DAILY, start.plusDays(2)).copy(reminderEnabled = false)
        val now = LocalDateTime.of(start, LocalTime.of(7, 0))

        assertNull(disabled.nextReminderOccurrence(now))

        val ended = plan(PlanRepeat.DAILY, start)
        assertNull(
            ended.nextReminderOccurrence(
                LocalDateTime.of(start.plusDays(1), LocalTime.of(7, 0)),
            ),
        )
    }

    @Test
    fun monthlyReminderHandlesMonthEndClamping() {
        val item = plan(PlanRepeat.MONTHLY, LocalDate.of(2026, 4, 30))
        val now = LocalDateTime.of(LocalDate.of(2026, 2, 28), LocalTime.of(9, 0))

        val reminder = item.nextReminderOccurrence(now)

        assertEquals(LocalDate.of(2026, 3, 31), reminder?.date)
    }

    @Test
    fun planSummaryAddsFoodAndBurnButExcludesCompletedAndPills() {
        val food = PlannedFood(
            meal = MealSlot.LUNCH,
            grams = 200.0,
            nutrients = NutrientTotals.of(
                NutrientKey.CALORIES to 500.0,
                NutrientKey.PROTEIN to 30.0,
                NutrientKey.CARBS to 50.0,
                NutrientKey.FAT to 20.0,
            ),
        )
        val exercise = PlannedExercise(
            isStrength = false,
            durationMinutes = 60,
            met = 6.0,
        )
        val foodItem = PlanItem(
            id = "food",
            title = "Chicken and rice",
            type = PlanItemType.FOOD,
            startDate = start,
            time = LocalTime.NOON,
            food = food,
        )
        val exerciseItem = PlanItem(
            id = "exercise",
            title = "Brisk walk",
            type = PlanItemType.EXERCISE,
            startDate = start,
            time = LocalTime.of(18, 0),
            exercise = exercise,
        )
        val pillItem = PlanItem(
            id = "pill",
            title = "Vitamin D",
            type = PlanItemType.PILL,
            startDate = start,
            time = LocalTime.of(8, 0),
            pill = PlannedPill(dosage = "1000 IU"),
        )
        val completedFood = PlanOccurrence(
            item = foodItem.copy(id = "food-done"),
            date = start,
            completedAt = Instant.parse("2026-01-31T12:00:00Z"),
        )

        val summary = planDaySummary(
            occurrences = listOf(
                PlanOccurrence(foodItem, start),
                PlanOccurrence(exerciseItem, start),
                PlanOccurrence(pillItem, start),
                completedFood,
            ),
            bodyWeightKg = 80.0,
        )

        assertEquals(500, summary.plannedCalories)
        assertEquals(30.0, summary.plannedProteinG, 0.01)
        assertEquals(50.0, summary.plannedCarbsG, 0.01)
        assertEquals(20.0, summary.plannedFatG, 0.01)
        assertEquals(480, summary.plannedBurnCalories)
        assertEquals(20, summary.plannedNetCalories)
    }

    @Test
    fun planSummaryIncludesCompletedWhenRequested() {
        val foodItem = PlanItem(
            id = "food",
            title = "Oats",
            type = PlanItemType.FOOD,
            startDate = start,
            time = LocalTime.of(8, 0),
            food = PlannedFood(
                meal = MealSlot.BREAKFAST,
                grams = 100.0,
                nutrients = NutrientTotals.of(NutrientKey.CALORIES to 380.0),
            ),
        )
        val occurrence = PlanOccurrence(
            item = foodItem,
            date = start,
            completedAt = Instant.parse("2026-01-31T08:00:00Z"),
        )

        val summary = planDaySummary(listOf(occurrence), 80.0, includeCompleted = true)

        assertEquals(380, summary.plannedCalories)
        assertEquals(PlanDaySummary.EMPTY, planDaySummary(emptyList(), 80.0))
    }

    @Test
    fun planSummaryShowsWaterAndFastingWithoutAddingCalories() {
        val foodItem = PlanItem(
            id = "food",
            title = "Oats",
            type = PlanItemType.FOOD,
            startDate = start,
            time = LocalTime.of(8, 0),
            food = PlannedFood(
                meal = MealSlot.BREAKFAST,
                grams = 100.0,
                nutrients = NutrientTotals.of(NutrientKey.CALORIES to 380.0),
            ),
        )
        val waterItem = PlanItem(
            id = "water",
            title = "500 mL water",
            type = PlanItemType.WATER,
            startDate = start,
            time = LocalTime.NOON,
            water = PlannedWater(amountMl = 500),
        )
        val fastingItem = PlanItem(
            id = "fast",
            title = "16h fast",
            type = PlanItemType.FASTING,
            startDate = start,
            time = LocalTime.of(20, 0),
            fasting = PlannedFasting(targetHours = 16),
        )

        val summary = planDaySummary(
            occurrences = listOf(
                PlanOccurrence(foodItem, start),
                PlanOccurrence(waterItem, start),
                PlanOccurrence(fastingItem, start),
            ),
            bodyWeightKg = 80.0,
        )

        assertEquals(380, summary.plannedCalories)
        assertEquals(500, summary.plannedWaterMl)
        assertEquals(16, summary.plannedFastingHours)
    }

    @Test
    fun planSummaryExcludesSkippedOccurrences() {
        val foodItem = PlanItem(
            id = "food",
            title = "Oats",
            type = PlanItemType.FOOD,
            startDate = start,
            time = LocalTime.of(8, 0),
            food = PlannedFood(
                meal = MealSlot.BREAKFAST,
                grams = 100.0,
                nutrients = NutrientTotals.of(NutrientKey.CALORIES to 380.0),
            ),
        )
        val skipped = PlanOccurrence(
            item = foodItem,
            date = start,
            skippedAt = Instant.parse("2026-01-31T08:00:00Z"),
        )

        val summary = planDaySummary(listOf(skipped), 80.0)

        assertEquals(0, summary.plannedCalories)
        assertEquals(0, summary.plannedWaterMl)
        assertEquals(0, summary.plannedFastingHours)
        assertFalse(skipped.isPending)
    }
}
