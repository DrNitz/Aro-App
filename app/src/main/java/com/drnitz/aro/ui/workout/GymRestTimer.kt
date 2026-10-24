package com.drnitz.aro.ui.workout

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drnitz.aro.domain.calculation.RestTimerEngine
import com.drnitz.aro.domain.calculation.RestTimerSnapshot
import com.drnitz.aro.theme.AroTheme
import kotlinx.coroutines.delay

/**
 * Controller managing the interactive gym rest timer state, countdown loop,
 * and audio/vibration completion feedback.
 */
@Stable
class GymRestTimerState(
    initialSeconds: Int = 90,
    val context: Context,
) {
    var snapshot by mutableStateOf(RestTimerEngine.createInitial(initialSeconds))
        private set

    var autoStartOnSetComplete by mutableStateOf(true)
    var soundEnabled by mutableStateOf(true)
    var vibrationEnabled by mutableStateOf(true)
    var isMinimized by mutableStateOf(false)
    var lastCompletionHandled by mutableStateOf(false)

    fun start(seconds: Int? = null) {
        val duration = seconds ?: snapshot.totalSeconds
        snapshot = RestTimerEngine.start(duration, SystemClock.elapsedRealtime())
        lastCompletionHandled = false
    }

    fun pause() {
        snapshot = RestTimerEngine.pause(snapshot, SystemClock.elapsedRealtime())
    }

    fun resume() {
        snapshot = RestTimerEngine.resume(snapshot, SystemClock.elapsedRealtime())
    }

    fun togglePlayPause() {
        if (snapshot.isRunning) {
            pause()
        } else if (snapshot.isPaused) {
            resume()
        } else {
            start()
        }
    }

    fun adjustTime(deltaSeconds: Int) {
        snapshot = RestTimerEngine.adjustTime(snapshot, deltaSeconds, SystemClock.elapsedRealtime())
    }

    fun skip() {
        snapshot = RestTimerEngine.skip(snapshot)
    }

    fun reset(defaultSeconds: Int? = null) {
        snapshot = RestTimerEngine.reset(snapshot, defaultSeconds)
        lastCompletionHandled = false
    }

    fun notifySetCompleted(setIndex: Int) {
        if (autoStartOnSetComplete) {
            start()
        }
    }

    internal fun tick() {
        if (snapshot.isRunning && !snapshot.isPaused) {
            val updated = RestTimerEngine.tick(snapshot, SystemClock.elapsedRealtime())
            snapshot = updated
            if (updated.isFinished && !lastCompletionHandled) {
                lastCompletionHandled = true
                triggerAlerts()
            }
        }
    }

    private fun triggerAlerts() {
        if (soundEnabled) {
            playRestTimerChime(context)
        }
        if (vibrationEnabled) {
            triggerRestTimerVibration(context)
        }
    }
}

@Composable
fun rememberGymRestTimerState(
    initialSeconds: Int = 90,
): GymRestTimerState {
    val context = LocalContext.current.applicationContext
    val timerState = remember(initialSeconds) {
        GymRestTimerState(initialSeconds = initialSeconds, context = context)
    }

    LaunchedEffect(timerState.snapshot.isRunning, timerState.snapshot.isPaused) {
        while (timerState.snapshot.isRunning && !timerState.snapshot.isPaused) {
            timerState.tick()
            delay(200)
        }
    }

    return timerState
}

internal fun playRestTimerChime(context: Context) {
    try {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 380)
    } catch (_: Throwable) {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(context, notificationUri)?.play()
        } catch (_: Throwable) {}
    }
}

internal fun triggerRestTimerVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 250), -1),
            )
        } else {
            @Suppress("DEPRECATION")
            val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 250), -1),
                )
            } else {
                @Suppress("DEPRECATION")
                vib?.vibrate(300)
            }
        }
    } catch (_: Throwable) {}
}

