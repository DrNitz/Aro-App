package com.drnitz.aro.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.drnitz.aro.data.library.OfflineExerciseLibrary
import com.drnitz.aro.data.library.OfflineFoodLibrary
import com.drnitz.aro.data.library.imageUrlAt
import com.drnitz.aro.data.network.OpenFoodFactsClient
import com.drnitz.aro.domain.calculation.MetActivity
import com.drnitz.aro.domain.calculation.MetCatalog
import com.drnitz.aro.domain.calculation.caloriesBurned
import com.drnitz.aro.domain.model.CuratedFoodItem
import com.drnitz.aro.domain.model.Exercise
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientKey
import com.drnitz.aro.domain.model.PlanItem
import com.drnitz.aro.domain.model.PlanItemType
import com.drnitz.aro.domain.model.PlanRepeat
import com.drnitz.aro.domain.model.PlannedExercise
import com.drnitz.aro.domain.model.PlannedFasting
import com.drnitz.aro.domain.model.PlannedFood
import com.drnitz.aro.domain.model.PlannedPill
import com.drnitz.aro.domain.model.PlannedWater
import com.drnitz.aro.notifications.NotificationPreferences
import com.drnitz.aro.domain.model.MedicationReminder
import com.drnitz.aro.theme.AroTheme
import com.drnitz.aro.ui.components.AroDialogSurface
import com.drnitz.aro.ui.components.ExerciseImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Shared picker shell
// ---------------------------------------------------------------------------

@Composable
private fun PlanPickerShell(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AroDialogSurface {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                content()
            }
        }
    }
}

@Composable
private fun PickerBackHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        )
    }
}

// ---------------------------------------------------------------------------
// Food picker
// ---------------------------------------------------------------------------

