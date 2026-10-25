package com.drnitz.aro.ui.main

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.drnitz.aro.theme.AroTheme
import kotlin.math.roundToInt

@Composable
internal fun HydrationWidget(
    waterMl: Int,
    targetWaterMl: Int,
    waterLogCount: Int = 0,
    planningMode: Boolean = false,
    plannedWaterMl: Int = 0,
    onAddWater: (Int) -> Unit,
    onResetWater: () -> Unit,
    onOpenCustomWater: () -> Unit,
    onOpenReminders: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
) {
    val shownWaterMl = if (planningMode) plannedWaterMl else waterMl

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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = null,
                    tint = AroTheme.macroColors.water,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (planningMode) "Hydration plan" else "Hydration",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onOpenReminders,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Hydration Reminders",
                        tint = AroTheme.macroColors.water.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val shownProgress = if (targetWaterMl <= 0) 0f else {
                (shownWaterMl.toFloat() / targetWaterMl).coerceIn(0f, 1f)
            }
            val percent = (shownProgress * 100).roundToInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (planningMode) {
                        "$shownWaterMl / $targetWaterMl mL planned"
                    } else {
                        "$waterMl / $targetWaterMl mL"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AroTheme.macroColors.water,
                )
            }
            if (planningMode && shownWaterMl > 0) {
                Text(
                    text = "Not logged",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (!planningMode && waterLogCount > 0) {
                Text(
                    text = if (waterLogCount == 1) {
                        "1 drink logged"
                    } else {
                        "$waterLogCount drinks logged"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { shownProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = AroTheme.macroColors.water,
                trackColor = AroTheme.macroColors.waterBg,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OutlinedButton(
                    onClick = { onAddWater(250) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp),
                ) {
                    Text("+250", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = { onAddWater(500) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(30.dp),
                ) {
                    Text("+500", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onOpenCustomWater,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier
                        .weight(0.9f)
                        .height(30.dp),
                ) {
                    Text("More", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onOpenHistory,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = AroTheme.macroColors.water,
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(26.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = AroTheme.macroColors.water.copy(alpha = 0.8f),
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View water log", style = MaterialTheme.typography.labelSmall)
                }
                if (!planningMode && waterMl > 0) {
                    IconButton(
                        onClick = onResetWater,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Water",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun CustomHydrationDialog(
    planningMode: Boolean = false,
    onDismiss: () -> Unit,
    onAddWater: (Int) -> Unit,
) {
    var mlText by remember { mutableStateOf("250") }
    val presets = listOf(100, 250, 500, 750, 1000)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (planningMode) "Plan Water" else "Log Water",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (planningMode) {
                    Text(
                        text = "Adds a water plan to this future day. It will not count as drunk water until you confirm it.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = mlText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 5) {
                            mlText = input
                        }
                    },
                    label = { Text("Volume (mL)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presets.forEach { preset ->
                        OutlinedButton(
                            onClick = { mlText = preset.toString() },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                        ) {
                            Text("+$preset", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Text(
                    text = "Maintains plasma volume, renal clearance, and lipolysis during workouts and fasting.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = mlText.toIntOrNull()?.coerceAtLeast(1) ?: 250
                    onAddWater(amount)
                },
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (planningMode) "Add Water to Plan" else "Add Water")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        },
    )
}
