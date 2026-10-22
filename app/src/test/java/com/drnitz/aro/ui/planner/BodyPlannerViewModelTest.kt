package com.drnitz.aro.ui.planner

import com.drnitz.aro.data.db.FixedDateProvider
import com.drnitz.aro.data.repository.ActiveFast
import com.drnitz.aro.data.repository.AroRepository
import com.drnitz.aro.data.repository.DayTotals
import com.drnitz.aro.data.repository.ProfileSettings
import com.drnitz.aro.domain.calculation.BmiCategory
import com.drnitz.aro.domain.clinical.FindingCode
import com.drnitz.aro.domain.clinical.RatioStatus
import com.drnitz.aro.domain.model.ActivityLevel
import com.drnitz.aro.domain.model.ClinicalCatalog
import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.Goal
import com.drnitz.aro.domain.model.MacroPreset
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.Sex
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.domain.model.VitalsEntry
import com.drnitz.aro.domain.model.DoctorConsultation
import com.drnitz.aro.domain.model.WorkoutLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class BodyPlannerViewModelTest {

    private val testToday = LocalDate.of(2026, 10, 21)
    private val dateProvider = FixedDateProvider(testToday)

    @Test
    fun uiState_emitsInitialSuccessWithDefaultProfileAndAssessment() = runTest {
        val repo = FakePlannerRepository()
        val viewModel = BodyPlannerViewModel(repo, dateProvider)

        val success = viewModel.uiState.filterIsInstance<BodyPlannerUiState.Success>().first()

        assertEquals(testToday, success.selectedDate)
        assertTrue(success.isToday)
        assertEquals(75.0, success.profile.weightKg, 0.01)
        assertEquals(178.0, success.profile.heightCm, 0.01)
        assertNotNull(success.bmiResult)
        assertEquals(23.7, success.bmiResult?.value ?: 0.0, 0.05)
        assertEquals(BmiCategory.NORMAL, success.bmiResult?.category)

        // BMR for male, 75kg, 178cm, 26y = 10(75) + 6.25(178) - 5(26) + 5 = 750 + 1112.5 - 130 + 5 = 1738
        assertEquals(1738, success.energyPlan.bmr)
        // Moderate activity = 1.55x => 2694
        assertEquals(2694, success.energyPlan.tdee)
        // Mild cut (-250 kcal) => 2444
        assertEquals(2444, success.energyPlan.targetCalories)

        // Active protocol default AHA
        assertEquals("aha_standard", success.activeProtocol.id)
    }

    @Test
    fun updateProfile_recalculatesBmrTdeeAndBmi() = runTest {
        val repo = FakePlannerRepository()
        val viewModel = BodyPlannerViewModel(repo, dateProvider)

        val initial = viewModel.uiState.filterIsInstance<BodyPlannerUiState.Success>().first()

        val updated = initial.profile.copy(
            weightKg = 85.0,
            heightCm = 180.0,
            ageYears = 30,
            sex = Sex.MALE,
            activityLevel = ActivityLevel.VERY_ACTIVE,
            goal = Goal.AGGRESSIVE_CUT,
            macroPreset = MacroPreset.BALANCED,
        )

        viewModel.updateProfile(updated)

        val updatedState = viewModel.uiState.filterIsInstance<BodyPlannerUiState.Success>().first {
            it.profile.weightKg == 85.0
        }

        // BMI = 85 / (1.80^2) = 26.2 => OVERWEIGHT
        assertNotNull(updatedState.bmiResult)
        assertEquals(26.2, updatedState.bmiResult?.value ?: 0.0, 0.1)
        assertEquals(BmiCategory.OVERWEIGHT, updatedState.bmiResult?.category)

        // BMR = 10(85) + 6.25(180) - 5(30) + 5 = 850 + 1125 - 150 + 5 = 1830
        assertEquals(1830, updatedState.energyPlan.bmr)
        // Very active = 1.725x => 3157
        assertEquals(3157, updatedState.energyPlan.tdee)
        // Aggressive cut = -500 => 2657
        assertEquals(2657, updatedState.energyPlan.targetCalories)
    }

    @Test
    fun logWeight_recordsWeighInAndUpdateProfile() = runTest {
        val repo = FakePlannerRepository()
        val viewModel = BodyPlannerViewModel(repo, dateProvider)

        viewModel.logWeight(72.5)

        val stateAfterWeight = viewModel.uiState.filterIsInstance<BodyPlannerUiState.Success>().first {
            it.profile.weightKg == 72.5 && it.weightHistory.isNotEmpty()
        }

        assertEquals(72.5, stateAfterWeight.profile.weightKg, 0.01)
        assertEquals(1, stateAfterWeight.weightHistory.size)
        assertEquals(72.5, stateAfterWeight.weightHistory.first().second, 0.01)
    }

    @Test
    fun logWeight_afterBrowsingPastAuditDate_stillLogsToday() = runTest {
        val repo = FakePlannerRepository()
        val viewModel = BodyPlannerViewModel(repo, dateProvider)
        val pastAuditDate = testToday.minusDays(4)

        viewModel.selectDate(pastAuditDate)
        viewModel.uiState.filterIsInstance<BodyPlannerUiState.Success>().first {
            it.selectedDate == pastAuditDate
        }

        viewModel.logWeight(72.5)

        val stateAfterWeight = viewModel.uiState.filterIsInstance<BodyPlannerUiState.Success>().first {
            it.profile.weightKg == 72.5 && it.weightHistory.isNotEmpty()
        }

        assertEquals(testToday, stateAfterWeight.weightHistory.first().first)
        assertEquals(72.5, stateAfterWeight.weightHistory.first().second, 0.01)
        assertEquals(pastAuditDate, stateAfterWeight.selectedDate)
    }

    @Test
    fun selectProtocol_switchesActiveProtocolAndReauditsIntake() = runTest {
        val repo = FakePlannerRepository()
        val viewModel = BodyPlannerViewModel(repo, dateProvider)

        val initial = viewModel.uiState.filterIsInstance<BodyPlannerUiState.Success>().first()
        assertEquals("aha_standard", initial.activeProtocol.id)

        viewModel.selectProtocol("dash_cardio")

        val dashState = viewModel.uiState.filterIsInstance<BodyPlannerUiState.Success>().first {
            it.activeProtocol.id == "dash_cardio"
        }

        assertEquals("DASH Protocol", dashState.activeProtocol.shortName)
        assertEquals(1500, dashState.activeProtocol.maxSodiumMg)
        assertEquals(4700, dashState.activeProtocol.targetPotassiumMg)
        assertFalse(dashState.activeProtocol.potassiumIsCeiling)

        viewModel.selectProtocol("renal_restricted")

        val renalState = viewModel.uiState.filterIsInstance<BodyPlannerUiState.Success>().first {
            it.activeProtocol.id == "renal_restricted"
        }

        assertEquals("Renal Guide", renalState.activeProtocol.shortName)
        assertTrue(renalState.activeProtocol.potassiumIsCeiling)
    }

    @Test
    fun dateNavigation_selectsPreviousDayAndToday() = runTest {
        val repo = FakePlannerRepository()
        val viewModel = BodyPlannerViewModel(repo, dateProvider)

        viewModel.previousDay()
        val prevDayState = viewModel.uiState.filterIsInstance<BodyPlannerUiState.Success>().first {
            it.selectedDate == testToday.minusDays(1)
        }
        assertEquals(testToday.minusDays(1), prevDayState.selectedDate)
        assertFalse(prevDayState.isToday)

        viewModel.today()
        val todayState = viewModel.uiState.filterIsInstance<BodyPlannerUiState.Success>().first {
            it.selectedDate == testToday
        }
        assertEquals(testToday, todayState.selectedDate)
        assertTrue(todayState.isToday)
    }

    @Test
    fun safetyFloor_clipsCaloricTargetAt1200Kcal() = runTest {
        val repo = FakePlannerRepository()
        val viewModel = BodyPlannerViewModel(repo, dateProvider)

        // Tiny female on aggressive cut
        val smallProfile = UserProfile(
            name = "Small User",
            sex = Sex.FEMALE,
            ageYears = 45,
            weightKg = 45.0,
            targetWeightKg = 42.0,
            heightCm = 145.0,
            activityLevel = ActivityLevel.SEDENTARY,
            goal = Goal.AGGRESSIVE_CUT,
        )

        viewModel.updateProfile(smallProfile)

        val state = viewModel.uiState.filterIsInstance<BodyPlannerUiState.Success>().first {
            it.profile.name == "Small User"
        }

        assertTrue(state.energyPlan.hitSafetyFloor)
        assertEquals(1200, state.energyPlan.targetCalories)
    }

    @Test
    fun clinicalAudit_evaluatesMolarRatioAndProtocolFindings() = runTest {
        val repo = FakePlannerRepository()
        val viewModel = BodyPlannerViewModel(repo, dateProvider)

        // Log sodium and potassium rich meal to today's diary
        repo.addFood(
            FoodLogEntry(
                id = "spinach-salmon",
                name = "Spinach and Wild Salmon",
                meal = MealSlot.DINNER,
                grams = 300.0,
                nutrients = NutrientTotals.of(
                    NutrientKey.CALORIES to 800.0,
                    NutrientKey.SODIUM to 400.0,
                    NutrientKey.POTASSIUM to 1600.0,
                    NutrientKey.FIBER to 12.0,
                ),
            ),
            testToday,
        )

        val auditedState = viewModel.uiState.filterIsInstance<BodyPlannerUiState.Success>().first {
            it.clinicalAssessment.naKRatio.isDetermined
        }

        val ratio = auditedState.clinicalAssessment.naKRatio
        assertTrue(ratio.isDetermined)
        // mass = 400 / 1600 = 0.25; molar = 0.25 * (39.10 / 22.99) = 0.43
        assertEquals(0.25, ratio.massRatio!!, 0.01)
        assertEquals(0.43, ratio.molarRatio!!, 0.02)
        assertEquals(RatioStatus.OPTIMAL, ratio.status)
    }
}

