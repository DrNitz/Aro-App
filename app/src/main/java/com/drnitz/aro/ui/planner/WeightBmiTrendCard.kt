package com.drnitz.aro.ui.planner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drnitz.aro.domain.calculation.BmiCategory
import com.drnitz.aro.domain.calculation.BmiResult
import com.drnitz.aro.domain.calculation.bodyMassIndex
import com.drnitz.aro.domain.model.UserProfile
import com.drnitz.aro.theme.AroClinicalHighRisk
import com.drnitz.aro.theme.AroClinicalModerate
import com.drnitz.aro.theme.AroClinicalOptimal
import com.drnitz.aro.theme.AroTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private enum class WeightTrendMetric(val label: String) {
    WEIGHT("Weight"),
    BMI("BMI"),
}

private data class WeightTrendRange(val label: String, val days: Int?)

private data class WeightTrendPoint(
    val epochDay: Long,
    val value: Double,
)

private val weightTrendRanges = listOf(
    WeightTrendRange("30D", 30),
    WeightTrendRange("90D", 90),
    WeightTrendRange("1Y", 365),
    WeightTrendRange("All", null),
)

/**
 * Combined weight history, BMI and auto-scaled trend graph.
 *
 * The graph uses the patient's logged weigh-ins as its source of truth. BMI is recalculated
 * for every plotted point from the current profile height, so it stays consistent with the
 * weight axis. Range chips only filter the visible window; the y-axis always scales to the
 * selected points and metric.
 */
