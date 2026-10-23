package com.drnitz.aro.ui.main

import com.drnitz.aro.data.db.FixedDateProvider
import com.drnitz.aro.data.repository.ActiveFast
import com.drnitz.aro.data.repository.AroRepository
import com.drnitz.aro.data.repository.DayTotals
import com.drnitz.aro.data.repository.ProfileSettings
import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.DoctorConsultation
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.PlanItem
import com.drnitz.aro.domain.model.PlanItemType
import com.drnitz.aro.domain.model.PlanOccurrence
import com.drnitz.aro.domain.model.PlanRepeat
import com.drnitz.aro.domain.model.PlannedFood
import com.drnitz.aro.domain.model.SetEntry
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.domain.model.VitalsEntry
import com.drnitz.aro.domain.model.WaterLog
import com.drnitz.aro.domain.model.WorkoutLog
import com.drnitz.aro.domain.model.WorkoutType
import com.drnitz.aro.notifications.PlanReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class MainScreenViewModelTest {

    private val testToday = LocalDate.of(2026, 10, 21)
    private val dateProvider = FixedDateProvider(testToday)

    @Test
    fun uiState_emitsInitialSuccessWithDefaultTargets() = runTest {
        val repo = FakeAroRepository()
        val viewModel = MainScreenViewModel(repo, dateProvider)

        val success = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first()

        assertEquals(testToday, success.selectedDate)
        assertTrue(success.isToday)
        assertEquals(2444, success.energyPlan.targetCalories)
        assertEquals(244, success.energyPlan.macros.proteinG)
        assertEquals(0, success.energyBalance.caloriesEaten)
        assertEquals(2444, success.energyBalance.remaining)
        assertEquals(3000, success.targetWaterMl)
    }

    @Test
    fun previousDayAndNextDay_updatesSelectedDateAndIsToday() = runTest {
        val repo = FakeAroRepository()
        val viewModel = MainScreenViewModel(repo, dateProvider)

        viewModel.previousDay()
        val prevDayState = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            it.selectedDate == testToday.minusDays(1)
        }
        assertEquals(testToday.minusDays(1), prevDayState.selectedDate)
        assertFalse(prevDayState.isToday)

        viewModel.goToToday()
        val todayState = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            it.selectedDate == testToday
        }
        assertEquals(testToday, todayState.selectedDate)
        assertTrue(todayState.isToday)
    }

    @Test
    fun selectDate_jumpsToChosenDayAndBackToToday() = runTest {
        val repo = FakeAroRepository()
        val viewModel = MainScreenViewModel(repo, dateProvider)

        val chosen = testToday.minusDays(3)
        viewModel.selectDate(chosen)

        val chosenState = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            it.selectedDate == chosen
        }
        assertEquals(chosen, chosenState.selectedDate)
        assertFalse(chosenState.isToday)
        assertTrue(chosenState.formattedDate.isNotBlank())

        viewModel.goToToday()
        val todayState = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            it.selectedDate == testToday
        }
        assertTrue(todayState.isToday)
    }

    @Test
    fun addWaterAndResetWater_updatesState() = runTest {
        val repo = FakeAroRepository()
        val viewModel = MainScreenViewModel(repo, dateProvider)

        viewModel.addWater(250)
        val stateWithWater = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            it.diaryDay.waterMl == 250
        }
        assertEquals(250, stateWithWater.diaryDay.waterMl)

        viewModel.addWater(500)
        val stateWithMoreWater = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            it.diaryDay.waterMl == 750 && it.waterLogCount == 2
        }
        assertEquals(750, stateWithMoreWater.diaryDay.waterMl)
        assertEquals(2, stateWithMoreWater.waterLogCount)

        viewModel.resetWater()
        val stateReset = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            it.diaryDay.waterMl == 0 && it.waterLogCount == 0
        }
        assertEquals(0, stateReset.diaryDay.waterMl)
        assertEquals(0, stateReset.waterLogCount)
    }

    @Test
    fun fastingControls_startAndEndFast() = runTest {
        val repo = FakeAroRepository()
        val viewModel = MainScreenViewModel(repo, dateProvider)

        viewModel.startFast(18)
        val activeState = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            it.activeFast != null
        }
        assertNotNull(activeState.activeFast)
        assertEquals(18, activeState.activeFast?.targetHours)

        viewModel.endFast()
        val endedState = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            it.activeFast == null
        }
        assertNull(endedState.activeFast)
    }

    @Test
    fun loggingFood_reflectsInEnergyBalanceAndTotals() = runTest {
        val repo = FakeAroRepository()
        val viewModel = MainScreenViewModel(repo, dateProvider)

        repo.addFood(
            FoodLogEntry(
                id = "food-1",
                name = "Grilled Chicken Breast",
                meal = MealSlot.LUNCH,
                grams = 200.0,
                nutrients = NutrientTotals.of(
                    NutrientKey.CALORIES to 330.0,
                    NutrientKey.PROTEIN to 62.0,
                    NutrientKey.FAT to 7.0,
                ),
            ),
            testToday,
        )

        val updated = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            it.energyBalance.caloriesEaten == 330
        }

        assertEquals(330, updated.energyBalance.caloriesEaten)
        assertEquals(2444 - 330, updated.energyBalance.remaining)
        assertEquals(62.0, updated.dayTotals.proteinG, 0.01)

        viewModel.deleteFood("food-1")
        val deleted = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            it.energyBalance.caloriesEaten == 0
        }
        assertEquals(0, deleted.energyBalance.caloriesEaten)
    }

    @Test
    fun addFood_and_updateFood_updatesPortionAndMacros() = runTest {
        val repo = FakeAroRepository()
        val viewModel = MainScreenViewModel(repo, dateProvider)

        val initialEntry = FoodLogEntry(
            id = "chicken-breast-1",
            name = "Chicken Breast",
            meal = MealSlot.LUNCH,
            grams = 100.0,
            nutrients = NutrientTotals.of(
                NutrientKey.CALORIES to 165.0,
                NutrientKey.PROTEIN to 31.0,
                NutrientKey.CARBS to 0.0,
                NutrientKey.FAT to 3.6,
            ),
        )

        viewModel.addFood(initialEntry)

        val addedState = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            it.diaryDay.entriesFor(MealSlot.LUNCH).isNotEmpty()
        }
        assertEquals(1, addedState.diaryDay.entriesFor(MealSlot.LUNCH).size)
        assertEquals(165, addedState.energyBalance.caloriesEaten)
        assertEquals(31.0, addedState.dayTotals.proteinG, 0.01)

        // Rescale portion to 200g
        val updatedEntry = initialEntry.withGrams(200.0)
        viewModel.updateFood(updatedEntry)

        val updatedState = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            it.diaryDay.entriesFor(MealSlot.LUNCH).first().grams == 200.0
        }
        assertEquals(200.0, updatedState.diaryDay.entriesFor(MealSlot.LUNCH).first().grams, 0.01)
        assertEquals(330, updatedState.energyBalance.caloriesEaten)
        assertEquals(62.0, updatedState.dayTotals.proteinG, 0.01)
    }

    @Test
    fun coverage_detectsMissingMacrosInMeals() = runTest {
        val repo = FakeAroRepository()
        val viewModel = MainScreenViewModel(repo, dateProvider)

        // Initially no food logged
        val initialState = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first()
        assertTrue(initialState.coverage.hasNothingLogged)
        assertEquals(4, initialState.coverage.emptyMeals.size)

        // Log protein-only food to Breakfast
        viewModel.addFood(
            FoodLogEntry(
                id = "egg-whites",
                name = "Egg Whites",
                meal = MealSlot.BREAKFAST,
                grams = 100.0,
                nutrients = NutrientTotals.of(
                    NutrientKey.CALORIES to 52.0,
                    NutrientKey.PROTEIN to 11.0,
                    NutrientKey.CARBS to 0.0,
                    NutrientKey.FAT to 0.0,
                ),
            ),
        )

        val stateAfterBreakfast = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            !it.coverage.hasNothingLogged
        }
        assertFalse(stateAfterBreakfast.coverage.hasNothingLogged)
        val breakfastCoverage = stateAfterBreakfast.coverage.meals.find { it.meal == MealSlot.BREAKFAST }
        assertNotNull(breakfastCoverage)
        assertFalse(breakfastCoverage!!.isEmpty)
        // Carbs and Fat should be flagged as missing
        assertTrue(breakfastCoverage.missingMacros.contains(NutrientKey.CARBS))
        assertTrue(breakfastCoverage.missingMacros.contains(NutrientKey.FAT))
        assertFalse(breakfastCoverage.missingMacros.contains(NutrientKey.PROTEIN))
    }

    @Test
    fun logWorkout_and_deleteWorkout_updatesDayWorkoutsAndEnergyBalance() = runTest {
        val repo = FakeAroRepository()
        val viewModel = MainScreenViewModel(repo, dateProvider)

        val workout = WorkoutLog(
            id = "bench-press-1",
            type = WorkoutType.STRENGTH,
            exerciseName = "Barbell Bench Press",
            loggedAt = Instant.now(),
            met = 6.0,
            durationMinutes = 45,
            bodyWeightKg = 75.0,
            sets = listOf(
                SetEntry(weightKg = 80.0, reps = 8),
                SetEntry(weightKg = 85.0, reps = 6),
                SetEntry(weightKg = 90.0, reps = 4),
            ),
        )

        viewModel.logWorkout(workout)

        val addedState = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            it.diaryDay.workouts.isNotEmpty()
        }
        assertEquals(1, addedState.diaryDay.workouts.size)
        assertEquals("Barbell Bench Press", addedState.diaryDay.workouts.first().exerciseName)
        assertTrue(addedState.energyBalance.caloriesBurned > 0)

        viewModel.deleteWorkout("bench-press-1")

        val deletedState = viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first {
            it.diaryDay.workouts.isEmpty()
        }
        assertEquals(0, deletedState.diaryDay.workouts.size)
        assertEquals(0, deletedState.energyBalance.caloriesBurned)
    }

    @Test
    fun planState_exposesSelectedDayAndCalendarMarkers() = runTest {
        val repo = FakeAroRepository()
        val viewModel = MainScreenViewModel(repo, dateProvider)
        val plan = plannedFood(
            id = "plan-lunch",
            startDate = testToday,
            repeat = PlanRepeat.DAILY,
            repeatUntil = testToday.plusDays(2),
        )
        repo.planItemsFlow.value = listOf(plan)

        val success = viewModel.planState.filterIsInstance<PlanUiState.Success>().first {
            it.selectedItems.isNotEmpty()
        }

        assertEquals(testToday, success.selectedDate)
        assertEquals(1, success.selectedItems.size)
        assertEquals("Planned oats", success.selectedItems.first().item.title)
        assertEquals(450, success.summary.plannedCalories)
        assertEquals(PlanItemType.FOOD, success.plannedDates[testToday]?.single())
        assertEquals(PlanItemType.FOOD, success.plannedDates[testToday.plusDays(2)]?.single())
    }

    @Test
    fun completingPlan_keepsPlannedCaloriesOutOfActualDiaryTotals() = runTest {
        val repo = FakeAroRepository()
        val viewModel = MainScreenViewModel(repo, dateProvider)
        repo.planItemsFlow.value = listOf(
            plannedFood(id = "plan-breakfast", startDate = testToday),
        )

        viewModel.planState.filterIsInstance<PlanUiState.Success>().first {
            it.selectedItems.isNotEmpty()
        }
        viewModel.completePlanOccurrence(
            planId = "plan-breakfast",
            occurrenceDate = testToday,
            actualLogId = "food-1",
        )

        val completed = viewModel.planState.filterIsInstance<PlanUiState.Success>().first {
            it.selectedItems.firstOrNull()?.isCompleted == true
        }
        assertEquals(0, completed.summary.plannedCalories)
        assertEquals(0, viewModel.uiState.filterIsInstance<MainScreenUiState.Success>().first()
            .energyBalance.caloriesEaten)
    }

    @Test
    fun repeatingPlanCompletion_isPerOccurrence() = runTest {
        val repo = FakeAroRepository()
        val viewModel = MainScreenViewModel(repo, dateProvider)
        repo.planItemsFlow.value = listOf(
            plannedFood(
                id = "plan-daily",
                startDate = testToday,
                repeat = PlanRepeat.DAILY,
                repeatUntil = testToday.plusDays(1),
            ),
        )

        viewModel.planState.filterIsInstance<PlanUiState.Success>().first {
            it.selectedItems.isNotEmpty()
        }
        viewModel.completePlanOccurrence("plan-daily", testToday)
        viewModel.planState.filterIsInstance<PlanUiState.Success>().first {
            it.selectedItems.firstOrNull()?.isCompleted == true
        }

        viewModel.selectDate(testToday.plusDays(1))
        val tomorrow = viewModel.planState.filterIsInstance<PlanUiState.Success>().first {
            it.selectedDate == testToday.plusDays(1) && it.selectedItems.isNotEmpty()
        }
        assertFalse(tomorrow.selectedItems.first().isCompleted)
        assertEquals(450, tomorrow.summary.plannedCalories)
    }

    @Test
    fun saveAndDeletePlan_updatesPlanState() = runTest {
        val repo = FakeAroRepository()
        val viewModel = MainScreenViewModel(repo, dateProvider)
        val plan = plannedFood(id = "plan-save", startDate = testToday)

        viewModel.savePlanItem(plan)
        val saved = viewModel.planState.filterIsInstance<PlanUiState.Success>().first {
            it.selectedItems.isNotEmpty()
        }
        assertEquals("plan-save", saved.selectedItems.first().item.id)

        viewModel.deletePlanItem("plan-save")
        val deleted = viewModel.planState.filterIsInstance<PlanUiState.Success>().first {
            it.selectedItems.isEmpty()
        }
        assertTrue(deleted.selectedItems.isEmpty())
    }

    @Test
    fun planActions_rescheduleAndCancelReminders() = runTest {
        val repo = FakeAroRepository()
        val reminders = FakePlanReminderScheduler()
        val viewModel = MainScreenViewModel(repo, dateProvider, reminders)
        val plan = plannedFood(id = "plan-reminder", startDate = testToday)

        viewModel.savePlanItem(plan)
        val savedCall = reminders.calls.first { it.isNotEmpty() }.last()
        assertEquals("reschedule:plan-reminder:null", savedCall)

        viewModel.completePlanOccurrence("plan-reminder", testToday)
        val completedCall = reminders.calls.first { call ->
            call.lastOrNull() == "reschedule:plan-reminder:$testToday"
        }.last()
        assertEquals("reschedule:plan-reminder:$testToday", completedCall)

        viewModel.uncompletePlanOccurrence("plan-reminder", testToday)
        reminders.calls.first { call ->
            call.size >= 3 && call.last() == "reschedule:plan-reminder:null"
        }

        viewModel.deletePlanItem("plan-reminder")
        reminders.calls.first { call -> call.lastOrNull() == "cancel:plan-reminder" }
    }

    @Test
    fun showPlanMonth_changesVisibleMonthWithoutChangingSelectedDate() = runTest {
        val repo = FakeAroRepository()
        val viewModel = MainScreenViewModel(repo, dateProvider)
        val nextMonth = YearMonth.from(testToday).plusMonths(1)

        viewModel.showPlanMonth(nextMonth)
        val success = viewModel.planState.filterIsInstance<PlanUiState.Success>().first {
            it.calendarMonth == nextMonth
        }

        assertEquals(nextMonth, success.calendarMonth)
        assertEquals(testToday, success.selectedDate)
    }

    @Test
    fun skipPlanOccurrence_marksOnlyThatDayAndReschedulesReminder() = runTest {
        val repo = FakeAroRepository()
        val reminders = FakePlanReminderScheduler()
        val viewModel = MainScreenViewModel(repo, dateProvider, reminders)
        repo.planItemsFlow.value = listOf(
            plannedFood(
                id = "plan-skip",
                startDate = testToday,
                repeat = PlanRepeat.DAILY,
                repeatUntil = testToday.plusDays(1),
            ),
        )

        viewModel.planState.filterIsInstance<PlanUiState.Success>().first {
            it.selectedItems.isNotEmpty()
        }
        viewModel.skipPlanOccurrence("plan-skip", testToday)

        val skipped = viewModel.planState.filterIsInstance<PlanUiState.Success>().first {
            it.selectedItems.firstOrNull()?.isSkipped == true
        }
        assertTrue(skipped.selectedItems.first().isSkipped)
        assertFalse(skipped.selectedItems.first().isCompleted)
        assertEquals(0, skipped.summary.plannedCalories)
        reminders.calls.first { calls ->
            calls.lastOrNull() == "reschedule:plan-skip:$testToday"
        }

        viewModel.selectDate(testToday.plusDays(1))
        val tomorrow = viewModel.planState.filterIsInstance<PlanUiState.Success>().first {
            it.selectedDate == testToday.plusDays(1) && it.selectedItems.isNotEmpty()
        }
        assertTrue(tomorrow.selectedItems.first().isPending)
        assertEquals(450, tomorrow.summary.plannedCalories)
    }

    @Test
    fun planState_upcomingSeriesShowsFutureAndRepeatingPlansOnly() = runTest {
        val repo = FakeAroRepository()
        val viewModel = MainScreenViewModel(repo, dateProvider)
        repo.planItemsFlow.value = listOf(
            plannedFood(id = "past", startDate = testToday.minusDays(1)),
            plannedFood(id = "future", startDate = testToday.plusDays(2)),
            plannedFood(
                id = "repeating",
                startDate = testToday,
                repeat = PlanRepeat.WEEKLY,
                repeatUntil = testToday.plusWeeks(2),
            ),
        )

        val success = viewModel.planState.filterIsInstance<PlanUiState.Success>().first {
            it.upcomingSeries.isNotEmpty()
        }

        assertEquals(
            listOf("repeating", "future"),
            success.upcomingSeries.map { it.id },
        )
    }
}

