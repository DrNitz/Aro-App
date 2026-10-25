package com.drnitz.aro.ui.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.drnitz.aro.AroApplication
import com.drnitz.aro.BodyPlanner
import com.drnitz.aro.ClinicalAudit
import com.drnitz.aro.ClinicalRecords
import com.drnitz.aro.DataManagement
import com.drnitz.aro.IntakeHistory
import com.drnitz.aro.PlanDeepLink
import com.drnitz.aro.R
import com.drnitz.aro.data.db.AroDatabase
import com.drnitz.aro.data.library.OfflineExerciseLibrary
import com.drnitz.aro.data.library.OfflineFoodLibrary
import com.drnitz.aro.data.network.OpenFoodFactsClient
import com.drnitz.aro.data.repository.RoomAroRepository
import com.drnitz.aro.domain.calculation.EnergyPlan
import com.drnitz.aro.domain.calculation.MetCatalog
import com.drnitz.aro.domain.clinical.MissingMacroAuditor
import com.drnitz.aro.domain.model.CuratedFoodItem
import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.EnergyBalance
import com.drnitz.aro.domain.model.Exercise
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.MacroTargets
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.PlanItem
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.domain.model.WorkoutLog
import com.drnitz.aro.domain.model.WorkoutType
import com.drnitz.aro.notifications.AroNotificationScheduler
import com.drnitz.aro.theme.AroTheme
import com.drnitz.aro.ui.components.AroHeader
import com.drnitz.aro.ui.components.AroLoggingDateLabel
import com.drnitz.aro.ui.exercise.ExerciseDatabaseScreen
import com.drnitz.aro.ui.food.FoodDatabaseScreen
import com.drnitz.aro.ui.goals.GoalsBodyScreen
import com.drnitz.aro.ui.notifications.NotificationSettingsDialog
import com.drnitz.aro.ui.plan.AroMonthCalendar
import com.drnitz.aro.ui.plan.AroWeekStrip
import com.drnitz.aro.ui.plan.PlanDayShortcut
import com.drnitz.aro.ui.plan.PlanExercisePickerDialog
import com.drnitz.aro.ui.plan.PlanTabScreen
import com.drnitz.aro.ui.plan.fastingPlanItem
import com.drnitz.aro.ui.plan.toPlanItem
import com.drnitz.aro.ui.plan.waterPlanItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onItemClick: (NavKey) -> Unit = {},
    viewModel: MainScreenViewModel? = null,
    foodLibrary: OfflineFoodLibrary? = null,
    exerciseLibrary: OfflineExerciseLibrary? = null,
    openFoodFactsClient: OpenFoodFactsClient? = null,
    planDeepLink: PlanDeepLink? = null,
    onPlanDeepLinkHandled: (Long) -> Unit = {},
) {
    val context = LocalContext.current.applicationContext
    val vm: MainScreenViewModel = viewModel ?: run {
        if (context is AroApplication) {
            viewModel(
                factory = MainScreenViewModel.factory(
                    repository = context.repository,
                    planReminderScheduler = context.planReminderScheduler,
                ),
            )
        } else {
            val db = AroDatabase.get(context)
            viewModel(factory = MainScreenViewModel.factory(RoomAroRepository(db)))
        }
    }
    val effectiveFoodLibrary = foodLibrary ?: run {
        if (context is AroApplication) {
            context.foodLibrary
        } else {
            OfflineFoodLibrary(context)
        }
    }
    val effectiveExerciseLibrary = exerciseLibrary ?: run {
        if (context is AroApplication) {
            context.exerciseLibrary
        } else {
            OfflineExerciseLibrary(context)
        }
    }
    val effectiveOffClient = openFoodFactsClient ?: run {
        if (context is AroApplication) {
            context.openFoodFactsClient
        } else {
            OpenFoodFactsClient()
        }
    }

    val state by vm.uiState.collectAsStateWithLifecycle()
    val planState by vm.planState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(planDeepLink?.requestId) {
        planDeepLink?.let { vm.selectDate(it.date) }
    }

    when (val s = state) {
        MainScreenUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is MainScreenUiState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.error_loading_dashboard),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = s.throwable.message ?: stringResource(R.string.error_unknown),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        is MainScreenUiState.Success -> {
            MainScreenContent(
                state = s,
                planState = planState,
                planDeepLink = planDeepLink,
                onPlanDeepLinkHandled = onPlanDeepLinkHandled,
                foodLibrary = effectiveFoodLibrary,
                exerciseLibrary = effectiveExerciseLibrary,
                openFoodFactsClient = effectiveOffClient,
                onSelectDate = vm::selectDate,
                onToday = vm::goToToday,
                onAddWater = vm::addWater,
                onResetWater = vm::resetWater,
                onStartFast = { hours ->
                    val startedAt = Instant.now()
                    vm.startFast(hours, startedAt)
                    scope.launch {
                        AroNotificationScheduler.scheduleFastingCompletion(
                            context,
                            hours,
                            startedAt,
                        )
                    }
                },
                onEndFast = {
                    vm.endFast()
                    AroNotificationScheduler.cancelFastingCompletion(context)
                },
                onAddFood = vm::addFood,
                onUpdateFood = vm::updateFood,
                onDeleteFood = vm::deleteFood,
                onLogWorkout = vm::logWorkout,
                onDeleteWorkout = vm::deleteWorkout,
                onToggleFavorite = vm::toggleFavorite,
                onRecordFoodUsage = vm::recordFoodUsage,
                onPlanMonthChange = vm::showPlanMonth,
                onSavePlanItem = vm::savePlanItem,
                onDeletePlanItem = vm::deletePlanItem,
                onCompletePlanOccurrence = vm::completePlanOccurrence,
                onSkipPlanOccurrence = vm::skipPlanOccurrence,
                onUncompletePlanOccurrence = vm::uncompletePlanOccurrence,
                onLoadPreviousWorkout = vm::getLastWorkoutForExercise,
                onItemClick = onItemClick,
                modifier = modifier,
            )
        }
    }
}

