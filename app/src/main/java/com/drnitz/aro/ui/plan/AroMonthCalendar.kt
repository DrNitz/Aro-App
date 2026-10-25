package com.drnitz.aro.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drnitz.aro.domain.calculation.monthGridDates
import com.drnitz.aro.domain.calculation.weekDaysInOrder
import com.drnitz.aro.domain.model.PlanItemType
import com.drnitz.aro.theme.AroTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Full six-week month calendar used on the Diary front page.
 *
 * The month is externally controlled by [PlanUiState] so plan dots and month navigation
 * stay in sync with the repository. Tapping a date does not itself decide whether to
 * plan or select; the caller decides based on whether the date is in the future.
 */
@Composable
fun AroMonthCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    plannedDates: Map<LocalDate, Set<PlanItemType>>,
    onMonthChange: (YearMonth) -> Unit,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = Locale.getDefault()
    val weekStart = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val gridDates = remember(month, weekStart) { monthGridDates(month, weekStart) }
    val monthLabel = remember(month, locale) {
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    }
    val hasPlans = plannedDates.isNotEmpty()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PLAN CALENDAR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.1.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                TextButton(
                    onClick = { onDateClick(today) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 8.dp,
                        vertical = 2.dp,
                    ),
                ) {
                    Text("Today", style = MaterialTheme.typography.labelMedium)
                }

                IconButton(
                    onClick = { onMonthChange(month.minusMonths(1)) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(
                    onClick = { onMonthChange(month.plusMonths(1)) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.size(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                weekDaysInOrder(weekStart).forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.SHORT, locale),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.size(4.dp))

            gridDates.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        CalendarDayCell(
                            date = date,
                            month = month,
                            selectedDate = selectedDate,
                            today = today,
                            plannedTypes = plannedDates[date].orEmpty(),
                            onClick = { onDateClick(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (hasPlans) {
                Spacer(modifier = Modifier.size(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendDot(AroTheme.macroColors.calories)
                    Text(
                        text = " Food",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    LegendDot(AroTheme.macroColors.cardio)
                    Text(
                        text = " Exercise",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    LegendDot(MaterialTheme.colorScheme.primary)
                    Text(
                        text = " Pills",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LegendDot(AroTheme.macroColors.fasting)
                    Text(
                        text = " Fast",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LegendDot(AroTheme.macroColors.water)
                    Text(
                        text = " Water",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    plannedTypes: Set<PlanItemType>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inMonth = YearMonth.from(date) == month
    val selected = date == selectedDate
    val isToday = date == today
    val textColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val description = buildString {
        append(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")))
        if (isToday) append(", today")
        if (plannedTypes.isNotEmpty()) {
            append(", planned ")
            append(plannedTypes.joinToString(" and ") { it.label })
        }
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .then(
                if (isToday && !selected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier
                },
            )
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                CircleShape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = description
                this.selected = selected
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Medium,
                ),
                color = textColor,
            )
            if (plannedTypes.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    if (PlanItemType.FOOD in plannedTypes) LegendDot(AroTheme.macroColors.calories, 3.dp)
                    if (PlanItemType.EXERCISE in plannedTypes) LegendDot(AroTheme.macroColors.cardio, 3.dp)
                    if (PlanItemType.PILL in plannedTypes) {
                        LegendDot(MaterialTheme.colorScheme.primary, 3.dp)
                    }
                    if (PlanItemType.FASTING in plannedTypes) {
                        LegendDot(AroTheme.macroColors.fasting, 3.dp)
                    }
                    if (PlanItemType.WATER in plannedTypes) {
                        LegendDot(AroTheme.macroColors.water, 3.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, size: androidx.compose.ui.unit.Dp = 5.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color, CircleShape),
    )
}
