package com.drnitz.aro.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drnitz.aro.data.db.DatabaseTestBase
import com.drnitz.aro.data.db.FixedDateProvider
import com.drnitz.aro.data.db.mapper.toDomain
import com.drnitz.aro.domain.calculation.caloriesBurned
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.SetEntry
import com.drnitz.aro.domain.model.WorkoutLog
import com.drnitz.aro.domain.model.WorkoutType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * End-to-end repository tests against real SQLite.
 *
 * These are the tests that would catch the mistake that matters most in a diary app:
 * logging a food landing on the wrong day, or yesterday's totals bleeding into today's
 * dashboard.
 */
@RunWith(AndroidJUnit4::class)
class RoomAroRepositoryTest : DatabaseTestBase() {

    private val repository by lazy { RoomAroRepository(db, FixedDateProvider(today)) }

    private fun food(
        id: String,
        meal: MealSlot,
        calories: Double,
        name: String = "Food $id",
    ) = FoodLogEntry(
        id = id,
        name = name,
        meal = meal,
        grams = 100.0,
        nutrients = NutrientTotals.of(
            NutrientKey.CALORIES to calories,
            NutrientKey.PROTEIN to 20.0,
            NutrientKey.CARBS to 30.0,
            NutrientKey.FAT to 5.0,
        ),
    )

    @Test
    fun loggingToYesterdayDoesNotChangeTodaysTotals() = runBlocking {
        repository.addFood(food("y1", MealSlot.BREAKFAST, 400.0), yesterday)
        repository.addFood(food("t1", MealSlot.BREAKFAST, 500.0), today)

        assertEquals(500.0, repository.observeDayTotals(today).first().calories, 0.0001)
        assertEquals(400.0, repository.observeDayTotals(yesterday).first().calories, 0.0001)
    }

    @Test
    fun aDayIsReadBackWithItsOwnMealsOnly() = runBlocking {
        repository.addFood(food("y1", MealSlot.BREAKFAST, 400.0), yesterday)
        repository.addFood(food("t1", MealSlot.BREAKFAST, 500.0), today)
        repository.addFood(food("t2", MealSlot.DINNER, 700.0), today)

        val day = repository.observeDiaryDay(today).first()
        assertEquals(today, day.date)
        assertEquals(2, day.foodEntries.size)
        assertEquals(1200, day.caloriesEaten)
        assertEquals(listOf("t1"), day.meals[MealSlot.BREAKFAST]!!.map { it.id })

        val yesterdayDay = repository.observeDiaryDay(yesterday).first()
        assertEquals(400, yesterdayDay.caloriesEaten)
        assertTrue(yesterdayDay.meals[MealSlot.DINNER]!!.isEmpty())
    }

    @Test
    fun anUnloggedDayIsEmptyRatherThanZero() = runBlocking {
        repository.addFood(food("t1", MealSlot.BREAKFAST, 500.0), today)

        val untouched = repository.observeDiaryDay(yesterday).first()
        assertTrue(untouched.isEmpty)
        assertEquals(0, untouched.caloriesEaten)
        assertFalse(untouched.nutrientTotals[NutrientKey.CALORIES].isReported)
    }

    @Test
    fun deletingFoodUpdatesTotalsAndRevertsToEmpty() = runBlocking {
        repository.addFood(food("t1", MealSlot.BREAKFAST, 500.0), today)
        assertEquals(500.0, repository.observeDayTotals(today).first().calories, 0.0001)

        repository.deleteFood("t1")
        assertEquals(0.0, repository.observeDayTotals(today).first().calories, 0.0001)
    }

    @Test
    fun movingFoodAcrossDaysMovesTheCaloriesToo() = runBlocking {
        repository.addFood(food("t1", MealSlot.BREAKFAST, 500.0), today)

        repository.moveFood("t1", toMeal = MealSlot.SNACKS, toDate = yesterday)

        assertEquals(0.0, repository.observeDayTotals(today).first().calories, 0.0001)
        assertEquals(500.0, repository.observeDayTotals(yesterday).first().calories, 0.0001)
        val moved = repository.observeDiaryDay(yesterday).first()
        assertEquals(MealSlot.SNACKS, moved.foodEntries.single().meal)
    }

