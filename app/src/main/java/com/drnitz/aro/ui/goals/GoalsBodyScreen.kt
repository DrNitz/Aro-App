package com.drnitz.aro.ui.goals

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drnitz.aro.domain.calculation.EnergyPlan
import com.drnitz.aro.domain.calculation.bodyMassIndex
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.theme.AroTheme
import kotlin.math.roundToInt

/**
 * Goals & Body hub.
 *
 * Body planner, weight history and the clinical audit are one destination. Vitals and
 * doctor visits remain a separate clinical records destination.
 */
@Composable
fun GoalsBodyScreen(
    profile: UserProfile,
    energyPlan: EnergyPlan,
    onOpenBodyPlanner: () -> Unit,
    onOpenVitals: () -> Unit,
    onOpenDataManagement: () -> Unit = {},
    onOpenIntakeHistory: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val bmi = bodyMassIndex(profile.weightKg, profile.heightCm).valueOrNull()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "goal_summary_card") {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(AroTheme.macroColors.clinicalOptimal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrackChanges,
                                contentDescription = null,
                                tint = AroTheme.macroColors.clinicalOptimal,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.goal.label,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${profile.activityLevel.label} activity • ${profile.macroPreset.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        BodyStat("Current", "${profile.weightKg.roundToInt()} kg", MaterialTheme.colorScheme.onSurface)
                        BodyStat("Target", "${profile.targetWeightKg.roundToInt()} kg", AroTheme.macroColors.water)
                        BodyStat(
                            label = "BMI",
                            value = bmi?.let { "${it.value}" } ?: "—",
                            color = AroTheme.macroColors.protein,
                        )
                        BodyStat("Height", "${profile.heightCm.roundToInt()} cm", MaterialTheme.colorScheme.onSurface)
                    }

                    if (bmi != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = AroTheme.macroColors.proteinBg,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "WHO classification: ${bmi.category.label}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = AroTheme.macroColors.protein,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DAILY ENERGY PLAN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        BodyStat("BMR", "${energyPlan.bmr}", MaterialTheme.colorScheme.onSurface)
                        BodyStat("TDEE", "${energyPlan.tdee}", AroTheme.macroColors.water)
                        BodyStat("Target", "${energyPlan.targetCalories}", AroTheme.macroColors.calories)
                        BodyStat(
                            "Adjust",
                            if (energyPlan.goalAdjustment > 0) "+${energyPlan.goalAdjustment}" else "${energyPlan.goalAdjustment}",
                            AroTheme.macroColors.clinicalModerate,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MacroChip("P ${energyPlan.macros.proteinG}g", AroTheme.macroColors.protein)
                        MacroChip("C ${energyPlan.macros.carbsG}g", AroTheme.macroColors.carbs)
                        MacroChip("F ${energyPlan.macros.fatG}g", AroTheme.macroColors.fat)
                        MacroChip("Fiber ${energyPlan.macros.fiberG}g", AroTheme.macroColors.fiber)
                    }
                }
            }
        }

        item {
            Text(
                text = "CLINICAL TOOLS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }

        item {
            DestinationCard(
                icon = Icons.Default.Tune,
                accent = AroTheme.macroColors.clinicalOptimal,
                title = "Body, Weight & Clinical Audit",
                subtitle = "TDEE, BMI trend graph, weigh-ins and electrolyte findings",
                onClick = onOpenBodyPlanner,
            )
        }
        item {
            DestinationCard(
                icon = Icons.Default.Favorite,
                accent = AroTheme.macroColors.calories,
                title = "Vitals & Doctor Consults",
                subtitle = "Blood pressure, glucose, ketones, SpO2 and visit notes",
                onClick = onOpenVitals,
            )
        }

        item {
            Text(
                text = "DAILY LOGS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }
        item {
            DestinationCard(
                icon = Icons.Default.History,
                accent = AroTheme.macroColors.water,
                title = "Water & Fasting History",
                subtitle = "Every drink with its time, and every fast with its duration",
                onClick = onOpenIntakeHistory,
            )
        }

        item {
            Text(
                text = "APP DATA",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }
        item {
            DestinationCard(
                icon = Icons.Default.Storage,
                accent = AroTheme.macroColors.water,
                title = "Data Management & Backup",
                subtitle = "Export a complete JSON archive or restore it on a new device",
                onClick = onOpenDataManagement,
            )
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun BodyStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MacroChip(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(6.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.sp),
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun DestinationCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
