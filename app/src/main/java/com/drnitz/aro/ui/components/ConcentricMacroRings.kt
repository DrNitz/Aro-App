package com.drnitz.aro.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drnitz.aro.theme.AroTheme

/**
 * Concentric Radial Rings for Calories, Protein, Carbs, and Fat.
 * Hardware-accelerated canvas with smooth entrance animations.
 */
@Composable
fun ConcentricMacroRings(
    caloriesEaten: Int,
    caloriesBurned: Int,
    targetCalories: Int,
    proteinEatenG: Double,
    targetProteinG: Double,
    carbsEatenG: Double,
    targetCarbsG: Double,
    fatEatenG: Double,
    targetFatG: Double,
    modifier: Modifier = Modifier,
    size: Dp = 230.dp,
    strokeWidth: Dp = 12.dp
) {
    val netCalories = caloriesEaten - caloriesBurned
    val remainingCalories = (targetCalories - netCalories).coerceAtLeast(0)

    val targetCalSafe = if (targetCalories > 0) targetCalories else 2000
    val targetProtSafe = if (targetProteinG > 0) targetProteinG else 150.0
    val targetCarbSafe = if (targetCarbsG > 0) targetCarbsG else 200.0
    val targetFatSafe = if (targetFatG > 0) targetFatG else 65.0

    val rawCalSweep = ((netCalories.toFloat() / targetCalSafe.toFloat()) * 360f).coerceIn(0f, 360f)
    val rawProtSweep = (((proteinEatenG / targetProtSafe) * 360f).toFloat()).coerceIn(0f, 360f)
    val rawCarbSweep = (((carbsEatenG / targetCarbSafe) * 360f).toFloat()).coerceIn(0f, 360f)
    val rawFatSweep = (((fatEatenG / targetFatSafe) * 360f).toFloat()).coerceIn(0f, 360f)

    // Animated sweeps for smooth visual feedback
    val animCalSweep = remember { Animatable(0f) }
    val animProtSweep = remember { Animatable(0f) }
    val animCarbSweep = remember { Animatable(0f) }
    val animFatSweep = remember { Animatable(0f) }

    LaunchedEffect(rawCalSweep, rawProtSweep, rawCarbSweep, rawFatSweep) {
        val animSpec = tween<Float>(durationMillis = 900, easing = FastOutSlowInEasing)
        animCalSweep.animateTo(rawCalSweep, animSpec)
        animProtSweep.animateTo(rawProtSweep, animSpec)
        animCarbSweep.animateTo(rawCarbSweep, animSpec)
        animFatSweep.animateTo(rawFatSweep, animSpec)
    }

    val macroColors = AroTheme.macroColors

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Dynamic radii proportional to canvas dimensions
            val baseRadius = this.size.minDimension / 2f - strokePx / 2f
            val ringGap = strokePx + 4.dp.toPx()

            val rCal = baseRadius
            val rProt = baseRadius - ringGap
            val rCarb = baseRadius - (ringGap * 2)
            val rFat = baseRadius - (ringGap * 3)

            val rings = listOf(
                RingData(rCal, animCalSweep.value, macroColors.calories, macroColors.caloriesBg),
                RingData(rProt, animProtSweep.value, macroColors.protein, macroColors.proteinBg),
                RingData(rCarb, animCarbSweep.value, macroColors.carbs, macroColors.carbsBg),
                RingData(rFat, animFatSweep.value, macroColors.fat, macroColors.fatBg)
            )

            rings.forEach { ring ->
                // Background Track
                drawCircle(
                    color = ring.trackColor,
                    radius = ring.radius,
                    center = center,
                    style = Stroke(width = strokePx)
                )

                // Active Progress Arc
                if (ring.sweepAngle > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(ring.activeColor, ring.activeColor.copy(alpha = 0.85f)),
                            center = center
                        ),
                        startAngle = -90f,
                        sweepAngle = ring.sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - ring.radius, center.y - ring.radius),
                        size = Size(ring.radius * 2, ring.radius * 2),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Center Metric Callout
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$remainingCalories",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "kcal left",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class RingData(
    val radius: Float,
    val sweepAngle: Float,
    val activeColor: Color,
    val trackColor: Color
)
