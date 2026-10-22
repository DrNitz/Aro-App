package com.drnitz.aro.ui.main

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.drnitz.aro.AroApplication
import com.drnitz.aro.BodyPlanner
import com.drnitz.aro.ClinicalAudit
import com.drnitz.aro.ClinicalRecords
import com.drnitz.aro.PlanDeepLink
import com.drnitz.aro.data.db.AroDatabase
import com.drnitz.aro.data.library.OfflineExerciseLibrary
import com.drnitz.aro.data.library.OfflineFoodLibrary
import com.drnitz.aro.data.library.imageUrlAt
import com.drnitz.aro.data.library.imageUrls
import com.drnitz.aro.data.network.OpenFoodFactsClient
import com.drnitz.aro.data.repository.RoomAroRepository
import com.drnitz.aro.domain.calculation.EnergyPlan
import com.drnitz.aro.domain.calculation.KCAL_PER_KG_TONNAGE
import com.drnitz.aro.domain.calculation.MetActivity
import com.drnitz.aro.domain.calculation.MetCatalog
import com.drnitz.aro.domain.calculation.OneRepMaxCalculator
import com.drnitz.aro.domain.calculation.caloriesBurned
import com.drnitz.aro.domain.clinical.DailyCoverage
import com.drnitz.aro.domain.clinical.MealCoverage
import com.drnitz.aro.domain.clinical.MissingMacroAuditor
import com.drnitz.aro.domain.model.CuratedFoodItem
import com.drnitz.aro.domain.model.DiaryDay
import com.drnitz.aro.domain.model.EnergyBalance
import com.drnitz.aro.domain.model.Exercise
import com.drnitz.aro.domain.model.FastingStage
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.MacroTargets
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.PlanItem
import com.drnitz.aro.domain.model.SetEntry
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.domain.model.WorkoutLog
import com.drnitz.aro.domain.model.WorkoutType
import com.drnitz.aro.notifications.AroNotificationScheduler
import com.drnitz.aro.theme.AroTheme
import com.drnitz.aro.ui.components.AroHeader
import com.drnitz.aro.ui.components.AroLoggingDateLabel
import com.drnitz.aro.ui.components.BarcodeScannerView
import com.drnitz.aro.ui.components.ConcentricMacroRings
import com.drnitz.aro.ui.components.ExerciseImage
import com.drnitz.aro.ui.exercise.ExerciseDatabaseScreen
import com.drnitz.aro.ui.food.FoodDatabaseScreen
import com.drnitz.aro.ui.goals.GoalsBodyScreen
import com.drnitz.aro.ui.notifications.NotificationSettingsDialog
import com.drnitz.aro.ui.plan.AroMonthCalendar
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
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier,
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
                        text = "Unable to load dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = s.throwable.message ?: "Unknown error",
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
                    vm.startFast(hours)
                    AroNotificationScheduler.scheduleFastingCompletion(context, hours, System.currentTimeMillis())
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
                onItemClick = onItemClick,
                modifier = modifier,
            )
        }
    }
}

private enum class AroTab(val label: String, val icon: ImageVector) {
    DIARY("Diary", Icons.AutoMirrored.Filled.MenuBook),
    FOOD("Food", Icons.Default.Restaurant),
    WORKOUTS("Workouts", Icons.Default.FitnessCenter),
    GOALS("Goals", Icons.Default.TrackChanges),
}

