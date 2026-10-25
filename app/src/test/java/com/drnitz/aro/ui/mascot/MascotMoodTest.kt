package com.drnitz.aro.ui.mascot

import com.drnitz.aro.data.repository.ActiveFast
import com.drnitz.aro.domain.Fixtures
import com.drnitz.aro.domain.calculation.EnergyPlan
import com.drnitz.aro.domain.clinical.MissingMacroAuditor
import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.EnergyBalance
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.Goal
import com.drnitz.aro.domain.model.MacroTargets
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.WorkoutLog
import com.drnitz.aro.domain.model.WorkoutType
import com.drnitz.aro.ui.main.MainScreenUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class MascotMoodTest {

    private fun createSuccessState(
        activeFast: ActiveFast? = null,
        targetProtein: Int = 150,
        loggedProtein: Double = 0.0,
        targetCalories: Int = 2000,
        consumedCalories: Int = 0,
        targetWaterMl: Int = 2500,
        loggedWaterMl: Int = 0,
        workouts: List<WorkoutLog> = emptyList(),
        meals: Map<MealSlot, List<FoodLogEntry>> = emptyMap(),
    ): MainScreenUiState.Success {
        val energyPlan = EnergyPlan(
            bmr = 1700,
            tdee = 2400,
            goal = Goal.MAINTAIN,
            goalAdjustment = 0,
            targetCalories = targetCalories,
            macros = MacroTargets(
                calories = targetCalories,
                proteinG = targetProtein,
                carbsG = 200,
                fatG = 60,
                fiberG = 30,
            ),
        )

        val energyBalance = EnergyBalance(
            targetCalories = targetCalories,
            caloriesEaten = consumedCalories,
            caloriesBurned = 0,
        )

        val dayTotals = NutrientTotals.of(
            NutrientKey.PROTEIN to loggedProtein,
            NutrientKey.CALORIES to consumedCalories.toDouble(),
        )

        val diaryDay = DiaryDay(
            date = LocalDate.of(2026, 10, 25),
            meals = meals,
            waterMl = loggedWaterMl,
            workouts = workouts,
        )

        return MainScreenUiState.Success(
            selectedDate = LocalDate.of(2026, 10, 25),
            isToday = true,
            formattedDate = "Sunday, Oct 25",
            profile = Fixtures.MALE,
            energyPlan = energyPlan,
            energyBalance = energyBalance,
            dayTotals = dayTotals,
            diaryDay = diaryDay,
            activeFast = activeFast,
            targetWaterMl = targetWaterMl,
            coverage = MissingMacroAuditor.audit(diaryDay, energyPlan.macros),
            waterLogCount = if (loggedWaterMl > 0) 1 else 0,
        )
    }

    @Test
    fun fastingActiveResolvesFastingZen() {
        val now = Instant.now()
        val state = createSuccessState(
            activeFast = ActiveFast.of("fast-1", now.minusSeconds(3600 * 4), 16, now),
        )
        val mood = resolveMascotMood(state, currentHour = 10)
        assertEquals(MascotMood.FASTING_ZEN, mood)

        val coaching = getMascotCoaching(state, currentHour = 10)
        assertEquals(MascotMood.FASTING_ZEN, coaching.mood)
        assertTrue(coaching.title.contains("Autophagy"))
    }

    @Test
    fun metProteinTargetResolvesCelebrating() {
        val state = createSuccessState(
            targetProtein = 140,
            loggedProtein = 145.0,
        )
        val mood = resolveMascotMood(state, currentHour = 14)
        assertEquals(MascotMood.CELEBRATING, mood)

        val coaching = getMascotCoaching(state, currentHour = 14)
        assertEquals(MascotMood.CELEBRATING, coaching.mood)
        assertTrue(coaching.title.contains("Milestone"))
    }

    @Test
    fun lateNightResolvesSleepy() {
        val state = createSuccessState()
        val moodLate = resolveMascotMood(state, currentHour = 23)
        assertEquals(MascotMood.SLEEPY, moodLate)

        val moodEarly = resolveMascotMood(state, currentHour = 3)
        assertEquals(MascotMood.SLEEPY, moodEarly)

        val coaching = getMascotCoaching(state, currentHour = 23)
        assertEquals(MascotMood.SLEEPY, coaching.mood)
        assertTrue(coaching.message.contains("growth hormone"))
    }

    @Test
    fun afternoonLowWaterResolvesThirsty() {
        val state = createSuccessState(
            targetWaterMl = 2500,
            loggedWaterMl = 500, // < 45% of 2500
        )
        val mood = resolveMascotMood(state, currentHour = 15)
        assertEquals(MascotMood.THIRSTY, mood)

        val coaching = getMascotCoaching(state, currentHour = 15)
        assertEquals(MascotMood.THIRSTY, coaching.mood)
        assertTrue(coaching.title.contains("Hydration"))
    }

    @Test
    fun daytimeActiveProgressResolvesEatingHealthy() {
        val state = createSuccessState(
            meals = mapOf(MealSlot.BREAKFAST to listOf(Fixtures.chickenBreast())),
        )
        val mood = resolveMascotMood(state, currentHour = 11)
        assertEquals(MascotMood.EATING_HEALTHY, mood)

        val coaching = getMascotCoaching(state, currentHour = 11)
        assertEquals(MascotMood.EATING_HEALTHY, coaching.mood)
        assertTrue(coaching.title.contains("Fueling"))
    }

    @Test
    fun workoutLoggedResolvesWorkoutBeast() {
        val state = createSuccessState(
            workouts = listOf(
                WorkoutLog(
                    id = "w1",
                    type = WorkoutType.STRENGTH,
                    exerciseName = "Bench Press",
                    loggedAt = Instant.now(),
                    met = 6.0,
                    durationMinutes = 45,
                ),
            ),
        )
        val mood = resolveMascotMood(state, currentHour = 16)
        assertEquals(MascotMood.WORKOUT_BEAST, mood)

        val coaching = getMascotCoaching(state, currentHour = 16)
        assertEquals(MascotMood.WORKOUT_BEAST, coaching.mood)
        assertTrue(coaching.title.contains("Beast Mode"))
    }

    @Test
    fun morningEmptyDayResolvesWelcoming() {
        val state = createSuccessState()
        val mood = resolveMascotMood(state, currentHour = 8)
        assertEquals(MascotMood.WELCOMING, mood)

        val coaching = getMascotCoaching(state, currentHour = 8)
        assertEquals(MascotMood.WELCOMING, coaching.mood)
        assertTrue(coaching.title.contains("Welcome"))
    }

    @Test
    fun afternoonEmptyDayResolvesHangingCurious() {
        val state = createSuccessState(targetWaterMl = 0) // No water deficit
        val mood = resolveMascotMood(state, currentHour = 14)
        assertEquals(MascotMood.HANGING_CURIOUS, mood)

        val coaching = getMascotCoaching(state, currentHour = 14)
        assertEquals(MascotMood.HANGING_CURIOUS, coaching.mood)
        assertTrue(coaching.title.contains("Aro is Here to Help"))
    }
}
