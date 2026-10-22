package com.drnitz.aro.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drnitz.aro.data.db.DateProvider
import com.drnitz.aro.data.db.SystemDateProvider
import com.drnitz.aro.data.repository.AroRepository
import com.drnitz.aro.data.repository.ProfileSettings
import com.drnitz.aro.domain.DomainResult
import com.drnitz.aro.domain.calculation.BmiResult
import com.drnitz.aro.domain.calculation.EnergyPlan
import com.drnitz.aro.domain.calculation.EnergyPlanner
import com.drnitz.aro.domain.calculation.bodyMassIndex
import com.drnitz.aro.domain.clinical.ClinicalAssessment
import com.drnitz.aro.domain.clinical.ClinicalAuditEngine
import com.drnitz.aro.domain.model.ClinicalCatalog
import com.drnitz.aro.domain.model.ClinicalProtocol
import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.MacroTargets
import com.drnitz.aro.domain.model.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed interface BodyPlannerUiState {
    data object Loading : BodyPlannerUiState

    data class Error(val throwable: Throwable) : BodyPlannerUiState

    data class Success(
        val profile: UserProfile,
        val settings: ProfileSettings,
        val energyPlan: EnergyPlan,
        val bmiResult: BmiResult?,
        val selectedDate: LocalDate,
        val isToday: Boolean,
        val formattedDate: String,
        val diaryDay: DiaryDay,
        val activeProtocol: ClinicalProtocol,
        val clinicalAssessment: ClinicalAssessment,
        val weightHistory: List<Pair<LocalDate, Double>>,
    ) : BodyPlannerUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class BodyPlannerViewModel(
    private val repository: AroRepository,
    private val dateProvider: DateProvider = SystemDateProvider(),
) : ViewModel() {

    private val selectedDateFlow = MutableStateFlow(dateProvider.today())

    val uiState: StateFlow<BodyPlannerUiState> = selectedDateFlow
        .flatMapLatest<LocalDate, BodyPlannerUiState> { date ->
            combine(
                repository.observeProfile(),
                repository.observeProfileSettings(),
                repository.observeDiaryDay(date),
                repository.observeWeightHistory(),
            ) { profileRaw, settingsRaw, diaryDay, weightHistory ->
                val profile = profileRaw ?: UserProfile.DEFAULT
                val settings = settingsRaw ?: ProfileSettings(
                    activeProtocolId = ClinicalCatalog.DEFAULT_PROTOCOL_ID,
                    targetWaterMl = DEFAULT_WATER_TARGET_ML,
                    onboardingCompleted = true,
                )

                val energyPlan = when (val res = EnergyPlanner.plan(profile)) {
                    is DomainResult.Success -> res.value
                    is DomainResult.Failure -> EnergyPlan(
                        bmr = 1750,
                        tdee = 2400,
                        goal = profile.goal,
                        goalAdjustment = profile.goal.calorieAdjustment,
                        targetCalories = 2150,
                        macros = MacroTargets(2150, 160, 215, 65, 30),
                    )
                }

                val bmiResult = when (val res = bodyMassIndex(profile.weightKg, profile.heightCm)) {
                    is DomainResult.Success -> res.value
                    is DomainResult.Failure -> null
                }

                val protocol = ClinicalCatalog.byId(settings.activeProtocolId)
                val assessment = ClinicalAuditEngine.assess(
                    totals = diaryDay.nutrientTotals,
                    protocol = protocol,
                    caloriesEaten = diaryDay.caloriesEaten,
                )

                val today = dateProvider.today()
                BodyPlannerUiState.Success(
                    profile = profile,
                    settings = settings,
                    energyPlan = energyPlan,
                    bmiResult = bmiResult,
                    selectedDate = date,
                    isToday = date == today,
                    formattedDate = formatDateLabel(date, today),
                    diaryDay = diaryDay,
                    activeProtocol = protocol,
                    clinicalAssessment = assessment,
                    weightHistory = weightHistory,
                )
            }
        }
        .catch { emit(BodyPlannerUiState.Error(it)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            BodyPlannerUiState.Loading,
        )

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
        }
    }

    /**
     * Records a current weigh-in. The Clinical Audit can browse past days, but a new
     * weigh-in is always today's measurement, matching the dialog copy on the Body tab.
     */
    fun logWeight(weightKg: Double, date: LocalDate = dateProvider.today()) {
        viewModelScope.launch {
            repository.logWeight(date, weightKg)
            val currentProfile = (uiState.value as? BodyPlannerUiState.Success)?.profile ?: UserProfile.DEFAULT
            repository.saveProfile(currentProfile.copy(weightKg = weightKg))
        }
    }

    fun selectProtocol(protocolId: String) {
        viewModelScope.launch {
            val currentSettings = (uiState.value as? BodyPlannerUiState.Success)?.settings
                ?: ProfileSettings(activeProtocolId = protocolId, targetWaterMl = DEFAULT_WATER_TARGET_ML, onboardingCompleted = true)
            repository.saveProfileSettings(currentSettings.copy(activeProtocolId = protocolId))
        }
    }

    fun selectDate(date: LocalDate) {
        selectedDateFlow.value = date
    }

    fun previousDay() {
        selectedDateFlow.value = selectedDateFlow.value.minusDays(1)
    }

    fun nextDay() {
        selectedDateFlow.value = selectedDateFlow.value.plusDays(1)
    }

    fun today() {
        selectedDateFlow.value = dateProvider.today()
    }

    companion object {
        const val DEFAULT_WATER_TARGET_ML = 3000

        fun factory(
            repository: AroRepository,
            dateProvider: DateProvider = SystemDateProvider(),
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BodyPlannerViewModel(repository, dateProvider) as T
            }
        }

        private fun formatDateLabel(date: LocalDate, today: LocalDate): String {
            return when (date) {
                today -> "Today, ${date.format(DateTimeFormatter.ofPattern("MMM d"))}"
                today.minusDays(1) -> "Yesterday, ${date.format(DateTimeFormatter.ofPattern("MMM d"))}"
                today.plusDays(1) -> "Tomorrow, ${date.format(DateTimeFormatter.ofPattern("MMM d"))}"
                else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
            }
        }
    }
}
