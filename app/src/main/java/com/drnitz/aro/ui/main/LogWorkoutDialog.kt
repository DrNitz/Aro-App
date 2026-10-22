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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.drnitz.aro.data.library.OfflineExerciseLibrary
import com.drnitz.aro.data.library.imageUrlAt
import com.drnitz.aro.data.library.imageUrls
import com.drnitz.aro.domain.calculation.KCAL_PER_KG_TONNAGE
import com.drnitz.aro.domain.calculation.MetActivity
import com.drnitz.aro.domain.calculation.MetCatalog
import com.drnitz.aro.domain.calculation.OneRepMaxCalculator
import com.drnitz.aro.domain.calculation.caloriesBurned
import com.drnitz.aro.domain.model.Exercise
import com.drnitz.aro.domain.model.SetEntry
import com.drnitz.aro.domain.model.WorkoutLog
import com.drnitz.aro.domain.model.WorkoutType
import com.drnitz.aro.theme.AroTheme
import com.drnitz.aro.ui.components.AroLoggingDateLabel
import com.drnitz.aro.ui.components.ExerciseImage
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

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

internal fun formatWorkoutDecimal(value: Double, decimals: Int = 1): String {
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
