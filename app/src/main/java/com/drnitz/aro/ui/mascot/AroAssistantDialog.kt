package com.drnitz.aro.ui.mascot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.drnitz.aro.theme.AroTheme
import com.drnitz.aro.ui.components.AroDialogSurface
import com.drnitz.aro.ui.main.MainScreenUiState

/**
 * Interactive Native Assistant Dialog triggered by tapping Aro Bot in the Top Bar.
 *
 * Displays live context-aware coaching, daily metabolic diagnosis, and
 * direct one-tap shortcuts for hydration, food, workouts, and fasting.
 */
@Composable
fun AroAssistantDialog(
    state: MainScreenUiState.Success,
    onDismiss: () -> Unit,
    onAddWater: (Int) -> Unit,
    onOpenFast: () -> Unit,
    onEndFast: () -> Unit,
    onOpenAddFood: () -> Unit,
    onOpenLogWorkout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coaching = remember(state) { getMascotCoaching(state) }
    val mood = coaching.mood
    val isFasting = state.activeFast != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AroDialogSurface(
            modifier = modifier,
            floatingCard = true,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(8.dp),
                        ) {}
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "ARO COMPANION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close assistant",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Hero Animated Cartoon Mascot
                AroMascot(
                    mood = mood,
                    size = 92.dp,
                    onClick = {
                        // Playful bounce on tap
                    },
                    contentDescription = "Aro Interactive Cartoon Mascot",
                )

                Spacer(Modifier.height(8.dp))

                // Cartoon Speech Pill
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = androidx.compose.foundation.BorderStroke(
                        0.75.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier.clip(RoundedCornerShape(14.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "🐾 ${mood.shortGreeting}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Contextual Coaching Bubble
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                    ) {
                        Text(
                            text = coaching.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = coaching.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp,
                        )

                        Spacer(Modifier.height(10.dp))

                        // Dr. Nitz Actionable Tip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                                    RoundedCornerShape(10.dp),
                                )
                                .padding(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = AroTheme.macroColors.calories,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = coaching.actionableTip,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 17.sp,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Vitals Snapshot Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VitalMiniCard(
                        icon = Icons.Default.LocalFireDepartment,
                        iconTint = AroTheme.macroColors.calories,
                        label = "Energy",
                        value = "${state.energyBalance.caloriesEaten} kcal",
                        modifier = Modifier.weight(1f),
                    )
                    VitalMiniCard(
                        icon = Icons.Default.Restaurant,
                        iconTint = AroTheme.macroColors.cardio,
                        label = "Protein",
                        value = "${state.dayTotals.proteinG.toInt()}g",
                        modifier = Modifier.weight(1f),
                    )
                    VitalMiniCard(
                        icon = Icons.Default.WaterDrop,
                        iconTint = AroTheme.macroColors.water,
                        label = "Hydration",
                        value = "${state.diaryDay.waterMl}ml",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Quick Native Actions Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "QUICK ACTIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickActionButton(
                        icon = Icons.Default.WaterDrop,
                        label = "+250ml",
                        tint = AroTheme.macroColors.water,
                        onClick = { onAddWater(250) },
                        modifier = Modifier.weight(1f),
                    )

                    QuickActionButton(
                        icon = Icons.Default.Timer,
                        label = if (isFasting) "End Fast" else "Start Fast",
                        tint = AroTheme.macroColors.fasting,
                        onClick = { if (isFasting) onEndFast() else onOpenFast() },
                        modifier = Modifier.weight(1f),
                    )

                    QuickActionButton(
                        icon = Icons.Default.Restaurant,
                        label = "Log Food",
                        tint = AroTheme.macroColors.calories,
                        onClick = onOpenAddFood,
                        modifier = Modifier.weight(1f),
                    )

                    QuickActionButton(
                        icon = Icons.Default.FitnessCenter,
                        label = "Workout",
                        tint = AroTheme.macroColors.cardio,
                        onClick = onOpenLogWorkout,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun VitalMiniCard(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}
