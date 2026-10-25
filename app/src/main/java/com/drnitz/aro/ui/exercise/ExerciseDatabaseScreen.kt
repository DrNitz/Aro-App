package com.drnitz.aro.ui.exercise

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drnitz.aro.data.library.OfflineExerciseLibrary
import com.drnitz.aro.data.library.imageUrlAt
import com.drnitz.aro.data.library.imageUrls
import com.drnitz.aro.domain.calculation.MetActivity
import com.drnitz.aro.domain.calculation.MetCatalog
import com.drnitz.aro.domain.model.Exercise
import com.drnitz.aro.domain.model.WorkoutLog
import com.drnitz.aro.domain.model.WorkoutType
import com.drnitz.aro.theme.AroClinicalModerate
import com.drnitz.aro.theme.AroColorCardio
import com.drnitz.aro.theme.AroColorFiber
import com.drnitz.aro.theme.AroColorWater
import com.drnitz.aro.theme.AroTheme
import com.drnitz.aro.ui.components.AroLoggingDateChip
import com.drnitz.aro.ui.components.ExerciseImage
import com.drnitz.aro.ui.main.LogWorkoutDialog
import java.time.LocalDate

private val MUSCLE_GROUPS = listOf("All", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Cardio")

private val MUSCLE_MATCHERS = mapOf(
    "Chest" to setOf("chest"),
    "Back" to setOf("lats", "middle back", "lower back", "traps"),
    "Legs" to setOf("quadriceps", "hamstrings", "glutes", "calves", "adductors", "abductors"),
    "Shoulders" to setOf("shoulders", "neck"),
    "Arms" to setOf("biceps", "triceps", "forearms"),
    "Core" to setOf("abdominals"),
)

private val EQUIPMENT_GROUPS = listOf("All", "Barbell", "Dumbbell", "Bodyweight", "Machine", "Cable")

private val EQUIPMENT_VALUES = mapOf(
    "Barbell" to "barbell",
    "Dumbbell" to "dumbbell",
    "Bodyweight" to "body only",
    "Machine" to "machine",
    "Cable" to "cable",
)

private data class QuickCardio(
    val label: String,
    val activity: MetActivity,
    val icon: ImageVector,
    val tint: Color,
)

private val QUICK_CARDIO = listOf(
    QuickCardio(
        label = "Walking",
        activity = MetCatalog.byId("walking-brisk") ?: MetCatalog.all.first(),
        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        tint = AroColorWater,
    ),
    QuickCardio(
        label = "Jogging",
        activity = MetCatalog.byId("running-5mph") ?: MetCatalog.all.first(),
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        tint = AroColorFiber,
    ),
    QuickCardio(
        label = "Running",
        activity = MetCatalog.byId("running-6mph") ?: MetCatalog.all.first(),
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        tint = AroColorCardio,
    ),
    QuickCardio(
        label = "Cycling",
        activity = MetCatalog.byId("cycling-moderate") ?: MetCatalog.all.first(),
        icon = Icons.AutoMirrored.Filled.DirectionsBike,
        tint = AroClinicalModerate,
    ),
)

/**
 * Workout database tab.
 *
 * The full Free Exercise DB corpus (876 entries) is parsed once and filtered in memory,
 * so typing or switching a chip is instant and works with the radio off.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDatabaseScreen(
    exerciseLibrary: OfflineExerciseLibrary,
    userWeightKg: Double,
    selectedDate: LocalDate,
    formattedDate: String,
    isToday: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    onToday: () -> Unit,
    onWorkoutLogged: (WorkoutLog) -> Unit,
    modifier: Modifier = Modifier,
    userHeightCm: Double = 170.0,
    isPlanning: Boolean = false,
    onExercisePlanned: (Exercise) -> Unit = {},
    onLoadPreviousWorkout: (suspend (String) -> WorkoutLog?)? = null,
) {
    var allExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var muscleGroup by remember { mutableStateOf("All") }
    var equipmentGroup by remember { mutableStateOf("All") }

    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var loggingExercise by remember { mutableStateOf<Exercise?>(null) }
    var quickLogActivity by remember { mutableStateOf<MetActivity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        allExercises = runCatching { exerciseLibrary.getExercises() }.getOrDefault(emptyList())
        isLoading = false
    }

    val filtered = remember(allExercises, query, muscleGroup, equipmentGroup) {
        val clean = query.trim().lowercase()
        val equipmentValue = EQUIPMENT_VALUES[equipmentGroup]
        allExercises.filter { exercise ->
            val matchesQuery = clean.isEmpty() || exercise.matchesQuery(clean)
            val matchesMuscle = when (muscleGroup) {
                "All" -> true
                "Cardio" -> exercise.category.equals("cardio", ignoreCase = true)
                else -> {
                    val group = MUSCLE_MATCHERS[muscleGroup].orEmpty()
                    exercise.primaryMuscles.any { it.lowercase() in group } ||
                        exercise.secondaryMuscles.any { it.lowercase() in group }
                }
            }
            val matchesEquipment = equipmentValue == null ||
                exercise.equipment.equals(equipmentValue, ignoreCase = true)
            matchesQuery && matchesMuscle && matchesEquipment
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "exercise_header_controls") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AroLoggingDateChip(
                    selectedDate = selectedDate,
                    formattedDate = formattedDate,
                    isToday = isToday,
                    onSelectDate = onSelectDate,
                    onToday = onToday,
                    labelPrefix = if (isPlanning) "Planning for" else "Logging for",
                )

                QuickCardioSection(
                    isPlanning = isPlanning,
                    onSelect = { quickLogActivity = it },
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            text = "Search exercises, muscles or equipment...",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "TARGET MUSCLE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            fontSize = 10.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MUSCLE_GROUPS, key = { it }) { group ->
                            FilterChip(
                                selected = muscleGroup == group,
                                onClick = { muscleGroup = group },
                                label = { Text(group, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "EQUIPMENT",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            fontSize = 10.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(EQUIPMENT_GROUPS, key = { it }) { equipment ->
                            FilterChip(
                                selected = equipmentGroup == equipment,
                                onClick = { equipmentGroup = equipment },
                                label = { Text(equipment, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                }
            }
        }

        if (isLoading) {
            item(key = "exercise_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Loading exercise library...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else if (filtered.isEmpty()) {
            item(key = "exercise_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.size(44.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No exercises match these filters",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Clear the search or choose a different muscle group.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            item(key = "exercise_count") {
                Text(
                    text = "${filtered.size} exercises",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            items(filtered, key = { it.id }) { exercise ->
                ExerciseCard(
                    exercise = exercise,
                    onOpen = { selectedExercise = exercise },
                )
            }
            item(key = "exercise_bottom_spacing") {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    selectedExercise?.let { exercise ->
        ModalBottomSheet(
            onDismissRequest = { selectedExercise = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { selectedExercise = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                val postureUrls = exercise.imageUrls(limit = 2)
                if (postureUrls.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        postureUrls.forEachIndexed { index, url ->
                            ExerciseImage(
                                imageUrl = url,
                                contentDescription = "${exercise.name} posture ${index + 1}",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(150.dp),
                                cornerRadius = 12.dp,
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LevelBadge(exercise.level)
                    if (exercise.category.isNotBlank()) {
                        InfoPill(exercise.category.replaceFirstChar { it.uppercase() })
                    }
                    if (!exercise.equipment.isNullOrBlank()) {
                        InfoPill(exercise.equipment.replaceFirstChar { it.uppercase() })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MUSCLES WORKED",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Primary: ${exercise.primaryMuscles.joinToString(", ").ifBlank { "Full body" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (exercise.secondaryMuscles.isNotEmpty()) {
                    Text(
                        text = "Secondary: ${exercise.secondaryMuscles.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "HOW TO PERFORM",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (exercise.instructions.isEmpty()) {
                    Text(
                        text = "No instructions were bundled for this movement.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    exercise.instructions.forEachIndexed { index, step ->
                        Row(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(22.dp),
                            )
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (isPlanning) {
                            onExercisePlanned(exercise)
                            selectedExercise = null
                        } else {
                            loggingExercise = exercise
                            selectedExercise = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPlanning) "Plan Workout" else "Log Workout",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }

    loggingExercise?.let { exercise ->
        val isCardio = exercise.category.equals("cardio", ignoreCase = true)
        LogWorkoutDialog(
            exerciseLibrary = exerciseLibrary,
            userWeightKg = userWeightKg,
            userHeightCm = userHeightCm,
            preselectedExercise = exercise.takeIf { !isCardio },
            initialWorkoutType = if (isCardio) WorkoutType.CARDIO else WorkoutType.STRENGTH,
            targetDateLabel = formattedDate,
            targetDateIsToday = isToday,
            cardioActionLabel = if (isPlanning) "Plan Cardio Workout" else "Save Cardio Workout",
            onLoadPreviousWorkout = onLoadPreviousWorkout,
            onDismiss = { loggingExercise = null },
            onWorkoutLogged = { workout ->
                onWorkoutLogged(workout)
                loggingExercise = null
            },
        )
    }

    quickLogActivity?.let { activity ->
        LogWorkoutDialog(
            exerciseLibrary = exerciseLibrary,
            userWeightKg = userWeightKg,
            userHeightCm = userHeightCm,
            preselectedActivity = activity,
            initialWorkoutType = WorkoutType.CARDIO,
            targetDateLabel = formattedDate,
            targetDateIsToday = isToday,
            cardioActionLabel = if (isPlanning) "Plan Cardio Workout" else "Save Cardio Workout",
            onDismiss = { quickLogActivity = null },
            onWorkoutLogged = { workout ->
                onWorkoutLogged(workout)
                quickLogActivity = null
            },
        )
    }
}

@Composable
private fun QuickCardioSection(
    isPlanning: Boolean,
    onSelect: (MetActivity) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "QUICK CARDIO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (isPlanning) {
                            "Plan for the selected day • tap to add"
                        } else {
                            "Tap to log • phone step tracking available"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                QUICK_CARDIO.forEach { quick ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = quick.tint.copy(alpha = 0.14f),
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(quick.activity) },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 9.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = quick.icon,
                                contentDescription = "${quick.label} quick log",
                                tint = quick.tint,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = quick.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "${quick.activity.met} MET",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                color = quick.tint,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseCard(exercise: Exercise, onOpen: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExerciseImage(
                imageUrl = exercise.imageUrlAt(0),
                contentDescription = exercise.name,
                modifier = Modifier.size(52.dp),
                cornerRadius = 11.dp,
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = exercise.primaryMuscles.joinToString(", ").ifBlank { "full body" } +
                        exercise.equipment?.let { " • $it" }.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            LevelBadge(exercise.level)
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LevelBadge(level: String) {
    val color = when (level.lowercase()) {
        "beginner" -> AroTheme.macroColors.clinicalOptimal
        "intermediate" -> AroTheme.macroColors.clinicalModerate
        "expert" -> AroTheme.macroColors.clinicalHighRisk
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
        Text(
            text = level.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.sp),
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun InfoPill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
