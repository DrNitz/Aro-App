package com.drnitz.aro.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drnitz.aro.data.library.OfflineExerciseLibrary
import com.drnitz.aro.data.library.OfflineFoodLibrary
import com.drnitz.aro.data.library.imageUrlAt
import com.drnitz.aro.data.network.OpenFoodFactsClient
import com.drnitz.aro.domain.calculation.MetActivity
import com.drnitz.aro.domain.model.Exercise
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.PlanDaySummary
import com.drnitz.aro.domain.model.PlanItem
import com.drnitz.aro.domain.model.PlanItemType
import com.drnitz.aro.domain.model.PlanOccurrence
import com.drnitz.aro.domain.model.PlanRepeat
import com.drnitz.aro.domain.model.WorkoutLog
import com.drnitz.aro.domain.model.WorkoutType
import com.drnitz.aro.theme.AroTheme
import com.drnitz.aro.ui.components.AroLoggingDateChip
import com.drnitz.aro.ui.main.LogWorkoutDialog
import com.drnitz.aro.ui.main.MainScreenUiState
import com.drnitz.aro.ui.main.PlanUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
private val LONG_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Canonical plan surface: the selected day first, then an overview of upcoming and
 * repeating series. Add actions here are intentionally limited to future dates; today
 * and past days continue to use normal Diary logging.
 */
