package com.drnitz.aro.ui.plan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
 * Ultra-compact, sleek weekly calendar strip.
 *
 * Provides a low-profile single-week view with modern capsule day pills,
 * animated month dropdown, and horizontal week swiping.
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
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val locale = Locale.getDefault()
    val weekStart = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val currentWeekDates = remember(selectedDate, weekStart) {
        weekDatesFor(selectedDate, weekStart)
    }

    val monthLabel = remember(selectedDate, locale) {
        selectedDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            // Sleek Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Month & Year selector chip
                val chevronAngle by animateFloatAsState(
                    targetValue = if (isExpanded) 180f else 0f,
                    label = "chevronAngle",
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) { isExpanded = !isExpanded }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse calendar" else "Expand calendar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(chevronAngle),
                    )
                }

                // Controls: Today jump + Prev / Next week
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AnimatedVisibility(
                        visible = selectedDate != today,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally(),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(role = Role.Button) {
                                    onSelectDate(today)
                                    val newMonth = YearMonth.from(today)
                                    if (newMonth != calendarMonth) {
                                        onMonthChange(newMonth)
                                    }
                                },
                        ) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            )
                        }
                    }

                    // Prev week button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) {
                                val prevWeekDate = selectedDate.minusWeeks(1)
                                onSelectDate(prevWeekDate)
                                val newMonth = YearMonth.from(prevWeekDate)
                                if (newMonth != calendarMonth) {
                                    onMonthChange(newMonth)
                                }
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous week",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    // Next week button
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button) {
                                val nextWeekDate = selectedDate.plusWeeks(1)
                                onSelectDate(nextWeekDate)
                                val newMonth = YearMonth.from(nextWeekDate)
                                if (newMonth != calendarMonth) {
                                    onMonthChange(newMonth)
                                }
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next week",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

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
                            isExpanded = false
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Compact 7-Day Single Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(selectedDate) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccumulator = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                dragAccumulator += dragAmount
                            },
                            onDragEnd = {
                                if (dragAccumulator > 60f) {
                                    val prevWeekDate = selectedDate.minusWeeks(1)
                                    onSelectDate(prevWeekDate)
                                    val newMonth = YearMonth.from(prevWeekDate)
                                    if (newMonth != calendarMonth) {
                                        onMonthChange(newMonth)
                                    }
                                } else if (dragAccumulator < -60f) {
                                    val nextWeekDate = selectedDate.plusWeeks(1)
                                    onSelectDate(nextWeekDate)
                                    val newMonth = YearMonth.from(nextWeekDate)
                                    if (newMonth != calendarMonth) {
                                        onMonthChange(newMonth)
                                    }
                                }
                                dragAccumulator = 0f
                            },
                            onDragCancel = { dragAccumulator = 0f },
                        )
                    },
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val labelColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderStroke = when {
        isSelected -> null
        isToday -> BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
        else -> BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
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

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = borderStroke,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = accessibleDescription
                this.selected = isSelected
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(vertical = 5.dp, horizontal = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = dayName.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                ),
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )

            Text(
                text = dayNumber.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Bold,
                ),
                color = contentColor,
                maxLines = 1,
            )

            // Micro plan indicator or today dot
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
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
                } else if (isToday && !isSelected) {
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                    )
                }
            }
        }
    }
}

