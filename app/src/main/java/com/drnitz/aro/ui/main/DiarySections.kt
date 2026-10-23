package com.drnitz.aro.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.drnitz.aro.ClinicalAudit
import com.drnitz.aro.IntakeHistory
import com.drnitz.aro.domain.calculation.EnergyPlan
import com.drnitz.aro.domain.calculation.OneRepMaxCalculator
import com.drnitz.aro.domain.clinical.MealCoverage
import com.drnitz.aro.domain.model.EnergyBalance
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.WorkoutLog
import com.drnitz.aro.theme.AroTheme
import com.drnitz.aro.ui.components.ConcentricMacroRings
import com.drnitz.aro.domain.calculation.caloriesBurned
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun DiarySections(
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
                    waterLogCount = state.waterLogCount,
                    planningMode = planningMode,
                    plannedWaterMl = planSuccess
                        ?.takeIf { it.selectedDate == state.selectedDate }
                        ?.summary?.plannedWaterMl ?: 0,
                    onAddWater = onAddWater,
                    onResetWater = onResetWater,
                    onOpenCustomWater = onOpenCustomWater,
                    onOpenReminders = onOpenNotifications,
                    onOpenHistory = { onItemClick(IntakeHistory(0)) },
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
                    onOpenHistory = { onItemClick(IntakeHistory(1)) },
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
internal fun MacroRingsCard(
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
internal fun EnergyBalanceCard(balance: EnergyBalance) {
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
internal fun EquationItem(
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
internal fun MealSlotCard(
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
internal fun WorkoutsCard(
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
internal fun MacroBadge(
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
