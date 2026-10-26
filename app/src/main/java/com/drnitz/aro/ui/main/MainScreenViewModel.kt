package com.drnitz.aro.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.drnitz.aro.data.db.DateProvider
import com.drnitz.aro.data.db.SystemDateProvider
import com.drnitz.aro.data.repository.ActiveFast
import com.drnitz.aro.data.repository.AroRepository
import com.drnitz.aro.domain.DomainResult
import com.drnitz.aro.domain.calculation.EnergyPlan
import com.drnitz.aro.domain.calculation.EnergyPlanner
import com.drnitz.aro.domain.calculation.caloriesBurned
import com.drnitz.aro.domain.calculation.energyBalance
import com.drnitz.aro.domain.calculation.planDaySummary
import com.drnitz.aro.domain.clinical.DailyCoverage
import com.drnitz.aro.domain.clinical.MissingMacroAuditor
import com.drnitz.aro.domain.model.CuratedFoodItem
import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.EnergyBalance
import com.drnitz.aro.domain.model.Exercise
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.MacroTargets
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.PlanDaySummary
import com.drnitz.aro.domain.model.PlanItem
import com.drnitz.aro.domain.model.PlanItemType
import com.drnitz.aro.domain.model.PlanOccurrence
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.domain.model.WorkoutLog
import com.drnitz.aro.notifications.PlanReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * State for the Task 7 Main Dashboard screen.
 */
sealed interface MainScreenUiState {
    data object Loading : MainScreenUiState

    data class Error(val throwable: Throwable) : MainScreenUiState

    data class Success(
        val selectedDate: LocalDate,
        val isToday: Boolean,
        val formattedDate: String,
        val profile: UserProfile,
        val energyPlan: EnergyPlan,
        val energyBalance: EnergyBalance,
        val dayTotals: NutrientTotals,
        val diaryDay: DiaryDay,
        val activeFast: ActiveFast?,
        val targetWaterMl: Int,
        val coverage: DailyCoverage,
        /** Number of individual drinks logged on [selectedDate]. */
        val waterLogCount: Int = 0,
        val favoriteFoods: List<CuratedFoodItem> = emptyList(),
        val recentFoods: List<CuratedFoodItem> = emptyList(),
        /** Device "today", captured with the state so the calendar and future checks agree. */
        val today: LocalDate = LocalDate.now(),
        /** Trailing 7 days of workouts for volume heatmap rendering. */
        val weeklyWorkouts: List<WorkoutLog> = emptyList(),
    ) : MainScreenUiState
}

/** Calendar and planner state, kept separate from the diary's actual-log state. */
sealed interface PlanUiState {
    data object Loading : PlanUiState

    data class Error(val throwable: Throwable) : PlanUiState