/**
 * Modern Rest Timer Card embedded in the Gym Workout Logger.
 * Supports expanded detailed view and collapsible pill view for set editing.
 */
@Composable
fun GymRestTimerCard(
    timerState: GymRestTimerState,
    modifier: Modifier = Modifier,
) {
    val snapshot = timerState.snapshot
    val isRunning = snapshot.isRunning
    val isPaused = snapshot.isPaused
    val isFinished = snapshot.isFinished
    val isMinimized = timerState.isMinimized

    val containerColor = when {
        isFinished -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        isRunning -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            // Header: Title, Status Badge, Minimize/Expand Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Rest timer",
                        tint = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "REST TIMER",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    // Status Pill
                    val statusText = when {
                        isFinished -> "Rest Done!"
                        isRunning -> "Resting"
                        isPaused -> "Paused"
                        else -> "Ready"
                    }
                    val statusColor = when {
                        isFinished -> MaterialTheme.colorScheme.primary
                        isRunning -> AroTheme.macroColors.cardio
                        isPaused -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { timerState.isMinimized = !timerState.isMinimized },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = if (isMinimized) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (isMinimized) "Expand timer" else "Collapse timer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isMinimized,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Finished Celebration Alert
                    if (isFinished) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "⚡ Rest Complete! Ready for your next set!",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                OutlinedButton(
                                    onClick = { timerState.reset() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(28.dp),
                                ) {
                                    Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // Main Clock & Progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Digital Clock Readout
                        Column {
                            Text(
                                text = snapshot.formattedRemaining,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                ),
                                color = if (isFinished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "Target: ${snapshot.totalSeconds}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Delta Adjustments (-15s, +15s, +30s)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { timerState.adjustTime(-15) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp),
                            ) {
                                Text("-15s", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { timerState.adjustTime(+15) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp),
                            ) {
                                Text("+15s", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { timerState.adjustTime(+30) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp),
                            ) {
                                Text("+30s", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Progress Bar
                    val animatedProgress by animateFloatAsState(
                        targetValue = snapshot.progress,
                        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                        label = "restProgress",
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (isFinished) MaterialTheme.colorScheme.primary else AroTheme.macroColors.cardio,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )

                    // Presets Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val presets = listOf(
                            30 to "30s",
                            60 to "1 min",
                            90 to "1.5 min",
                            120 to "2 min",
                            180 to "3 min",
                            300 to "5 min",
                        )
                        items(presets) { (secs, label) ->
                            FilterChip(
                                selected = snapshot.totalSeconds == secs,
                                onClick = { timerState.start(secs) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            )
                        }
                    }

                    // Primary Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { timerState.togglePlayPause() },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isRunning) "Pause" else if (isPaused) "Resume" else "Start Timer",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            )
                        }

                        OutlinedButton(
                            onClick = { timerState.reset() },
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset timer", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }

                        if (isRunning || isPaused) {
                            OutlinedButton(
                                onClick = { timerState.skip() },
                                modifier = Modifier.height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Skip rest", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Skip", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Settings Footer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Switch(
                                checked = timerState.autoStartOnSetComplete,
                                onCheckedChange = { timerState.autoStartOnSetComplete = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                            )
                            Text(
                                text = "Auto-start on set check",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            IconButton(
                                onClick = { timerState.soundEnabled = !timerState.soundEnabled },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = if (timerState.soundEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                    contentDescription = "Toggle sound",
                                    tint = if (timerState.soundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            IconButton(
                                onClick = { timerState.vibrationEnabled = !timerState.vibrationEnabled },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = "Toggle vibration",
                                    tint = if (timerState.vibrationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Minimized View
            AnimatedVisibility(
                visible = isMinimized,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = snapshot.formattedRemaining,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = if (isFinished) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = { timerState.adjustTime(+30) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                        ) {
                            Text("+30s", style = MaterialTheme.typography.labelSmall)
                        }

                        IconButton(
                            onClick = { timerState.togglePlayPause() },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isRunning) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