@Composable
internal fun MainScreenContent(
    state: MainScreenUiState.Success,
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
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier,
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AroHeader(
                onOpenNotifications = { showNotificationSettingsDialog = true },
                onOpenGoals = { selectedTab = AroTab.GOALS.ordinal },
            )
        },
        bottomBar = {
            NavigationBar {
                AroTab.entries.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = currentTab.ordinal == index,
                        onClick = {
                            if (index == AroTab.DIARY.ordinal) {
                                // Tapping the already-selected Diary tab leaves the
                                // full-screen plan view and returns to the diary.
                                if (currentTab == AroTab.DIARY) showPlanView = false
                            } else {
                                showPlanView = false
                            }
                            selectedTab = index
                            if (index != AroTab.FOOD.ordinal && index != AroTab.WORKOUTS.ordinal) {
                                returnToPlanAfterPlanAdd = false
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
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
                        MissingLibrary("The food library is still loading.")
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
                        )
                    } else {
                        MissingLibrary("The exercise library is still loading.")
                    }
                }

                AroTab.GOALS -> GoalsBodyScreen(
                    profile = state.profile,
                    energyPlan = state.energyPlan,
                    onOpenBodyPlanner = { onItemClick(BodyPlanner) },
                    onOpenVitals = { onItemClick(ClinicalRecords(0)) },
                )
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
    onItemClick: (NavKey) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var addFoodSlot by remember { mutableStateOf<MealSlot?>(null) }
    var editingFood by remember { mutableStateOf<FoodLogEntry?>(null) }
    var showFastingProtocolDialog by remember { mutableStateOf(false) }
    var showCustomHydrationDialog by remember { mutableStateOf(false) }
    var showLogWorkoutDialog by remember { mutableStateOf(false) }
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
                AroMonthCalendar(
                    month = calendarMonth,
                    selectedDate = state.selectedDate,
                    today = state.today,
                    plannedDates = planSuccess?.plannedDates ?: emptyMap(),
                    onMonthChange = onPlanMonthChange,
                    onDateClick = onCalendarDate,
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
                onDismiss = { showLogWorkoutDialog = false },
                onWorkoutLogged = { workout ->
                    onLogWorkout(workout)
                    showLogWorkoutDialog = false
                },
            )
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

@Composable
private fun DiarySections(
    state: MainScreenUiState.Success,
    planSuccess: PlanUiState.Success?,
    planningMode: Boolean,
    onToday: () -> Unit,
    onAddWater: (Int) -> Unit,
    onResetWater: () -> Unit,
    onOpenCustomWater: () -> Unit,
    onOpenStartFast: () -> Unit,
    onEndFast: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAddFood: (MealSlot) -> Unit,
    onEditFood: (FoodLogEntry) -> Unit,
    onDeleteFood: (String) -> Unit,
    onOpenLogWorkout: () -> Unit,
    onDeleteWorkout: (String) -> Unit,
    onItemClick: (NavKey) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DIARY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = state.formattedDate,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (!state.isToday) {
                TextButton(
                    onClick = onToday,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("Today")
                }
            }
        }

        MacroRingsCard(
            energyBalance = state.energyBalance,
            energyPlan = state.energyPlan,
            dayTotals = state.dayTotals,
            onAuditClick = { onItemClick(ClinicalAudit) },
        )

        EnergyBalanceCard(balance = state.energyBalance)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                HydrationWidget(
                    waterMl = state.diaryDay.waterMl,
                    targetWaterMl = state.targetWaterMl,
                    planningMode = planningMode,
                    plannedWaterMl = planSuccess
                        ?.takeIf { it.selectedDate == state.selectedDate }
                        ?.summary?.plannedWaterMl ?: 0,
                    onAddWater = onAddWater,
                    onResetWater = onResetWater,
                    onOpenCustomWater = onOpenCustomWater,
                    onOpenReminders = onOpenNotifications,
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                FastingWidget(
                    activeFast = if (planningMode) null else state.activeFast,
                    planningMode = planningMode,
                    plannedFastingHours = planSuccess
                        ?.takeIf { it.selectedDate == state.selectedDate }
                        ?.summary?.plannedFastingHours ?: 0,
                    onOpenStartFast = onOpenStartFast,
                    onEndFast = onEndFast,
                    onOpenReminders = onOpenNotifications,
                )
            }
        }

        Text(
            text = "MEALS & NUTRITION",
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        )

        MealSlot.ORDERED.forEach { slot ->
            val entries = state.diaryDay.entriesFor(slot)
            val mealTotals = state.diaryDay.totalsFor(slot)
            val mealCoverage = state.coverage.meals.find { it.meal == slot }
            MealSlotCard(
                slot = slot,
                entries = entries,
                mealTotals = mealTotals,
                mealCoverage = mealCoverage,
                planningMode = planningMode,
                onOpenAddFood = { onOpenAddFood(slot) },
                onEditFood = onEditFood,
                onDeleteFood = onDeleteFood,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "WORKOUTS & EXPENDITURE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val totalBurn = state.diaryDay.caloriesBurned(state.profile.weightKg)
                if (totalBurn > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = AroTheme.macroColors.cardio.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "+$totalBurn kcal",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = AroTheme.macroColors.cardio,
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            TextButton(
                onClick = onOpenLogWorkout,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (planningMode) "Plan Workout" else "Log Workout",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }

        if (state.diaryDay.workouts.isNotEmpty()) {
            WorkoutsCard(
                workouts = state.diaryDay.workouts,
                onDeleteWorkout = onDeleteWorkout,
            )
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenLogWorkout),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = AroTheme.macroColors.cardio,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (planningMode) "Nothing planned for this day" else "No workouts logged today",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (planningMode) {
                                "Add a future workout plan; it will appear in the Plan view"
                            } else {
                                "Track resistance (1RM & tonnage) or cardio (MET)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (planningMode) "Plan workout" else "Log workout",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}


@Composable
private fun MacroRingsCard(
    energyBalance: EnergyBalance,
    energyPlan: EnergyPlan,
    dayTotals: NutrientTotals,
    onAuditClick: () -> Unit = {},
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ConcentricMacroRings(
                caloriesEaten = energyBalance.caloriesEaten,
                caloriesBurned = energyBalance.caloriesBurned,
                targetCalories = energyPlan.targetCalories,
                proteinEatenG = dayTotals.proteinG,
                targetProteinG = energyPlan.macros.proteinG.toDouble(),
                carbsEatenG = dayTotals.carbsG,
                targetCarbsG = energyPlan.macros.carbsG.toDouble(),
                fatEatenG = dayTotals.fatG,
                targetFatG = energyPlan.macros.fatG.toDouble(),
                size = 220.dp,
                strokeWidth = 11.dp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Macro Badges Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MacroBadge(
                    label = "Protein",
                    amount = "${dayTotals.proteinG.roundToInt()}g",
                    target = "${energyPlan.macros.proteinG}g",
                    color = AroTheme.macroColors.protein,
                )
                MacroBadge(
                    label = "Carbs",
                    amount = "${dayTotals.carbsG.roundToInt()}g",
                    target = "${energyPlan.macros.carbsG}g",
                    color = AroTheme.macroColors.carbs,
                )
                MacroBadge(
                    label = "Fat",
                    amount = "${dayTotals.fatG.roundToInt()}g",
                    target = "${energyPlan.macros.fatG}g",
                    color = AroTheme.macroColors.fat,
                )
                MacroBadge(
                    label = "Fiber",
                    amount = "${dayTotals.fiberG.roundToInt()}g",
                    target = "${energyPlan.macros.fiberG}g",
                    color = AroTheme.macroColors.fiber,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAuditClick),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = AroTheme.macroColors.fiber,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Clinical Electrolyte & Na:K Audit",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EnergyBalanceCard(balance: EnergyBalance) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EquationItem(
                label = "Goal",
                value = "${balance.targetCalories}",
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "−",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EquationItem(
                label = "Eaten",
                value = "${balance.caloriesEaten}",
                color = AroTheme.macroColors.calories,
            )
            Text(
                text = "+",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EquationItem(
                label = "Burned",
                value = "${balance.caloriesBurned}",
                color = AroTheme.macroColors.cardio,
            )
            Text(
                text = "=",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EquationItem(
                label = if (balance.isOverTarget) "Surplus" else "Remaining",
                value = if (balance.isOverTarget) "+${balance.surplus}" else "${balance.remaining}",
                color = if (balance.isOverTarget) AroTheme.macroColors.calories else AroTheme.macroColors.calories,
                isHighlight = true,
            )
        }
    }
}

@Composable
private fun EquationItem(
    label: String,
    value: String,
    color: Color,
    isHighlight: Boolean = false,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = if (isHighlight) {
                MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            },
            color = color,
        )
    }
}

@Composable
private fun HydrationWidget(
    waterMl: Int,
    targetWaterMl: Int,
    planningMode: Boolean = false,
    plannedWaterMl: Int = 0,
    onAddWater: (Int) -> Unit,
    onResetWater: () -> Unit,
    onOpenCustomWater: () -> Unit,
    onOpenReminders: () -> Unit = {},
) {
    val shownWaterMl = if (planningMode) plannedWaterMl else waterMl

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = AroTheme.macroColors.water,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (planningMode) "Hydration plan" else "Hydration",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onOpenReminders,
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Hydration Reminders",
                            tint = AroTheme.macroColors.water.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val shownProgress = if (targetWaterMl <= 0) 0f else {
                        (shownWaterMl.toFloat() / targetWaterMl).coerceIn(0f, 1f)
                    }
                    val percent = (shownProgress * 100).roundToInt()
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AroTheme.macroColors.water,
                    )
                    if (!planningMode && waterMl > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onResetWater,
                            modifier = Modifier.size(22.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Water",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (planningMode) {
                    "$shownWaterMl / $targetWaterMl mL planned"
                } else {
                    "$waterMl / $targetWaterMl mL"
                },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (planningMode && shownWaterMl > 0) {
                Text(
                    text = "Not logged",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val shownProgress = if (targetWaterMl <= 0) 0f else {
                (shownWaterMl.toFloat() / targetWaterMl).coerceIn(0f, 1f)
            }
            LinearProgressIndicator(
                progress = { shownProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = AroTheme.macroColors.water,
                trackColor = AroTheme.macroColors.waterBg,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OutlinedButton(
                    onClick = { onAddWater(250) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp),
                ) {
                    Text("+250", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = { onAddWater(500) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp),
                ) {
                    Text("+500", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onOpenCustomWater,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(30.dp),
                ) {
                    Text("More", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun FastingWidget(
    activeFast: com.drnitz.aro.data.repository.ActiveFast?,
    planningMode: Boolean = false,
    plannedFastingHours: Int = 0,
    onOpenStartFast: () -> Unit,
    onEndFast: () -> Unit,
    onOpenReminders: () -> Unit = {},
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = AroTheme.macroColors.fasting,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Fasting",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onOpenReminders,
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Fasting Alerts",
                            tint = AroTheme.macroColors.fasting.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }

                if (activeFast != null) {
                    val percent = (activeFast.progress * 100).roundToInt()
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AroTheme.macroColors.fasting,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activeFast != null) {
                val elapsedHrs = activeFast.elapsedMinutes / 60
                val elapsedMins = activeFast.elapsedMinutes % 60
                val stage = FastingStage.forElapsedHours(activeFast.elapsedMinutes / 60.0)

                Text(
                    text = "${elapsedHrs}h ${elapsedMins}m / ${activeFast.targetHours}h",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stage.title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = AroTheme.macroColors.fasting,
                )

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { activeFast.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = AroTheme.macroColors.fasting,
                    trackColor = AroTheme.macroColors.fasting.copy(alpha = 0.15f),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onEndFast,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("End Fast", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text(
                    text = if (planningMode && plannedFastingHours > 0) {
                        "${plannedFastingHours}h fast planned"
                    } else if (planningMode) {
                        "No fast planned"
                    } else {
                        "Ready to Fast"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (planningMode) {
                        "Set a start time and reminder for this future day"
                    } else {
                        "Select 16h, 18h, 20h or 24h"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onOpenStartFast,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AroTheme.macroColors.fasting,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (planningMode) "Plan Fast" else "Start Fast",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun StartFastProtocolDialog(
    planningMode: Boolean = false,
    onDismiss: () -> Unit,
    onStartFast: (Int) -> Unit,
) {
    val protocols = listOf(
        Triple(16, "16:8 Standard", "16h fast • 8h eating window (Optimal daily balance)"),
        Triple(18, "18:6 Accelerated", "18h fast • 6h eating window (Enhanced fat oxidation)"),
        Triple(20, "20:4 Warrior", "20h fast • 4h eating window (Advanced cellular autophagy)"),
        Triple(24, "24:0 OMAD", "24h fast • One meal a day (Peak metabolic renewal)"),
    )
    var selectedHours by remember { mutableStateOf(16) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (planningMode) "Plan Fasting Protocol" else "Select Fasting Protocol",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = if (planningMode) {
                        "Add a fast to this future day. No fast starts until you confirm it later."
                    } else {
                        "Circadian & Metabolic Intermittent Fasting"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                protocols.forEach { (hours, title, description) ->
                    val isSelected = selectedHours == hours
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                AroTheme.macroColors.fasting.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedHours = hours },
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) AroTheme.macroColors.fasting else MaterialTheme.colorScheme.onSurface,
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AroTheme.macroColors.fasting,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStartFast(selectedHours) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AroTheme.macroColors.fasting,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (planningMode) "Add Fast to Plan" else "Start Fast")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun CustomHydrationDialog(
    planningMode: Boolean = false,
    onDismiss: () -> Unit,
    onAddWater: (Int) -> Unit,
) {
    var mlText by remember { mutableStateOf("250") }
    val presets = listOf(100, 250, 500, 750, 1000)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (planningMode) "Plan Water" else "Log Water",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (planningMode) {
                    Text(
                        text = "Adds a water plan to this future day. It will not count as drunk water until you confirm it.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = mlText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 5) {
                            mlText = input
                        }
                    },
                    label = { Text("Volume (mL)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presets.forEach { preset ->
                        OutlinedButton(
                            onClick = { mlText = preset.toString() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                        ) {
                            Text("+$preset", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Text(
                    text = "Maintains plasma volume, renal clearance, and lipolysis during workouts and fasting.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = mlText.toIntOrNull()?.coerceAtLeast(1) ?: 250
                    onAddWater(amount)
                },
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (planningMode) "Add Water to Plan" else "Add Water")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun MealSlotCard(
    slot: MealSlot,
    entries: List<FoodLogEntry>,
    mealTotals: NutrientTotals,
    mealCoverage: MealCoverage?,
    planningMode: Boolean = false,
    onOpenAddFood: () -> Unit,
    onEditFood: (FoodLogEntry) -> Unit,
    onDeleteFood: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = slot.label,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${mealTotals.calories.roundToInt()} kcal",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (mealTotals.calories > 0) AroTheme.macroColors.calories else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onOpenAddFood,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = if (planningMode) {
                                "Plan food for ${slot.label}"
                            } else {
                                "Add food to ${slot.label}"
                            },
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Missing Macro Alert (if meal has food logged but lacks target macros)
            if (mealCoverage != null && !mealCoverage.isEmpty && mealCoverage.missingMacros.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = AroTheme.macroColors.clinicalModerate.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Missing macro alert",
                        tint = AroTheme.macroColors.clinicalModerate,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Missing: " + mealCoverage.missingMacros.joinToString(", ") { it.label },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = AroTheme.macroColors.clinicalModerate,
                    )
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    entries.forEach { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onEditFood(entry) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${entry.grams.roundToInt()}g • P: ${entry.nutrients.proteinG.roundToInt()}g  C: ${entry.nutrients.carbsG.roundToInt()}g  F: ${entry.nutrients.fatG.roundToInt()}g",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${entry.calories.roundToInt()} kcal",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onDeleteFood(entry.id) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete item",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenAddFood,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (planningMode) {
                            "Plan Food for ${slot.label}"
                        } else {
                            "Add Food to ${slot.label}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditPortionDialog(
    entry: FoodLogEntry,
    onDismiss: () -> Unit,
    onConfirm: (FoodLogEntry) -> Unit,
) {
    var gramsText by remember { mutableStateOf(entry.grams.roundToInt().toString()) }
    val currentGrams = gramsText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: entry.grams
    val updatedEntry = entry.withGrams(currentGrams)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Adjust Portion",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 5) {
                            gramsText = input
                        }
                    },
                    label = { Text("Portion (grams)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(-50, -10, 10, 50).forEach { delta ->
                        OutlinedButton(
                            onClick = {
                                val next = (currentGrams + delta).coerceAtLeast(1.0)
                                gramsText = next.roundToInt().toString()
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                        ) {
                            Text(
                                text = if (delta > 0) "+$delta" else "$delta",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        MacroMiniStat(
                            label = "Calories",
                            value = "${updatedEntry.calories.roundToInt()} kcal",
                            color = AroTheme.macroColors.calories,
                        )
                        MacroMiniStat(
                            label = "Protein",
                            value = "${updatedEntry.proteinG.roundToInt()}g",
                            color = AroTheme.macroColors.protein,
                        )
                        MacroMiniStat(
                            label = "Carbs",
                            value = "${updatedEntry.nutrients.carbsG.roundToInt()}g",
                            color = AroTheme.macroColors.carbs,
                        )
                        MacroMiniStat(
                            label = "Fat",
                            value = "${updatedEntry.nutrients.fatG.roundToInt()}g",
                            color = AroTheme.macroColors.fat,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(updatedEntry) },
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Save Portion")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun MacroMiniStat(
    label: String,
    value: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}

@Composable
private fun NutriScoreBadge(grade: String) {
    val (bg, label) = when (grade.lowercase()) {
        "a" -> Color(0xFF038141) to "NUTRI-SCORE A"
        "b" -> Color(0xFF85BB2F) to "NUTRI-SCORE B"
        "c" -> Color(0xFFFECB02) to "NUTRI-SCORE C"
        "d" -> Color(0xFFEE8100) to "NUTRI-SCORE D"
        "e" -> Color(0xFFE63E11) to "NUTRI-SCORE E"
        else -> Color.Gray to "NUTRI-SCORE ${grade.uppercase()}"
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun NovaBadge(group: Int) {
    val bg = when (group) {
        1 -> Color(0xFF038141)
        2 -> Color(0xFF85BB2F)
        3 -> Color(0xFFEE8100)
        4 -> Color(0xFFE63E11)
        else -> Color.Gray
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = "NOVA $group",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun FoodItemCard(
    item: CuratedFoodItem,
    slot: MealSlot,
    onSelect: () -> Unit,
    onQuickAdd: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val subtitle = item.brand?.takeIf { it != "Whole Food / Fresh" }?.let { "$it • " } ?: ""
                val servingSubtitle = item.commonServing?.let {
                    "${it.name} (${it.grams.roundToInt()}g)"
                } ?: "100g serving"
                Text(
                    text = "$subtitle$servingSubtitle • P: ${item.protein.roundToInt()}g  C: ${item.carbs.roundToInt()}g  F: ${item.fat.roundToInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.nutriscore != null || item.nova != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.nutriscore?.let { NutriScoreBadge(it) }
                        item.nova?.let { NovaBadge(it) }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${item.calories.roundToInt()} kcal",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AroTheme.macroColors.calories,
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onQuickAdd,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick add ${item.name}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddFoodDialog(
    slot: MealSlot,
    foodLibrary: OfflineFoodLibrary,
    openFoodFactsClient: OpenFoodFactsClient? = null,
    targetDateLabel: String = "",
    targetDateIsToday: Boolean = true,
    planningMode: Boolean = false,
    onRecordUsage: (CuratedFoodItem, Double) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
    onFoodLogged: (FoodLogEntry, CuratedFoodItem) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var isOnlineMode by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var categories by remember { mutableStateOf(listOf("All")) }
    var offlineResults by remember { mutableStateOf<List<CuratedFoodItem>>(emptyList()) }
    var onlineResults by remember { mutableStateOf<List<CuratedFoodItem>>(emptyList()) }
    var isOnlineLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var configuringFood by remember { mutableStateOf<CuratedFoodItem?>(null) }
    var portionGramsText by remember { mutableStateOf("100") }
    var showScanner by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val cats = foodLibrary.getCategories()
        categories = listOf("All") + cats
    }

    LaunchedEffect(query, selectedCategory) {
        val catFilter = if (selectedCategory == "All") null else selectedCategory
        offlineResults = foodLibrary.search(query = query, category = catFilter)
    }

    LaunchedEffect(query, isOnlineMode) {
        if (isOnlineMode) {
            val cleanQ = query.trim()
            if (cleanQ.length >= 2 && openFoodFactsClient != null) {
                isOnlineLoading = true
                searchError = null
                delay(400)
                try {
                    onlineResults = openFoodFactsClient.searchProducts(cleanQ)
                    if (onlineResults.isEmpty()) {
                        searchError = "No products found on Open Food Facts for \"$cleanQ\""
                    }
                } catch (e: Exception) {
                    searchError = "Failed to query Open Food Facts: ${e.message}"
                } finally {
                    isOnlineLoading = false
                }
            } else {
                onlineResults = emptyList()
                searchError = null
            }
        }
    }

    if (showScanner) {
        Dialog(
            onDismissRequest = { showScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            BarcodeScannerView(
                onBarcodeDetected = { barcode ->
                    coroutineScope.launch {
                        searchError = null
                        val offlineItem = foodLibrary.findByBarcode(barcode)
                        if (offlineItem != null) {
                            configuringFood = offlineItem
                            portionGramsText = (offlineItem.commonServing?.grams ?: 100.0).roundToInt().toString()
                            showScanner = false
                            return@launch
                        }
                        if (openFoodFactsClient != null) {
                            val onlineItem = openFoodFactsClient.getProductByBarcode(barcode)
                            if (onlineItem != null) {
                                configuringFood = onlineItem
                                portionGramsText = (onlineItem.commonServing?.grams ?: 100.0).roundToInt().toString()
                                showScanner = false
                                return@launch
                            }
                        }
                        searchError = "No product found for barcode: $barcode"
                        showScanner = false
                    }
                },
                onClose = { showScanner = false },
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val foodToConfig = configuringFood
                    if (foodToConfig != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { configuringFood = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to list")
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Set Portion",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = "Add to ${slot.label}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = if (isOnlineMode) "Open Food Facts Live Catalog" else "Curated Whole Foods & Staples (Offline)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                if (targetDateLabel.isNotBlank()) {
                    AroLoggingDateLabel(
                        formattedDate = targetDateLabel,
                        isToday = targetDateIsToday,
                        labelPrefix = if (planningMode) "Planning for" else "Logging for",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val selectedFood = configuringFood
                if (selectedFood != null) {
                    val currentGrams = portionGramsText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 100.0
                    val entryPreview = selectedFood.toFoodLogEntry(meal = slot, grams = currentGrams)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = selectedFood.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${selectedFood.brand ?: "Whole Food"} • ${selectedFood.category ?: "Food"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )

                                if (selectedFood.nutriscore != null || selectedFood.nova != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        selectedFood.nutriscore?.let { NutriScoreBadge(it) }
                                        selectedFood.nova?.let { NovaBadge(it) }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            selectedFood.commonServing?.let { common ->
                                OutlinedButton(
                                    onClick = { portionGramsText = common.grams.roundToInt().toString() },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(
                                        text = "${common.name} (${common.grams.roundToInt()}g)",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { portionGramsText = "100" },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(0.7f),
                            ) {
                                Text("100g", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { portionGramsText = "200" },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(0.7f),
                            ) {
                                Text("200g", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        OutlinedTextField(
                            value = portionGramsText,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() } && input.length <= 5) {
                                    portionGramsText = input
                                }
                            },
                            label = { Text("Grams (g)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "NUTRITION FOR ${currentGrams.roundToInt()}g",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                ) {
                                    MacroMiniStat(
                                        label = "Calories",
                                        value = "${entryPreview.calories.roundToInt()} kcal",
                                        color = AroTheme.macroColors.calories,
                                    )
                                    MacroMiniStat(
                                        label = "Protein",
                                        value = "${entryPreview.proteinG.roundToInt()}g",
                                        color = AroTheme.macroColors.protein,
                                    )
                                    MacroMiniStat(
                                        label = "Carbs",
                                        value = "${entryPreview.nutrients.carbsG.roundToInt()}g",
                                        color = AroTheme.macroColors.carbs,
                                    )
                                    MacroMiniStat(
                                        label = "Fat",
                                        value = "${entryPreview.nutrients.fatG.roundToInt()}g",
                                        color = AroTheme.macroColors.fat,
                                    )
                                }

                                if (entryPreview.nutrients.fiberG > 0 || entryPreview.nutrients.sodiumMg > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                    ) {
                                        MacroMiniStat(
                                            label = "Fiber",
                                            value = "${entryPreview.nutrients.fiberG.roundToInt()}g",
                                            color = AroTheme.macroColors.fiber,
                                        )
                                        MacroMiniStat(
                                            label = "Sodium",
                                            value = "${entryPreview.nutrients.sodiumMg.roundToInt()}mg",
                                            color = AroTheme.macroColors.clinicalModerate,
                                        )
                                        MacroMiniStat(
                                            label = "Potassium",
                                            value = "${entryPreview.nutrients.potassiumMg.roundToInt()}mg",
                                            color = AroTheme.macroColors.clinicalOptimal,
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                if (!planningMode) onRecordUsage(selectedFood, currentGrams)
                                onFoodLogged(entryPreview, selectedFood)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (planningMode) {
                                    "Plan to ${slot.label}"
                                } else {
                                    "Log to ${slot.label}"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                if (isOnlineMode) "Search Open Food Facts (e.g. Cheerios)..."
                                else "Search 40+ curated foods..."
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { query = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                                IconButton(onClick = { showScanner = true }) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = "Scan Barcode",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = !isOnlineMode,
                            onClick = {
                                isOnlineMode = false
                                searchError = null
                            },
                            label = { Text("Curated Whole Foods", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                        )
                        FilterChip(
                            selected = isOnlineMode,
                            onClick = {
                                isOnlineMode = true
                                searchError = null
                            },
                            label = { Text("Open Food Facts (Live)", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!isOnlineMode) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(categories.size) { index ->
                                val cat = categories[index]
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (offlineResults.isEmpty() && query.isNotBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = "No curated foods matching \"$query\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { isOnlineMode = true },
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Search Open Food Facts Online")
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(offlineResults.size) { index ->
                                    val item = offlineResults[index]
                                    FoodItemCard(
                                        item = item,
                                        slot = slot,
                                        onSelect = {
                                            portionGramsText = (item.commonServing?.grams ?: 100.0).roundToInt().toString()
                                            configuringFood = item
                                        },
                                        onQuickAdd = {
                                            val grams = item.commonServing?.grams ?: item.servingSize
                                            onRecordUsage(item, grams)
                                            onFoodLogged(item.toFoodLogEntry(meal = slot, grams = grams), item)
                                        },
                                    )
                                }
                            }
                        }
                    } else {
                        if (isOnlineLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Searching Open Food Facts...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else if (searchError != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = searchError!!,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else if (query.trim().length < 2) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Type at least 2 characters or scan a barcode to search Open Food Facts live database.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(onlineResults.size) { index ->
                                    val item = onlineResults[index]
                                    FoodItemCard(
                                        item = item,
                                        slot = slot,
                                        onSelect = {
                                            portionGramsText = (item.commonServing?.grams ?: 100.0).roundToInt().toString()
                                            configuringFood = item
                                        },
                                        onQuickAdd = {
                                            val grams = item.commonServing?.grams ?: item.servingSize
                                            onRecordUsage(item, grams)
                                            onFoodLogged(item.toFoodLogEntry(meal = slot, grams = grams), item)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutsCard(
    workouts: List<WorkoutLog>,
    onDeleteWorkout: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            workouts.forEach { workout ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            shape = RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (workout.isStrength) Icons.Default.FitnessCenter else Icons.Default.Timer,
                            contentDescription = null,
                            tint = AroTheme.macroColors.cardio,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = workout.exerciseName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val detail = if (workout.isStrength) {
                                val best1Rm = OneRepMaxCalculator.bestOf(workout.sets)?.oneRepMaxKg?.roundToInt()
                                val best1RmText = if (best1Rm != null) " • Est. 1RM: ${best1Rm}kg" else ""
                                "${workout.sets.size} sets • ${workout.totalVolumeKg.roundToInt()}kg tonnage$best1RmText"
                            } else {
                                buildList {
                                    add("${workout.durationMinutes} min")
                                    workout.distanceKm?.let { add("${formatWorkoutDecimal(it)} km") }
                                    workout.averageSpeedKph?.let { add("${formatWorkoutDecimal(it)} km/h") }
                                    workout.steps?.let { add("${String.format(Locale.US, "%,d", it)} steps") }
                                    add("MET ${formatWorkoutDecimal(workout.met)}")
                                }.joinToString(" • ")
                            }
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "+${workout.caloriesBurned} kcal",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = AroTheme.macroColors.cardio,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { onDeleteWorkout(workout.id) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete workout",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun supportsStepTracking(activity: MetActivity): Boolean =
    activity.category == "Walking" ||
        activity.category == "Running" ||
        activity.id == "hiking-crosscountry"

/**
 * Estimates outdoor distance from step count using the common height-based stride
 * approximation: about 0.414 x height for walking and 0.45 x height for running, with
 * height converted from centimetres to metres.
 */
private fun estimatedDistanceKm(
    activity: MetActivity,
    heightCm: Double,
    steps: Int,
): Double {
    val strideMeters = heightCm.coerceIn(100.0, 250.0) *
        if (activity.category == "Running") 0.0045 else 0.00414
    return steps.coerceAtLeast(0) * strideMeters / 1000.0
}

private fun formatElapsedTime(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return String.format(Locale.US, "%d:%02d", safe / 60, safe % 60)
}

private fun formatWorkoutDecimal(value: Double, decimals: Int = 1): String {
    if (!value.isFinite()) return "—"
    val text = String.format(Locale.US, "%.${decimals}f", value)
    return text.trimEnd('0').trimEnd('.').ifBlank { "0" }
}

private fun sanitizeDecimalInput(raw: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    val dot = filtered.indexOf('.')
    val singleDot = if (dot < 0) {
        filtered
    } else {
        filtered.substring(0, dot + 1) + filtered.substring(dot + 1).replace(".", "")
    }
    return singleDot.take(7)
}

@Composable
internal fun LogWorkoutDialog(
    exerciseLibrary: OfflineExerciseLibrary,
    userWeightKg: Double,
    userHeightCm: Double = 170.0,
    preselectedExercise: Exercise? = null,
    preselectedActivity: MetActivity? = null,
    initialWorkoutType: WorkoutType = WorkoutType.STRENGTH,
    initialDurationMinutes: Int? = null,
    targetDateLabel: String = "",
    targetDateIsToday: Boolean = true,
    cardioActionLabel: String = "Save Cardio Workout",
    onDismiss: () -> Unit,
    onWorkoutLogged: (WorkoutLog) -> Unit,
) {
    var workoutType by remember {
        mutableStateOf(
            if (preselectedActivity != null) WorkoutType.CARDIO else initialWorkoutType,
        )
    }

    // Resistance State
    var exerciseQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf("All") }
    var exerciseResults by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var selectedExercise by remember { mutableStateOf(preselectedExercise) }
    val sets = remember { mutableStateListOf(SetEntry(weightKg = 60.0, reps = 10)) }

    // Cardio State
    var cardioQuery by remember { mutableStateOf("") }
    var selectedCardioCategory by remember { mutableStateOf("All") }
    var selectedActivity by remember { mutableStateOf(preselectedActivity) }
    var durationMinutesText by remember {
        mutableStateOf((initialDurationMinutes ?: 30).toString())
    }
    var distanceText by remember { mutableStateOf("") }
    var speedText by remember { mutableStateOf("") }

    // Phone step tracking. The sensor reports a cumulative count since boot, so the
    // first reading becomes the baseline and only steps taken after Start are counted.
    val context = LocalContext.current
    val sensorManager = remember(context) {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }
    val stepCounterSensor = remember(sensorManager) {
        sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }
    var isTracking by remember { mutableStateOf(false) }
    var trackingStartedAtElapsed by remember { mutableStateOf(0L) }
    var trackingElapsedSeconds by remember { mutableIntStateOf(0) }
    var trackedSteps by remember { mutableIntStateOf(0) }
    var stepCounterBaseline by remember { mutableStateOf<Float?>(null) }
    var trackingError by remember { mutableStateOf<String?>(null) }

    val activityPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACTIVITY_RECOGNITION,
        ) == PackageManager.PERMISSION_GRANTED

    val beginTracking: () -> Unit = {
        isTracking = true
        trackingStartedAtElapsed = SystemClock.elapsedRealtime()
        trackingElapsedSeconds = 0
        trackedSteps = 0
        stepCounterBaseline = null
        trackingError = null
        // Start from measured data, not from a previously typed estimate.
        distanceText = ""
        speedText = ""
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            beginTracking()
        } else {
            trackingError = "Activity recognition permission is needed for step tracking."
        }
    }

    fun startTracking() {
        when {
            stepCounterSensor == null -> {
                trackingError = "This phone does not report a step-counter sensor. Enter duration and speed manually."
            }
            !activityPermissionGranted -> {
                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            else -> beginTracking()
        }
    }

    fun stopTracking() {
        val elapsed = ((SystemClock.elapsedRealtime() - trackingStartedAtElapsed) / 1000L)
            .toInt()
            .coerceAtLeast(0)
        isTracking = false
        if (elapsed > 0) {
            trackingElapsedSeconds = elapsed
            val minutes = ceil(elapsed / 60.0).toInt().coerceAtLeast(1)
            durationMinutesText = minutes.toString()
            selectedActivity?.let { activity ->
                if (trackedSteps > 0) {
                    val km = estimatedDistanceKm(activity, userHeightCm, trackedSteps)
                    distanceText = formatWorkoutDecimal(km, decimals = 2)
                    // Use the exact elapsed time rather than the rounded-up whole
                    // minute, otherwise a short walk reports an artificially low speed.
                    speedText = formatWorkoutDecimal(km / (elapsed / 3600.0))
                }
            }
        }
    }

    val stepListener = remember(stepCounterSensor) {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
                val reading = event.values.firstOrNull() ?: return
                val baseline = stepCounterBaseline
                if (baseline == null) {
                    stepCounterBaseline = reading
                } else {
                    trackedSteps = (reading - baseline).toInt().coerceAtLeast(0)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
    }

    DisposableEffect(isTracking, stepCounterSensor, sensorManager) {
        if (isTracking && stepCounterSensor != null && sensorManager != null) {
            sensorManager.registerListener(
                stepListener,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_UI,
            )
        }
        onDispose {
            if (isTracking && stepCounterSensor != null && sensorManager != null) {
                sensorManager.unregisterListener(stepListener)
            }
        }
    }

    LaunchedEffect(isTracking) {
        while (isTracking) {
            trackingElapsedSeconds = (
                (SystemClock.elapsedRealtime() - trackingStartedAtElapsed) / 1000L
            ).toInt().coerceAtLeast(0)
            delay(500)
        }
    }

    LaunchedEffect(isTracking, trackingElapsedSeconds) {
        if (isTracking) {
            durationMinutesText = ceil(trackingElapsedSeconds / 60.0)
                .toInt()
                .coerceAtLeast(1)
                .toString()
        }
    }

    LaunchedEffect(isTracking, trackedSteps, trackingElapsedSeconds, selectedActivity?.id) {
        val activity = selectedActivity ?: return@LaunchedEffect
        if (isTracking && trackedSteps > 0) {
            val km = estimatedDistanceKm(activity, userHeightCm, trackedSteps)
            val hours = trackingElapsedSeconds.coerceAtLeast(1) / 3600.0
            distanceText = formatWorkoutDecimal(km, decimals = 2)
            speedText = formatWorkoutDecimal(km / hours)
        }
    }

    LaunchedEffect(selectedActivity?.id) {
        isTracking = false
        trackingElapsedSeconds = 0
        trackedSteps = 0
        stepCounterBaseline = null
        trackingError = null
        durationMinutesText = (initialDurationMinutes ?: 30).toString()
        distanceText = ""
        speedText = ""
    }

    val muscleGroups = remember {
        listOf("All", "Chest", "Back", "Shoulders", "Biceps", "Triceps", "Quadriceps", "Hamstrings", "Glutes", "Abdominals", "Calves")
    }

    val cardioCategories = remember {
        listOf("All", "Running", "Walking", "Cycling", "Swimming", "Gym & Strength", "Conditioning", "Sports", "Mind & Body", "Outdoors")
    }

    LaunchedEffect(exerciseQuery, selectedMuscle) {
        exerciseResults = exerciseLibrary.search(
            query = exerciseQuery,
            muscle = if (selectedMuscle == "All") null else selectedMuscle,
        )
    }

    val filteredCardioActivities = remember(cardioQuery, selectedCardioCategory) {
        val q = cardioQuery.trim().lowercase()
        val cat = if (selectedCardioCategory == "All") null else selectedCardioCategory.lowercase()
        MetCatalog.all.filter { act ->
            val matchQ = q.isEmpty() || act.name.lowercase().contains(q) || act.category.lowercase().contains(q)
            val matchCat = cat == null || act.category.lowercase() == cat
            matchQ && matchCat
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 28.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selectedExercise != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedExercise = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to exercises")
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Log Sets",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    } else if (selectedActivity != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedActivity = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to cardio")
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (cardioActionLabel.startsWith("Plan")) "Plan Cardio" else "Log Cardio",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = "Log Workout",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            )
                            Text(
                                text = "Resistance (1RM & Tonnage) or Cardio (MET)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (targetDateLabel.isNotBlank()) {
                    AroLoggingDateLabel(
                        formattedDate = targetDateLabel,
                        isToday = targetDateIsToday,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Mode Tabs (only when selecting an exercise/activity)
                if (selectedExercise == null && selectedActivity == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = workoutType == WorkoutType.STRENGTH,
                            onClick = { workoutType = WorkoutType.STRENGTH },
                            label = { Text("Resistance Training", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            shape = RoundedCornerShape(8.dp),
                        )
                        FilterChip(
                            selected = workoutType == WorkoutType.CARDIO,
                            onClick = { workoutType = WorkoutType.CARDIO },
                            label = { Text("Cardio & Sports", style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // BODY: STRENGTH vs CARDIO
                if (workoutType == WorkoutType.STRENGTH) {
                    val ex = selectedExercise
                    if (ex != null) {
                        // Exercise Selected: Working Sets Logger
                        val sessionTonnage = sets.filter { it.isValid }.sumOf { it.volumeKg }
                        val best1Rm = OneRepMaxCalculator.bestOf(sets.toList())
                        val estBurn = (sessionTonnage * KCAL_PER_KG_TONNAGE).roundToInt()

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            val postureUrls = ex.imageUrls(limit = 2)
                            if (postureUrls.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    postureUrls.forEachIndexed { index, url ->
                                        ExerciseImage(
                                            imageUrl = url,
                                            contentDescription = "${ex.name} posture ${index + 1}",
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(96.dp),
                                            cornerRadius = 12.dp,
                                        )
                                    }
                                }
                            }

                            // Exercise summary banner
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = ex.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${ex.targetMuscleSummary} • ${ex.equipment ?: "Bodyweight"} • ${ex.level.replaceFirstChar { it.uppercase() }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }

                            // Clinical Metrics Strip
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Volume Tonnage",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = "${sessionTonnage.roundToInt()} kg",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Est. 1RM",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = if (best1Rm != null) "${best1Rm.oneRepMaxKg.roundToInt()} kg" else "—",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = AroTheme.macroColors.calories,
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "Est. Burn",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            Text(
                                                text = "+$estBurn kcal",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = AroTheme.macroColors.cardio,
                                            )
                                        }
                                    }

                                    if (best1Rm != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Formula: ${best1Rm.method.label}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.align(Alignment.CenterHorizontally),
                                        )
                                        if (!best1Rm.isReliable && best1Rm.caveat != null) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "⚠️ ${best1Rm.caveat}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = AroTheme.macroColors.clinicalModerate,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                }
                            }

                            // Sets Table Header
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "WORKING SETS (${sets.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedButton(
                                    onClick = {
                                        val lastSet = sets.lastOrNull()
                                        sets.add(SetEntry(weightKg = lastSet?.weightKg ?: 60.0, reps = lastSet?.reps ?: 10))
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add Set", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            // Sets List
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(sets.size) { index ->
                                    val currentSet = sets[index]
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            Text(
                                                text = "#${index + 1}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.width(28.dp),
                                            )

                                            OutlinedTextField(
                                                value = if (currentSet.weightKg == 0.0) "" else currentSet.weightKg.roundToInt().toString(),
                                                onValueChange = { str ->
                                                    val clean = str.filter { it.isDigit() }
                                                    val w = clean.toDoubleOrNull() ?: 0.0
                                                    sets[index] = currentSet.copy(weightKg = w)
                                                },
                                                label = { Text("kg") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                            )

                                            OutlinedTextField(
                                                value = if (currentSet.reps == 0) "" else currentSet.reps.toString(),
                                                onValueChange = { str ->
                                                    val clean = str.filter { it.isDigit() }
                                                    val r = clean.toIntOrNull() ?: 0
                                                    sets[index] = currentSet.copy(reps = r)
                                                },
                                                label = { Text("reps") },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp),
                                            )

                                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(64.dp)) {
                                                Text(
                                                    text = "${currentSet.volumeKg.roundToInt()} kg",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                                val set1Rm = OneRepMaxCalculator.estimate(currentSet.weightKg, currentSet.reps)
                                                Text(
                                                    text = if (set1Rm != null) "1RM: ${set1Rm.oneRepMaxKg.roundToInt()}kg" else "—",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }

                                            if (sets.size > 1) {
                                                IconButton(
                                                    onClick = { sets.removeAt(index) },
                                                    modifier = Modifier.size(28.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove set",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Save Button
                            Button(
                                onClick = {
                                    val validSets = sets.filter { it.isValid }
                                    if (validSets.isNotEmpty()) {
                                        val workout = WorkoutLog(
                                            id = "strength-${java.util.UUID.randomUUID()}",
                                            type = WorkoutType.STRENGTH,
                                            exerciseName = ex.name,
                                            loggedAt = Instant.now(),
                                            met = 6.0,
                                            durationMinutes = (validSets.size * 3).coerceAtLeast(15),
                                            bodyWeightKg = userWeightKg,
                                            sets = validSets,
                                            category = ex.category,
                                            muscle = ex.primaryMuscles.firstOrNull(),
                                            equipment = ex.equipment,
                                        )
                                        onWorkoutLogged(workout)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Save Resistance Workout",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    } else {
                        // Exercise Search & Selection
                        OutlinedTextField(
                            value = exerciseQuery,
                            onValueChange = { exerciseQuery = it },
                            placeholder = { Text("Search 870+ exercises (e.g. Bench, Squat, Row)...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (exerciseQuery.isNotEmpty()) {
                                    IconButton(onClick = { exerciseQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(muscleGroups.size) { index ->
                                val muscle = muscleGroups[index]
                                FilterChip(
                                    selected = selectedMuscle == muscle,
                                    onClick = { selectedMuscle = muscle },
                                    label = { Text(muscle, style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(exerciseResults.size) { index ->
                                val exercise = exerciseResults[index]
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedExercise = exercise
                                            sets.clear()
                                            sets.add(SetEntry(weightKg = 60.0, reps = 10))
                                        },
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        ExerciseImage(
                                            imageUrl = exercise.imageUrlAt(0),
                                            contentDescription = exercise.name,
                                            modifier = Modifier.size(44.dp),
                                            cornerRadius = 10.dp,
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = exercise.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = "${exercise.targetMuscleSummary} • ${exercise.equipment ?: "Bodyweight"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = "Select exercise",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // CARDIO LOGGING
                    val act = selectedActivity
                    if (act != null) {
                        val enteredDuration = durationMinutesText.toIntOrNull()?.coerceAtLeast(1) ?: 30
                        val duration = if (isTracking) {
                            ceil(trackingElapsedSeconds / 60.0).toInt().coerceAtLeast(1)
                        } else {
                            enteredDuration
                        }
                        val burn = caloriesBurned(act.met, userWeightKg, duration)
                        val planningCardio = cardioActionLabel.startsWith("Plan")
                        val stepTracking = supportsStepTracking(act)
                        val trackedDistanceKm = if (trackedSteps > 0) {
                            estimatedDistanceKm(act, userHeightCm, trackedSteps)
                        } else {
                            null
                        }
                        val parsedDistance = distanceText.toDoubleOrNull()?.takeIf { it > 0.0 }
                        val parsedSpeed = speedText.toDoubleOrNull()?.takeIf { it > 0.0 }
                        val durationHours = duration / 60.0
                        val resolvedDistance = parsedDistance ?: parsedSpeed?.times(durationHours)
                        val resolvedSpeed = parsedSpeed ?: parsedDistance?.div(durationHours)

                        fun applyDuration(minutes: Int) {
                            durationMinutesText = minutes.toString()
                            val speed = speedText.toDoubleOrNull()
                            if (speed != null && speed > 0.0) {
                                distanceText = formatWorkoutDecimal(
                                    speed * (minutes / 60.0),
                                    decimals = 2,
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = act.name,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${act.category} • MET ${act.met} (Compendium of Physical Activities)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }

                                // Quick duration buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    listOf(15, 30, 45, 60).forEach { mins ->
                                        OutlinedButton(
                                            onClick = { applyDuration(mins) },
                                            enabled = !isTracking,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("${mins}m", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }

                                Slider(
                                    value = duration.coerceIn(5, 180).toFloat(),
                                    onValueChange = { value ->
                                        if (!isTracking) applyDuration(value.roundToInt())
                                    },
                                    valueRange = 5f..180f,
                                    steps = 34,
                                    enabled = !isTracking,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                OutlinedTextField(
                                    value = durationMinutesText,
                                    onValueChange = { str ->
                                        if (str.all { it.isDigit() } && str.length <= 4 && !isTracking) {
                                            durationMinutesText = str
                                            val mins = str.toIntOrNull()
                                            val speed = speedText.toDoubleOrNull()
                                            if (mins != null && mins > 0 && speed != null && speed > 0.0) {
                                                distanceText = formatWorkoutDecimal(
                                                    speed * (mins / 60.0),
                                                    decimals = 2,
                                                )
                                            }
                                        }
                                    },
                                    label = { Text("Duration (minutes)") },
                                    singleLine = true,
                                    enabled = !isTracking,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                )

                                if (planningCardio) {
                                    Text(
                                        text = "Distance, average speed and phone step tracking can be added when you complete this planned session.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else {
                                    if (stepTracking) {
                                        Card(
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isTracking) {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainer
                                                },
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (act.category == "Walking") {
                                                            Icons.AutoMirrored.Filled.DirectionsWalk
                                                        } else {
                                                            Icons.AutoMirrored.Filled.DirectionsRun
                                                        },
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(22.dp),
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = "PHONE STEP TRACKING",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.SemiBold,
                                                                letterSpacing = 0.8.sp,
                                                            ),
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                        )
                                                        Text(
                                                            text = "Uses this phone's step-counter sensor",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        )
                                                    }
                                                }

                                                if (isTracking) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceAround,
                                                    ) {
                                                        TrackingMetric(
                                                            label = "Elapsed",
                                                            value = formatElapsedTime(trackingElapsedSeconds),
                                                        )
                                                        TrackingMetric(
                                                            label = "Steps",
                                                            value = String.format(Locale.US, "%,d", trackedSteps),
                                                        )
                                                        TrackingMetric(
                                                            label = "Distance",
                                                            value = trackedDistanceKm?.let {
                                                                "${formatWorkoutDecimal(it, decimals = 2)} km"
                                                            } ?: "—",
                                                        )
                                                    }
                                                    Text(
                                                        text = "Keep the phone with you. Stopping fills duration, distance and average speed.",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                } else {
                                                    Text(
                                                        text = "Start tracking to measure steps, distance and average speed automatically.",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }

                                                trackingError?.let { error ->
                                                    Text(
                                                        text = error,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.error,
                                                    )
                                                }

                                                OutlinedButton(
                                                    onClick = { if (isTracking) stopTracking() else startTracking() },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp),
                                                ) {
                                                    Icon(
                                                        imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp),
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isTracking) {
                                                            "Stop & use tracked time"
                                                        } else {
                                                            "Start phone tracking"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                    )
                                                }
                                            }
                                        }
                                    } else if (act.category == "Cycling") {
                                        Text(
                                            text = "Step sensors cannot measure cycling. Enter your duration, distance or average speed below, or use a bike computer/GPS.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        OutlinedTextField(
                                            value = distanceText,
                                            onValueChange = { raw ->
                                                if (!isTracking) {
                                                    val clean = sanitizeDecimalInput(raw)
                                                    distanceText = clean
                                                    val km = clean.toDoubleOrNull()
                                                    val mins = durationMinutesText.toIntOrNull()
                                                    if (km != null && km > 0.0 && mins != null && mins > 0) {
                                                        speedText = formatWorkoutDecimal(km / (mins / 60.0))
                                                    }
                                                }
                                            },
                                            label = { Text("Distance (km)") },
                                            placeholder = { Text("Optional") },
                                            singleLine = true,
                                            enabled = !isTracking,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                        OutlinedTextField(
                                            value = speedText,
                                            onValueChange = { raw ->
                                                if (!isTracking) {
                                                    val clean = sanitizeDecimalInput(raw)
                                                    speedText = clean
                                                    val speed = clean.toDoubleOrNull()
                                                    val mins = durationMinutesText.toIntOrNull()
                                                    if (speed != null && speed > 0.0 && mins != null && mins > 0) {
                                                        distanceText = formatWorkoutDecimal(
                                                            speed * (mins / 60.0),
                                                            decimals = 2,
                                                        )
                                                    }
                                                }
                                            },
                                            label = { Text("Avg speed (km/h)") },
                                            placeholder = { Text("Optional") },
                                            singleLine = true,
                                            enabled = !isTracking,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                    }
                                }

                                // Clinical Calorie Burn Summary
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "CALORIC EXPENDITURE",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceAround,
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "Duration",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                                Text(
                                                    text = "$duration min",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "MET Intensity",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                                Text(
                                                    text = "${act.met}",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = AroTheme.macroColors.calories,
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "Calories Burned",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                                Text(
                                                    text = "+$burn kcal",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = AroTheme.macroColors.cardio,
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Calculated for body weight: ${userWeightKg.roundToInt()} kg",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.align(Alignment.CenterHorizontally),
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    val workout = WorkoutLog(
                                        id = "cardio-${java.util.UUID.randomUUID()}",
                                        type = WorkoutType.CARDIO,
                                        exerciseName = act.name,
                                        loggedAt = Instant.now(),
                                        met = act.met,
                                        durationMinutes = duration,
                                        distanceKm = resolvedDistance,
                                        averageSpeedKph = resolvedSpeed,
                                        steps = trackedSteps.takeIf { it > 0 },
                                        bodyWeightKg = userWeightKg,
                                        category = act.category,
                                    )
                                    onWorkoutLogged(workout)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = cardioActionLabel,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }
                    } else {
                        // Cardio Search & Selection
                        OutlinedTextField(
                            value = cardioQuery,
                            onValueChange = { cardioQuery = it },
                            placeholder = { Text("Search 26 MET activities (e.g. Running, Cycling)...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (cardioQuery.isNotEmpty()) {
                                    IconButton(onClick = { cardioQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(cardioCategories.size) { index ->
                                val cat = cardioCategories[index]
                                FilterChip(
                                    selected = selectedCardioCategory == cat,
                                    onClick = { selectedCardioCategory = cat },
                                    label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filteredCardioActivities.size) { index ->
                                val activity = filteredCardioActivities[index]
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedActivity = activity },
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = activity.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                text = activity.category,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Surface(
                                            color = AroTheme.macroColors.cardio.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp),
                                        ) {
                                            Text(
                                                text = "MET ${activity.met}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = AroTheme.macroColors.cardio,
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroBadge(
    label: String,
    amount: String,
    target: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "of $target",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
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