    data class Success(
        val selectedDate: LocalDate,
        val calendarMonth: YearMonth,
        val selectedItems: List<PlanOccurrence>,
        val plannedDates: Map<LocalDate, Set<PlanItemType>>,
        val summary: PlanDaySummary,
        /** One-off and repeating series with a next occurrence on or after today. */
        val upcomingSeries: List<PlanItem> = emptyList(),
    ) : PlanUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModel(
    private val repository: AroRepository,
    private val dateProvider: DateProvider = SystemDateProvider(),
    private val planReminderScheduler: PlanReminderScheduler = PlanReminderScheduler.NoOp,
) : ViewModel() {

    private val selectedDateFlow = MutableStateFlow(dateProvider.today())
    private val calendarMonthFlow = MutableStateFlow(YearMonth.from(selectedDateFlow.value))

    val uiState: StateFlow<MainScreenUiState> = selectedDateFlow
        .flatMapLatest<LocalDate, MainScreenUiState> { date ->
            combine(
                repository.observeProfile(),
                repository.observeProfileSettings(),
                repository.observeDiaryDay(date),
                repository.observeActiveFast(),
                repository.observeWaterLogsForDate(date),
            ) { profileRaw, settingsRaw, diaryDay, activeFast, waterLogs ->
                val profile = profileRaw ?: UserProfile.DEFAULT
                val energyPlan = when (val res = EnergyPlanner.plan(profile)) {
                    is DomainResult.Success -> res.value
                    is DomainResult.Failure -> when (val defaultRes = EnergyPlanner.plan(UserProfile.DEFAULT)) {
                        is DomainResult.Success -> defaultRes.value
                        is DomainResult.Failure -> EnergyPlan(
                            bmr = 1750,
                            tdee = 2400,
                            goal = profile.goal,
                            goalAdjustment = profile.goal.calorieAdjustment,
                            targetCalories = 2150,
                            macros = MacroTargets(2150, 160, 215, 65, 30),
                        )
                    }
                }

                val burned = diaryDay.caloriesBurned(profile.weightKg)
                val balance = energyBalance(
                    targetCalories = energyPlan.targetCalories,
                    caloriesEaten = diaryDay.caloriesEaten,
                    caloriesBurned = burned,
                )

                val coverage = MissingMacroAuditor.audit(diaryDay, energyPlan.macros)

                val today = dateProvider.today()
                MainScreenUiState.Success(
                    selectedDate = date,
                    isToday = date == today,
                    formattedDate = formatDateLabel(date, today),
                    profile = profile,
                    energyPlan = energyPlan,
                    energyBalance = balance,
                    dayTotals = diaryDay.nutrientTotals,
                    diaryDay = diaryDay,
                    activeFast = activeFast,
                    targetWaterMl = settingsRaw?.targetWaterMl ?: DEFAULT_WATER_TARGET_ML,
                    coverage = coverage,
                    waterLogCount = waterLogs.size,
                    today = today,
                )
            }.combine(repository.observeWorkoutsInRange(date.minusDays(6), date)) { successState, weeklyWorkouts ->
                successState.copy(weeklyWorkouts = weeklyWorkouts)
            }
        }
        .combine(repository.observeFavoriteFoods()) { state, favorites ->
            if (state is MainScreenUiState.Success) state.copy(favoriteFoods = favorites) else state
        }
        .combine(repository.observeRecentFoods()) { state, recents ->
            if (state is MainScreenUiState.Success) state.copy(recentFoods = recents) else state
        }
        .catch { emit(MainScreenUiState.Error(it)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            MainScreenUiState.Loading,
        )

    val planState: StateFlow<PlanUiState> = combine(
        selectedDateFlow,
        calendarMonthFlow,
    ) { date, month -> date to month }
        .flatMapLatest { (date, month) ->
            combine(
                repository.observePlanOccurrences(date),
                repository.observePlanOccurrencesInRange(
                    month.atDay(1).minusDays(7),
                    month.atEndOfMonth().plusDays(7),
                ),
                repository.observeProfile(),
                repository.observePlanItems(),
            ) { selected, monthOccurrences, profileRaw, allSeries ->
                val weightKg = (profileRaw ?: UserProfile.DEFAULT).weightKg
                val today = dateProvider.today()
                val upcoming = allSeries
                    .mapNotNull { item ->
                        item.nextOccurrenceOnOrAfter(today)?.let { next -> next to item }
                    }
                    .sortedWith(
                        compareBy(
                            { (next, _) -> next },
                            { (_, item) -> item.time },
                            { (_, item) -> item.title },
                        ),
                    )
                    .map { (_, item) -> item }
                PlanUiState.Success(
                    selectedDate = date,
                    calendarMonth = month,
                    selectedItems = selected.sortedWith(
                        compareBy({ it.item.time }, { it.item.title }),
                    ),
                    plannedDates = monthOccurrences
                        .groupBy { it.date }
                        .mapValues { (_, items) -> items.map { it.item.type }.toSet() },
                    summary = planDaySummary(selected, weightKg),
                    upcomingSeries = upcoming,
                ) as PlanUiState
            }
        }
        .catch { emit(PlanUiState.Error(it)) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            PlanUiState.Loading,
        )

    val customExercises: StateFlow<List<Exercise>> = repository.observeCustomExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteExerciseIds: StateFlow<Set<String>> = repository.observeFavoriteExerciseIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun selectDate(date: LocalDate) {
        selectedDateFlow.value = date
        calendarMonthFlow.value = YearMonth.from(date)
    }

    fun previousDay() {
        updateSelectedDate(selectedDateFlow.value.minusDays(1))
    }

    fun nextDay() {
        updateSelectedDate(selectedDateFlow.value.plusDays(1))
    }

    fun goToToday() {
        updateSelectedDate(dateProvider.today())
    }

    /** Changes only the visible calendar month; the selected day stays put. */
    fun showPlanMonth(month: YearMonth) {
        calendarMonthFlow.value = month
    }

    private fun updateSelectedDate(date: LocalDate) {
        selectedDateFlow.value = date
        calendarMonthFlow.value = YearMonth.from(date)
    }

    fun addWater(deltaMl: Int) {
        viewModelScope.launch {
            repository.addWater(selectedDateFlow.value, deltaMl)
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            repository.setWater(selectedDateFlow.value, 0)
        }
    }

    fun startFast(targetHours: Int = 16, startedAt: Instant = Instant.now()) {
        viewModelScope.launch {
            repository.startFast(targetHours, startedAt)
        }
    }

    fun endFast() {
        viewModelScope.launch {
            repository.endFast()
        }
    }

    fun addFood(entry: FoodLogEntry) {
        viewModelScope.launch {
            repository.addFood(entry, selectedDateFlow.value)
        }
    }

    fun updateFood(entry: FoodLogEntry) {
        viewModelScope.launch {
            repository.updateFood(entry, selectedDateFlow.value)
        }
    }

    fun deleteFood(id: String) {
        viewModelScope.launch {
            repository.deleteFood(id)
        }
    }

    fun logWorkout(workout: WorkoutLog) {
        viewModelScope.launch {
            repository.logWorkout(workout, selectedDateFlow.value)
        }
    }

    fun deleteWorkout(id: String) {
        viewModelScope.launch {
            repository.deleteWorkout(id)
        }
    }

    suspend fun getLastWorkoutForExercise(exerciseName: String): WorkoutLog? =
        repository.getLastWorkoutForExercise(exerciseName)

    fun toggleFavoriteExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.toggleFavoriteExercise(exercise)
        }
    }

