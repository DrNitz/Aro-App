package com.drnitz.aro.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.drnitz.aro.ClinicalAudit
import com.drnitz.aro.IntakeHistory
import com.drnitz.aro.R
import com.drnitz.aro.domain.calculation.EnergyPlan
import com.drnitz.aro.domain.calculation.OneRepMaxCalculator
import com.drnitz.aro.domain.clinical.MealCoverage
import com.drnitz.aro.domain.model.EnergyBalance
import com.drnitz.aro.domain.model.FoodLogEntry
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.NutrientTotals
import com.drnitz.aro.domain.model.WorkoutLog
import com.drnitz.aro.theme.AroTheme
import com.drnitz.aro.ui.bodymap.BodySoundHaptics
import com.drnitz.aro.ui.components.ConcentricMacroRings
import com.drnitz.aro.ui.mascot.AroMascot
import com.drnitz.aro.ui.mascot.MascotMood
import com.drnitz.aro.ui.mascot.getMascotCoaching
import com.drnitz.aro.ui.mascot.resolveMascotMood
import com.drnitz.aro.domain.calculation.caloriesBurned
import java.util.Locale
import kotlin.math.roundToInt

@Composable
private fun MealSlot.localizedName(): String = when (this) {
    MealSlot.BREAKFAST -> stringResource(R.string.meal_breakfast)
    MealSlot.LUNCH -> stringResource(R.string.meal_lunch)
    MealSlot.DINNER -> stringResource(R.string.meal_dinner)
    MealSlot.SNACKS -> stringResource(R.string.meal_snacks)
}

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
    onDuplicateWorkout: (WorkoutLog) -> Unit = {},
    onOpenAssistant: () -> Unit = {},
    onItemClick: (NavKey) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.diary_header_label),
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
                    Text(stringResource(R.string.btn_today))
                }
            }
        }

        AroCompanionHubCard(
            state = state,
            onAuditClick = { onItemClick(ClinicalAudit) },
            onOpenAssistant = onOpenAssistant,
        )

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
            text = stringResource(R.string.section_meals_nutrition),
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
                    text = stringResource(R.string.section_workouts_expenditure),
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
                            text = stringResource(R.string.kcal_burned_plus, totalBurn),
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
                    text = stringResource(if (planningMode) R.string.btn_plan_workout else R.string.btn_log_workout),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }

        if (state.diaryDay.workouts.isNotEmpty()) {
            WorkoutsCard(
                workouts = state.diaryDay.workouts,
                onDeleteWorkout = onDeleteWorkout,
                onDuplicateWorkout = onDuplicateWorkout,
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
                            text = if (planningMode) {
                                stringResource(R.string.workouts_empty_plan)
                            } else {
                                stringResource(R.string.workouts_empty_today)
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (planningMode) {
                                stringResource(R.string.workouts_empty_plan_hint)
                            } else {
                                stringResource(R.string.workouts_empty_today_hint)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(if (planningMode) R.string.cd_plan_workout else R.string.cd_log_workout),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AroHaloRing(
    caloriesEaten: Int,
    caloriesBurned: Int,
    targetCalories: Int,
    proteinEatenG: Double,
    targetProteinG: Double,
    modifier: Modifier = Modifier,
    size: Dp = 192.dp,
    strokeWidth: Dp = 9.dp,
    content: @Composable () -> Unit,
) {
    val netCalories = caloriesEaten - caloriesBurned
    val targetCalSafe = if (targetCalories > 0) targetCalories else 2000
    val targetProtSafe = if (targetProteinG > 0) targetProteinG else 150.0

    val rawCalSweep = ((netCalories.toFloat() / targetCalSafe.toFloat()) * 360f).coerceIn(0f, 360f)
    val rawCalOver = ((((netCalories.toFloat() / targetCalSafe.toFloat()) - 1.0f) * 360f)).coerceIn(0f, 360f)
    val rawProtSweep = (((proteinEatenG / targetProtSafe) * 360f).toFloat()).coerceIn(0f, 360f)

    val animCalSweep = remember { Animatable(0f) }
    val animCalOver = remember { Animatable(0f) }
    val animProtSweep = remember { Animatable(0f) }

    LaunchedEffect(rawCalSweep, rawCalOver, rawProtSweep) {
        val animSpec = tween<Float>(durationMillis = 900, easing = FastOutSlowInEasing)
        animCalSweep.animateTo(rawCalSweep, animSpec)
        animCalOver.animateTo(rawCalOver, animSpec)
        animProtSweep.animateTo(rawProtSweep, animSpec)
    }

    val macroColors = AroTheme.macroColors

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Outer Calorie Halo Track
            val rCal = this.size.minDimension / 2f - strokePx / 2f - 2.dp.toPx()
            // Inner Protein Accent Track (slim micro-track)
            val rProt = rCal - strokePx - 3.dp.toPx()
            val protStrokePx = strokePx * 0.42f

            // 1. Calorie Ambient Background Track
            drawCircle(
                color = macroColors.caloriesBg,
                radius = rCal,
                center = center,
                style = Stroke(width = strokePx),
            )

            // 2. Protein Ambient Background Track
            drawCircle(
                color = macroColors.proteinBg.copy(alpha = 0.5f),
                radius = rProt,
                center = center,
                style = Stroke(width = protStrokePx),
            )

            // 3. Active Calorie Sweep Arc
            if (animCalSweep.value > 0f) {
                rotate(degrees = -90f, pivot = center) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(macroColors.calories, macroColors.calories.copy(alpha = 0.82f)),
                            center = center,
                        ),
                        startAngle = 0f,
                        sweepAngle = animCalSweep.value,
                        useCenter = false,
                        topLeft = Offset(center.x - rCal, center.y - rCal),
                        size = Size(rCal * 2, rCal * 2),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round),
                    )

                    // Target Overfill Glow Arc
                    if (animCalOver.value > 0f) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.85f),
                            startAngle = 0f,
                            sweepAngle = animCalOver.value,
                            useCenter = false,
                            topLeft = Offset(center.x - rCal, center.y - rCal),
                            size = Size(rCal * 2, rCal * 2),
                            style = Stroke(width = strokePx * 0.45f, cap = StrokeCap.Round),
                        )
                    }
                }
            }

            // 4. Active Protein Sweep Arc
            if (animProtSweep.value > 0f) {
                rotate(degrees = -90f, pivot = center) {
                    drawArc(
                        color = macroColors.protein,
                        startAngle = 0f,
                        sweepAngle = animProtSweep.value,
                        useCenter = false,
                        topLeft = Offset(center.x - rProt, center.y - rProt),
                        size = Size(rProt * 2, rProt * 2),
                        style = Stroke(width = protStrokePx, cap = StrokeCap.Round),
                    )
                }
            }
        }

        // Center Content (Aro companion inside the halo)
        content()
    }
}

