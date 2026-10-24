package com.drnitz.aro.ui.vitals

import com.drnitz.aro.MainDispatcherRule
import com.drnitz.aro.data.db.FixedDateProvider
import com.drnitz.aro.data.repository.ActiveFast
import com.drnitz.aro.data.repository.AroRepository
import com.drnitz.aro.data.repository.DayTotals
import com.drnitz.aro.data.repository.ProfileSettings
import com.drnitz.aro.domain.model.BloodPressure
import com.drnitz.aro.domain.model.BloodPressureStage
import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.DoctorConsultation
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.domain.model.VitalsEntry
import com.drnitz.aro.domain.model.WorkoutLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class VitalsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fixedToday = LocalDate.of(2026, 10, 21)
    private val dateProvider = FixedDateProvider(fixedToday)

    @Test
    fun `initial state loads profile and empty vitals`() = runTest {
        val repo = FakeVitalsRepository()
        val vm = VitalsViewModel(repo, dateProvider)

        val state = vm.uiState.filterIsInstance<VitalsUiState.Success>().first()
        assertEquals(UserProfile.DEFAULT.name, state.profile.name)
        assertTrue(state.vitals.isEmpty())
        assertTrue(state.consultations.isEmpty())
    }

    @Test
    fun `logVitals and deleteVitals mutate repository flow`() = runTest {
        val repo = FakeVitalsRepository()
        val vm = VitalsViewModel(repo, dateProvider)

        val entry = VitalsEntry(
            id = "v-1",
            date = fixedToday,
            bloodPressure = BloodPressure(118, 76, pulse = 68),
        )

        vm.logVitals(entry)

        val state1 = vm.uiState.filterIsInstance<VitalsUiState.Success>().first { it.vitals.isNotEmpty() }
        assertEquals(1, state1.vitals.size)
        assertEquals("v-1", state1.vitals[0].id)
        assertEquals(BloodPressureStage.NORMAL, state1.vitals[0].bloodPressure?.stage)

        vm.deleteVitals("v-1")
        val state2 = vm.uiState.filterIsInstance<VitalsUiState.Success>().first { it.vitals.isEmpty() }
        assertTrue(state2.vitals.isEmpty())
    }

    @Test
    fun `logConsultation and deleteConsultation mutate repository flow`() = runTest {
        val repo = FakeVitalsRepository()
        val vm = VitalsViewModel(repo, dateProvider)

        val consult = DoctorConsultation(
            id = "c-1",
            date = fixedToday,
            doctorName = "Dr. Robert Chen",
            specialty = "Cardiology",
            reasonForVisit = "Hypertension follow-up",
            diagnosisNotes = "Stable blood pressure under current protocol",
        )

        vm.logConsultation(consult)

        val state1 = vm.uiState.filterIsInstance<VitalsUiState.Success>().first { it.consultations.isNotEmpty() }
        assertEquals(1, state1.consultations.size)
        assertEquals("Dr. Robert Chen", state1.consultations[0].doctorName)

        vm.deleteConsultation("c-1")
        val state2 = vm.uiState.filterIsInstance<VitalsUiState.Success>().first { it.consultations.isEmpty() }
        assertTrue(state2.consultations.isEmpty())
    }

    @Test
    fun `selectTab updates active tab state`() = runTest {
        val repo = FakeVitalsRepository()
        val vm = VitalsViewModel(repo, dateProvider)

        assertEquals(0, vm.selectedTab.value)
        vm.selectTab(1)
        assertEquals(1, vm.selectedTab.value)
        vm.selectTab(2)
        assertEquals(2, vm.selectedTab.value)
    }
}

private class FakeVitalsRepository(
    initialProfile: UserProfile? = UserProfile.DEFAULT,
) : AroRepository {
    val profileFlow = MutableStateFlow(initialProfile)
    val vitalsFlow = MutableStateFlow<List<VitalsEntry>>(emptyList())
    val consultsFlow = MutableStateFlow<List<DoctorConsultation>>(emptyList())

    override fun observeProfile(): Flow<UserProfile?> = profileFlow
    override suspend fun saveProfile(profile: UserProfile) { profileFlow.value = profile }
    override fun observeProfileSettings(): Flow<ProfileSettings?> = flowOf(ProfileSettings("aha_standard", 3000, true))
    override suspend fun saveProfileSettings(settings: ProfileSettings) {}

    override fun observeDiaryDay(date: LocalDate): Flow<DiaryDay> = flowOf(DiaryDay(date))
    override fun observeDayTotals(date: LocalDate): Flow<NutrientTotals> = flowOf(NutrientTotals.EMPTY)
    override suspend fun addFood(entry: FoodLogEntry, date: LocalDate) {}
    override suspend fun updateFood(entry: FoodLogEntry, date: LocalDate) {}
    override suspend fun deleteFood(id: String) {}
    override suspend fun moveFood(id: String, toMeal: MealSlot, toDate: LocalDate) {}
    override suspend fun addWater(date: LocalDate, deltaMl: Int) {}
    override suspend fun setWater(date: LocalDate, waterMl: Int) {}
    override suspend fun setNotes(date: LocalDate, notes: String) {}
    override fun observeRecentLoggedDays(limit: Int): Flow<List<LocalDate>> = flowOf(emptyList())
    override fun observeTotalsInRange(from: LocalDate, toInclusive: LocalDate): Flow<List<DayTotals>> = flowOf(emptyList())

    override suspend fun logWorkout(workout: WorkoutLog, date: LocalDate) {}
    override suspend fun deleteWorkout(id: String) {}
    override fun observeWeightHistory(): Flow<List<Pair<LocalDate, Double>>> = flowOf(emptyList())
    override suspend fun logWeight(date: LocalDate, weightKg: Double) {}

    override fun observeActiveFast(): Flow<ActiveFast?> = flowOf(null)
    override suspend fun startFast(targetHours: Int, startedAt: Instant) {}
    override suspend fun endFast(endedAt: Instant) {}

    override fun observeAllVitals(): Flow<List<VitalsEntry>> = vitalsFlow
    override fun observeVitalsForDate(date: LocalDate): Flow<List<VitalsEntry>> = flowOf(vitalsFlow.value.filter { it.date == date })
    override suspend fun logVitals(entry: VitalsEntry) {
        val current = vitalsFlow.value.toMutableList()
        current.removeAll { it.id == entry.id }
        current.add(0, entry)
        vitalsFlow.value = current
    }
    override suspend fun deleteVitals(id: String) {
        val current = vitalsFlow.value.toMutableList()
        current.removeAll { it.id == id }
        vitalsFlow.value = current
    }
    override suspend fun getVitalsInRange(from: LocalDate, toInclusive: LocalDate): List<VitalsEntry> =
        vitalsFlow.value.filter { it.date in from..toInclusive }

    override fun observeAllConsultations(): Flow<List<DoctorConsultation>> = consultsFlow
    override fun observeConsultationById(id: String): Flow<DoctorConsultation?> =
        flowOf(consultsFlow.value.find { it.id == id })
    override suspend fun logConsultation(consultation: DoctorConsultation) {
        val current = consultsFlow.value.toMutableList()
        current.removeAll { it.id == consultation.id }
        current.add(0, consultation)
        consultsFlow.value = current
    }
    override suspend fun deleteConsultation(id: String) {
        val current = consultsFlow.value.toMutableList()
        current.removeAll { it.id == id }
        consultsFlow.value = current
    }
    override suspend fun getConsultationsInRange(from: LocalDate, toInclusive: LocalDate): List<DoctorConsultation> =
        consultsFlow.value.filter { it.date in from..toInclusive }
}
