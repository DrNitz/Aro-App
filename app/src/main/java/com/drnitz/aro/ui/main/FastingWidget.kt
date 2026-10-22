package com.drnitz.aro.ui.main

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drnitz.aro.data.repository.ActiveFast
import com.drnitz.aro.domain.model.FastingStage
import com.drnitz.aro.theme.AroTheme
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.math.roundToInt

@Composable
internal fun FastingWidget(
    activeFast: ActiveFast?,
    planningMode: Boolean = false,
    plannedFastingHours: Int = 0,
    onOpenStartFast: () -> Unit,
    onEndFast: () -> Unit,
    onOpenReminders: () -> Unit = {},
) {
    var currentTime by remember(activeFast?.id) { mutableStateOf(Instant.now()) }
    LaunchedEffect(activeFast?.id) {
        if (activeFast != null) {
            currentTime = Instant.now()
            while (true) {
                delay(30_000L)
                currentTime = Instant.now()
            }
        }
    }

    val liveFast = activeFast?.let {
        ActiveFast.of(
            id = it.id,
            startedAt = it.startedAt,
            targetHours = it.targetHours,
            now = currentTime,
        )
    }

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
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = AroTheme.macroColors.fasting,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Fasting",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onOpenReminders,
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Fasting Alerts",
                            tint = AroTheme.macroColors.fasting.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }

                if (liveFast != null) {
                    val percent = (liveFast.progress * 100).roundToInt()
                    Text(
                        text = "$percent%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AroTheme.macroColors.fasting,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (liveFast != null) {
                val elapsedHrs = liveFast.elapsedMinutes / 60
                val elapsedMins = liveFast.elapsedMinutes % 60
                val stage = FastingStage.forElapsedHours(liveFast.elapsedMinutes / 60.0)

                Text(
                    text = "${elapsedHrs}h ${elapsedMins}m / ${liveFast.targetHours}h",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stage.title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = AroTheme.macroColors.fasting,
                )

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { liveFast.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = AroTheme.macroColors.fasting,
                    trackColor = AroTheme.macroColors.fasting.copy(alpha = 0.15f),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onEndFast,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("End Fast", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text(
                    text = if (planningMode && plannedFastingHours > 0) {
                        "${plannedFastingHours}h fast planned"
                    } else if (planningMode) {
                        "No fast planned"
                    } else {
                        "Ready to Fast"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (planningMode) {
                        "Set a start time and reminder for this future day"
                    } else {
                        "Select 16h, 18h, 20h or 24h"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onOpenStartFast,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AroTheme.macroColors.fasting,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (planningMode) "Plan Fast" else "Start Fast",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
internal fun StartFastProtocolDialog(
    planningMode: Boolean = false,
    onDismiss: () -> Unit,
    onStartFast: (Int) -> Unit,
) {
    val protocols = listOf(
        Triple(16, "16:8 Standard", "16h fast • 8h eating window (Optimal daily balance)"),
        Triple(18, "18:6 Accelerated", "18h fast • 6h eating window (Enhanced fat oxidation)"),
        Triple(20, "20:4 Warrior", "20h fast • 4h eating window (Advanced cellular autophagy)"),
        Triple(24, "24:0 OMAD", "24h fast • One meal a day (Peak metabolic renewal)"),
    )
    var selectedHours by remember { mutableIntStateOf(16) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (planningMode) "Plan Fasting Protocol" else "Select Fasting Protocol",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = if (planningMode) {
                        "Add a fast to this future day. No fast starts until you confirm it later."
                    } else {
                        "Circadian & Metabolic Intermittent Fasting"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                protocols.forEach { (hours, title, description) ->
                    val isSelected = selectedHours == hours
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                AroTheme.macroColors.fasting.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedHours = hours },
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) AroTheme.macroColors.fasting else MaterialTheme.colorScheme.onSurface,
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AroTheme.macroColors.fasting,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onStartFast(selectedHours) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AroTheme.macroColors.fasting,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(if (planningMode) "Add Fast to Plan" else "Start Fast")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(10.dp)) {
                Text("Cancel")
            }
        },
    )
}
