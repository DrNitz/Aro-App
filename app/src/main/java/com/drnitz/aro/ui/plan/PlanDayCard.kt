package com.drnitz.aro.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drnitz.aro.domain.calculation.plannedBurnCalories
import com.drnitz.aro.domain.calculation.plannedIntakeCalories
import com.drnitz.aro.domain.model.PlanDaySummary
import com.drnitz.aro.domain.model.PlanItemType
import com.drnitz.aro.domain.model.PlanOccurrence
import com.drnitz.aro.theme.AroTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Diary summary of what is planned on the selected day.
 *
 * This card never feeds the calorie rings or the actual calorie balance; it only shows
 * estimates until the user marks an occurrence done, at which point a real food or
 * workout log is created and the regular Diary totals take over.
 */
@Composable
fun PlanDayCard(
    date: LocalDate,
    today: LocalDate,
    items: List<PlanOccurrence>,
    summary: PlanDaySummary,
    targetCalories: Int,
    userWeightKg: Double,
    onOpenPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val future = date.isAfter(today)
    if (items.isEmpty() && !future) return

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(AroTheme.macroColors.calories.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = AroTheme.macroColors.calories,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (future) "PLANNED FOR THIS DAY" else "PLANNED DAY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.1.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (future) {
                            date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
                        } else {
                            "Plans not yet marked done"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                TextButton(onClick = onOpenPlan) {
                    Text(if (future) "Plan" else "Open")
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            if (items.isEmpty()) {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Nothing planned yet. Add meals, a workout, or pills so this day is ready when it arrives.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(10.dp))
                OutlinedButton(
                    onClick = onOpenPlan,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Plan this day")
                }
            } else {
                Spacer(Modifier.size(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    PlanMiniStat(
                        label = "Planned in",
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

                Spacer(Modifier.size(4.dp))
                val delta = summary.plannedNetCalories - targetCalories
                val hasCaloriePlan = summary.plannedCalories > 0 || summary.plannedBurnCalories > 0
                val comparison = when {
                    targetCalories <= 0 || !hasCaloriePlan -> ""
                    delta <= 0 -> " • ${-delta} kcal under target"
                    else -> " • $delta kcal over target"
                }
                Text(
                    text = "Target $targetCalories kcal$comparison • " +
                        "P ${summary.plannedProteinG.roundToInt()}g " +
                        "C ${summary.plannedCarbsG.roundToInt()}g F ${summary.plannedFatG.roundToInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.size(8.dp))
                items.take(3).forEach { occurrence ->
                    val item = occurrence.item
                    val detail = when (item.type) {
                        PlanItemType.FOOD ->
                            "${item.food?.meal?.label.orEmpty()} • " +
                                "${(item.food?.grams ?: 0.0).roundToInt()}g"
                        PlanItemType.EXERCISE ->
                            "${item.exercise?.durationMinutes ?: 0} min • " +
                                "~${occurrence.plannedBurnCalories(userWeightKg)} kcal"
                        PlanItemType.PILL ->
                            item.pill?.dosage.orEmpty().ifBlank { "Planned" }
                        PlanItemType.FASTING ->
                            "${item.fasting?.targetHours ?: 16}h fast"
                        PlanItemType.WATER ->
                            "${item.water?.amountMl ?: 250} mL"
                    }
                    val right = if (occurrence.isCompleted) {
                        "Done"
                    } else {
                        when (item.type) {
                            PlanItemType.FOOD -> "${occurrence.plannedIntakeCalories} kcal"
                            PlanItemType.EXERCISE ->
                                "+${occurrence.plannedBurnCalories(userWeightKg)} kcal"
                            PlanItemType.PILL,
                            PlanItemType.FASTING,
                            PlanItemType.WATER,
                            -> ""
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlanTypeIcon(item.type, Modifier.size(15.dp))
                        Spacer(Modifier.width(7.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "$detail • ${item.time.format(TIME_FORMAT)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (right.isNotBlank()) {
                            Text(
                                text = right,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when {
                                    occurrence.isCompleted -> AroTheme.macroColors.clinicalOptimal
                                    item.type == PlanItemType.EXERCISE -> AroTheme.macroColors.cardio
                                    else -> AroTheme.macroColors.calories
                                },
                            )
                        }
                    }
                }

                if (items.size > 3) {
                    Text(
                        text = "+${items.size - 3} more planned item${if (items.size - 3 == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
        }
    }
}
