package com.drnitz.aro.ui.plan

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.drnitz.aro.domain.calculation.weekDatesFor
import com.drnitz.aro.domain.model.PlanItemType
import com.drnitz.aro.theme.AroTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Modern collapsible weekly calendar strip.
 *
 * Provides a fast, space-efficient single-week view for daily logging while allowing
 * instant 1-tap expansion to the full month grid.
 */
@Composable
fun AroWeekStrip(
    selectedDate: LocalDate,
    today: LocalDate,
    calendarMonth: YearMonth,
    plannedDates: Map<LocalDate, Set<PlanItemType>>,
    onSelectDate: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val locale = Locale.getDefault()
    val weekStart = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val currentWeekDates = remember(selectedDate, weekStart) {
        weekDatesFor(selectedDate, weekStart)
    }

    val monthLabel = remember(selectedDate, locale) {
        selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
        ) {
            // Header Row: Month title + Expand chevron + Prev/Next buttons + Today jump
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) { isExpanded = !isExpanded }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse to week" else "Expand full month calendar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                if (selectedDate != today) {
                    TextButton(
                        onClick = {
                            onSelectDate(today)
                            onMonthChange(YearMonth.from(today))
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val prevWeekDate = selectedDate.minusWeeks(1)
                        onSelectDate(prevWeekDate)
                        val newMonth = YearMonth.from(prevWeekDate)
                        if (newMonth != calendarMonth) {
                            onMonthChange(newMonth)
                        }
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous week",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }

                IconButton(
                    onClick = {
                        val nextWeekDate = selectedDate.plusWeeks(1)
                        onSelectDate(nextWeekDate)
                        val newMonth = YearMonth.from(nextWeekDate)
                        if (newMonth != calendarMonth) {
                            onMonthChange(newMonth)
                        }
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next week",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Expandable full month view
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    AroMonthCalendar(
                        month = calendarMonth,
                        selectedDate = selectedDate,
                        today = today,
                        plannedDates = plannedDates,
                        onMonthChange = onMonthChange,
                        onDateClick = { clickedDate ->
                            onSelectDate(clickedDate)
                            // Keep expanded or collapse on selection
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Compact 7-Day Single Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                currentWeekDates.forEach { date ->
                    val isSelected = date == selectedDate
                    val isToday = date == today
                    val dayPlans = plannedDates[date].orEmpty()
                    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale).take(3)

                    DayPill(
                        dayName = dayName,
                        dayNumber = date.dayOfMonth,
                        isSelected = isSelected,
                        isToday = isToday,
                        hasPlans = dayPlans.isNotEmpty(),
                        planTypes = dayPlans,
                        onClick = {
                            onSelectDate(date)
                            val newMonth = YearMonth.from(date)
                            if (newMonth != calendarMonth) {
                                onMonthChange(newMonth)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayPill(
    dayName: String,
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasPlans: Boolean,
    planTypes: Set<PlanItemType>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val labelColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val accessibleDescription = buildString {
        append("$dayName ")
        append("$dayNumber")
        if (isToday) append(", today")
        if (isSelected) append(", selected")
        if (hasPlans) {
            append(", planned ")
            append(planTypes.joinToString(" and ") { it.label })
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = accessibleDescription
                this.selected = isSelected
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )

        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(34.dp)
                .then(
                    if (isSelected) {
                        Modifier.background(containerColor, CircleShape)
                    } else if (isToday) {
                        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                ),
                color = contentColor,
            )
        }

        Spacer(Modifier.height(3.dp))

        // Plan indicator dots
        Row(
            modifier = Modifier.height(5.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasPlans) {
                val dotColor = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    planTypes.contains(PlanItemType.EXERCISE) -> AroTheme.macroColors.cardio
                    planTypes.contains(PlanItemType.PILL) -> AroTheme.macroColors.clinicalOptimal
                    planTypes.contains(PlanItemType.FOOD) -> AroTheme.macroColors.calories
                    planTypes.contains(PlanItemType.WATER) -> AroTheme.macroColors.water
                    planTypes.contains(PlanItemType.FASTING) -> AroTheme.macroColors.fasting
                    else -> MaterialTheme.colorScheme.primary
                }
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(dotColor, CircleShape),
                )
            } else {
                Spacer(Modifier.size(4.dp))
            }
        }
    }
}