private enum class AroTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    DIARY(R.string.tab_diary, Icons.AutoMirrored.Filled.MenuBook),
    FOOD(R.string.tab_food, Icons.Default.Restaurant),
    WORKOUTS(R.string.tab_workouts, Icons.Default.FitnessCenter),
    GOALS(R.string.tab_goals, Icons.Default.TrackChanges),
}

@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
@Composable
internal fun MainScreenContent(
    state: MainScreenUiState.Success,
    modifier: Modifier = Modifier,
    planState: PlanUiState = PlanUiState.Loading,
    planDeepLink: PlanDeepLink? = null,
    onPlanDeepLinkHandled: (Long) -> Unit = {},
    foodLibrary: OfflineFoodLibrary? = null,
    exerciseLibrary: OfflineExerciseLibrary? = null,
    openFoodFactsClient: OpenFoodFactsClient? = null,
    onSelectDate: (LocalDate) -> Unit = {},
    onToday: () -> Unit = {},
    onAddWater: (Int) -> Unit = {},
    onResetWater: () -> Unit = {},
    onStartFast: (Int) -> Unit = {},
    onEndFast: () -> Unit = {},
    onAddFood: (FoodLogEntry) -> Unit = {},
    onUpdateFood: (FoodLogEntry) -> Unit = {},
    onDeleteFood: (String) -> Unit = {},
    onLogWorkout: (WorkoutLog) -> Unit = {},
    onDeleteWorkout: (String) -> Unit = {},
    onToggleFavorite: (CuratedFoodItem) -> Unit = {},
    onRecordFoodUsage: (CuratedFoodItem, Double) -> Unit = { _, _ -> },
    onPlanMonthChange: (YearMonth) -> Unit = {},
    onSavePlanItem: (PlanItem) -> Unit = {},
    onDeletePlanItem: (String) -> Unit = {},
    onCompletePlanOccurrence: (String, LocalDate, String?) -> Unit = { _, _, _ -> },
    onSkipPlanOccurrence: (String, LocalDate) -> Unit = { _, _ -> },
    onUncompletePlanOccurrence: (String, LocalDate) -> Unit = { _, _ -> },
    onLoadPreviousWorkout: (suspend (String) -> WorkoutLog?)? = null,
    onItemClick: (NavKey) -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showPlanView by rememberSaveable { mutableStateOf(false) }
    var returnToPlanAfterPlanAdd by rememberSaveable { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    val currentTab = AroTab.entries.getOrElse(selectedTab) { AroTab.DIARY }
    val planningFuture = state.selectedDate.isAfter(state.today)

    LaunchedEffect(planDeepLink?.requestId) {
        val link = planDeepLink ?: return@LaunchedEffect
        selectedTab = AroTab.DIARY.ordinal
        showPlanView = true
        returnToPlanAfterPlanAdd = false
        onPlanDeepLinkHandled(link.requestId)
    }

    // The full-screen plan view lives inside the Main nav entry as a state swap, so the
    // Navigation 3 stack does not know about it. Intercept system back while it is open
    // so back returns to the diary instead of popping the root screen (app exit).
    BackHandler(enabled = showPlanView) {
        showPlanView = false
        returnToPlanAfterPlanAdd = false
    }

    val selectTab: (Int) -> Unit = { index ->
        if (index == AroTab.DIARY.ordinal) {
            // Tapping the already-selected Diary tab leaves the full-screen plan view
            // and returns to the diary.
            if (currentTab == AroTab.DIARY) showPlanView = false
        } else {
            showPlanView = false
        }
        selectedTab = index
        if (index != AroTab.FOOD.ordinal && index != AroTab.WORKOUTS.ordinal) {
            returnToPlanAfterPlanAdd = false
        }
    }

    // Adaptive navigation suite: bottom NavigationBar on compact phones, left
    // NavigationRail on medium/expanded widths (foldables, tablets, landscape)
    // as decided by the material3-adaptive window size classes.
    NavigationSuiteScaffold(
        modifier = modifier.fillMaxSize(),
        navigationSuiteItems = {
            AroTab.entries.forEachIndexed { index, tab ->
                item(
                    selected = currentTab.ordinal == index,
                    onClick = { selectTab(index) },
                    icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                    label = {
                        Text(
                            text = stringResource(tab.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                AroHeader(
                    onOpenNotifications = { showNotificationSettingsDialog = true },
                    onOpenGoals = { selectedTab = AroTab.GOALS.ordinal },
                )
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (currentTab) {
                    AroTab.DIARY -> {
                        if (showPlanView) {
                            PlanTabScreen(
                                state = state,
                                planState = planState,
                                foodLibrary = foodLibrary,
                                exerciseLibrary = exerciseLibrary,
                                openFoodFactsClient = openFoodFactsClient,
                                onBack = {
                                    showPlanView = false
                                    returnToPlanAfterPlanAdd = false
                                },
                                onSelectDate = onSelectDate,
                                onToday = onToday,
                                onPlanFoodInFoodTab = {
                                    returnToPlanAfterPlanAdd = true
                                    showPlanView = false
                                    selectedTab = AroTab.FOOD.ordinal
                                },
                                onPlanExerciseInWorkoutTab = {
                                    returnToPlanAfterPlanAdd = true
                                    showPlanView = false
                                    selectedTab = AroTab.WORKOUTS.ordinal
                                },
                                onSavePlanItem = onSavePlanItem,
                                onDeletePlanItem = onDeletePlanItem,
                                onCompletePlanOccurrence = onCompletePlanOccurrence,
                                onSkipPlanOccurrence = onSkipPlanOccurrence,
                                onUncompletePlanOccurrence = onUncompletePlanOccurrence,
                                onAddFood = onAddFood,
                                onAddWater = onAddWater,
                                onStartFast = onStartFast,
                                onLogWorkout = onLogWorkout,
                            )
                        } else {
                            DiaryTabContent(
                                state = state,
                                planState = planState,
                                foodLibrary = foodLibrary,
                                exerciseLibrary = exerciseLibrary,
                                openFoodFactsClient = openFoodFactsClient,
                                onSelectDate = onSelectDate,
                                onToday = onToday,
                                onAddWater = onAddWater,
                                onResetWater = onResetWater,
                                onStartFast = onStartFast,
                                onEndFast = onEndFast,
                                onAddFood = onAddFood,
                                onUpdateFood = onUpdateFood,
                                onDeleteFood = onDeleteFood,
                                onLogWorkout = onLogWorkout,
                                onDeleteWorkout = onDeleteWorkout,
                                onRecordFoodUsage = onRecordFoodUsage,
                                onPlanMonthChange = onPlanMonthChange,
                                onSavePlanItem = onSavePlanItem,
                                onOpenPlan = { showPlanView = true },
                                onOpenNotifications = { showNotificationSettingsDialog = true },
                                onLoadPreviousWorkout = onLoadPreviousWorkout,
                                onItemClick = onItemClick,
                            )
                        }
                    }

                    AroTab.FOOD -> {
                        if (foodLibrary != null) {
                            FoodDatabaseScreen(
                                foodLibrary = foodLibrary,
                                favoriteFoods = state.favoriteFoods,
                                recentFoods = state.recentFoods,
                                openFoodFactsClient = openFoodFactsClient,
                                selectedDate = state.selectedDate,
                                formattedDate = state.formattedDate,
                                isToday = state.isToday,
                                isPlanning = planningFuture,
                                onSelectDate = onSelectDate,
                                onToday = onToday,
                                onFoodLogged = { entry, food ->
                                    if (planningFuture) {
                                        onSavePlanItem(
                                            entry.toPlanItem(state.selectedDate, imageUrl = food.image),
                                        )
                                        if (returnToPlanAfterPlanAdd) {
                                            returnToPlanAfterPlanAdd = false
                                            selectedTab = AroTab.DIARY.ordinal
                                            showPlanView = true
                                        }
                                    } else {
                                        onAddFood(entry)
                                        onRecordFoodUsage(food, entry.grams)
                                    }
                                },
                                onToggleFavorite = onToggleFavorite,
                            )
                        } else {
                            MissingLibrary(stringResource(R.string.library_food_loading))
                        }
                    }

                    AroTab.WORKOUTS -> {
                        if (exerciseLibrary != null) {
                            ExerciseDatabaseScreen(
                                exerciseLibrary = exerciseLibrary,
                                userWeightKg = state.profile.weightKg,
                                userHeightCm = state.profile.heightCm,
                                selectedDate = state.selectedDate,
                                formattedDate = state.formattedDate,
                                isToday = state.isToday,
                                isPlanning = planningFuture,
                                onSelectDate = onSelectDate,
                                onToday = onToday,
                                onWorkoutLogged = { workout ->
                                    if (planningFuture) {
                                        onSavePlanItem(workout.toPlanItem(state.selectedDate))
                                        if (returnToPlanAfterPlanAdd) {
                                            returnToPlanAfterPlanAdd = false
                                            selectedTab = AroTab.DIARY.ordinal
                                            showPlanView = true
                                        }
                                    } else {
                                        onLogWorkout(workout)
                                    }
                                },
                                onExercisePlanned = { exercise ->
                                    onSavePlanItem(exercise.toPlanItem(state.selectedDate))
                                    if (returnToPlanAfterPlanAdd) {
                                        returnToPlanAfterPlanAdd = false
                                        selectedTab = AroTab.DIARY.ordinal
                                        showPlanView = true
                                    }
                                },
                                onLoadPreviousWorkout = onLoadPreviousWorkout,
                            )
                        } else {
                            MissingLibrary(stringResource(R.string.library_exercise_loading))
                        }
                    }

                    AroTab.GOALS -> GoalsBodyScreen(
                        profile = state.profile,
                        energyPlan = state.energyPlan,
                        onOpenBodyPlanner = { onItemClick(BodyPlanner) },
                        onOpenVitals = { onItemClick(ClinicalRecords(0)) },
                        onOpenDataManagement = { onItemClick(DataManagement) },
                        onOpenIntakeHistory = { onItemClick(IntakeHistory(0)) },
                    )
                }
            }
        }
    }

    if (showNotificationSettingsDialog) {
        NotificationSettingsDialog(onDismiss = { showNotificationSettingsDialog = false })
    }
}

@Composable
private fun MissingLibrary(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DiaryTabContent(
    state: MainScreenUiState.Success,
    modifier: Modifier = Modifier,
    planState: PlanUiState = PlanUiState.Loading,
    foodLibrary: OfflineFoodLibrary? = null,
    exerciseLibrary: OfflineExerciseLibrary? = null,
    openFoodFactsClient: OpenFoodFactsClient? = null,
    onSelectDate: (LocalDate) -> Unit = {},
    onToday: () -> Unit = {},
    onAddWater: (Int) -> Unit = {},
    onResetWater: () -> Unit = {},
    onStartFast: (Int) -> Unit = {},
    onEndFast: () -> Unit = {},
    onAddFood: (FoodLogEntry) -> Unit = {},
    onUpdateFood: (FoodLogEntry) -> Unit = {},
    onDeleteFood: (String) -> Unit = {},
    onLogWorkout: (WorkoutLog) -> Unit = {},
    onDeleteWorkout: (String) -> Unit = {},
    onRecordFoodUsage: (CuratedFoodItem, Double) -> Unit = { _, _ -> },
    onPlanMonthChange: (YearMonth) -> Unit = {},
    onSavePlanItem: (PlanItem) -> Unit = {},
    onOpenPlan: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onLoadPreviousWorkout: (suspend (String) -> WorkoutLog?)? = null,
    onItemClick: (NavKey) -> Unit = {},
) {
    var addFoodSlot by remember { mutableStateOf<MealSlot?>(null) }
    var editingFood by remember { mutableStateOf<FoodLogEntry?>(null) }
    var showFastingProtocolDialog by remember { mutableStateOf(false) }
    var showCustomHydrationDialog by remember { mutableStateOf(false) }
    var showLogWorkoutDialog by remember { mutableStateOf(false) }
    var duplicatingWorkout by remember { mutableStateOf<WorkoutLog?>(null) }
    var duplicatingExercise by remember { mutableStateOf<Exercise?>(null) }

    LaunchedEffect(duplicatingWorkout?.id, duplicatingWorkout?.exerciseName) {
        val workout = duplicatingWorkout
        if (workout != null && exerciseLibrary != null && workout.isStrength) {
            val matches = exerciseLibrary.search(workout.exerciseName)
            duplicatingExercise = matches.firstOrNull { it.name.equals(workout.exerciseName, ignoreCase = true) }
                ?: matches.firstOrNull()
                ?: Exercise(
                    id = "workout-${workout.exerciseName.lowercase().replace(" ", "-")}",
                    name = workout.exerciseName,
                    category = workout.category.ifBlank { "strength" },
                    primaryMuscles = listOfNotNull(workout.muscle).ifEmpty { listOf("chest") },
                    secondaryMuscles = emptyList(),
                    equipment = workout.equipment ?: "barbell",
                    level = "intermediate",
                    instructions = emptyList(),
                    images = emptyList(),
                )
        } else {
            duplicatingExercise = null
        }
    }
    var showPlanExercisePicker by remember { mutableStateOf(false) }

    val planning = state.selectedDate.isAfter(state.today)
    val planSuccess = planState as? PlanUiState.Success
    val calendarMonth = planSuccess?.calendarMonth ?: YearMonth.from(state.selectedDate)
    val selectedPlan = planSuccess?.takeIf { it.selectedDate == state.selectedDate }
    val onCalendarDate: (LocalDate) -> Unit = { date -> onSelectDate(date) }
    val planShortcut: @Composable () -> Unit = {
        PlanDayShortcut(
            date = state.selectedDate,
            today = state.today,
            itemCount = selectedPlan?.selectedItems?.size ?: 0,
            plannedCalories = selectedPlan?.summary?.plannedCalories ?: 0,
            onOpenPlan = onOpenPlan,
        )
    }
    val waterAction: (Int) -> Unit = { amount ->
        if (planning) {
            onSavePlanItem(waterPlanItem(state.selectedDate, amount))
        } else {
            onAddWater(amount)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // A landscape phone can be 720dp wide while only ~360dp tall; keep the stacked
        // layout there so the six-week calendar is not clipped.
        val wide = maxWidth >= 720.dp && maxHeight >= 560.dp

        if (wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.width(360.dp)) {
                    AroMonthCalendar(
                        month = calendarMonth,
                        selectedDate = state.selectedDate,
                        today = state.today,
                        plannedDates = planSuccess?.plannedDates ?: emptyMap(),
                        onMonthChange = onPlanMonthChange,
                        onDateClick = onCalendarDate,
                    )
                    Spacer(Modifier.height(12.dp))
                    planShortcut()
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    DiarySections(
                        state = state,
                        planSuccess = planSuccess,
                        planningMode = planning,
                        onToday = onToday,
                        onAddWater = waterAction,
                        onResetWater = onResetWater,
                        onOpenCustomWater = { showCustomHydrationDialog = true },
                        onOpenStartFast = { showFastingProtocolDialog = true },
                        onEndFast = onEndFast,
                        onOpenNotifications = onOpenNotifications,
                        onOpenAddFood = { addFoodSlot = it },
                        onEditFood = { editingFood = it },
                        onDeleteFood = onDeleteFood,
                        onOpenLogWorkout = {
                            if (planning) showPlanExercisePicker = true else showLogWorkoutDialog = true
                        },
                        onDeleteWorkout = onDeleteWorkout,
                        onDuplicateWorkout = { duplicatingWorkout = it },
                        onItemClick = onItemClick,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AroWeekStrip(
                    selectedDate = state.selectedDate,
                    today = state.today,
                    calendarMonth = calendarMonth,
                    plannedDates = planSuccess?.plannedDates ?: emptyMap(),
                    onSelectDate = onCalendarDate,
                    onMonthChange = onPlanMonthChange,
                )

                planShortcut()

                DiarySections(
                    state = state,
                    planSuccess = planSuccess,
                    planningMode = planning,
                    onToday = onToday,
                    onAddWater = waterAction,
                    onResetWater = onResetWater,
                    onOpenCustomWater = { showCustomHydrationDialog = true },
                    onOpenStartFast = { showFastingProtocolDialog = true },
                    onEndFast = onEndFast,
                    onOpenNotifications = onOpenNotifications,
                    onOpenAddFood = { addFoodSlot = it },
                    onEditFood = { editingFood = it },
                    onDeleteFood = onDeleteFood,
                    onOpenLogWorkout = {
                        if (planning) showPlanExercisePicker = true else showLogWorkoutDialog = true
                    },
                    onDeleteWorkout = onDeleteWorkout,
                    onDuplicateWorkout = { duplicatingWorkout = it },
                    onItemClick = onItemClick,
                )
            }
        }
    }

    addFoodSlot?.let { slot ->
        if (foodLibrary != null) {
            AddFoodDialog(
                slot = slot,
                foodLibrary = foodLibrary,
                openFoodFactsClient = openFoodFactsClient,
                targetDateLabel = state.formattedDate,
                targetDateIsToday = state.isToday,
                planningMode = planning,
                onRecordUsage = onRecordFoodUsage,
                onDismiss = { addFoodSlot = null },
                onFoodLogged = { entry, food ->
                    if (planning) {
                        onSavePlanItem(
                            entry.toPlanItem(state.selectedDate, imageUrl = food.image),
                        )
                    } else {
                        onAddFood(entry)
                    }
                    addFoodSlot = null
                },
            )
        }
    }

    editingFood?.let { food ->
        EditPortionDialog(
            entry = food,
            onDismiss = { editingFood = null },
            onConfirm = { updated ->
                onUpdateFood(updated)
                editingFood = null
            },
        )
    }

    if (showFastingProtocolDialog) {
        StartFastProtocolDialog(
            planningMode = planning,
            onDismiss = { showFastingProtocolDialog = false },
            onStartFast = { hours ->
                if (planning) {
                    onSavePlanItem(fastingPlanItem(state.selectedDate, hours))
                } else {
                    onStartFast(hours)
                }
                showFastingProtocolDialog = false
            },
        )
    }

    if (showCustomHydrationDialog) {
        CustomHydrationDialog(
            planningMode = planning,
            onDismiss = { showCustomHydrationDialog = false },
            onAddWater = { amount ->
                waterAction(amount)
                showCustomHydrationDialog = false
            },
        )
    }

    if (showLogWorkoutDialog) {
        if (exerciseLibrary != null) {
            LogWorkoutDialog(
                exerciseLibrary = exerciseLibrary,
                userWeightKg = state.profile.weightKg,
                userHeightCm = state.profile.heightCm,
                targetDateLabel = state.formattedDate,
                targetDateIsToday = state.isToday,
                onLoadPreviousWorkout = onLoadPreviousWorkout,
                onDismiss = { showLogWorkoutDialog = false },
                onWorkoutLogged = { workout ->
                    onLogWorkout(workout)
                    showLogWorkoutDialog = false
                },
            )
        }
    }

    duplicatingWorkout?.let { workout ->
        if (exerciseLibrary != null) {
            val exercise = if (workout.isStrength) duplicatingExercise else null
            val activity = if (workout.isCardio) {
                MetCatalog.all.firstOrNull { it.name.equals(workout.exerciseName, ignoreCase = true) }
            } else {
                null
            }

            if (!workout.isStrength || exercise != null) {
                LogWorkoutDialog(
                    exerciseLibrary = exerciseLibrary,
                    userWeightKg = state.profile.weightKg,
                    userHeightCm = state.profile.heightCm,
                    preselectedExercise = exercise,
                    preselectedActivity = activity,
                    initialWorkoutType = workout.type,
                    initialDurationMinutes = workout.durationMinutes,
                    initialSets = workout.sets.map { it.copy(isCompleted = false) },
                    targetDateLabel = state.formattedDate,
                    targetDateIsToday = state.isToday,
                    onDismiss = { duplicatingWorkout = null },
                    onWorkoutLogged = { newWorkout ->
                        onLogWorkout(newWorkout)
                        duplicatingWorkout = null
                    },
                )
            }
        }
    }

    if (showPlanExercisePicker) {
        if (exerciseLibrary != null) {
            PlanExercisePickerDialog(
                targetDate = state.selectedDate,
                exerciseLibrary = exerciseLibrary,
                userWeightKg = state.profile.weightKg,
                onDismiss = { showPlanExercisePicker = false },
                onSave = { item ->
                    onSavePlanItem(item)
                    showPlanExercisePicker = false
                },
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val defaultProfile = UserProfile.DEFAULT
    val defaultPlan = EnergyPlan(
        bmr = 1750,
        tdee = 2400,
        goal = defaultProfile.goal,
        goalAdjustment = -250,
        targetCalories = 2150,
        macros = MacroTargets(2150, 160, 215, 65, 30),
    )
    AroTheme {
        MainScreenContent(
            state = MainScreenUiState.Success(
                selectedDate = LocalDate.now(),
                isToday = true,
                formattedDate = "Today, Oct 21",
                profile = defaultProfile,
                energyPlan = defaultPlan,
                energyBalance = EnergyBalance(2150, 1420, 320),
                dayTotals = NutrientTotals.of(
                    com.drnitz.aro.domain.model.NutrientKey.CALORIES to 1420.0,
                    com.drnitz.aro.domain.model.NutrientKey.PROTEIN to 115.0,
                    com.drnitz.aro.domain.model.NutrientKey.CARBS to 140.0,
                    com.drnitz.aro.domain.model.NutrientKey.FAT to 42.0,
                    com.drnitz.aro.domain.model.NutrientKey.FIBER to 18.0,
                ),
                diaryDay = DiaryDay(LocalDate.now(), waterMl = 1250),
                activeFast = null,
                targetWaterMl = 3000,
                coverage = MissingMacroAuditor.audit(
                    DiaryDay(LocalDate.now(), waterMl = 1250),
                    defaultPlan.macros,
                ),
            ),
        )
    }
}
