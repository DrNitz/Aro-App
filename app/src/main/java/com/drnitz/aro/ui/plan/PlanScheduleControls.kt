package com.drnitz.aro.ui.plan

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.sp
import com.drnitz.aro.domain.model.MealSlot
import com.drnitz.aro.domain.model.PlanItemType
import com.drnitz.aro.domain.model.PlanRepeat
import com.drnitz.aro.ui.components.datePickerMillisToLocalDate
import com.drnitz.aro.ui.components.toDatePickerMillis
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

/** Meal slot inference used when a food is added to a plan. */
fun defaultMealFor(time: LocalTime): MealSlot = when {
    time < LocalTime.of(10, 30) -> MealSlot.BREAKFAST
    time < LocalTime.of(15, 0) -> MealSlot.LUNCH
    time < LocalTime.of(21, 0) -> MealSlot.DINNER
    else -> MealSlot.SNACKS
}

/** Sensible initial time for a new plan item. */
fun defaultPlanTime(type: PlanItemType, meal: MealSlot? = null): LocalTime = when (type) {
    PlanItemType.FOOD -> when (meal) {
        MealSlot.BREAKFAST -> LocalTime.of(8, 0)
        MealSlot.LUNCH -> LocalTime.of(12, 30)
        MealSlot.DINNER -> LocalTime.of(19, 0)
        MealSlot.SNACKS, null -> LocalTime.of(16, 0)
    }
    PlanItemType.EXERCISE -> LocalTime.of(18, 0)
    PlanItemType.PILL -> LocalTime.of(8, 0)
    PlanItemType.FASTING -> LocalTime.of(20, 0)
    PlanItemType.WATER -> LocalTime.of(12, 0)
}

/**
 * Time, reminder and repeat controls shared by all three pickers.
 *
 * The time and repeat rule become the next AlarmManager reminder for this series.
 */
@Composable
fun PlanScheduleControls(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    repeat: PlanRepeat,
    repeatUntil: LocalDate?,
    startDate: LocalDate,
    onRepeatChange: (PlanRepeat, LocalDate?) -> Unit,
    reminderEnabled: Boolean,
    onReminderEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTimePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "TIME & REPEAT",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Time",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = { showTimePicker = true },
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(time.format(TIME_FORMAT))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Reminder",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (reminderEnabled) {
                        "Notify at ${time.format(TIME_FORMAT)}"
                    } else {
                        "No notification for this plan"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = reminderEnabled,
                onCheckedChange = onReminderEnabledChange,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PlanRepeat.entries.forEach { rule ->
                FilterChip(
                    selected = repeat == rule,
                    onClick = {
                        if (rule == repeat) return@FilterChip
                        val end = when {
                            rule == PlanRepeat.NONE -> null
                            repeatUntil != null && !repeatUntil.isBefore(startDate) -> repeatUntil
                            else -> rule.defaultEndDate(startDate)
                        }
                        onRepeatChange(rule, end)
                    },
                    label = {
                        Text(rule.label, style = MaterialTheme.typography.labelSmall)
                    },
                    shape = RoundedCornerShape(8.dp),
                )
            }
        }

        if (repeat != PlanRepeat.NONE) {
            OutlinedButton(
                onClick = { showEndDatePicker = true },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text(
                    text = "Repeat until " +
                        (repeatUntil ?: repeat.defaultEndDate(startDate))!!.format(DATE_FORMAT),
                )
            }
            Text(
                text = "The plan repeats on this schedule. Each occurrence can be marked done separately.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showTimePicker) {
        PlanTimePickerDialog(
            initial = time,
            onDismiss = { showTimePicker = false },
            onConfirm = {
                onTimeChange(it)
                showTimePicker = false
            },
        )
    }

    if (showEndDatePicker) {
        val initialEnd = repeatUntil ?: repeat.defaultEndDate(startDate) ?: startDate
        PlanDatePickerDialog(
            initialDate = if (initialEnd.isBefore(startDate)) startDate else initialEnd,
            onDismiss = { showEndDatePicker = false },
            onConfirm = {
                onRepeatChange(repeat, if (it.isBefore(startDate)) startDate else it)
                showEndDatePicker = false
            },
            title = "Repeat until",
        )
    }
}

@Composable
fun PlanTimePickerDialog(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
    title: String = "Plan time",
    confirmLabel: String = "Set time",
) {
    var hourText by remember { mutableStateOf(initial.hour.toString().padStart(2, '0')) }
    var minuteText by remember { mutableStateOf(initial.minute.toString().padStart(2, '0')) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { input ->
                            if (input.length <= 2 && input.all { it.isDigit() }) hourText = input
                        },
                        label = { Text("Hour") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Text(":", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { input ->
                            if (input.length <= 2 && input.all { it.isDigit() }) minuteText = input
                        },
                        label = { Text("Minute") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 30),
                        LocalTime.of(18, 0),
                        LocalTime.of(21, 0),
                    ).forEach { preset ->
                        TextButton(
                            onClick = {
                                hourText = preset.hour.toString().padStart(2, '0')
                                minuteText = preset.minute.toString().padStart(2, '0')
                            },
                        ) {
                            Text(preset.format(TIME_FORMAT))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hour = hourText.toIntOrNull()?.coerceIn(0, 23) ?: initial.hour
                    val minute = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: initial.minute
                    onConfirm(LocalTime.of(hour, minute))
                },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    title: String = "Choose date",
    confirmLabel: String = "Use date",
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toDatePickerMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selected = state.selectedDateMillis?.let(::datePickerMillisToLocalDate)
                        ?: initialDate
                    onConfirm(selected)
                },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(start = 24.dp, top = 16.dp),
            )
            DatePicker(
                state = state,
                showModeToggle = false,
            )
        }
    }
}
