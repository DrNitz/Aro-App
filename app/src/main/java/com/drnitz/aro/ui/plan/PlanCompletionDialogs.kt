package com.drnitz.aro.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.PlanOccurrence
import com.drnitz.aro.theme.AroTheme
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt

private val PLAN_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

/**
 * Confirmation before a planned food becomes a real diary entry.
 *
 * The plan's nutrients were frozen at plan time. Confirming a different portion scales
 * those frozen values; it does not re-read the food library, so the plan and the log
 * cannot drift apart silently.
 */
@Composable
fun PlanFoodCompletionDialog(
    occurrence: PlanOccurrence,
    onDismiss: () -> Unit,
    onConfirm: (FoodLogEntry) -> Unit,
) {
    val planned = occurrence.item.food ?: return
    var gramsText by remember {
        mutableStateOf(planned.grams.roundToInt().toString())
    }
    val grams = gramsText.toDoubleOrNull()?.coerceAtLeast(1.0)
    val factor = if (grams != null && planned.grams > 0.0) grams / planned.grams else 0.0
    val nutrients = if (factor > 0.0) planned.nutrients * factor else NutrientTotals.EMPTY

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log planned food?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = occurrence.item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "Planned ${planned.meal.label.lowercase()} for " +
                        occurrence.date.format(PLAN_DATE_FORMAT) +
                        ". Confirming creates a real food log for that date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = gramsText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 5) gramsText = input
                    },
                    label = { Text("Portion (grams)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    PlanMiniStat(
                        label = "Calories",
                        value = "${nutrients.calories.roundToInt()} kcal",
                        color = AroTheme.macroColors.calories,
                    )
                    PlanMiniStat(
                        label = "Protein",
                        value = "${nutrients.proteinG.roundToInt()}g",
                        color = AroTheme.macroColors.protein,
                    )
                    PlanMiniStat(
                        label = "Carbs",
                        value = "${nutrients.carbsG.roundToInt()}g",
                        color = AroTheme.macroColors.carbs,
                    )
                    PlanMiniStat(
                        label = "Fat",
                        value = "${nutrients.fatG.roundToInt()}g",
                        color = AroTheme.macroColors.fat,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val safeGrams = grams ?: return@TextButton
                    if (planned.grams <= 0.0) return@TextButton
                    val scaled = planned.nutrients * (safeGrams / planned.grams)
                    val entry = FoodLogEntry(
                        id = UUID.randomUUID().toString(),
                        name = occurrence.item.title,
                        meal = planned.meal,
                        grams = safeGrams,
                        nutrients = scaled,
                        brand = planned.brand,
                        foodId = planned.foodId,
                        isCustom = planned.isCustom,
                    )
                    onConfirm(entry)
                },
                enabled = grams != null && planned.grams > 0.0,
            ) {
                Text("Log as eaten")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun PlanPillCompletionDialog(
    occurrence: PlanOccurrence,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val dosage = occurrence.item.pill?.dosage.orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark pill as taken?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = occurrence.item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                if (dosage.isNotBlank()) {
                    Text(
                        text = dosage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = "This marks the occurrence on " +
                        occurrence.date.format(PLAN_DATE_FORMAT) +
                        " as done. It does not change your daily medication reminders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Mark taken") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun PlanWaterCompletionDialog(
    occurrence: PlanOccurrence,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val plannedMl = occurrence.item.water?.amountMl ?: 250
    var mlText by remember { mutableStateOf(plannedMl.toString()) }
    val amount = mlText.toIntOrNull()?.coerceAtLeast(1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log planned water?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = occurrence.item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "Planned for " + occurrence.date.format(PLAN_DATE_FORMAT) +
                        ". Planned water has not been counted yet; confirming adds the amount " +
                        "below to that day's real diary.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = mlText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 5) mlText = input
                    },
                    label = { Text("Actual volume") },
                    suffix = { Text("mL") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { amount?.let(onConfirm) },
                enabled = amount != null,
            ) {
                Text("Add to diary")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun PlanFastingCompletionDialog(
    occurrence: PlanOccurrence,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val plannedHours = occurrence.item.fasting?.targetHours ?: 16
    var hoursText by remember { mutableStateOf(plannedHours.toString()) }
    val hours = hoursText.toIntOrNull()?.coerceIn(1, 72)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start this fast now?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = occurrence.item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "The planned start was " + occurrence.date.format(PLAN_DATE_FORMAT) +
                        " at " + occurrence.item.time.format(DateTimeFormatter.ofPattern("HH:mm")) +
                        ". Confirming starts a live fast from now; Aro does not backdate a fast.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 2) hoursText = input
                    },
                    label = { Text("Fasting target") },
                    suffix = { Text("h") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(16, 18, 20, 24).forEach { preset ->
                        TextButton(onClick = { hoursText = preset.toString() }) {
                            Text("${preset}h", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { hours?.let(onConfirm) },
                enabled = hours != null,
            ) {
                Text("Start fast now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
fun PlanLoadingDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Loading plan...", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