@Composable
fun PlanTabScreen(
    state: MainScreenUiState.Success,
    planState: PlanUiState,
    foodLibrary: OfflineFoodLibrary?,
    exerciseLibrary: OfflineExerciseLibrary?,
    openFoodFactsClient: OpenFoodFactsClient? = null,
    onBack: () -> Unit = {},
    onSelectDate: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onPlanFoodInFoodTab: () -> Unit = {},
    onPlanExerciseInWorkoutTab: () -> Unit = {},
    onSavePlanItem: (PlanItem) -> Unit,
    onDeletePlanItem: (String) -> Unit,
    onCompletePlanOccurrence: (String, LocalDate, String?) -> Unit,
    onSkipPlanOccurrence: (String, LocalDate) -> Unit,
    onUncompletePlanOccurrence: (String, LocalDate) -> Unit,
    onAddFood: (FoodLogEntry) -> Unit,
    onAddWater: (Int) -> Unit,
    onStartFast: (Int) -> Unit,
    onLogWorkout: (WorkoutLog) -> Unit,
    modifier: Modifier = Modifier,
) {
    val planSuccess = (planState as? PlanUiState.Success)
        ?.takeIf { it.selectedDate == state.selectedDate }
    val planning = state.selectedDate.isAfter(state.today)
    val items = planSuccess?.selectedItems.orEmpty()
    val summary = planSuccess?.summary ?: PlanDaySummary.EMPTY

    var pickerType by remember { mutableStateOf<PlanItemType?>(null) }
    var editingItem by remember { mutableStateOf<PlanItem?>(null) }
    var deleteTarget by remember { mutableStateOf<PlanItem?>(null) }
    var skipTarget by remember { mutableStateOf<PlanOccurrence?>(null) }
    var completingPlan by remember { mutableStateOf<PlanOccurrence?>(null) }
    var foodImagesById by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var exerciseImagesById by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Older plans predate the stored image_url column. Resolve those from the local
    // libraries so already-planned food and workouts still get their real photo.
    LaunchedEffect(foodLibrary) {
        foodImagesById = foodLibrary?.getCuratedFoods()
            ?.mapNotNull { food ->
                food.image?.takeIf { it.isNotBlank() }?.let { image -> food.id to image }
            }
            ?.toMap()
            .orEmpty()
    }
    LaunchedEffect(exerciseLibrary) {
        exerciseImagesById = exerciseLibrary?.getExercises()
            ?.mapNotNull { exercise ->
                exercise.imageUrlAt(0)?.let { image -> exercise.id to image }
            }
            ?.toMap()
            .orEmpty()
    }

    val imageFor: (PlanItem) -> String? = { item ->
        item.imageUrl ?: when (item.type) {
            PlanItemType.FOOD -> item.food?.foodId?.let(foodImagesById::get)
            PlanItemType.EXERCISE -> item.exercise?.exerciseId?.let(exerciseImagesById::get)
            else -> null
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Diary",
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Plan",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "Plan ahead. Nothing here counts as eaten, burned, drunk or fasted until you confirm it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            AroLoggingDateChip(
                selectedDate = state.selectedDate,
                formattedDate = state.formattedDate,
                isToday = state.isToday,
                onSelectDate = onSelectDate,
                onToday = onToday,
                labelPrefix = "Planning for",
            )
        }

        if (planSuccess == null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Loading plan...", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            item {
                PlanSummaryCard(
                    summary = summary,
                    targetCalories = state.energyPlan.targetCalories,
                )
            }

            if (planning) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "ADD TO THIS DAY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.1.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PlanItemType.entries, key = { it.name }) { type ->
                                OutlinedButton(
                                    onClick = {
                                        editingItem = null
                                        when (type) {
                                            PlanItemType.FOOD -> onPlanFoodInFoodTab()
                                            PlanItemType.EXERCISE -> onPlanExerciseInWorkoutTab()
                                            else -> pickerType = type
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    PlanTypeIcon(type, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(type.label, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Text(
                            text = "Food and Workouts open their full image libraries so you can plan with the same photos and details.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Planning is for future days. Today and past days use normal Diary logging; " +
                                "open the Food, Workouts or Diary tabs to log real food, workouts, water or a fast.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            item {
                SectionTitle(
                    title = if (state.selectedDate == state.today) {
                        "TODAY'S PLAN"
                    } else {
                        state.selectedDate.format(LONG_DATE).uppercase(Locale.getDefault()) + " PLAN"
                    },
                    trailing = if (items.isEmpty()) null else "${items.size} item${if (items.size == 1) "" else "s"}",
                )
            }

            if (items.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Nothing planned for this day",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                text = if (planning) {
                                    "Use the add buttons above, or add from the Food and Workouts tabs."
                                } else {
                                    "Log real entries in the Diary or the relevant tab."
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(items, key = { it.item.id + it.date.toEpochDay() }) { occurrence ->
                    PlanOccurrenceCard(
                        occurrence = occurrence,
                        userWeightKg = state.profile.weightKg,
                        imageUrl = imageFor(occurrence.item),
                        canComplete = !occurrence.date.isAfter(state.today),
                        onComplete = { completingPlan = occurrence },
                        onEdit = {
                            editingItem = occurrence.item
                            pickerType = occurrence.item.type
                        },
                        onDelete = { deleteTarget = occurrence.item },
                        canSkip = true,
                        onSkip = { skipTarget = occurrence },
                        onUndo = {
                            onUncompletePlanOccurrence(occurrence.item.id, occurrence.date)
                        },
                    )
                }
            }

            item {
                SectionTitle(
                    title = "UPCOMING & REPEATING",
                    trailing = planSuccess.upcomingSeries.size.takeIf { it > 0 }?.toString(),
                )
            }

            val upcoming = planSuccess.upcomingSeries
                .mapNotNull { item ->
                    item.nextOccurrenceOnOrAfter(state.today)?.let { next -> next to item }
                }
                .sortedWith(
                    compareBy(
                        { (next, _) -> next },
                        { (_, item) -> item.time },
                        { (_, item) -> item.title },
                    ),
                )
                .take(10)

            if (upcoming.isEmpty()) {
                item {
                    Text(
                        text = "No upcoming or repeating plans. Future quick-adds and repeating plans appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    )
                }
            } else {
                items(upcoming, key = { (next, item) -> "${item.id}-${next.toEpochDay()}" }) { pair ->
                    val (next, item) = pair
                    UpcomingPlanRow(
                        item = item,
                        nextDate = next,
                        imageUrl = imageFor(item),
                        onClick = { onSelectDate(next) },
                    )
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Completed or skipped occurrences stop their own reminder, but repeating plans " +
                            "keep their future occurrences. Logging different food or a different workout does " +
                            "not auto-complete a plan; use Done or Skip explicitly.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }

    if (pickerType != null) {
        val existing = editingItem
        when (pickerType) {
            PlanItemType.FOOD -> {
                if (foodLibrary != null) {
                    PlanFoodPickerDialog(
                        targetDate = state.selectedDate,
                        existing = existing,
                        foodLibrary = foodLibrary,
                        openFoodFactsClient = openFoodFactsClient,
                        onDismiss = { pickerType = null; editingItem = null },
                        onSave = {
                            onSavePlanItem(it)
                            pickerType = null
                            editingItem = null
                        },
                    )
                }
            }
            PlanItemType.EXERCISE -> {
                if (exerciseLibrary != null) {
                    PlanExercisePickerDialog(
                        targetDate = state.selectedDate,
                        existing = existing,
                        exerciseLibrary = exerciseLibrary,
                        userWeightKg = state.profile.weightKg,
                        onDismiss = { pickerType = null; editingItem = null },
                        onSave = {
                            onSavePlanItem(it)
                            pickerType = null
                            editingItem = null
                        },
                    )
                }
            }
            PlanItemType.PILL -> PlanPillPickerDialog(
                targetDate = state.selectedDate,
                existing = existing,
                onDismiss = { pickerType = null; editingItem = null },
                onSave = {
                    onSavePlanItem(it)
                    pickerType = null
                    editingItem = null
                },
            )
            PlanItemType.FASTING -> PlanFastingPickerDialog(
                targetDate = state.selectedDate,
                existing = existing,
                onDismiss = { pickerType = null; editingItem = null },
                onSave = {
                    onSavePlanItem(it)
                    pickerType = null
                    editingItem = null
                },
            )
            PlanItemType.WATER -> PlanWaterPickerDialog(
                targetDate = state.selectedDate,
                existing = existing,
                onDismiss = { pickerType = null; editingItem = null },
                onSave = {
                    onSavePlanItem(it)
                    pickerType = null
                    editingItem = null
                },
            )
            null -> Unit
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete planned ${target.type.label.lowercase()}?") },
            text = {
                Text(
                    if (target.repeat == PlanRepeat.NONE) {
                        "${target.title} will be removed from this day."
                    } else {
                        "${target.title} repeats ${target.repeat.label.lowercase()}. " +
                            "Deleting it removes the whole series and all its future reminders."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePlanItem(target.id)
                        deleteTarget = null
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }

    skipTarget?.let { occurrence ->
        AlertDialog(
            onDismissRequest = { skipTarget = null },
            title = { Text("Skip this occurrence?") },
            text = {
                Text(
                    "${occurrence.item.title} will stay on this day as skipped. " +
                        if (occurrence.item.repeat == PlanRepeat.NONE) {
                            "It will not remind you again."
                        } else {
                            "Future occurrences of the repeating series are not affected."
                        },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSkipPlanOccurrence(occurrence.item.id, occurrence.date)
                        skipTarget = null
                    },
                ) {
                    Text("Skip")
                }
            },
            dismissButton = {
                TextButton(onClick = { skipTarget = null }) { Text("Cancel") }
            },
        )
    }

    completingPlan?.let { occurrence ->
        when (occurrence.item.type) {
            PlanItemType.FOOD -> PlanFoodCompletionDialog(
                occurrence = occurrence,
                onDismiss = { completingPlan = null },
                onConfirm = { entry ->
                    onAddFood(entry)
                    onCompletePlanOccurrence(occurrence.item.id, occurrence.date, entry.id)
                    completingPlan = null
                },
            )

            PlanItemType.PILL -> PlanPillCompletionDialog(
                occurrence = occurrence,
                onDismiss = { completingPlan = null },
                onConfirm = {
                    onCompletePlanOccurrence(occurrence.item.id, occurrence.date, null)
                    completingPlan = null
                },
            )

            PlanItemType.WATER -> PlanWaterCompletionDialog(
                occurrence = occurrence,
                onDismiss = { completingPlan = null },
                onConfirm = { amountMl ->
                    onAddWater(amountMl)
                    onCompletePlanOccurrence(occurrence.item.id, occurrence.date, null)
                    completingPlan = null
                },
            )

            PlanItemType.FASTING -> PlanFastingCompletionDialog(
                occurrence = occurrence,
                onDismiss = { completingPlan = null },
                onConfirm = { hours ->
                    onStartFast(hours)
                    onCompletePlanOccurrence(occurrence.item.id, occurrence.date, null)
                    completingPlan = null
                },
            )

            PlanItemType.EXERCISE -> {
                if (exerciseLibrary != null) {
                    val exercisePlan = occurrence.item.exercise
                    val plannedExercise = if (exercisePlan?.isStrength == true) {
                        Exercise(
                            id = exercisePlan.exerciseId ?: "planned-exercise",
                            name = occurrence.item.title,
                            category = exercisePlan.category,
                            primaryMuscles = listOfNotNull(exercisePlan.muscle),
                            equipment = exercisePlan.equipment,
                        )
                    } else {
                        null
                    }
                    val plannedActivity = if (exercisePlan?.isStrength == false) {
                        MetActivity(
                            id = "planned-activity",
                            name = occurrence.item.title,
                            category = exercisePlan.category.ifBlank { "Planned activity" },
                            met = exercisePlan.met,
                        )
                    } else {
                        null
                    }

                    LogWorkoutDialog(
                        exerciseLibrary = exerciseLibrary,
                        userWeightKg = state.profile.weightKg,
                        userHeightCm = state.profile.heightCm,
                        preselectedExercise = plannedExercise,
                        preselectedActivity = plannedActivity,
                        initialWorkoutType = if (exercisePlan?.isStrength == false) {
                            WorkoutType.CARDIO
                        } else {
                            WorkoutType.STRENGTH
                        },
                        initialDurationMinutes = exercisePlan?.durationMinutes,
                        targetDateLabel = occurrence.date.format(LONG_DATE),
                        targetDateIsToday = occurrence.date == state.today,
                        onDismiss = { completingPlan = null },
                        onWorkoutLogged = { workout ->
                            onLogWorkout(workout)
                            onCompletePlanOccurrence(occurrence.item.id, occurrence.date, workout.id)
                            completingPlan = null
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.1.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UpcomingPlanRow(
    item: PlanItem,
    nextDate: LocalDate,
    imageUrl: String?,
    onClick: () -> Unit,
) {
    val schedule = buildString {
        append(nextDate.format(SHORT_DATE))
        append(" • ")
        append(item.time.format(TIME))
        if (item.repeat != PlanRepeat.NONE) {
            append(" • ")
            append(item.repeat.label)
        }
    }
    val detail = when (item.type) {
        PlanItemType.FOOD -> "${item.food?.meal?.label.orEmpty()} • ${item.food?.grams?.toInt() ?: 0}g"
        PlanItemType.EXERCISE -> "${item.exercise?.durationMinutes ?: 0} min"
        PlanItemType.PILL -> item.pill?.dosage.orEmpty().ifBlank { "Scheduled dose" }
        PlanItemType.FASTING -> "${item.fasting?.targetHours ?: 16}h fast"
        PlanItemType.WATER -> "${item.water?.amountMl ?: 250} mL"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlanItemImage(
                type = item.type,
                imageUrl = imageUrl,
                contentDescription = item.title,
                modifier = Modifier.size(38.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = schedule,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open ${item.title}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