private class FakePlannerRepository(
    initialProfile: UserProfile? = UserProfile.DEFAULT,
    initialSettings: ProfileSettings? = ProfileSettings("aha_standard", 3000, true),
) : AroRepository {
    val profileFlow = MutableStateFlow(initialProfile)
    val settingsFlow = MutableStateFlow(initialSettings)
    val diaryDays = mutableMapOf<LocalDate, MutableStateFlow<DiaryDay>>()
    val weightEntries = MutableStateFlow<List<Pair<LocalDate, Double>>>(emptyList())

    private fun getDayFlow(date: LocalDate): MutableStateFlow<DiaryDay> =
        diaryDays.getOrPut(date) { MutableStateFlow(DiaryDay(date)) }

    override fun observeProfile(): Flow<UserProfile?> = profileFlow
    override suspend fun saveProfile(profile: UserProfile) { profileFlow.value = profile }
    override fun observeProfileSettings(): Flow<ProfileSettings?> = settingsFlow
    override suspend fun saveProfileSettings(settings: ProfileSettings) { settingsFlow.value = settings }

    override fun observeDiaryDay(date: LocalDate): Flow<DiaryDay> = getDayFlow(date)
    override fun observeDayTotals(date: LocalDate): Flow<NutrientTotals> = getDayFlow(date).map { it.nutrientTotals }

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
    override suspend fun addWater(date: LocalDate, deltaMl: Int) {}
    override suspend fun setWater(date: LocalDate, waterMl: Int) {}
    override suspend fun setNotes(date: LocalDate, notes: String) {}
    override fun observeRecentLoggedDays(limit: Int): Flow<List<LocalDate>> = flowOf(emptyList())
    override fun observeTotalsInRange(from: LocalDate, toInclusive: LocalDate): Flow<List<DayTotals>> = flowOf(emptyList())
    override suspend fun logWorkout(workout: WorkoutLog, date: LocalDate) {}
    override suspend fun deleteWorkout(id: String) {}

    override fun observeWeightHistory(): Flow<List<Pair<LocalDate, Double>>> = weightEntries

    override suspend fun logWeight(date: LocalDate, weightKg: Double) {
        val current = weightEntries.value.toMutableList()
        current.removeAll { it.first == date }
        current.add(0, date to weightKg)
        weightEntries.value = current
    }

    override fun observeActiveFast(): Flow<ActiveFast?> = flowOf(null)
    override suspend fun startFast(targetHours: Int, startedAt: Instant) {}
    override suspend fun endFast(endedAt: Instant) {}

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