    @Test
    fun waterAccumulatesAcrossCallsAndIsPerDay() = runBlocking {
        repository.addWater(today, 250)
        repository.addWater(today, 500)
        repository.addWater(yesterday, 750)

        assertEquals(750, repository.observeDiaryDay(today).first().waterMl)
        assertEquals(750, repository.observeDiaryDay(yesterday).first().waterMl)

        val todayLogs = repository.observeWaterLogsForDate(today).first()
        assertEquals(2, todayLogs.size)
        assertEquals(750, todayLogs.sumOf { it.amountMl })
        assertEquals(1, repository.observeWaterLogsForDate(yesterday).first().size)

        // Deleting one drink keeps the aggregate consistent with the remaining log.
        val smallDrink = todayLogs.first { it.amountMl == 250 }
        repository.deleteWaterLog(smallDrink.id)
        assertEquals(500, repository.observeDiaryDay(today).first().waterMl)
        assertEquals(1, repository.observeWaterLogsForDate(today).first().size)

        repository.setWater(today, 0)
        assertEquals(0, repository.observeDiaryDay(today).first().waterMl)
        assertTrue(repository.observeWaterLogsForDate(today).first().isEmpty())
    }

    @Test
    fun notesArePersistedPerDay() = runBlocking {
        repository.setNotes(today, "leg day, felt strong")
        repository.setNotes(yesterday, "rest day")

        assertEquals("leg day, felt strong", repository.observeDiaryDay(today).first().notes)
        assertEquals("rest day", repository.observeDiaryDay(yesterday).first().notes)
    }

    @Test
    fun loggingFoodCreatesTheDayRowSoTheDateStripNotices() = runBlocking {
        assertNull(db.diaryDao().getDay(today.toEpochDay()))
        repository.addFood(food("t1", MealSlot.BREAKFAST, 500.0), today)
        assertTrue(db.diaryDao().getDay(today.toEpochDay()) != null)
    }

    @Test
    fun workoutsJoinTheDayWithTheirSetsAndBurn() = runBlocking {
        repository.logWorkout(
            WorkoutLog(
                id = "w1",
                type = WorkoutType.STRENGTH,
                exerciseName = "Bench Press",
                loggedAt = Instant.parse("2026-10-18T18:00:00Z"),
                met = 6.0,
                durationMinutes = 45,
                bodyWeightKg = 80.0,
                sets = listOf(SetEntry(100.0, 5), SetEntry(100.0, 5), SetEntry(100.0, 5)),
            ),
            today,
        )

        val day = repository.observeDiaryDay(today).first()
        assertEquals(1, day.workouts.size)
        assertEquals(3, day.workouts.single().sets.size)
        assertEquals(1500.0, day.workouts.single().totalVolumeKg, 0.0001)
        assertEquals(98, day.caloriesBurned())

        assertTrue("yesterday must stay clean", repository.observeDiaryDay(yesterday).first().workouts.isEmpty())
    }

    @Test
    fun deletingAWorkoutRemovesItFromTheDay() = runBlocking {
        repository.logWorkout(
            WorkoutLog(
                id = "w1", type = WorkoutType.CARDIO, exerciseName = "Run",
                loggedAt = Instant.parse("2026-10-18T07:00:00Z"), met = 8.0,
                durationMinutes = 45, bodyWeightKg = 80.0,
            ),
            today,
        )
        repository.deleteWorkout("w1")
        assertTrue(repository.observeDiaryDay(today).first().workouts.isEmpty())
    }