private fun plannedFood(
    id: String,
    startDate: LocalDate,
    repeat: PlanRepeat = PlanRepeat.NONE,
    repeatUntil: LocalDate? = null,
) = PlanItem(
    id = id,
    title = "Planned oats",
    type = PlanItemType.FOOD,
    startDate = startDate,
    time = LocalTime.of(8, 0),
    repeat = repeat,
    repeatUntil = repeatUntil,
    food = PlannedFood(
        meal = MealSlot.BREAKFAST,
        grams = 100.0,
        nutrients = NutrientTotals.of(
            NutrientKey.CALORIES to 450.0,
            NutrientKey.PROTEIN to 20.0,
            NutrientKey.CARBS to 60.0,
            NutrientKey.FAT to 12.0,
        ),
    ),
)

private class FakeAroRepository(
    initialProfile: UserProfile? = UserProfile.DEFAULT,
    initialSettings: ProfileSettings? = ProfileSettings("aha_standard", 3000, true),
) : AroRepository {
    val profileFlow = MutableStateFlow(initialProfile)
    val settingsFlow = MutableStateFlow(initialSettings)
    val diaryDays = mutableMapOf<LocalDate, MutableStateFlow<DiaryDay>>()
    val waterLogsFlow = MutableStateFlow<Map<LocalDate, List<WaterLog>>>(emptyMap())
    val activeFastFlow = MutableStateFlow<ActiveFast?>(null)
    val planItemsFlow = MutableStateFlow<List<PlanItem>>(emptyList())
    val planCompletionsFlow = MutableStateFlow<Map<Pair<String, LocalDate>, Instant>>(emptyMap())
    val planSkippedFlow = MutableStateFlow<Set<Pair<String, LocalDate>>>(emptySet())

    private fun getDayFlow(date: LocalDate): MutableStateFlow<DiaryDay> =
        diaryDays.getOrPut(date) { MutableStateFlow(DiaryDay(date)) }

    override fun observeProfile(): Flow<UserProfile?> = profileFlow
    override suspend fun saveProfile(profile: UserProfile) { profileFlow.value = profile }
    override fun observeProfileSettings(): Flow<ProfileSettings?> = settingsFlow
    override suspend fun saveProfileSettings(settings: ProfileSettings) { settingsFlow.value = settings }

    override fun observeDiaryDay(date: LocalDate): Flow<DiaryDay> = getDayFlow(date)
    override fun observeDayTotals(date: LocalDate): Flow<NutrientTotals> = getDayFlow(date).map { it.nutrientTotals }

    override fun observePlanOccurrences(date: LocalDate): Flow<List<PlanOccurrence>> =
        combine(planItemsFlow, planCompletionsFlow, planSkippedFlow) { items, completions, skipped ->
            items.filter { it.occursOn(date) }
                .map { item ->
                    PlanOccurrence(
                        item = item,
                        date = date,
                        completedAt = completions[item.id to date],
                        skippedAt = if (item.id to date in skipped) Instant.EPOCH else null,
                    )
                }
                .sortedWith(compareBy({ it.item.time }, { it.item.title }))
        }

    override fun observePlanOccurrencesInRange(
        from: LocalDate,
        toInclusive: LocalDate,
    ): Flow<List<PlanOccurrence>> =
        combine(planItemsFlow, planCompletionsFlow, planSkippedFlow) { items, completions, skipped ->
            items.flatMap { item ->
                item.occurrencesInRange(from, toInclusive).map { date ->
                    PlanOccurrence(
                        item = item,
                        date = date,
                        completedAt = completions[item.id to date],
                        skippedAt = if (item.id to date in skipped) Instant.EPOCH else null,
                    )
                }
            }
        }

    override fun observePlanItems(): Flow<List<PlanItem>> = planItemsFlow

    override suspend fun savePlanItem(item: PlanItem) {
        planItemsFlow.value = planItemsFlow.value.filterNot { it.id == item.id } + item
    }

    override suspend fun deletePlanItem(id: String) {
        planItemsFlow.value = planItemsFlow.value.filterNot { it.id == id }
    }

    override suspend fun completePlanOccurrence(
        planId: String,
        occurrenceDate: LocalDate,
        actualLogId: String?,
    ) {
        planCompletionsFlow.value =
            planCompletionsFlow.value + ((planId to occurrenceDate) to Instant.now())
    }

    override suspend fun uncompletePlanOccurrence(planId: String, occurrenceDate: LocalDate) {
        planCompletionsFlow.value =
            planCompletionsFlow.value - (planId to occurrenceDate)
        planSkippedFlow.value = planSkippedFlow.value - (planId to occurrenceDate)
    }

    override suspend fun skipPlanOccurrence(planId: String, occurrenceDate: LocalDate) {
        planCompletionsFlow.value =
            planCompletionsFlow.value - (planId to occurrenceDate)
        planSkippedFlow.value = planSkippedFlow.value + (planId to occurrenceDate)
    }

    override suspend fun addFood(entry: FoodLogEntry, date: LocalDate) {
        val flow = getDayFlow(date)
        flow.value = flow.value.withAddedFood(entry)
    }

    override suspend fun updateFood(entry: FoodLogEntry, date: LocalDate) {
        val flow = getDayFlow(date)
        flow.value = flow.value.withUpdatedFood(entry)
    }

    override suspend fun deleteFood(id: String) {
        diaryDays.values.forEach { flow ->
            val day = flow.value
            val newMeals = day.meals.mapValues { (_, list) -> list.filterNot { it.id == id } }
            flow.value = day.copy(meals = newMeals)
        }
    }

    override suspend fun moveFood(id: String, toMeal: MealSlot, toDate: LocalDate) {}

    override suspend fun addWater(date: LocalDate, deltaMl: Int) {
        val flow = getDayFlow(date)
        val current = flow.value.waterMl
        flow.value = flow.value.copy(waterMl = (current + deltaMl).coerceAtLeast(0))
        if (deltaMl > 0) {
            val logs = waterLogsFlow.value[date].orEmpty()
            val updatedLogs = logs + WaterLog(
                id = "water-${date}-${logs.size + 1}",
                date = date,
                amountMl = deltaMl,
                loggedAt = Instant.ofEpochMilli(1_000L + logs.size),
            )
            waterLogsFlow.value = waterLogsFlow.value + (date to updatedLogs)
        }
    }

    override suspend fun setWater(date: LocalDate, waterMl: Int) {
        val flow = getDayFlow(date)
        flow.value = flow.value.copy(waterMl = waterMl.coerceAtLeast(0))
        waterLogsFlow.value = waterLogsFlow.value - date
    }

    override fun observeAllWaterLogs(): Flow<List<WaterLog>> =
        waterLogsFlow.map { it.values.flatten().sortedByDescending { log -> log.loggedAt } }

    override fun observeWaterLogsForDate(date: LocalDate): Flow<List<WaterLog>> =
        waterLogsFlow.map { it[date].orEmpty() }

    override suspend fun deleteWaterLog(id: String) {
        waterLogsFlow.value = waterLogsFlow.value.mapValues { (_, logs) ->
            logs.filterNot { it.id == id }
        }
    }

    override suspend fun setNotes(date: LocalDate, notes: String) {}

    override fun observeRecentLoggedDays(limit: Int): Flow<List<LocalDate>> = flowOf(emptyList())

    override fun observeTotalsInRange(from: LocalDate, toInclusive: LocalDate): Flow<List<DayTotals>> = flowOf(emptyList())

    override suspend fun logWorkout(workout: WorkoutLog, date: LocalDate) {
        val flow = getDayFlow(date)
        flow.value = flow.value.copy(workouts = flow.value.workouts + workout)
    }

    override suspend fun deleteWorkout(id: String) {
        diaryDays.values.forEach { flow ->
            val day = flow.value
            flow.value = day.copy(workouts = day.workouts.filterNot { it.id == id })
        }
    }

    override fun observeWeightHistory(): Flow<List<Pair<LocalDate, Double>>> = flowOf(emptyList())

    override suspend fun logWeight(date: LocalDate, weightKg: Double) {}

    override fun observeActiveFast(): Flow<ActiveFast?> = activeFastFlow

    override suspend fun startFast(targetHours: Int, startedAt: Instant) {
        activeFastFlow.value = ActiveFast("fast-1", startedAt, targetHours, 0, 0f)
    }

    override suspend fun endFast(endedAt: Instant) {
        activeFastFlow.value = null
    }

    override fun observeAllVitals(): Flow<List<VitalsEntry>> = flowOf(emptyList())
    override fun observeVitalsForDate(date: LocalDate): Flow<List<VitalsEntry>> = flowOf(emptyList())
    override suspend fun logVitals(entry: VitalsEntry) {}
    override suspend fun deleteVitals(id: String) {}
    override suspend fun getVitalsInRange(from: LocalDate, toInclusive: LocalDate): List<VitalsEntry> = emptyList()

    override fun observeAllConsultations(): Flow<List<DoctorConsultation>> = flowOf(emptyList())
    override fun observeConsultationById(id: String): Flow<DoctorConsultation?> = flowOf(null)
    override suspend fun logConsultation(consultation: DoctorConsultation) {}
    override suspend fun deleteConsultation(id: String) {}
    override suspend fun getConsultationsInRange(from: LocalDate, toInclusive: LocalDate): List<DoctorConsultation> = emptyList()
}

private class FakePlanReminderScheduler : PlanReminderScheduler {
    val calls = MutableStateFlow<List<String>>(emptyList())

    override suspend fun reschedulePlan(
        planId: String,
        completedDate: LocalDate?,
        dismissNotification: Boolean,
    ) {
        calls.value = calls.value + "reschedule:$planId:$completedDate"
    }

    override suspend fun cancelPlan(planId: String) {
        calls.value = calls.value + "cancel:$planId"
    }

    override suspend fun rescheduleAll() {
        calls.value = calls.value + "reschedule-all"
    }
}