@Composable
private fun CompanionMacroPill(
    label: String,
    currentG: Double,
    targetG: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val progress = if (targetG > 0) (currentG / targetG).toFloat().coerceIn(0f, 1f) else 0f
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                    color = color,
                )
                Text(
                    text = "${currentG.roundToInt()}g",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
            )
        }
    }
}

private enum class CompanionViewMode { CALORIES, PROTEIN, ADVICE }

@Composable
internal fun AroCompanionHubCard(
    state: MainScreenUiState.Success,
    onAuditClick: () -> Unit = {},
    onOpenAssistant: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val mood = resolveMascotMood(state)
    val coaching = remember(state) { getMascotCoaching(state) }
    var viewMode by remember { mutableStateOf(CompanionViewMode.CALORIES) }

    val energyBalance = state.energyBalance
    val energyPlan = state.energyPlan
    val dayTotals = state.dayTotals

    val remainingCalories = energyBalance.remaining
    val isOverCalories = energyBalance.isOverTarget
    val overCalories = energyBalance.surplus

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. Interactive Speech / Coaching Bubble (Tap to open Assistant)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = {
                        BodySoundHaptics.playClickSound()
                        onOpenAssistant()
                    }),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = coaching.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = coaching.message,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "Coach",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }

            // 2. Stripped-Down Halo Ring filled with interactive Aro Companion
            AroHaloRing(
                caloriesEaten = energyBalance.caloriesEaten,
                caloriesBurned = energyBalance.caloriesBurned,
                targetCalories = energyPlan.targetCalories,
                proteinEatenG = dayTotals.proteinG,
                targetProteinG = energyPlan.macros.proteinG.toDouble(),
                size = 192.dp,
                strokeWidth = 9.dp,
            ) {
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AroMascot(
                        mood = mood,
                        size = 125.dp,
                        isTopBar = false,
                        onClick = {
                            BodySoundHaptics.playClickSound()
                        },
                    )
                }
            }

            // 3. Interactive Metric Pill (Tap to cycle Calories / Protein / Mood)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = when (viewMode) {
                    CompanionViewMode.CALORIES -> AroTheme.macroColors.calories.copy(alpha = 0.15f)
                    CompanionViewMode.PROTEIN -> AroTheme.macroColors.protein.copy(alpha = 0.15f)
                    CompanionViewMode.ADVICE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                },
                border = BorderStroke(
                    1.dp,
                    when (viewMode) {
                        CompanionViewMode.CALORIES -> AroTheme.macroColors.calories.copy(alpha = 0.4f)
                        CompanionViewMode.PROTEIN -> AroTheme.macroColors.protein.copy(alpha = 0.4f)
                        CompanionViewMode.ADVICE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    },
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        viewMode = when (viewMode) {
                            CompanionViewMode.CALORIES -> CompanionViewMode.PROTEIN
                            CompanionViewMode.PROTEIN -> CompanionViewMode.ADVICE
                            CompanionViewMode.ADVICE -> CompanionViewMode.CALORIES
                        }
                        BodySoundHaptics.playClickSound()
                    },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when (viewMode) {
                        CompanionViewMode.CALORIES -> {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = AroTheme.macroColors.calories,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = if (isOverCalories) "+$overCalories kcal over" else "$remainingCalories kcal left",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                color = if (isOverCalories) AroTheme.macroColors.calories else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        CompanionViewMode.PROTEIN -> {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = AroTheme.macroColors.protein,
                                modifier = Modifier.size(15.dp),
                            )
                            Text(
                                text = "${dayTotals.proteinG.roundToInt()}/${energyPlan.macros.proteinG}g Protein",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        CompanionViewMode.ADVICE -> {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = mood.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }

            // 4. Streamlined Macro Progress Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CompanionMacroPill(
                    label = "Protein",
                    currentG = dayTotals.proteinG,
                    targetG = energyPlan.macros.proteinG.toDouble(),
                    color = AroTheme.macroColors.protein,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                CompanionMacroPill(
                    label = "Carbs",
                    currentG = dayTotals.carbsG,
                    targetG = energyPlan.macros.carbsG.toDouble(),
                    color = AroTheme.macroColors.carbs,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                CompanionMacroPill(
                    label = "Fat",
                    currentG = dayTotals.fatG,
                    targetG = energyPlan.macros.fatG.toDouble(),
                    color = AroTheme.macroColors.fat,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                CompanionMacroPill(
                    label = "Fiber",
                    currentG = dayTotals.fiberG,
                    targetG = energyPlan.macros.fiberG.toDouble(),
                    color = AroTheme.macroColors.fiber,
                    modifier = Modifier.weight(1f),
                )
            }

            // 5. Energy Balance Equation & Clinical Audit Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Goal ${energyBalance.targetCalories} − Eaten ${energyBalance.caloriesEaten} + Burned ${energyBalance.caloriesBurned}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onAuditClick),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = AroTheme.macroColors.fiber,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            text = "Audit",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
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
                    label = stringResource(R.string.macro_protein),
                    amount = "${dayTotals.proteinG.roundToInt()}g",
                    target = "${energyPlan.macros.proteinG}g",
                    color = AroTheme.macroColors.protein,
                    fraction = (dayTotals.proteinG / energyPlan.macros.proteinG.coerceAtLeast(1)).toFloat(),
                )
                MacroBadge(
                    label = stringResource(R.string.macro_carbs),
                    amount = "${dayTotals.carbsG.roundToInt()}g",
                    target = "${energyPlan.macros.carbsG}g",
                    color = AroTheme.macroColors.carbs,
                    fraction = (dayTotals.carbsG / energyPlan.macros.carbsG.coerceAtLeast(1)).toFloat(),
                )
                MacroBadge(
                    label = stringResource(R.string.macro_fat),
                    amount = "${dayTotals.fatG.roundToInt()}g",
                    target = "${energyPlan.macros.fatG}g",
                    color = AroTheme.macroColors.fat,
                    fraction = (dayTotals.fatG / energyPlan.macros.fatG.coerceAtLeast(1)).toFloat(),
                )
                MacroBadge(
                    label = stringResource(R.string.macro_fiber),
                    amount = "${dayTotals.fiberG.roundToInt()}g",
                    target = "${energyPlan.macros.fiberG}g",
                    color = AroTheme.macroColors.fiber,
                    fraction = (dayTotals.fiberG / energyPlan.macros.fiberG.coerceAtLeast(1)).toFloat(),
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
                        text = stringResource(R.string.clinical_audit_card_title),
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
                label = stringResource(R.string.energy_goal),
                value = "${balance.targetCalories}",
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "−",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EquationItem(
                label = stringResource(R.string.energy_eaten),
                value = "${balance.caloriesEaten}",
                color = AroTheme.macroColors.calories,
            )
            Text(
                text = "+",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EquationItem(
                label = stringResource(R.string.energy_burned),
                value = "${balance.caloriesBurned}",
                color = AroTheme.macroColors.cardio,
            )
            Text(
                text = "=",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EquationItem(
                label = stringResource(if (balance.isOverTarget) R.string.energy_surplus else R.string.energy_remaining),
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
                        text = slot.localizedName(),
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
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(
                                if (planningMode) R.string.cd_plan_food_slot else R.string.cd_add_food_slot,
                                slot.localizedName(),
                            ),
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
                        contentDescription = stringResource(R.string.cd_missing_macro_alert),
                        tint = AroTheme.macroColors.clinicalModerate,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(
                            R.string.missing_macros_prefix,
                            mealCoverage.missingMacros.joinToString(", ") { it.label },
                        ),
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
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.cd_delete_item),
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
                        text = stringResource(
                            if (planningMode) R.string.btn_plan_food_slot else R.string.btn_add_food_slot,
                            slot.localizedName(),
                        ),
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
    onDuplicateWorkout: ((WorkoutLog) -> Unit)? = null,
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
                        if (onDuplicateWorkout != null) {
                            IconButton(
                                onClick = { onDuplicateWorkout(workout) },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.cd_duplicate_workout),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        IconButton(
                            onClick = { onDeleteWorkout(workout.id) },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.cd_delete_workout),
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
    fraction: Float = 0f,
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
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.macro_of_target, target),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        if (fraction > 0f) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(4.dp)
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(2.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(4.dp)
                        .background(color, RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}
