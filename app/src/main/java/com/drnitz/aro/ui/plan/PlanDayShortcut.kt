package com.drnitz.aro.ui.plan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drnitz.aro.theme.AroTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Diary shortcut directly below the calendar.
 *
 * The label follows the selected day so it remains honest when the user browses a
 * future date; opening it always lands on the full-screen Plan view, not inside the
 * Diary log.
 */
@Composable
fun PlanDayShortcut(
    date: LocalDate,
    today: LocalDate,
    itemCount: Int,
    plannedCalories: Int,
    onOpenPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = when (date) {
        today -> "Today's plan"
        today.plusDays(1) -> "Tomorrow's plan"
        else -> "Plan for " + date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    }
    val subtitle = if (itemCount == 0) {
        "Nothing planned yet · add food, a workout, a pill, fasting or water"
    } else {
        "$itemCount planned item${if (itemCount == 1) "" else "s"}" +
            if (plannedCalories > 0) " · $plannedCalories kcal planned" else ""
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onOpenPlan),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = AroTheme.macroColors.calories,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open plan",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
