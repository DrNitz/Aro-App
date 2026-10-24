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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.drnitz.aro.data.library.OfflineExerciseLibrary
import com.drnitz.aro.data.library.OfflineFoodLibrary
import com.drnitz.aro.data.network.OpenFoodFactsClient
import com.drnitz.aro.domain.calculation.plannedBurnCalories
import com.drnitz.aro.domain.calculation.plannedIntakeCalories
import com.drnitz.aro.domain.model.PlanDaySummary
import com.drnitz.aro.domain.model.PlanItem
import com.drnitz.aro.domain.model.PlanItemType
import com.drnitz.aro.domain.model.PlanOccurrence
import com.drnitz.aro.domain.model.PlanRepeat
import com.drnitz.aro.theme.AroTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

/**
 * Full-screen planner for one future date.
 *
 * The sheet is a view over the plan repository, not a form that commits at the end:
 * each add/edit/delete is persisted immediately, so closing the sheet never loses a
 * user's work.
 */
@Composable
fun DayPlanSheet(
    date: LocalDate,
    formattedDate: String,
    items: List<PlanOccurrence>,
    summary: PlanDaySummary,
    targetCalories: Int,
    userWeightKg: Double,
    foodLibrary: OfflineFoodLibrary,
    exerciseLibrary: OfflineExerciseLibrary,
    openFoodFactsClient: OpenFoodFactsClient? = null,
    canComplete: Boolean = false,
    initialItemId: String? = null,
    onDismiss: () -> Unit,
    onSave: (PlanItem) -> Unit,
    onDelete: (String) -> Unit,
    onComplete: (PlanOccurrence) -> Unit,
) {
    val requestedType = remember(initialItemId, items) {
        initialItemId?.let { id ->
            items.firstOrNull { it.item.id == id }?.item?.type
        }
    }
    var activeType by remember(initialItemId) {
        mutableStateOf(requestedType ?: PlanItemType.FOOD)
    }
    var pickerType by remember { mutableStateOf<PlanItemType?>(null) }
    var editingItem by remember { mutableStateOf<PlanItem?>(null) }
    var deleteTarget by remember { mutableStateOf<PlanItem?>(null) }

    val visibleItems = items.filter { it.item.type == activeType }
        .sortedWith(compareBy({ it.item.time }, { it.item.title }))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
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
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Plan this day",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close planner")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                PlanSummaryCard(
                    summary = summary,
                    targetCalories = targetCalories,
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PlanItemType.entries, key = { it.name }) { type ->
                        val count = items.count { it.item.type == type }
                        FilterChip(
                            selected = activeType == type,
                            onClick = { activeType = type },
                            label = {
                                Text(
                                    text = if (count > 0) "${type.label} ($count)" else type.label,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            leadingIcon = {
                                PlanTypeIcon(type, Modifier.size(15.dp))
                            },
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    if (visibleItems.isEmpty()) {
                        item {
                            EmptyPlanType(
                                type = activeType,
                                hasFutureDate = !canComplete,
                            )
                        }
                    }

                    items(visibleItems, key = { it.item.id + it.date }) { occurrence ->
                        PlanOccurrenceCard(
                            occurrence = occurrence,
                            userWeightKg = userWeightKg,
                            imageUrl = occurrence.item.imageUrl,
                            canComplete = canComplete,
                            onComplete = { onComplete(occurrence) },
                            onEdit = {
                                editingItem = occurrence.item
                                pickerType = occurrence.item.type
                            },
                            onDelete = { deleteTarget = occurrence.item },
                        )
                    }

                    item {
                        OutlinedButton(
                            onClick = {
                                editingItem = null
                                pickerType = activeType
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("Add ${activeType.label.lowercase()} to this day")
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    RoundedCornerShape(10.dp),
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Plans are estimates. Reminders follow each item's own time and repeat schedule; tap a reminder to open this day.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (pickerType != null) {
        val existing = editingItem
        when (pickerType) {
            PlanItemType.FOOD -> PlanFoodPickerDialog(
                targetDate = date,
                existing = existing,
                foodLibrary = foodLibrary,
                openFoodFactsClient = openFoodFactsClient,
                onDismiss = { pickerType = null; editingItem = null },
                onSave = {
                    onSave(it)
                    pickerType = null
                    editingItem = null
                },
            )
            PlanItemType.EXERCISE -> PlanExercisePickerDialog(
                targetDate = date,
                existing = existing,
                exerciseLibrary = exerciseLibrary,
                userWeightKg = userWeightKg,
                onDismiss = { pickerType = null; editingItem = null },
                onSave = {
                    onSave(it)
                    pickerType = null
                    editingItem = null
                },
            )
            PlanItemType.PILL -> PlanPillPickerDialog(
                targetDate = date,
                existing = existing,
                onDismiss = { pickerType = null; editingItem = null },
                onSave = {
                    onSave(it)
                    pickerType = null
                    editingItem = null
                },
            )
            PlanItemType.FASTING -> PlanFastingPickerDialog(
                targetDate = date,
                existing = existing,
                onDismiss = { pickerType = null; editingItem = null },
                onSave = {
                    onSave(it)
                    pickerType = null
                    editingItem = null
                },
            )
            PlanItemType.WATER -> PlanWaterPickerDialog(
                targetDate = date,
                existing = existing,
                onDismiss = { pickerType = null; editingItem = null },
                onSave = {
                    onSave(it)
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
                            "Deleting it removes the whole series."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(target.id)
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
}

@Composable
internal fun PlanSummaryCard(
    summary: PlanDaySummary,
    targetCalories: Int,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                PlanMiniStat(
                    label = "Planned intake",
                    value = "${summary.plannedCalories} kcal",
                    color = AroTheme.macroColors.calories,
                )
                PlanMiniStat(
                    label = "Planned burn",
                    value = "${summary.plannedBurnCalories} kcal",
                    color = AroTheme.macroColors.cardio,
                )
                PlanMiniStat(
                    label = "Net",
                    value = "${summary.plannedNetCalories} kcal",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            val delta = summary.plannedNetCalories - targetCalories
            val hasCaloriePlan = summary.plannedCalories > 0 || summary.plannedBurnCalories > 0
            val comparison = when {
                targetCalories <= 0 || !hasCaloriePlan -> ""
                delta <= 0 -> "${-delta} kcal under target"
                else -> "$delta kcal over target"
            }
            Text(
                text = "Daily target: $targetCalories kcal" +
                    if (comparison.isBlank()) "" else "  •  $comparison",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "P ${summary.plannedProteinG.roundToInt()}g  " +
                    "C ${summary.plannedCarbsG.roundToInt()}g  " +
                    "F ${summary.plannedFatG.roundToInt()}g",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            if (summary.plannedWaterMl > 0 || summary.plannedFastingHours > 0) {
                Text(
                    text = buildList {
                        if (summary.plannedFastingHours > 0) {
                            add("${summary.plannedFastingHours}h fasting")
                        }
                        if (summary.plannedWaterMl > 0) {
                            add("${summary.plannedWaterMl} mL water")
                        }
                    }.joinToString("  •  ") + " planned (not logged)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
internal fun PlanOccurrenceCard(
    occurrence: PlanOccurrence,
    userWeightKg: Double,
    imageUrl: String? = null,
    canComplete: Boolean,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    canSkip: Boolean = false,
    onSkip: () -> Unit = {},
    onUndo: () -> Unit = {},
) {
    val item = occurrence.item
    val completed = occurrence.isCompleted
    val skipped = occurrence.isSkipped
    val handled = completed || skipped
    val detail = when (item.type) {
        PlanItemType.FOOD -> item.food?.let {
            "${it.meal.label} • ${it.grams.roundToInt()}g"
        }.orEmpty()
        PlanItemType.EXERCISE -> item.exercise?.let {
            "${it.durationMinutes} min • ~${occurrence.plannedBurnCalories(userWeightKg)} kcal burn"
        }.orEmpty()
        PlanItemType.PILL -> item.pill?.dosage.orEmpty().ifBlank { "No dose entered" }
        PlanItemType.FASTING -> "${item.fasting?.targetHours ?: 16}h fast • start when ready"
        PlanItemType.WATER -> "${item.water?.amountMl ?: 250} mL • confirm before it counts"
    }
    val calories = when (item.type) {
        PlanItemType.FOOD -> "${occurrence.plannedIntakeCalories} kcal"
        PlanItemType.EXERCISE -> "+${occurrence.plannedBurnCalories(userWeightKg)} kcal"
        PlanItemType.PILL,
        PlanItemType.FASTING,
        PlanItemType.WATER,
        -> ""
    }
    val schedule = buildString {
        append(item.time.format(DateTimeFormatter.ofPattern("HH:mm")))
        if (item.repeat != PlanRepeat.NONE) {
            append(" • ")
            append(item.repeat.label)
            item.repeatUntil?.let { append(" until ${it.format(SHORT_DATE)}") }
        }
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (handled) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onEdit),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlanItemImage(
                    type = item.type,
                    imageUrl = imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier.size(42.dp),
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
                if (calories.isNotBlank()) {
                    Text(
                        text = calories,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (item.type == PlanItemType.EXERCISE) {
                            AroTheme.macroColors.cardio
                        } else {
                            AroTheme.macroColors.calories
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                if (completed) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = AroTheme.macroColors.clinicalOptimal,
                        modifier = Modifier.size(17.dp),
                    )
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AroTheme.macroColors.clinicalOptimal,
                    )
                    TextButton(
                        onClick = onUndo,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    ) {
                        Text("Undo", style = MaterialTheme.typography.labelSmall)
                    }
                } else if (skipped) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                    Text(
                        text = "Skipped",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = onUndo,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    ) {
                        Text("Undo", style = MaterialTheme.typography.labelSmall)
                    }
                } else if (canComplete) {
                    OutlinedButton(
                        onClick = onComplete,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Done", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                if (!completed && !skipped && canSkip) {
                    TextButton(
                        onClick = onSkip,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    ) {
                        Text("Skip", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.width(2.dp))
                }

                IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit ${item.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete ${item.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPlanType(type: PlanItemType, hasFutureDate: Boolean) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlanTypeIcon(type, Modifier.size(34.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = "No planned ${type.label.lowercase()} yet",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                text = if (hasFutureDate) {
                    "Add an estimate so this day is ready when it arrives."
                } else {
                    "Add anything you still intend to eat, do, or take."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