@Composable
fun PlanFoodPickerDialog(
    targetDate: LocalDate,
    existing: PlanItem? = null,
    foodLibrary: OfflineFoodLibrary,
    openFoodFactsClient: OpenFoodFactsClient? = null,
    onDismiss: () -> Unit,
    onSave: (PlanItem) -> Unit,
) {
    val existingFood = existing?.food
    val initialMeal = existingFood?.meal
        ?: defaultMealFor(existing?.time ?: defaultPlanTime(PlanItemType.FOOD))
    var query by remember { mutableStateOf("") }
    var onlineMode by remember { mutableStateOf(false) }
    var offlineResults by remember { mutableStateOf<List<CuratedFoodItem>>(emptyList()) }
    var onlineResults by remember { mutableStateOf<List<CuratedFoodItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedFood by remember {
        mutableStateOf(
            existing?.let { item ->
                item.food?.let { food -> food.toPickerFood(item.title, food.foodId, item.imageUrl) }
            },
        )
    }
    var gramsText by remember {
        mutableStateOf((existingFood?.grams ?: 100.0).roundToInt().toString())
    }
    var meal by remember { mutableStateOf(initialMeal) }
    var time by remember {
        mutableStateOf(existing?.time ?: defaultPlanTime(PlanItemType.FOOD, initialMeal))
    }
    var repeat by remember { mutableStateOf(existing?.repeat ?: PlanRepeat.NONE) }
    var repeatUntil by remember { mutableStateOf(existing?.repeatUntil) }
    var reminderEnabled by remember { mutableStateOf(existing?.reminderEnabled ?: true) }

    LaunchedEffect(query, onlineMode) {
        if (onlineMode) {
            val clean = query.trim()
            if (clean.length >= 2 && openFoodFactsClient != null) {
                loading = true
                error = null
                delay(400)
                try {
                    onlineResults = openFoodFactsClient.searchProducts(clean)
                    if (onlineResults.isEmpty()) error = "No Open Food Facts results for \"$clean\""
                } catch (e: Exception) {
                    error = e.message ?: "Open Food Facts search failed"
                } finally {
                    loading = false
                }
            } else {
                onlineResults = emptyList()
                error = null
                loading = false
            }
        } else {
            offlineResults = foodLibrary.search(query)
            error = null
            loading = false
        }
    }

    val results = if (onlineMode) onlineResults else offlineResults

    PlanPickerShell(
        title = if (existing == null) "Plan Food" else "Edit Planned Food",
        subtitle = "Choose what you intend to eat on this day.",
        onClose = onDismiss,
    ) {
        val food = selectedFood
        if (food == null) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search foods (e.g. oats, chicken, banana)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !onlineMode,
                    onClick = { onlineMode = false },
                    label = { Text("Offline foods", style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(8.dp),
                )
                if (openFoodFactsClient != null) {
                    FilterChip(
                        selected = onlineMode,
                        onClick = { onlineMode = true },
                        label = { Text("Open Food Facts", style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(8.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                results.isEmpty() -> Box(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = error ?: if (onlineMode) {
                            "Type at least 2 characters to search online."
                        } else {
                            "No foods found. Try a different search."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.id + it.name }) { item ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedFood = item
                                    gramsText = (item.commonServing?.grams ?: 100.0)
                                        .roundToInt().toString()
                                },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PlanItemImage(
                                    type = PlanItemType.FOOD,
                                    imageUrl = item.image,
                                    contentDescription = item.name,
                                    modifier = Modifier.size(46.dp),
                                    cornerRadius = 10.dp,
                                    fallbackIconSize = 22.dp,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${item.brand ?: "Whole food"} • ${item.calories.roundToInt()} kcal / 100g",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Select ${item.name}",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            PickerBackHeader("Set portion", onBack = { selectedFood = null })
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlanItemImage(
                                type = PlanItemType.FOOD,
                                imageUrl = food.image,
                                contentDescription = food.name,
                                modifier = Modifier.size(58.dp),
                                cornerRadius = 12.dp,
                                fallbackIconSize = 26.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = food.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    text = food.brand ?: "Planned food",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "MEAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MealSlot.ORDERED.forEach { slot ->
                            FilterChip(
                                selected = meal == slot,
                                onClick = { meal = slot },
                                label = { Text(slot.label, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = gramsText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 5) gramsText = input
                        },
                        label = { Text("Portion (grams)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(50, 100, 150, 200).forEach { grams ->
                            OutlinedButton(
                                onClick = { gramsText = grams.toString() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            ) {
                                Text("${grams}g", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                item {
                    val grams = gramsText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 100.0
                    val preview = food.toFoodLogEntry(meal = meal, grams = grams)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            PlanMiniStat("Calories", "${preview.calories.roundToInt()} kcal", AroTheme.macroColors.calories)
                            PlanMiniStat("Protein", "${preview.proteinG.roundToInt()}g", AroTheme.macroColors.protein)
                            PlanMiniStat("Carbs", "${preview.nutrients.carbsG.roundToInt()}g", AroTheme.macroColors.carbs)
                            PlanMiniStat("Fat", "${preview.nutrients.fatG.roundToInt()}g", AroTheme.macroColors.fat)
                        }
                    }
                }

                item {
                    PlanScheduleControls(
                        time = time,
                        onTimeChange = { time = it },
                        repeat = repeat,
                        repeatUntil = repeatUntil,
                        startDate = existing?.startDate ?: targetDate,
                        onRepeatChange = { newRepeat, end ->
                            repeat = newRepeat
                            repeatUntil = end
                        },
                        reminderEnabled = reminderEnabled,
                        onReminderEnabledChange = { reminderEnabled = it },
                    )
                }

                item {
                    val grams = gramsText.toDoubleOrNull()?.coerceAtLeast(1.0) ?: 100.0
                    val preview = food.toFoodLogEntry(meal = meal, grams = grams)
                    Button(
                        onClick = {
                            onSave(
                                PlanItem(
                                    id = existing?.id ?: UUID.randomUUID().toString(),
                                    title = food.name,
                                    type = PlanItemType.FOOD,
                                    startDate = existing?.startDate ?: targetDate,
                                    time = time,
                                    repeat = repeat,
                                    repeatUntil = repeatUntil,
                                    reminderEnabled = reminderEnabled,
                                    imageUrl = food.image,
                                    food = PlannedFood(
                                        meal = meal,
                                        grams = grams,
                                        nutrients = preview.nutrients,
                                        foodId = food.id.takeUnless { it == "planned" },
                                        brand = food.brand,
                                    ),
                                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = if (existing == null) "Add food to plan" else "Save food plan",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

private fun PlannedFood.toPickerFood(
    title: String,
    foodId: String?,
    imageUrl: String?,
): CuratedFoodItem {
    val factor = if (grams > 0.0) 100.0 / grams else 1.0
    fun value(key: NutrientKey): Double = nutrients.amountOf(key) * factor
    return CuratedFoodItem(
        id = foodId ?: "planned",
        name = title,
        brand = brand,
        category = "Planned",
        image = imageUrl,
        calories = value(NutrientKey.CALORIES),
        protein = value(NutrientKey.PROTEIN),
        carbs = value(NutrientKey.CARBS),
        fat = value(NutrientKey.FAT),
        saturatedFat = value(NutrientKey.SATURATED_FAT),
        fiber = value(NutrientKey.FIBER),
        sugar = value(NutrientKey.SUGAR),
        sodium = value(NutrientKey.SODIUM),
        potassium = value(NutrientKey.POTASSIUM),
        calcium = value(NutrientKey.CALCIUM),
        magnesium = value(NutrientKey.MAGNESIUM),
        servingSize = 100.0,
    )
}

// ---------------------------------------------------------------------------
// Exercise picker
// ---------------------------------------------------------------------------

@Composable
fun PlanExercisePickerDialog(
    targetDate: LocalDate,
    existing: PlanItem? = null,
    exerciseLibrary: OfflineExerciseLibrary,
    userWeightKg: Double,
    onDismiss: () -> Unit,
    onSave: (PlanItem) -> Unit,
) {
    val existingExercise = existing?.exercise
    var mode by remember {
        mutableStateOf(if (existingExercise?.isStrength == false) ExerciseMode.CARDIO else ExerciseMode.STRENGTH)
    }
    var query by remember { mutableStateOf("") }
    var strengthResults by remember { mutableStateOf<List<Exercise>>(emptyList()) }
    var selectedExercise by remember {
        mutableStateOf(existingExercise?.takeIf { it.isStrength }?.toPickerExercise(existing.title))
    }
    var selectedActivity by remember {
        mutableStateOf(existingExercise?.takeIf { !it.isStrength }?.toPickerActivity(existing.title))
    }
    var durationText by remember {
        mutableStateOf((existingExercise?.durationMinutes ?: 30).toString())
    }
    var time by remember {
        mutableStateOf(existing?.time ?: defaultPlanTime(PlanItemType.EXERCISE))
    }
    var repeat by remember { mutableStateOf(existing?.repeat ?: PlanRepeat.NONE) }
    var repeatUntil by remember { mutableStateOf(existing?.repeatUntil) }
    var reminderEnabled by remember { mutableStateOf(existing?.reminderEnabled ?: true) }

    LaunchedEffect(query, mode) {
        if (mode == ExerciseMode.STRENGTH) {
            strengthResults = exerciseLibrary.search(query = query)
        }
    }

    val cardioResults = remember(query, mode) {
        if (mode != ExerciseMode.CARDIO) emptyList() else {
            val q = query.trim().lowercase()
            MetCatalog.all.filter {
                q.isEmpty() ||
                    it.name.lowercase().contains(q) ||
                    it.category.lowercase().contains(q)
            }
        }
    }

    PlanPickerShell(
        title = if (existing == null) "Plan Exercise" else "Edit Planned Exercise",
        subtitle = "Choose a strength exercise or a cardio activity.",
        onClose = onDismiss,
    ) {
        val selected = selectedExercise ?: selectedActivity
        if (selected == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == ExerciseMode.STRENGTH,
                    onClick = {
                        mode = ExerciseMode.STRENGTH
                        selectedActivity = null
                    },
                    label = { Text("Strength", style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                    shape = RoundedCornerShape(8.dp),
                )
                FilterChip(
                    selected = mode == ExerciseMode.CARDIO,
                    onClick = {
                        mode = ExerciseMode.CARDIO
                        selectedExercise = null
                    },
                    label = { Text("Cardio & sports", style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(8.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(if (mode == ExerciseMode.STRENGTH) "Search exercises..." else "Search activities...")
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (mode == ExerciseMode.STRENGTH) {
                if (strengthResults.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No exercises found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(strengthResults.take(100), key = { it.id }) { exercise ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedExercise = exercise },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    ExerciseImage(
                                        imageUrl = exercise.imageUrlAt(0),
                                        contentDescription = exercise.name,
                                        modifier = Modifier.size(42.dp),
                                        cornerRadius = 10.dp,
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exercise.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${exercise.targetMuscleSummary} • ${exercise.equipment ?: "Bodyweight"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Select ${exercise.name}",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                if (cardioResults.isEmpty()) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No activities found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(cardioResults, key = { it.id }) { activity ->
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
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = activity.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
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
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = AroTheme.macroColors.cardio,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            PickerBackHeader("Set exercise", onBack = {
                selectedExercise = null
                selectedActivity = null
            })
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    val title = selectedExercise?.name ?: selectedActivity?.name.orEmpty()
                    val subtitle = selectedExercise?.let {
                        "${it.targetMuscleSummary} • ${it.equipment ?: "Bodyweight"}"
                    } ?: selectedActivity?.let { "${it.category} • MET ${it.met}" }.orEmpty()
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ExerciseImage(
                                imageUrl = selectedExercise?.imageUrlAt(0)
                                    ?: existing?.imageUrl?.takeIf {
                                        selectedExercise?.id == existing.exercise?.exerciseId
                                    },
                                contentDescription = title,
                                modifier = Modifier.size(58.dp),
                                cornerRadius = 12.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = durationText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 4) durationText = input
                        },
                        label = { Text("Duration (minutes)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(15, 30, 45, 60).forEach { minutes ->
                            OutlinedButton(
                                onClick = { durationText = minutes.toString() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            ) {
                                Text("${minutes}m", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                item {
                    val duration = durationText.toIntOrNull()?.coerceAtLeast(1) ?: 30
                    val met = selectedActivity?.met ?: 6.0
                    val burn = caloriesBurned(met, userWeightKg, duration)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            PlanMiniStat("Duration", "$duration min", MaterialTheme.colorScheme.primary)
                            PlanMiniStat("MET", "%.1f".format(met), AroTheme.macroColors.calories)
                            PlanMiniStat("Estimated burn", "$burn kcal", AroTheme.macroColors.cardio)
                        }
                    }
                }

                item {
                    PlanScheduleControls(
                        time = time,
                        onTimeChange = { time = it },
                        repeat = repeat,
                        repeatUntil = repeatUntil,
                        startDate = existing?.startDate ?: targetDate,
                        onRepeatChange = { newRepeat, end ->
                            repeat = newRepeat
                            repeatUntil = end
                        },
                        reminderEnabled = reminderEnabled,
                        onReminderEnabledChange = { reminderEnabled = it },
                    )
                }

                item {
                    val duration = durationText.toIntOrNull()?.coerceAtLeast(1) ?: 30
                    Button(
                        onClick = {
                            val exercise = selectedExercise
                            val activity = selectedActivity
                            val imageUrl = when {
                                exercise == null -> null
                                exercise.id == existing?.exercise?.exerciseId && existing.imageUrl != null ->
                                    existing.imageUrl
                                else -> exercise.imageUrlAt(0)
                            }
                            onSave(
                                PlanItem(
                                    id = existing?.id ?: UUID.randomUUID().toString(),
                                    title = exercise?.name ?: activity?.name.orEmpty(),
                                    type = PlanItemType.EXERCISE,
                                    startDate = existing?.startDate ?: targetDate,
                                    time = time,
                                    repeat = repeat,
                                    repeatUntil = repeatUntil,
                                    reminderEnabled = reminderEnabled,
                                    imageUrl = imageUrl,
                                    exercise = if (exercise != null) {
                                        PlannedExercise(
                                            exerciseId = exercise.id,
                                            isStrength = true,
                                            durationMinutes = duration,
                                            met = 6.0,
                                            category = exercise.category,
                                            muscle = exercise.primaryMuscles.firstOrNull(),
                                            equipment = exercise.equipment,
                                        )
                                    } else {
                                        PlannedExercise(
                                            exerciseId = null,
                                            isStrength = false,
                                            durationMinutes = duration,
                                            met = activity?.met ?: 6.0,
                                            category = activity?.category.orEmpty(),
                                        )
                                    },
                                    createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = if (existing == null) "Add exercise to plan" else "Save exercise plan",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

private enum class ExerciseMode { STRENGTH, CARDIO }

private fun PlannedExercise.toPickerExercise(title: String): Exercise = Exercise(
    id = exerciseId ?: "planned",
    name = title,
    category = category,
    primaryMuscles = listOfNotNull(muscle),
    equipment = equipment,
)

private fun PlannedExercise.toPickerActivity(title: String): MetActivity = MetActivity(
    id = "planned",
    name = title,
    category = category.ifBlank { "Planned activity" },
    met = met,
)

// ---------------------------------------------------------------------------
// Pill picker
// ---------------------------------------------------------------------------

@Composable
fun PlanPillPickerDialog(
    targetDate: LocalDate,
    existing: PlanItem? = null,
    onDismiss: () -> Unit,
    onSave: (PlanItem) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { NotificationPreferences(context) }
    val savedMedsFlow = remember(prefs) {
        prefs.settings.map { it.medicationReminders }
    }
    val savedMeds by savedMedsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    var name by remember { mutableStateOf(existing?.title.orEmpty()) }
    var dosage by remember { mutableStateOf(existing?.pill?.dosage.orEmpty()) }
    var savedReminderId by remember { mutableStateOf(existing?.pill?.savedReminderId) }
    var time by remember {
        mutableStateOf(existing?.time ?: defaultPlanTime(PlanItemType.PILL))
    }
    var repeat by remember { mutableStateOf(existing?.repeat ?: PlanRepeat.NONE) }
    var repeatUntil by remember { mutableStateOf(existing?.repeatUntil) }
    var reminderEnabled by remember { mutableStateOf(existing?.reminderEnabled ?: true) }

    PlanPickerShell(
        title = if (existing == null) "Plan Pill" else "Edit Planned Pill",
        subtitle = "Saved medications and supplements, or a one-off entry.",
        onClose = onDismiss,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (savedMeds.isNotEmpty()) {
                item {
                    Text(
                        text = "SAVED MEDICATIONS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(savedMeds, key = { it.id }) { reminder ->
                            SavedPillCard(
                                reminder = reminder,
                                selected = savedReminderId == reminder.id,
                                onClick = {
                                    savedReminderId = reminder.id
                                    name = reminder.name
                                    dosage = reminder.dosage
                                    time = LocalTime.of(reminder.hour, reminder.minute)
                                },
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        savedReminderId = null
                    },
                    label = { Text("Medication or supplement name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            item {
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dose / instructions (optional)") },
                    placeholder = { Text("e.g. 1 tablet, 1000 IU") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Planned pills are separate from your daily medication reminders.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                PlanScheduleControls(
                    time = time,
                    onTimeChange = { time = it },
                    repeat = repeat,
                    repeatUntil = repeatUntil,
                    startDate = existing?.startDate ?: targetDate,
                    onRepeatChange = { newRepeat, end ->
                        repeat = newRepeat
                        repeatUntil = end
                    },
                    reminderEnabled = reminderEnabled,
                    onReminderEnabledChange = { reminderEnabled = it },
                )
            }

            item {
                Button(
                    onClick = {
                        onSave(
                            PlanItem(
                                id = existing?.id ?: UUID.randomUUID().toString(),
                                title = name.trim(),
                                type = PlanItemType.PILL,
                                startDate = existing?.startDate ?: targetDate,
                                time = time,
                                repeat = repeat,
                                repeatUntil = repeatUntil,
                                reminderEnabled = reminderEnabled,
                                imageUrl = existing?.imageUrl,
                                pill = PlannedPill(
                                    dosage = dosage.trim(),
                                    savedReminderId = savedReminderId,
                                ),
                                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            ),
                        )
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = if (existing == null) "Add pill to plan" else "Save pill plan",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Fasting picker
// ---------------------------------------------------------------------------

@Composable
fun PlanFastingPickerDialog(
    targetDate: LocalDate,
    existing: PlanItem? = null,
    onDismiss: () -> Unit,
    onSave: (PlanItem) -> Unit,
) {
    var hoursText by remember {
        mutableStateOf((existing?.fasting?.targetHours ?: 16).toString())
    }
    var time by remember {
        mutableStateOf(existing?.time ?: defaultPlanTime(PlanItemType.FASTING))
    }
    var repeat by remember { mutableStateOf(existing?.repeat ?: PlanRepeat.NONE) }
    var repeatUntil by remember { mutableStateOf(existing?.repeatUntil) }
    var reminderEnabled by remember { mutableStateOf(existing?.reminderEnabled ?: true) }

    val protocols = listOf(
        16 to "16:8 Standard",
        18 to "18:6 Accelerated",
        20 to "20:4 Warrior",
        24 to "24:0 OMAD",
    )

    PlanPickerShell(
        title = if (existing == null) "Plan Fasting" else "Edit Planned Fast",
        subtitle = "Plan when to start. Aro will not record a fast until you actually start one.",
        onClose = onDismiss,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "PROTOCOL",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                protocols.forEach { (hours, label) ->
                    val selected = hoursText.toIntOrNull() == hours
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) {
                                AroTheme.macroColors.fasting.copy(alpha = 0.14f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { hoursText = hours.toString() },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = AroTheme.macroColors.fasting,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                                Text(
                                    text = "${hours}h fast • reminder on the planned day",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = AroTheme.macroColors.fasting,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { input ->
                        if (input.length <= 2 && input.all { it.isDigit() }) hoursText = input
                    },
                    label = { Text("Fasting hours") },
                    suffix = { Text("h") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
            }

            item {
                PlanScheduleControls(
                    time = time,
                    onTimeChange = { time = it },
                    repeat = repeat,
                    repeatUntil = repeatUntil,
                    startDate = existing?.startDate ?: targetDate,
                    onRepeatChange = { newRepeat, end ->
                        repeat = newRepeat
                        repeatUntil = end
                    },
                    reminderEnabled = reminderEnabled,
                    onReminderEnabledChange = { reminderEnabled = it },
                )
            }

            item {
                val hours = hoursText.toIntOrNull()?.coerceIn(1, 72) ?: 16
                Button(
                    onClick = {
                        onSave(
                            PlanItem(
                                id = existing?.id ?: UUID.randomUUID().toString(),
                                title = "${hours}h fast",
                                type = PlanItemType.FASTING,
                                startDate = existing?.startDate ?: targetDate,
                                time = time,
                                repeat = repeat,
                                repeatUntil = repeatUntil,
                                reminderEnabled = reminderEnabled,
                                imageUrl = existing?.imageUrl,
                                fasting = PlannedFasting(targetHours = hours),
                                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = if (existing == null) "Add fast to plan" else "Save fast plan",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Water picker
// ---------------------------------------------------------------------------

@Composable
fun PlanWaterPickerDialog(
    targetDate: LocalDate,
    existing: PlanItem? = null,
    onDismiss: () -> Unit,
    onSave: (PlanItem) -> Unit,
) {
    var mlText by remember {
        mutableStateOf((existing?.water?.amountMl ?: 500).toString())
    }
    var time by remember {
        mutableStateOf(existing?.time ?: defaultPlanTime(PlanItemType.WATER))
    }
    var repeat by remember { mutableStateOf(existing?.repeat ?: PlanRepeat.NONE) }
    var repeatUntil by remember { mutableStateOf(existing?.repeatUntil) }
    var reminderEnabled by remember { mutableStateOf(existing?.reminderEnabled ?: true) }

    PlanPickerShell(
        title = if (existing == null) "Plan Water" else "Edit Planned Water",
        subtitle = "Set a hydration intention. Water is only logged after you confirm it.",
        onClose = onDismiss,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = AroTheme.macroColors.water,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hydration plan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "Planned water stays separate from the diary until confirmed.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = mlText,
                    onValueChange = { input ->
                        if (input.length <= 5 && input.all { it.isDigit() }) mlText = input
                    },
                    label = { Text("Planned volume") },
                    suffix = { Text("mL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(250, 500, 750, 1000).forEach { amount ->
                        OutlinedButton(
                            onClick = { mlText = amount.toString() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        ) {
                            Text("${amount}mL", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            item {
                PlanScheduleControls(
                    time = time,
                    onTimeChange = { time = it },
                    repeat = repeat,
                    repeatUntil = repeatUntil,
                    startDate = existing?.startDate ?: targetDate,
                    onRepeatChange = { newRepeat, end ->
                        repeat = newRepeat
                        repeatUntil = end
                    },
                    reminderEnabled = reminderEnabled,
                    onReminderEnabledChange = { reminderEnabled = it },
                )
            }

            item {
                val amount = mlText.toIntOrNull()?.coerceAtLeast(1) ?: 500
                Button(
                    onClick = {
                        onSave(
                            PlanItem(
                                id = existing?.id ?: UUID.randomUUID().toString(),
                                title = "$amount mL water",
                                type = PlanItemType.WATER,
                                startDate = existing?.startDate ?: targetDate,
                                time = time,
                                repeat = repeat,
                                repeatUntil = repeatUntil,
                                reminderEnabled = reminderEnabled,
                                imageUrl = existing?.imageUrl,
                                water = PlannedWater(amountMl = amount),
                                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = if (existing == null) "Add water to plan" else "Save water plan",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SavedPillCard(
    reminder: MedicationReminder,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        modifier = Modifier
            .width(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = reminder.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (reminder.dosage.isNotBlank()) {
                Text(
                    text = reminder.dosage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "%02d:%02d".format(reminder.hour, reminder.minute),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Small shared UI
// ---------------------------------------------------------------------------

@Composable
internal fun PlanMiniStat(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
    }
}

@Composable
internal fun PlanTypeIcon(type: PlanItemType, modifier: Modifier = Modifier) {
    val icon = when (type) {
        PlanItemType.FOOD -> Icons.Default.Restaurant
        PlanItemType.EXERCISE -> Icons.Default.FitnessCenter
        PlanItemType.PILL -> Icons.Default.Medication
        PlanItemType.FASTING -> Icons.Default.Timer
        PlanItemType.WATER -> Icons.Default.WaterDrop
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = when (type) {
            PlanItemType.FOOD -> AroTheme.macroColors.calories
            PlanItemType.EXERCISE -> AroTheme.macroColors.cardio
            PlanItemType.PILL -> MaterialTheme.colorScheme.primary
            PlanItemType.FASTING -> AroTheme.macroColors.fasting
            PlanItemType.WATER -> AroTheme.macroColors.water
        },
        modifier = modifier,
    )
}
