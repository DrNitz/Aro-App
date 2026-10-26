package com.drnitz.aro.ui.exercise

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
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
import com.drnitz.aro.ui.bodymap.BodyGender
import com.drnitz.aro.ui.bodymap.BodySide
import com.drnitz.aro.ui.bodymap.BodySoundHaptics
import com.drnitz.aro.ui.bodymap.BodyView
import com.drnitz.aro.ui.bodymap.BodyViewStyle
import com.drnitz.aro.ui.bodymap.HeatmapColorScale
import com.drnitz.aro.ui.bodymap.Muscle
import com.drnitz.aro.ui.bodymap.MuscleVolumeCalculator
import com.drnitz.aro.ui.bodymap.MuscleVolumeStat
import com.drnitz.aro.ui.bodymap.heatmapHighlights
import com.drnitz.aro.ui.components.AroLoggingDateChip
import com.drnitz.aro.ui.components.ExerciseImage
import com.drnitz.aro.ui.main.LogWorkoutDialog
import com.drnitz.aro.ui.mascot.AroMascot
import com.drnitz.aro.ui.mascot.MascotMood
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

enum class ExerciseFilterTab(val label: String) {
    ALL("All"),
    STRENGTH("Strength"),
    CARDIO("Cardio"),
    FAVORITES("Favorites"),
    CUSTOM("Custom"),
}

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
 * Interactive Workout Database Screen.
 *
 * Primary Mode: Full-height 2D anatomical human body map taking center stage with
 * 3D card rotation between Anterior and Posterior views, micro-haptic/sound feedback,
 * and an emerging sliding bottom drawer for muscle-filtered exercise exploration.
 *
 * Secondary Mode: Classic scrollable exercise list with multi-token search and categories.
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
    customExercises: List<Exercise> = emptyList(),
    favoriteExerciseIds: Set<String> = emptySet(),
    onToggleFavoriteExercise: (Exercise) -> Unit = {},
    onSaveCustomExercise: (Exercise) -> Unit = {},
    onDeleteCustomExercise: (String) -> Unit = {},
    weeklyWorkouts: List<WorkoutLog> = emptyList(),
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    // View & Body Map States
    var isBodyMapMode by remember { mutableStateOf(true) }
    var isHeatmapMode by remember { mutableStateOf(false) }
    var bodySide by remember { mutableStateOf(BodySide.Front) }
    var bodyGender by remember { mutableStateOf(BodyGender.Male) }
    var isSoundMuted by remember { mutableStateOf(false) }
    var selectedMuscle by remember { mutableStateOf<Muscle?>(null) }
    var drawerExpanded by remember { mutableStateOf(false) }
    var drawerFullScreen by remember { mutableStateOf(false) }
    var canvasScale by remember { mutableFloatStateOf(1f) }
    var canvasPan by remember { mutableStateOf(Offset.Zero) }

    // Smooth 3D Card Rotation between Anterior (0°) and Posterior (180°)
    val rotation by animateFloatAsState(
        targetValue = if (bodySide == BodySide.Front) 0f else 180f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "body_3d_card_flip",
    )

    // Data States
    var bundledExercises by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(ExerciseFilterTab.ALL) }
    var muscleGroup by remember { mutableStateOf("All") }
    var equipmentGroup by remember { mutableStateOf("All") }

    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var loggingExercise by remember { mutableStateOf<Exercise?>(null) }
    var quickLogActivity by remember { mutableStateOf<MetActivity?>(null) }
    var showCreateCustomDialog by remember { mutableStateOf(false) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(exerciseLibrary) {
        bundledExercises = runCatching { exerciseLibrary.getExercises() }.getOrDefault(emptyList())
        isLoading = false
    }

    val allExercises = remember(bundledExercises, customExercises) {
        if (customExercises.isEmpty()) bundledExercises else customExercises + bundledExercises
    }

    // Weekly Muscle Volume and Heatmap Calculations
    val muscleVolumeMap = remember(weeklyWorkouts, allExercises) {
        MuscleVolumeCalculator.calculateMuscleVolume(
            workouts = weeklyWorkouts,
            exerciseLookup = { name -> allExercises.firstOrNull { it.name.equals(name, ignoreCase = true) } },
        )
    }
    val muscleIntensities = remember(muscleVolumeMap) {
        MuscleVolumeCalculator.toMuscleIntensities(muscleVolumeMap)
    }
    val heatmapHighlightsMap = remember(muscleIntensities, isHeatmapMode) {
        if (isHeatmapMode) {
            heatmapHighlights(muscleIntensities, HeatmapColorScale.AroDarkWorkout)
        } else {
            emptyMap()
        }
    }

    val filtered = remember(allExercises, query, selectedMuscle, muscleGroup, equipmentGroup, activeTab, favoriteExerciseIds) {
        val clean = query.trim().lowercase()
        val equipmentValue = EQUIPMENT_VALUES[equipmentGroup]

        allExercises.filter { exercise ->
            // Tab filter
            val matchesTab = when (activeTab) {
                ExerciseFilterTab.ALL -> true
                ExerciseFilterTab.STRENGTH -> !exercise.category.equals("cardio", ignoreCase = true)
                ExerciseFilterTab.CARDIO -> exercise.category.equals("cardio", ignoreCase = true)
                ExerciseFilterTab.FAVORITES -> favoriteExerciseIds.contains(exercise.id)
                ExerciseFilterTab.CUSTOM -> exercise.isCustom
            }
            if (!matchesTab) return@filter false

            // Search query filter
            val matchesQuery = clean.isEmpty() || exercise.matchesQuery(clean)
            if (!matchesQuery) return@filter false

            // Target muscle filter: Body Map selected muscle takes precedence when active
            val matchesMuscle = when {
                selectedMuscle != null -> {
                    val targets = selectedMuscle!!.toExerciseFilterMuscles()
                    if (targets.isEmpty()) true
                    else {
                        exercise.primaryMuscles.any { pm -> targets.any { tm -> pm.lowercase().contains(tm) } } ||
                            exercise.secondaryMuscles.any { sm -> targets.any { tm -> sm.lowercase().contains(tm) } }
                    }
                }
                muscleGroup != "All" -> {
                    if (muscleGroup == "Cardio") exercise.category.equals("cardio", ignoreCase = true)
                    else {
                        val group = MUSCLE_MATCHERS[muscleGroup].orEmpty()
                        exercise.primaryMuscles.any { it.lowercase() in group } ||
                            exercise.secondaryMuscles.any { it.lowercase() in group }
                    }
                }
                else -> true
            }
            if (!matchesMuscle) return@filter false

            // Equipment filter
            val matchesEquipment = equipmentValue == null ||
                exercise.equipment.equals(equipmentValue, ignoreCase = true)
            if (!matchesEquipment) return@filter false

            true
        }
    }

    if (isBodyMapMode) {
        // =========================================================================
        // PRIMARY VIEW: FULL-HEIGHT 3D ANATOMICAL BODY MAP WITH EMERGING DRAWER
        // =========================================================================
        Box(modifier = modifier.fillMaxSize()) {
            // Layer 0: Full-Height Interactive Canvas with 3D Card Rotation
            val isFrontShowing = rotation <= 90f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 74.dp, bottom = if (drawerExpanded) 60.dp else 70.dp)
                    .graphicsLayer {
                        rotationY = if (isFrontShowing) rotation else rotation - 180f
                        cameraDistance = 14f * density
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (isFrontShowing) {
                    BodyView(
                        side = BodySide.Front,
                        gender = bodyGender,
                        highlights = heatmapHighlightsMap,
                        selectedMuscles = if (selectedMuscle != null) setOf(selectedMuscle!!) else emptySet(),
                        style = BodyViewStyle.AroDark,
                        enableSensoryFeedback = true,
                        isSoundMuted = isSoundMuted,
                        scale = canvasScale,
                        panOffset = canvasPan,
                        onTransformChanged = { scale, pan ->
                            canvasScale = scale
                            canvasPan = pan
                        },
                        onMuscleSelected = { muscle, _ ->
                            selectedMuscle = if (selectedMuscle == muscle) null else muscle
                            if (selectedMuscle != null) {
                                drawerExpanded = true
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    BodyView(
                        side = BodySide.Back,
                        gender = bodyGender,
                        highlights = heatmapHighlightsMap,
                        selectedMuscles = if (selectedMuscle != null) setOf(selectedMuscle!!) else emptySet(),
                        style = BodyViewStyle.AroDark,
                        enableSensoryFeedback = true,
                        isSoundMuted = isSoundMuted,
                        scale = canvasScale,
                        panOffset = canvasPan,
                        onTransformChanged = { scale, pan ->
                            canvasScale = scale
                            canvasPan = pan
                        },
                        onMuscleSelected = { muscle, _ ->
                            selectedMuscle = if (selectedMuscle == muscle) null else muscle
                            if (selectedMuscle != null) {
                                drawerExpanded = true
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Layer 1: Top Status & Active Muscle Indicator (Left-aligned, leaving center open)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 68.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AroLoggingDateChip(
                    selectedDate = selectedDate,
                    formattedDate = formattedDate,
                    isToday = isToday,
                    onSelectDate = onSelectDate,
                    onToday = onToday,
                    labelPrefix = if (isPlanning) "Plan for" else "Log for",
                )

                if (selectedMuscle != null) {
                    val stat = muscleVolumeMap[selectedMuscle]
                    val volumeHint = if (isHeatmapMode && stat != null && stat.sets > 0) {
                        " • ${stat.sets.toInt()}s"
                    } else ""
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        shadowElevation = 2.dp,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 3.dp, bottom = 3.dp),
                        ) {
                            Text(
                                text = "🔥 ${selectedMuscle!!.displayName}$volumeHint (${filtered.size})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.width(2.dp))
                            IconButton(
                                onClick = { selectedMuscle = null },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear muscle filter",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                } else if (isHeatmapMode) {
                    Surface(
                        color = Color(0xFFEF4444).copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.45f)),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Whatshot,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "Heatmap",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Layer 2: Embedded Vertical Side Dock (TopEnd, off Anatomical Diagram)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                shadowElevation = 4.dp,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                ) {
                    // 3D Flip (Front / Back)
                    IconButton(
                        onClick = {
                            bodySide = if (bodySide == BodySide.Front) BodySide.Back else BodySide.Front
                            BodySoundHaptics.playClickSound(isSoundMuted)
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Cached,
                                contentDescription = "Flip 3D body",
                                tint = AroTheme.macroColors.carbs,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = if (bodySide == BodySide.Front) "ANT" else "POST",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    // Gender Toggle (♂ M / ♀ F)
                    IconButton(
                        onClick = {
                            bodyGender = if (bodyGender == BodyGender.Male) BodyGender.Female else BodyGender.Male
                            BodySoundHaptics.playClickSound(isSoundMuted)
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Text(
                            text = if (bodyGender == BodyGender.Male) "♂" else "♀",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    // Heatmap Volume / Recovery Mode Toggle
                    IconButton(
                        onClick = {
                            isHeatmapMode = !isHeatmapMode
                            BodySoundHaptics.playClickSound(isSoundMuted)
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = if (isHeatmapMode) "Heatmap mode active" else "Toggle volume heatmap",
                            tint = if (isHeatmapMode) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(17.dp),
                        )
                    }

                    // Zoom Indicator / Quick Reset (appears when zoomed in or panned)
                    if (canvasScale > 1.05f || canvasPan != Offset.Zero) {
                        IconButton(
                            onClick = {
                                canvasScale = 1f
                                canvasPan = Offset.Zero
                                BodySoundHaptics.playClickSound(isSoundMuted)
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset Zoom to 1x",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = "${(canvasScale * 10).roundToInt() / 10f}x",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    // Audio Mute Toggle
                    IconButton(
                        onClick = { isSoundMuted = !isSoundMuted },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = if (isSoundMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSoundMuted) "Unmute audio" else "Mute audio",
                            tint = if (isSoundMuted) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.width(22.dp).padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )

                    // Switch to Classic List Mode
                    IconButton(
                        onClick = { isBodyMapMode = false },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                            contentDescription = "Switch to classic list view",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            // Layer 2: Emerging Sliding Bottom Exercise Drawer
            AnimatedVisibility(
                visible = !drawerExpanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                // Collapsed Peek Bar
                Surface(
                    onClick = { drawerExpanded = true },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount < -10f) {
                                    drawerExpanded = true
                                }
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isHeatmapMode) Icons.Default.Whatshot else Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = if (isHeatmapMode) Color(0xFFEF4444) else AroTheme.macroColors.carbs,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (selectedMuscle != null) {
                                    val stat = muscleVolumeMap[selectedMuscle]
                                    val statSuffix = if (isHeatmapMode && stat != null && stat.sets > 0) " • ${stat.sets.toInt()} sets" else ""
                                    "${selectedMuscle!!.displayName}$statSuffix (${filtered.size} movements) • Tap to view"
                                } else if (isHeatmapMode) {
                                    "7-Day Volume Heatmap active • ${filtered.size} movements"
                                } else {
                                    "Tap any muscle to filter • 1,389 movements"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Expand exercise drawer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            // Expanded Drawer Sheet
            AnimatedVisibility(
                visible = drawerExpanded,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                val drawerHeightFraction = if (drawerFullScreen) 0.92f else 0.58f

                Card(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(drawerHeightFraction),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp),
                    ) {
                        // Top Drag Handle & Controls Row with Vertical Swipe Physics
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(drawerFullScreen) {
                                    detectVerticalDragGestures { _, dragAmount ->
                                        if (dragAmount < -15f && !drawerFullScreen) {
                                            drawerFullScreen = true
                                        } else if (dragAmount > 15f) {
                                            if (drawerFullScreen) {
                                                drawerFullScreen = false
                                            } else {
                                                drawerExpanded = false
                                            }
                                        }
                                    }
                                }
                                .padding(vertical = 4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    text = if (selectedMuscle != null) {
                                        "${selectedMuscle!!.displayName} Exercises"
                                    } else {
                                        "All Exercises"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text(
                                        text = "${filtered.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                                if (selectedMuscle != null) {
                                    TextButton(
                                        onClick = { selectedMuscle = null },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Fullscreen / Partial height toggle
                                IconButton(
                                    onClick = { drawerFullScreen = !drawerFullScreen },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = if (drawerFullScreen) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                        contentDescription = if (drawerFullScreen) "Collapse height" else "Expand full height",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                // Close drawer
                                IconButton(
                                    onClick = { drawerExpanded = false },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close exercise drawer",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        // Heatmap Volume & Recovery Landmark Banner
                        if (selectedMuscle != null || isHeatmapMode) {
                            HeatmapVolumeBanner(
                                selectedMuscle = selectedMuscle,
                                stat = selectedMuscle?.let { muscleVolumeMap[it] },
                                volumeStats = muscleVolumeMap,
                                isHeatmapMode = isHeatmapMode,
                                onSelectMuscle = { selectedMuscle = it },
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }

                        // Compact Search Bar & Create Action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                        if (query.isEmpty()) {
                                            Text(
                                                text = if (selectedMuscle != null) "Search ${selectedMuscle!!.displayName}..." else "Search 1,389 exercises...",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        BasicTextField(
                                            value = query,
                                            onValueChange = { query = it },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 13.sp,
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                    if (query.isNotEmpty()) {
                                        IconButton(
                                            onClick = { query = "" },
                                            modifier = Modifier.size(24.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear search",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            Surface(
                                onClick = { showCreateCustomDialog = true },
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(40.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "New Exercise",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "New",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }

                        // Category Tabs (All, Strength, Cardio, Favorites, Custom)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                        ) {
                            items(ExerciseFilterTab.entries.toList(), key = { it.name }) { tab ->
                                FilterChip(
                                    selected = activeTab == tab,
                                    onClick = { activeTab = tab },
                                    label = {
                                        val countText = when (tab) {
                                            ExerciseFilterTab.FAVORITES -> if (favoriteExerciseIds.isNotEmpty()) " (${favoriteExerciseIds.size})" else ""
                                            ExerciseFilterTab.CUSTOM -> if (customExercises.isNotEmpty()) " (${customExercises.size})" else ""
                                            else -> ""
                                        }
                                        Text("${tab.label}$countText", style = MaterialTheme.typography.labelSmall)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }
                        }

                        // Quick Cardio Shortcut Row (only when Cardio tab selected)
                        if (activeTab == ExerciseFilterTab.CARDIO) {
                            QuickCardioSection(
                                isPlanning = isPlanning,
                                onSelect = { quickLogActivity = it },
                            )
                            Spacer(Modifier.height(6.dp))
                        }

                        // Exercise List
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        } else if (filtered.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(40.dp),
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "No matching exercises found",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    TextButton(
                                        onClick = {
                                            query = ""
                                            selectedMuscle = null
                                            activeTab = ExerciseFilterTab.ALL
                                        },
                                    ) {
                                        Text("Reset Filters")
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 16.dp),
                            ) {
                                items(filtered, key = { it.id }) { exercise ->
                                    val isFav = favoriteExerciseIds.contains(exercise.id)
                                    ExerciseCard(
                                        exercise = exercise,
                                        isFavorite = isFav,
                                        onToggleFavorite = { onToggleFavoriteExercise(exercise) },
                                        onOpen = { selectedExercise = exercise },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // =========================================================================
        // SECONDARY VIEW: CLASSIC SCROLLABLE EXERCISE LIST
        // =========================================================================
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        AroLoggingDateChip(
                            selectedDate = selectedDate,
                            formattedDate = formattedDate,
                            isToday = isToday,
                            onSelectDate = onSelectDate,
                            onToday = onToday,
                            labelPrefix = if (isPlanning) "Planning for" else "Logging for",
                        )

                        // Button to return to Interactive 3D Body Map
                        Button(
                            onClick = { isBodyMapMode = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Accessibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Body Map", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (activeTab == ExerciseFilterTab.ALL || activeTab == ExerciseFilterTab.CARDIO) {
                        QuickCardioSection(
                            isPlanning = isPlanning,
                            onSelect = { quickLogActivity = it },
                        )
                    }

                    // Compact Search Bar + New Custom Exercise Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp),
                                )
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = when (activeTab) {
                                                ExerciseFilterTab.FAVORITES -> "Search favorites..."
                                                ExerciseFilterTab.CUSTOM -> "Search custom..."
                                                else -> "Search exercises, muscles..."
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    BasicTextField(
                                        value = query,
                                        onValueChange = { query = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                if (query.isNotEmpty()) {
                                    IconButton(
                                        onClick = { query = "" },
                                        modifier = Modifier.size(24.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            onClick = { showCreateCustomDialog = true },
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(40.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New Exercise",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "New",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }

                    // Category Filter Tabs
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(ExerciseFilterTab.entries.toList(), key = { it.name }) { tab ->
                            FilterChip(
                                selected = activeTab == tab,
                                onClick = { activeTab = tab },
                                leadingIcon = when (tab) {
                                    ExerciseFilterTab.FAVORITES -> {
                                        {
                                            Icon(
                                                imageVector = if (activeTab == tab) Icons.Default.Star else Icons.Default.StarBorder,
                                                contentDescription = null,
                                                tint = if (activeTab == tab) AroTheme.macroColors.carbs else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    ExerciseFilterTab.CARDIO -> {
                                        {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    ExerciseFilterTab.STRENGTH -> {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.FitnessCenter,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    ExerciseFilterTab.CUSTOM -> {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    ExerciseFilterTab.ALL -> null
                                },
                                label = {
                                    val countText = when (tab) {
                                        ExerciseFilterTab.FAVORITES -> if (favoriteExerciseIds.isNotEmpty()) " (${favoriteExerciseIds.size})" else ""
                                        ExerciseFilterTab.CUSTOM -> if (customExercises.isNotEmpty()) " (${customExercises.size})" else ""
                                        else -> ""
                                    }
                                    Text(
                                        text = "${tab.label}$countText",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                            )
                        }
                    }

                    // Secondary Muscle Filter
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

                    // Secondary Equipment Filter
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
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    query = ""
                                    muscleGroup = "All"
                                    equipmentGroup = "All"
                                },
                            ) {
                                Text("Reset Filters")
                            }
                        }
                    }
                }
            } else {
                item(key = "exercise_count") {
                    val label = when (activeTab) {
                        ExerciseFilterTab.FAVORITES -> "${filtered.size} favorite exercises"
                        ExerciseFilterTab.CUSTOM -> "${filtered.size} custom exercises"
                        else -> "${filtered.size} exercises"
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
                items(filtered, key = { it.id }) { exercise ->
                    val isFav = favoriteExerciseIds.contains(exercise.id)
                    ExerciseCard(
                        exercise = exercise,
                        isFavorite = isFav,
                        onToggleFavorite = { onToggleFavoriteExercise(exercise) },
                        onOpen = { selectedExercise = exercise },
                    )
                }
                item(key = "exercise_bottom_spacing") {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    // =========================================================================
    // EXERCISE DETAILS MODAL SHEET (POSTURES, COACH FINCH BEAST CUES, PRS)
    // =========================================================================
    selectedExercise?.let { exercise ->
        val isFav = favoriteExerciseIds.contains(exercise.id)
        var previousWorkout by remember(exercise.name) { mutableStateOf<WorkoutLog?>(null) }
        var isLoadingPrevious by remember(exercise.name) { mutableStateOf(true) }

        val exerciseWeeklySets = remember(weeklyWorkouts, exercise.name) {
            weeklyWorkouts.filter { it.exerciseName.equals(exercise.name, ignoreCase = true) }
                .sumOf { w ->
                    if (w.workingSets.isNotEmpty()) w.workingSets.size
                    else if (w.sets.isNotEmpty()) w.sets.size
                    else 1
                }
        }
        val primaryMuscle = remember(exercise.primaryMuscles) {
            exercise.primaryMuscles.firstOrNull()?.let { Muscle.fromExerciseMuscle(it) }
        }
        val muscleVolumeStat = remember(primaryMuscle, muscleVolumeMap) {
            primaryMuscle?.let { muscleVolumeMap[it] }
        }

        LaunchedEffect(exercise.name) {
            isLoadingPrevious = true
            previousWorkout = onLoadPreviousWorkout?.invoke(exercise.name)
            isLoadingPrevious = false
        }

        ModalBottomSheet(
            onDismissRequest = { selectedExercise = null },
            sheetState = detailSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header with Star Bookmark and Close
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onToggleFavoriteExercise(exercise) }) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isFav) "Remove favorite" else "Save favorite",
                            tint = if (isFav) AroTheme.macroColors.carbs else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { selectedExercise = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Dual Posture Pictures
                val postureUrls = exercise.imageUrls(limit = 2)
                if (postureUrls.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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

                // Badges & Metadata Pill Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item { LevelBadge(exercise.level) }
                    if (exerciseWeeklySets > 0) {
                        item {
                            Surface(
                                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Whatshot,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(11.dp),
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = "$exerciseWeeklySets sets (7d)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                        color = Color(0xFFEF4444),
                                    )
                                }
                            }
                        }
                    } else if (muscleVolumeStat != null && muscleVolumeStat.sets > 0) {
                        item {
                            val color = HeatmapColorScale.AroDarkWorkout.colorFor(muscleVolumeStat.intensity)
                            Surface(
                                color = color.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = color,
                                        modifier = Modifier.size(11.dp),
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = "${muscleVolumeStat.muscle.displayName}: ${muscleVolumeStat.sets.toInt()}s (7d)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                        color = color,
                                    )
                                }
                            }
                        }
                    }
                    if (exercise.isCustom) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = "Custom",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    if (exercise.category.isNotBlank()) {
                        item { InfoPill(exercise.category.replaceFirstChar { it.uppercase() }) }
                    }
                    if (!exercise.equipment.isNullOrBlank()) {
                        item { InfoPill(exercise.equipment.replaceFirstChar { it.uppercase() }) }
                    }
                    exercise.met?.let { metVal ->
                        item { InfoPill("${metVal} MET") }
                    }
                }

                // Coach Finch Beast Mode Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AroMascot(
                                mood = MascotMood.WORKOUT_BEAST,
                                size = 52.dp,
                                contentDescription = "Coach Aro pumping dumbbells",
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "COACH ARO'S FORM CUES",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = getCoachingHeader(exercise),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }

                        val cues = if (exercise.tips.isNotEmpty()) exercise.tips else getDefaultCoachingCues(exercise)
                        cues.forEach { cue ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    text = "💡",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                                Text(
                                    text = cue,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                // Personal Record & Previous History
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = "PERSONAL RECORD & HISTORY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (isLoadingPrevious) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 4.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text(
                                    text = "Checking previous session logs...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else if (previousWorkout != null) {
                            val pw = previousWorkout!!
                            val dateStr = remember(pw.loggedAt) {
                                runCatching {
                                    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
                                        .format(LocalDate.ofInstant(pw.loggedAt, ZoneId.systemDefault()))
                                }.getOrDefault("Recent session")
                            }

                            Text(
                                text = "Last recorded: $dateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            if (pw.isStrength) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    val topSet = pw.topSetKg.let { if (it % 1.0 == 0.0) "${it.toInt()} kg" else "$it kg" }
                                    PerformanceStatPill(
                                        label = "TOP SET",
                                        value = topSet,
                                        color = AroTheme.macroColors.clinicalOptimal,
                                        modifier = Modifier.weight(1f),
                                    )
                                    PerformanceStatPill(
                                        label = "VOLUME",
                                        value = "${pw.totalVolumeKg.toInt()} kg",
                                        color = AroTheme.macroColors.protein,
                                        modifier = Modifier.weight(1f),
                                    )
                                    PerformanceStatPill(
                                        label = "WORKING SETS",
                                        value = "${pw.workingSets.size}",
                                        color = AroTheme.macroColors.carbs,
                                        modifier = Modifier.weight(1f),
                                    )
                                }

                                if (pw.sets.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Session breakdown:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        itemsIndexed(pw.sets) { idx, set ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                                shape = RoundedCornerShape(8.dp),
                                            ) {
                                                val w = if (set.weightKg % 1.0 == 0.0) set.weightKg.toInt() else set.weightKg
                                                Text(
                                                    text = "S${idx + 1}: ${w}kg × ${set.reps}",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    PerformanceStatPill(
                                        label = "DURATION",
                                        value = "${pw.durationMinutes} min",
                                        color = AroColorCardio,
                                        modifier = Modifier.weight(1f),
                                    )
                                    pw.distanceKm?.let { dist ->
                                        PerformanceStatPill(
                                            label = "DISTANCE",
                                            value = "${dist} km",
                                            color = AroColorWater,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    pw.steps?.let { steps ->
                                        PerformanceStatPill(
                                            label = "STEPS",
                                            value = "$steps",
                                            color = AroTheme.macroColors.clinicalModerate,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No previous workouts logged yet. Complete your first session to establish personal records!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Muscles Worked
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "MUSCLES WORKED",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                    if (muscleVolumeStat != null && muscleVolumeStat.sets > 0) {
                        val color = HeatmapColorScale.AroDarkWorkout.colorFor(muscleVolumeStat.intensity)
                        Text(
                            text = "Trailing 7-day volume: ${muscleVolumeStat.sets.toInt()} sets logged (${(muscleVolumeStat.intensity * 100).toInt()}% RP fatigue)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = color,
                        )
                    }
                }

                // Execution Instructions
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "HOW TO PERFORM",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (exercise.instructions.isEmpty()) {
                        Text(
                            text = "No detailed execution steps were bundled for this movement.",
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
                }

                // Custom Exercise Management (Delete Option)
                if (exercise.isCustom) {
                    var showDeleteConfirm by remember { mutableStateOf(false) }

                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Custom Exercise", style = MaterialTheme.typography.labelMedium)
                    }

                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text("Delete Exercise?") },
                            text = {
                                Text("Are you sure you want to delete \"${exercise.name}\"? Past logged sessions will retain their logs, but this movement will be removed from your exercise library.")
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        onDeleteCustomExercise(exercise.id)
                                        showDeleteConfirm = false
                                        selectedExercise = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                ) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text("Cancel")
                                }
                            },
                        )
                    }
                }

                // Primary Logging Action Button
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

    // New Custom Exercise Creation Dialog
    if (showCreateCustomDialog) {
        CreateCustomExerciseDialog(
            onDismiss = { showCreateCustomDialog = false },
            onSave = { newExercise ->
                onSaveCustomExercise(newExercise)
                showCreateCustomDialog = false
            },
        )
    }

    // Workout Logging Dialog
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
            customExercises = customExercises,
            onDismiss = { loggingExercise = null },
            onWorkoutLogged = { workout ->
                onWorkoutLogged(workout)
                loggingExercise = null
            },
        )
    }

    // Quick Cardio Dialog
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
            customExercises = customExercises,
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
private fun ExerciseCard(
    exercise: Exercise,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpen: () -> Unit,
) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (exercise.isCustom) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = "CUSTOM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = exercise.primaryMuscles.joinToString(", ").ifBlank { "full body" } +
                        exercise.equipment?.let { " • $it" }.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(6.dp))
            LevelBadge(exercise.level)

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (isFavorite) "Remove favorite" else "Save favorite",
                    tint = if (isFavorite) AroTheme.macroColors.carbs else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
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

@Composable
private fun PerformanceStatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                ),
                color = color,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HeatmapVolumeBanner(
    selectedMuscle: Muscle?,
    stat: MuscleVolumeStat?,
    volumeStats: Map<Muscle, MuscleVolumeStat>,
    isHeatmapMode: Boolean,
    onSelectMuscle: (Muscle) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selectedMuscle != null) {
        val sets = stat?.sets ?: 0.0
        val volKg = stat?.volumeKg ?: 0.0
        val sessions = stat?.workoutCount ?: 0
        val intensity = stat?.intensity ?: 0f
        val themeColor = if (intensity > 0f) HeatmapColorScale.AroDarkWorkout.colorFor(intensity) else Color(0xFF10B981)

        val landmark = when {
            sets <= 0.0 -> "Rested / Ready"
            sets <= 4.0 -> "Light Stimulus (Maintenance)"
            sets <= 10.0 -> "Moderate Volume (Growth)"
            sets <= 18.0 -> "Optimal Hypertrophy (MAV)"
            else -> "Peak Volume (MRV Limit)"
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, themeColor.copy(alpha = 0.4f)),
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(themeColor),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${selectedMuscle.displayName}: $landmark",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Text(
                        text = if (sets > 0) "${sets.toInt()} sets (7d)" else "0 sets",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = themeColor,
                    )
                }

                // Visual progress bar toward 20 sets (MRV)
                LinearProgressIndicator(
                    progress = { (sets / 20.0).coerceIn(0.0, 1.0).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = themeColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )

                // Detail metrics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (volKg > 0) "${volKg.toInt()} kg volume • $sessions sessions" else if (sessions > 0) "$sessions sessions" else "No logged fatigue",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${(intensity * 100).toInt()}% RP fatigue",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = themeColor,
                    )
                }
            }
        }
    } else if (isHeatmapMode) {
        val topMuscles = volumeStats.values
            .filter { it.sets > 0 }
            .sortedByDescending { it.sets }
            .take(4)
        val totalSets = volumeStats.values.sumOf { it.sets }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
            modifier = modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(13.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Trailing 7-Day Training Load",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Text(
                        text = "${totalSets.toInt()} total sets",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        ),
                        color = Color(0xFFEF4444),
                    )
                }

                if (topMuscles.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(topMuscles) { topStat ->
                            val color = HeatmapColorScale.AroDarkWorkout.colorFor(topStat.intensity)
                            Surface(
                                onClick = { onSelectMuscle(topStat.muscle) },
                                color = color.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(color),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${topStat.muscle.displayName}: ${topStat.sets.toInt()}s",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No workouts logged in trailing 7 days. Tap any muscle to log sets!",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun getCoachingHeader(exercise: Exercise): String {
    val muscle = exercise.primaryMuscles.firstOrNull()?.lowercase() ?: ""
    return when {
        exercise.category.equals("cardio", ignoreCase = true) -> "Rhythm, pacing & aerobic conditioning"
        muscle.contains("chest") -> "Scapular depression, chest stretch & drive"
        muscle.contains("lat") || muscle.contains("back") || muscle.contains("trap") -> "Full lat stretch, elbow pull & neutral spine"
        muscle.contains("quad") || muscle.contains("glute") || muscle.contains("hamstring") -> "Core bracing, hip hinge & knee alignment"
        muscle.contains("shoulder") || muscle.contains("delt") -> "Scapular stability & rotator cuff control"
        muscle.contains("bicep") || muscle.contains("tricep") || muscle.contains("arm") -> "Joint isolation & peak muscular tension"
        muscle.contains("ab") || muscle.contains("core") -> "Pelvic tilt, intra-abdominal pressure & tempo"
        else -> "Strict tempo, full range of motion & mindful form"
    }
}

private fun getDefaultCoachingCues(exercise: Exercise): List<String> {
    val muscle = exercise.primaryMuscles.firstOrNull()?.lowercase() ?: ""
    return when {
        exercise.category.equals("cardio", ignoreCase = true) -> listOf(
            "Maintain an upright posture with relaxed shoulders and natural arm carriage.",
            "Breathe in a consistent rhythmic cadence (e.g. 2 strides in, 2 strides out).",
            "Pace your output: sustain a conversational pace for aerobic base building.",
        )
        muscle.contains("chest") -> listOf(
            "Retract and depress your scapulae into a stable packed base before pressing.",
            "Control the eccentric descent for 2–3 seconds to maximize muscular stretch.",
            "Drive through your palms and squeeze your inner pecs firmly at peak contraction.",
        )
        muscle.contains("lat") || muscle.contains("back") || muscle.contains("trap") -> listOf(
            "Initiate each repetition by pulling your elbows downward and backward.",
            "Keep your chest proud and avoid excessive forward torso swinging under load.",
            "Pause for 1 second at peak contraction to recruit stubborn deep back fibers.",
        )
        muscle.contains("quad") || muscle.contains("glute") || muscle.contains("hamstring") -> listOf(
            "Take a deep diaphragmatic breath and brace your core 360° before descending.",
            "Keep knees tracking over your midfoot and maintain three points of contact on your feet.",
            "Drive the floor away smoothly without letting your hips shoot up prematurely.",
        )
        muscle.contains("shoulder") || muscle.contains("delt") -> listOf(
            "Keep your ribs pulled down and glutes clenched to prevent lower-back hyperextension.",
            "Control the negative descent smoothly; avoid bouncing out of the bottom position.",
            "Focus on moving the weight with deltoid tension rather than shrugging your traps.",
        )
        muscle.contains("bicep") || muscle.contains("tricep") || muscle.contains("arm") -> listOf(
            "Pin your upper arms firmly to your sides to keep tension isolated on the target muscle.",
            "Move through a full range of motion with an emphatic squeeze at peak contraction.",
            "Eliminate body english; let the arm musculature do 100% of the mechanical work.",
        )
        muscle.contains("ab") || muscle.contains("core") -> listOf(
            "Exhale completely and tuck your pelvis to engage the deep transverse abdominis.",
            "Perform reps slowly under strict control; momentum robs core engagement.",
            "Focus on the curling/bracing movement rather than flexing through the neck or hip flexors.",
        )
        else -> listOf(
            "Prioritize strict biomechanical form over weight lifted on every single rep.",
            "Exhale smoothly through the exertion phase and inhale during the lowering phase.",
            "Control the eccentric tempo to stimulate hypertrophy and protect joint integrity.",
        )
    }
}

/**
 * Dialog for creating custom movements (e.g. specialized gym equipment, sports, home workouts).
 */
@Composable
fun CreateCustomExerciseDialog(
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Strength") }
    var muscle by remember { mutableStateOf("Chest") }
    var secondaryMuscle by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("Machine") }
    var level by remember { mutableStateOf("Beginner") }
    var instructionsText by remember { mutableStateOf("") }
    var tipsText by remember { mutableStateOf("") }
    var metText by remember { mutableStateOf("6.0") }

    val categories = remember { listOf("Strength", "Cardio", "Stretching", "Plyometrics") }
    val muscles = remember { listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body") }
    val equipmentList = remember { listOf("Machine", "Dumbbell", "Barbell", "Cable", "Bodyweight", "Bands", "Other") }
    val levels = remember { listOf("Beginner", "Intermediate", "Expert") }

    val canSave = name.trim().isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Custom Exercise",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name *") },
                    placeholder = { Text("e.g. Incline Smith Machine Press") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )

                // Category Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "CATEGORY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = {
                                    category = cat
                                    if (cat == "Cardio" && metText == "6.0") metText = "7.0"
                                    if (cat == "Strength" && metText == "7.0") metText = "6.0"
                                },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                }

                // Primary Muscle Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "PRIMARY MUSCLE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(muscles) { m ->
                            FilterChip(
                                selected = muscle == m,
                                onClick = { muscle = m },
                                label = { Text(m, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                }

                // Secondary Muscle Field
                OutlinedTextField(
                    value = secondaryMuscle,
                    onValueChange = { secondaryMuscle = it },
                    label = { Text("Secondary Muscle (optional)") },
                    placeholder = { Text("e.g. Triceps, Front Deltoids") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )

                // Equipment Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "EQUIPMENT",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(equipmentList) { eq ->
                            FilterChip(
                                selected = equipment == eq,
                                onClick = { equipment = eq },
                                label = { Text(eq, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                }

                // Difficulty Level Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "DIFFICULTY LEVEL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        levels.forEach { lvl ->
                            FilterChip(
                                selected = level == lvl,
                                onClick = { level = lvl },
                                label = { Text(lvl, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                }

                // Form Instructions
                OutlinedTextField(
                    value = instructionsText,
                    onValueChange = { instructionsText = it },
                    label = { Text("Execution Instructions (optional)") },
                    placeholder = { Text("Steps to perform (one per line)...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )

                // Coaching Tips
                OutlinedTextField(
                    value = tipsText,
                    onValueChange = { tipsText = it },
                    label = { Text("Coach Form Cues & Tips (optional)") },
                    placeholder = { Text("e.g. Keep chest up, pause at the bottom...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )

                // MET Value
                OutlinedTextField(
                    value = metText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                            metText = input
                        }
                    },
                    label = { Text("MET Rating (Energy burn)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newEx = Exercise(
                        id = "custom-" + UUID.randomUUID().toString().take(8),
                        name = name.trim(),
                        category = category.lowercase(),
                        equipment = equipment.lowercase(),
                        primaryMuscles = listOf(muscle.lowercase()),
                        secondaryMuscles = secondaryMuscle.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() },
                        level = level.lowercase(),
                        instructions = instructionsText.lines().map { it.trim() }.filter { it.isNotEmpty() },
                        tips = tipsText.lines().map { it.trim() }.filter { it.isNotEmpty() },
                        met = metText.toDoubleOrNull() ?: if (category == "Cardio") 7.0 else 6.0,
                        isCustom = true,
                    )
                    onSave(newEx)
                },
                enabled = canSave,
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Save Exercise")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