    @Test
    fun weightHistoryKeepsOneEntryPerDayNewestFirst() = runBlocking {
        repository.logWeight(dayBeforeYesterday, 76.0)
        repository.logWeight(yesterday, 75.5)
        repository.logWeight(today, 75.0)
        repository.logWeight(today, 74.8) // re-weigh the same day

        val history = repository.observeWeightHistory().first()
        // Three distinct days, three entries: the re-weigh must replace, not append.
        assertEquals(3, history.size)
        assertEquals(today to 74.8, history.first())
        assertEquals(yesterday to 75.5, history[1])
    }

    @Test
    fun theFastLifecycleIsFullyPersisted() = runBlocking {
        val start = Instant.parse("2026-10-18T06:00:00Z")
        repository.startFast(targetHours = 16, startedAt = start)

        val active = repository.observeActiveFast().first()
        assertTrue(active != null)
        assertEquals(16, active!!.targetHours)

        repository.endFast(endedAt = start.plusSeconds(3600 * 10))
        assertNull(repository.observeActiveFast().first())

        val history = repository.observeFastHistory().first()
        assertEquals(1, history.size)
        val session = history.single()
        assertFalse(session.isActive)
        assertEquals(16, session.targetHours)
        assertEquals(600L, session.durationMinutes())
        assertFalse(session.metTarget())
        assertEquals(today, session.date)

        // Removing a completed session removes it from the history flow.
        repository.deleteFastingSession(session.id)
        assertTrue(repository.observeFastHistory().first().isEmpty())
    }

    @Test
    fun theProfileRoundTripsThroughTheDatabase() = runBlocking {
        val profile = com.drnitz.aro.domain.model.UserProfile(
            name = "Arjun",
            sex = com.drnitz.aro.domain.model.Sex.MALE,
            ageYears = 26,
            weightKg = 75.0,
            targetWeightKg = 70.0,
            heightCm = 178.0,
            activityLevel = com.drnitz.aro.domain.model.ActivityLevel.MODERATE,
            goal = com.drnitz.aro.domain.model.Goal.MILD_CUT,
            macroPreset = com.drnitz.aro.domain.model.MacroPreset.HIGH_PROTEIN,
        )

        repository.saveProfile(profile)
        val loaded = repository.observeProfile().first()

        assertEquals(profile, loaded)
        assertEquals(1738, com.drnitz.aro.domain.calculation.basalMetabolicRate(loaded!!).valueOrNull())
    }

    @Test
    fun savingTheProfileKeepsItsNonNutritionSettings() = runBlocking {
        repository.saveProfile(com.drnitz.aro.domain.model.UserProfile.DEFAULT)
        repository.saveProfileSettings(ProfileSettings("renal_restricted", 3500, true))

        repository.saveProfile(com.drnitz.aro.domain.model.UserProfile.DEFAULT.copy(name = "Arjun"))

        val settings = repository.observeProfileSettings().first()
        assertEquals("renal_restricted", settings!!.activeProtocolId)
        assertEquals(3500, settings.targetWaterMl)
    }

    @Test
    fun recentLoggedDaysListsOnlyTouchedDays() = runBlocking {
        repository.addFood(food("t1", MealSlot.BREAKFAST, 500.0), today)
        repository.addFood(food("d1", MealSlot.BREAKFAST, 500.0), dayBeforeYesterday)

        val recent = repository.observeRecentLoggedDays(10).first()
        assertEquals(listOf(today, dayBeforeYesterday), recent)
        assertFalse(yesterday in recent)
    }

    @Test
    fun rangeTotalsCoverOnlyTheRequestedWindow() = runBlocking {
        repository.addFood(food("d1", MealSlot.BREAKFAST, 100.0), dayBeforeYesterday)
        repository.addFood(food("y1", MealSlot.BREAKFAST, 200.0), yesterday)
        repository.addFood(food("t1", MealSlot.BREAKFAST, 300.0), today)

        val totals = repository.observeTotalsInRange(yesterday, today).first()
        assertEquals(2, totals.size)
        assertEquals(200.0, totals[0].totals.calories, 0.0001)
        assertEquals(300.0, totals[1].totals.calories, 0.0001)
    }
}