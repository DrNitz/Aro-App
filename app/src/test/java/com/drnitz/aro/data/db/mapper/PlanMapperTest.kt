package com.drnitz.aro.data.db.mapper

import com.drnitz.aro.data.db.entity.PlanCompletionEntity
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.PlanItem
import com.drnitz.aro.domain.model.PlanItemType
import com.drnitz.aro.domain.model.PlanRepeat
import com.drnitz.aro.domain.model.PlannedFasting
import com.drnitz.aro.domain.model.PlannedFood
import com.drnitz.aro.domain.model.PlannedWater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class PlanMapperTest {

    private val date = LocalDate.of(2026, 10, 21)

    private fun foodPlan(id: String, title: String): PlanItem = PlanItem(
        id = id,
        title = title,
        type = PlanItemType.FOOD,
        startDate = date,
        time = LocalTime.of(8, 0),
        repeat = PlanRepeat.DAILY,
        repeatUntil = date.plusDays(1),
        food = PlannedFood(
            meal = MealSlot.BREAKFAST,
            grams = 100.0,
            nutrients = NutrientTotals.of(NutrientKey.CALORIES to 300.0),
        ),
        createdAt = 1L,
    )

    @Test
    fun planOccurrencesForDay_attachesCompletionToMatchingPlanOnly() {
        val first = foodPlan("first", "Oats")
        val second = foodPlan("second", "Eggs")
        val completion = PlanCompletionEntity(
            planId = "first",
            occurrenceEpochDay = date.toEpochDay(),
            completedAt = 1234L,
            actualLogId = "food-1",
        )

        val occurrences = planOccurrencesForDay(
            series = listOf(first.toEntity(), second.toEntity()),
            completions = listOf(completion),
            date = date,
        )

        assertEquals(2, occurrences.size)
        assertTrue(occurrences.first { it.item.id == "first" }.isCompleted)
        assertFalse(occurrences.first { it.item.id == "second" }.isCompleted)
        assertEquals("food-1", occurrences.first { it.item.id == "first" }.actualLogId)
    }

    @Test
    fun planOccurrencesInRange_completesOnlyMatchingOccurrenceDay() {
        val plan = foodPlan("daily", "Oats")
        val completion = PlanCompletionEntity(
            planId = "daily",
            occurrenceEpochDay = date.toEpochDay(),
            completedAt = 1234L,
        )

        val occurrences = planOccurrencesInRange(
            series = listOf(plan.toEntity()),
            completions = listOf(completion),
            from = date,
            toInclusive = date.plusDays(1),
        )

        assertEquals(2, occurrences.size)
        assertTrue(occurrences.first { it.date == date }.isCompleted)
        assertFalse(occurrences.first { it.date == date.plusDays(1) }.isCompleted)
    }

    @Test
    fun fastingAndWaterRoundTripPreservesTargets() {
        val fasting = PlanItem(
            id = "fast",
            title = "18h fast",
            type = PlanItemType.FASTING,
            startDate = date,
            time = LocalTime.of(20, 0),
            fasting = PlannedFasting(targetHours = 18),
        )
        val water = PlanItem(
            id = "water",
            title = "500 mL water",
            type = PlanItemType.WATER,
            startDate = date,
            time = LocalTime.NOON,
            water = PlannedWater(amountMl = 500),
        )

        assertEquals(18, fasting.toEntity().fastingTargetHours)
        assertEquals(18, fasting.toEntity().toDomain()?.fasting?.targetHours)
        assertEquals(500, water.toEntity().waterMl)
        assertEquals(500, water.toEntity().toDomain()?.water?.amountMl)
        assertNull(fasting.toEntity().waterMl)
        assertNull(water.toEntity().fastingTargetHours)
    }

    @Test
    fun foodAndExerciseImageRoundTrips() {
        val imageUrl = "https://example.com/food/oats.jpg"
        val plan = foodPlan("with-image", "Oats").copy(imageUrl = imageUrl)

        assertEquals(imageUrl, plan.toEntity().imageUrl)
        assertEquals(imageUrl, plan.toEntity().toDomain()?.imageUrl)
    }

    @Test
    fun skippedCompletionMapsToSkippedInsteadOfCompleted() {
        val plan = foodPlan("skip-me", "Oats")
        val completion = PlanCompletionEntity(
            planId = "skip-me",
            occurrenceEpochDay = date.toEpochDay(),
            completedAt = 1234L,
            skippedAt = 5678L,
        )

        val occurrence = planOccurrencesForDay(
            series = listOf(plan.toEntity()),
            completions = listOf(completion),
            date = date,
        ).single()

        assertTrue(occurrence.isSkipped)
        assertFalse(occurrence.isCompleted)
        assertFalse(occurrence.isPending)
        assertNull(occurrence.completedAt)
    }
}
