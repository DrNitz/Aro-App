package com.drnitz.aro.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.drnitz.aro.domain.calculation.PlateCalculator
import com.drnitz.aro.domain.calculation.PlatePair
import kotlin.math.roundToInt

private fun getPlateColor(weightKg: Double): Color = when {
    weightKg >= 25.0 -> Color(0xFFEF4444) // Red
    weightKg >= 20.0 -> Color(0xFF3B82F6) // Blue
    weightKg >= 15.0 -> Color(0xFFF59E0B) // Yellow
    weightKg >= 10.0 -> Color(0xFF10B981) // Green
    weightKg >= 5.0 -> Color(0xFFE2E8F0)  // White
    weightKg >= 2.5 -> Color(0xFF334155)  // Black / Slate
    else -> Color(0xFF94A3B8)             // Silver
}

private fun getPlateHeight(weightKg: Double): Int = when {
    weightKg >= 25.0 -> 72
    weightKg >= 20.0 -> 64
    weightKg >= 15.0 -> 56
    weightKg >= 10.0 -> 48
    weightKg >= 5.0 -> 40
    weightKg >= 2.5 -> 32
    else -> 24
}

/**
 * Interactive Barbell Plate Calculator Dialog.
 *
 * Visually displays the barbell sleeve with standard Olympic color-coded plates,
 * showing the exact breakdown needed per side to reach the target load.
 */
@Composable
fun PlateCalculatorDialog(
    initialWeightKg: Double = 60.0,
    onDismiss: () -> Unit,
    onApplyWeight: ((Double) -> Unit)? = null,
) {
    var targetWeightText by remember {
        mutableStateOf(if (initialWeightKg > 0.0) initialWeightKg.roundToInt().toString() else "60")
    }
    var selectedBarWeight by remember { mutableDoubleStateOf(PlateCalculator.OLYMPIC_BAR_KG) }

    val currentWeight = targetWeightText.toDoubleOrNull() ?: 0.0
    val calculation = remember(currentWeight, selectedBarWeight) {
        PlateCalculator.calculate(targetWeightKg = currentWeight, barWeightKg = selectedBarWeight)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Text(
                            text = "Plate Calculator",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close dialog")
                    }
                }

                // Target Weight Input & Steppers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = targetWeightText,
                        onValueChange = { str ->
                            val clean = str.filter { it.isDigit() || it == '.' }.take(6)
                            targetWeightText = clean
                        },
                        label = { Text("Target Load") },
                        suffix = { Text("kg") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = {
                                val w = (targetWeightText.toDoubleOrNull() ?: 0.0) - 2.5
                                targetWeightText = (w.coerceAtLeast(0.0)).roundToInt().toString()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text("-2.5", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = {
                                val w = (targetWeightText.toDoubleOrNull() ?: 0.0) + 2.5
                                targetWeightText = w.roundToInt().toString()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text("+2.5", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Bar Weight Selector Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "BARBELL TYPE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val barOptions = listOf(
                            PlateCalculator.OLYMPIC_BAR_KG to "20kg (Olympic)",
                            PlateCalculator.WOMENS_BAR_KG to "15kg",
                            PlateCalculator.EZ_CURL_BAR_KG to "10kg",
                            PlateCalculator.MACHINE_ZERO_BAR_KG to "0kg",
                        )
                        barOptions.forEach { (barWeight, label) ->
                            FilterChip(
                                selected = selectedBarWeight == barWeight,
                                onClick = { selectedBarWeight = barWeight },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }
                }

                // Visual Barbell Graphic
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "PLATES PER SIDE (ONE SIDE SHOWN)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // Barbell Graphic Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            // Barbell Shaft / Sleeve Line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(Color(0xFF64748B), RoundedCornerShape(4.dp)),
                            )

                            // Barbell Collar
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height(54.dp)
                                    .background(Color(0xFF334155), RoundedCornerShape(4.dp)),
                            )

                            // Stacked Plates on Collar
                            Row(
                                modifier = Modifier
                                    .padding(start = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (calculation.platesPerSide.isEmpty()) {
                                    Text(
                                        text = "Empty sleeve (no plates needed)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 12.dp),
                                    )
                                } else {
                                    calculation.platesPerSide.forEach { pair ->
                                        repeat(pair.countPerSide) {
                                            val plateColor = getPlateColor(pair.weightKg)
                                            val plateHeight = getPlateHeight(pair.weightKg)
                                            val label = if (pair.weightKg % 1.0 == 0.0) {
                                                pair.weightKg.roundToInt().toString()
                                            } else {
                                                pair.weightKg.toString()
                                            }
                                            val isDarkText = pair.weightKg == 5.0 || pair.weightKg == 15.0

                                            Box(
                                                modifier = Modifier
                                                    .width(20.dp)
                                                    .height(plateHeight.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(plateColor)
                                                    .border(1.dp, Color(0x33000000), RoundedCornerShape(3.dp)),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                    ),
                                                    color = if (isDarkText) Color.Black else Color.White,
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Summary Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Text(
                                text = calculation.sideSummary,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                // Math & Remainder Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Weight per side",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${calculation.weightPerSideKg} kg",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Barbell shaft",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${calculation.barWeightKg} kg",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                if (!calculation.isExact) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    ) {
                        Text(
                            text = "⚠️ Target has +${calculation.remainderKg}kg remainder not matchable with standard plates.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Close")
                    }

                    if (onApplyWeight != null) {
                        Button(
                            onClick = {
                                onApplyWeight(calculation.achievedTotalWeightKg)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Apply Weight")
                        }
                    }
                }
            }
        }
    }
}