    fun saveCustomExercise(exercise: Exercise) {
        viewModelScope.launch {
            repository.saveCustomExercise(exercise)
        }
    }

    fun deleteCustomExercise(id: String) {
        viewModelScope.launch {
            repository.deleteCustomExercise(id)
        }
    }

    fun toggleFavorite(food: CuratedFoodItem) {
        viewModelScope.launch {
            repository.toggleFavoriteFood(food)
        }
    }

    fun recordFoodUsage(food: CuratedFoodItem, grams: Double) {
        viewModelScope.launch {
            repository.recordFoodUsage(food, grams)
        }
    }

    // ---- Future-day plans -------------------------------------------------

    fun savePlanItem(item: PlanItem) {
        viewModelScope.launch {
            repository.savePlanItem(item)
            planReminderScheduler.reschedulePlan(item.id, dismissNotification = true)
        }
    }

    fun deletePlanItem(id: String) {
        viewModelScope.launch {
            repository.deletePlanItem(id)
            planReminderScheduler.cancelPlan(id)
        }
    }

    fun completePlanOccurrence(
        planId: String,
        occurrenceDate: LocalDate,
        actualLogId: String? = null,
    ) {
        viewModelScope.launch {
            repository.completePlanOccurrence(planId, occurrenceDate, actualLogId)
            planReminderScheduler.reschedulePlan(
                planId = planId,
                completedDate = occurrenceDate,
                dismissNotification = true,
            )
        }
    }

    fun skipPlanOccurrence(planId: String, occurrenceDate: LocalDate) {
        viewModelScope.launch {
            repository.skipPlanOccurrence(planId, occurrenceDate)
            planReminderScheduler.reschedulePlan(
                planId = planId,
                completedDate = occurrenceDate,
                dismissNotification = true,
            )
        }
    }

    fun uncompletePlanOccurrence(planId: String, occurrenceDate: LocalDate) {
        viewModelScope.launch {
            repository.uncompletePlanOccurrence(planId, occurrenceDate)
            planReminderScheduler.reschedulePlan(planId)
        }
    }

    companion object {
        const val DEFAULT_WATER_TARGET_ML = 3000

        fun factory(
            repository: AroRepository,
            dateProvider: DateProvider = SystemDateProvider(),
            planReminderScheduler: PlanReminderScheduler = PlanReminderScheduler.NoOp,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainScreenViewModel(
                    repository = repository,
                    dateProvider = dateProvider,
                    planReminderScheduler = planReminderScheduler,
                ) as T
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
