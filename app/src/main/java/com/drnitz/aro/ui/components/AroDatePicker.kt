package com.drnitz.aro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/**
 * Full-width date card used on the Diary and Clinical Audit screens.
 *
 * Tapping anywhere opens a month calendar. The quick "Today" action appears only when the
 * selected day is not today, keeping the card quiet for the common case.
 */
@Composable
fun AroDatePickerBar(
    selectedDate: LocalDate,
    formattedDate: String,
    isToday: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "DIARY DATE",
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button) { showDatePicker = true },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (!isToday) {
                OutlinedButton(
                    onClick = onToday,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Today", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Icon(
                imageVector = Icons.Default.EditCalendar,
                contentDescription = "Open calendar",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    if (showDatePicker) {
        AroDatePickerDialog(
            selectedDate = selectedDate,
            isToday = isToday,
            onDismiss = { showDatePicker = false },
            onSelectDate = onSelectDate,
            onToday = onToday,
        )
    }
}

/**
 * Compact, always-visible context banner for screens that write into the selected diary
 * day but do not show the Diary date card themselves.
 */
@Composable
fun AroLoggingDateChip(
    formattedDate: String,
    isToday: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    onToday: () -> Unit,
    selectedDate: LocalDate,
    modifier: Modifier = Modifier,
    labelPrefix: String = "Logging for",
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val container = if (isToday) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val content = if (isToday) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button) { showDatePicker = true },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = "$labelPrefix $formattedDate",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.EditCalendar,
                contentDescription = "Change logging date",
                tint = content.copy(alpha = 0.8f),
                modifier = Modifier.size(17.dp),
            )
        }
    }

    if (showDatePicker) {
        AroDatePickerDialog(
            selectedDate = selectedDate,
            isToday = isToday,
            onDismiss = { showDatePicker = false },
            onSelectDate = onSelectDate,
            onToday = onToday,
            confirmLabel = "Use date",
        )
    }
}

/** Static date reminder used inside Add Food and Log Workout dialogs. */
@Composable
fun AroLoggingDateLabel(
    formattedDate: String,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    labelPrefix: String = "Logging for",
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = if (isToday) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "$labelPrefix $formattedDate",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = if (isToday) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AroDatePickerDialog(
    selectedDate: LocalDate,
    isToday: Boolean,
    onDismiss: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onToday: () -> Unit,
    confirmLabel: String = "Open day",
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toDatePickerMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onSelectDate(datePickerMillisToLocalDate(millis))
                    }
                    onDismiss()
                },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            Row {
                if (!isToday) {
                    TextButton(
                        onClick = {
                            onToday()
                            onDismiss()
                        },
                    ) {
                        Text("Today")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false,
        )
    }
}