@Composable
internal fun WeightBmiTrendCard(
    profile: UserProfile,
    bmiResult: BmiResult?,
    weightHistory: List<Pair<LocalDate, Double>>,
    onOpenWeighIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var metric by remember { mutableStateOf(WeightTrendMetric.WEIGHT) }
    var selectedRange by remember { mutableStateOf<Int?>(null) }

    val orderedHistory = remember(weightHistory) {
        weightHistory
            .distinctBy { it.first }
            .sortedBy { it.first }
    }
    val visibleHistory = remember(orderedHistory, selectedRange) {
        val activeRange = selectedRange
        if (activeRange == null || orderedHistory.isEmpty()) {
            orderedHistory
        } else {
            val lastDate = orderedHistory.last().first
            val startDate = lastDate.minusDays((activeRange - 1).toLong())
            orderedHistory.filter { !it.first.isBefore(startDate) }
        }
    }
    val points = remember(visibleHistory, profile.heightCm, metric) {
        visibleHistory.mapNotNull { (date, weightKg) ->
            val value = when (metric) {
                WeightTrendMetric.WEIGHT -> weightKg
                WeightTrendMetric.BMI -> bodyMassIndex(weightKg, profile.heightCm).valueOrNull()?.value
            }
            value?.let { WeightTrendPoint(date.toEpochDay(), it) }
        }
    }

    val currentBmi = bmiResult ?: bodyMassIndex(profile.weightKg, profile.heightCm).valueOrNull()
    val rangeChangeKg = visibleHistory.takeIf { it.size >= 2 }?.let { entries ->
        entries.last().second - entries.first().second
    }
    val rangeLabel = weightTrendRanges.first { it.days == selectedRange }.label

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WEIGHT, BMI & TREND",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Every weigh-in updates BMI and the auto-scaled chart",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onOpenWeighIn) {
                    Icon(
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TrendStatTile(
                    label = "CURRENT",
                    value = "${formatDecimal(profile.weightKg)} kg",
                    subtitle = orderedHistory.lastOrNull()
                        ?.let { "Last logged ${it.first.format(shortDateFormatter)}" }
                        ?: "No weigh-ins yet",
                    accent = AroTheme.macroColors.water,
                    modifier = Modifier.weight(1f),
                )
                TrendStatTile(
                    label = "BMI",
                    value = currentBmi?.value?.let(::formatDecimal) ?: "—",
                    subtitle = currentBmi?.category?.label ?: "Enter height & weight",
                    accent = currentBmi?.category?.color() ?: MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TrendStatTile(
                    label = "CHANGE",
                    value = rangeChangeKg?.let { signedWeight(it) } ?: "—",
                    subtitle = if (rangeChangeKg == null) "Need 2 entries" else "in $rangeLabel",
                    accent = AroTheme.macroColors.clinicalOptimal,
                    modifier = Modifier.weight(1f),
                )
            }

            if (orderedHistory.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Scale,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = "No weigh-ins logged yet",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Log a weight above to start the dynamic weight and BMI graph.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        weightTrendRanges.forEach { range ->
                            FilterChip(
                                selected = selectedRange == range.days,
                                onClick = { selectedRange = range.days },
                                label = { Text(range.label) },
                                colors = trendChipColors(),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        WeightTrendMetric.entries.forEach { option ->
                            FilterChip(
                                selected = metric == option,
                                onClick = { metric = option },
                                label = { Text(option.label) },
                                colors = trendChipColors(),
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Auto-scaled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                WeightTrendChart(
                    points = points,
                    metric = metric,
                    targetWeightKg = profile.targetWeightKg,
                )

                if (points.size == 1) {
                    Text(
                        text = "Log another weigh-in to turn this point into a trend.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    text = "RECENT WEIGH-INS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )

                orderedHistory.takeLast(5).asReversed().forEach { (date, kg) ->
                    val entryBmi = bodyMassIndex(kg, profile.heightCm).valueOrNull()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = date.format(recentDateFormatter),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            entryBmi?.let { bmi ->
                                Text(
                                    text = "BMI ${formatDecimal(bmi.value)} • ${bmi.category.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = bmi.category.color(),
                                )
                            }
                        }
                        Text(
                            text = "${formatDecimal(kg)} kg",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            WeightGoalProjection(profile = profile)
        }
    }
}

@Composable
private fun TrendStatTile(
    label: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.10f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WeightTrendChart(
    points: List<WeightTrendPoint>,
    metric: WeightTrendMetric,
    targetWeightKg: Double,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return

    val accent = when (metric) {
        WeightTrendMetric.WEIGHT -> AroTheme.macroColors.water
        WeightTrendMetric.BMI -> AroTheme.macroColors.protein
    }
    val yDomain = remember(points, metric) { trendYDomain(points, metric) }
    val yMin = yDomain.first
    val yMax = yDomain.second
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val bandColor = AroClinicalOptimal.copy(alpha = 0.10f)
    val dotOutlineColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val pointRadius = 3.5.dp
    val lastPointRadius = 5.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
        ) {
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = formatDecimal(yMax),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatDecimal((yMax + yMin) / 2.0),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatDecimal(yMin),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                    ),
            ) {
                if (size.width <= 0f || size.height <= 0f || yMax <= yMin) return@Canvas

                val firstDay = points.first().epochDay
                val lastDay = points.last().epochDay
                val daySpan = (lastDay - firstDay).coerceAtLeast(1L).toFloat()
                val xInset = 6.dp.toPx()
                val plotWidth = (size.width - (xInset * 2f)).coerceAtLeast(1f)
                fun xFor(epochDay: Long): Float =
                    if (points.size == 1) size.width / 2f
                    else xInset + (((epochDay - firstDay).toFloat() / daySpan) * plotWidth)

                fun yFor(value: Double): Float {
                    val fraction = ((value - yMin) / (yMax - yMin)).toFloat()
                    return size.height - (fraction * size.height)
                }

                if (metric == WeightTrendMetric.BMI) {
                    val bandTopValue = min(24.9, yMax)
                    val bandBottomValue = max(18.5, yMin)
                    if (bandTopValue > bandBottomValue) {
                        val top = yFor(bandTopValue)
                        drawRect(
                            color = bandColor,
                            topLeft = Offset(0f, top),
                            size = Size(size.width, (yFor(bandBottomValue) - top).coerceAtLeast(0f)),
                        )
                    }
                }

                if (metric == WeightTrendMetric.WEIGHT && targetWeightKg in yMin..yMax) {
                    drawLine(
                        color = accent.copy(alpha = 0.55f),
                        start = Offset(0f, yFor(targetWeightKg)),
                        end = Offset(size.width, yFor(targetWeightKg)),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                    )
                }

                listOf(0f, 0.5f, 1f).forEach { fraction ->
                    val y = size.height * fraction
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                if (points.size > 1) {
                    val fillPath = Path().apply {
                        moveTo(xFor(points.first().epochDay), size.height)
                        points.forEach { point ->
                            lineTo(xFor(point.epochDay), yFor(point.value))
                        }
                        lineTo(xFor(points.last().epochDay), size.height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(accent.copy(alpha = 0.28f), Color.Transparent),
                            startY = 0f,
                            endY = size.height,
                        ),
                    )

                    val linePath = Path().apply {
                        moveTo(xFor(points.first().epochDay), yFor(points.first().value))
                        points.drop(1).forEach { point ->
                            lineTo(xFor(point.epochDay), yFor(point.value))
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = accent,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }

                points.forEachIndexed { index, point ->
                    val center = Offset(xFor(point.epochDay), yFor(point.value))
                    val isLast = index == points.lastIndex
                    val radius = if (isLast) lastPointRadius.toPx() else pointRadius.toPx()
                    drawCircle(
                        color = dotOutlineColor,
                        radius = radius + 1.5.dp.toPx(),
                        center = center,
                    )
                    drawCircle(
                        color = accent,
                        radius = radius,
                        center = center,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (points.size == 1) {
            Text(
                text = "1 weigh-in • ${LocalDate.ofEpochDay(points.first().epochDay).format(shortDateFormatter)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = LocalDate.ofEpochDay(points.first().epochDay).format(shortDateFormatter),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${points.size} weigh-ins",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = LocalDate.ofEpochDay(points.last().epochDay).format(shortDateFormatter),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = when (metric) {
                WeightTrendMetric.WEIGHT ->
                    "Dashed line: target ${formatDecimal(targetWeightKg)} kg"
                WeightTrendMetric.BMI ->
                    "Shaded band: WHO healthy BMI 18.5–24.9"
            },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeightGoalProjection(profile: UserProfile) {
    val delta = profile.weightDeltaKg
    if (abs(delta) < 0.1) {
        Text(
            text = "At target weight.",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = AroTheme.macroColors.clinicalOptimal,
        )
        return
    }

    val direction = if (delta > 0) "to lose" else "to gain"
    val weeklyChange = profile.estimatedWeeklyChangeKg()
    val timeline = if (abs(weeklyChange) > 0.01) {
        val weeks = (abs(delta) / abs(weeklyChange)).coerceAtLeast(1.0)
        " • ~${weeks.toInt().coerceAtLeast(1)} weeks at ${formatDecimal(abs(weeklyChange))} kg/week"
    } else {
        ""
    }

    Text(
        text = "${formatDecimal(abs(delta))} kg $direction to reach ${formatDecimal(profile.targetWeightKg)} kg$timeline",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun trendYDomain(
    points: List<WeightTrendPoint>,
    metric: WeightTrendMetric,
): Pair<Double, Double> {
    val smallest = points.minOf { it.value }
    val largest = points.maxOf { it.value }
    val spread = largest - smallest
    val padding = when {
        spread < 0.2 -> 0.5
        else -> max(0.4, spread * 0.18)
    }
    var low = smallest - padding
    var high = largest + padding
    if (metric == WeightTrendMetric.WEIGHT) {
        low = max(0.0, low)
    }
    if (high <= low) high = low + 1.0
    return low to high
}

@Composable
private fun trendChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

private fun BmiCategory.color(): Color = when (this) {
    BmiCategory.NORMAL -> AroClinicalOptimal
    BmiCategory.UNDERWEIGHT, BmiCategory.OVERWEIGHT -> AroClinicalModerate
    BmiCategory.OBESE -> AroClinicalHighRisk
}

private fun formatDecimal(value: Double): String =
    String.format(Locale.US, "%.1f", value)

private fun signedWeight(value: Double): String {
    val prefix = if (value > 0.0) "+" else ""
    return "$prefix${formatDecimal(value)} kg"
}

private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
private val recentDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US)
