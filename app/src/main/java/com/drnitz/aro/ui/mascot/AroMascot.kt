package com.drnitz.aro.ui.mascot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.drnitz.aro.theme.AroTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Pure Native Jetpack Compose Canvas Mascot ("Aro Bot").
 *
 * Runs at 60-120 FPS with zero external runtime overhead, zero image assets,
 * full dark/light theme adaptability, natural blinking, breathing physics,
 * and mood-reactive facial expressions.
 */
@Composable
fun AroMascot(
    mood: MascotMood,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = "Aro Assistant Mascot",
) {
    val coroutineScope = rememberCoroutineScope()
    val bounceScale = remember { Animatable(1f) }
    val bounceRotation = remember { Animatable(0f) }

    // Natural involuntary blinking physics
    var blinkProgress by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2800, 4800))
            animate(1f, 0.08f, animationSpec = tween(90, easing = FastOutLinearInEasing)) { value, _ ->
                blinkProgress = value
            }
            animate(0.08f, 1f, animationSpec = tween(110, easing = LinearOutSlowInEasing)) { value, _ ->
                blinkProgress = value
            }
        }
    }

    // Infinite breathing and micro-pulsing loop
    val infiniteTransition = rememberInfiniteTransition(label = "AroMascotBreathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathingScale",
    )
    val antennaPulse by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "antennaPulse",
    )
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "auraAlpha",
    )

    // Dynamic theme colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceLow = MaterialTheme.colorScheme.surfaceContainerLow
    val outlineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val waterColor = AroTheme.macroColors.water
    val fastingColor = AroTheme.macroColors.fasting

    val auraColor = when (mood) {
        MascotMood.FASTING_ZEN -> fastingColor.copy(alpha = auraAlpha)
        MascotMood.THIRSTY -> waterColor.copy(alpha = auraAlpha)
        MascotMood.CELEBRATING -> primaryColor.copy(alpha = auraAlpha * 1.3f)
        MascotMood.SLEEPY -> MaterialTheme.colorScheme.tertiary.copy(alpha = auraAlpha * 0.7f)
        else -> primaryColor.copy(alpha = auraAlpha)
    }

    val eyeGlowColor = when (mood) {
        MascotMood.FASTING_ZEN -> fastingColor
        MascotMood.THIRSTY -> waterColor
        MascotMood.CELEBRATING -> primaryColor
        MascotMood.SLEEPY -> MaterialTheme.colorScheme.tertiary
        else -> primaryColor
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Button,
        ) {
            coroutineScope.launch {
                // Interactive squish-and-stretch spring reaction
                bounceRotation.animateTo(if (Random.nextBoolean()) 9f else -9f, tween(80))
                bounceScale.animateTo(0.82f, tween(70))
                bounceRotation.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 400f))
                bounceScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 450f))
            }
            onClick()
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(breathingScale * bounceScale.value)
            .rotate(bounceRotation.value)
            .then(clickModifier)
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = "$contentDescription: ${mood.label}"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f

            // 1. Outer Ambient Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(auraColor, Color.Transparent),
                    center = Offset(cx, cy),
                    radius = w * 0.48f,
                ),
                radius = w * 0.48f,
                center = Offset(cx, cy),
            )

            // 2. Head Antenna
            val antennaBaseY = cy - (h * 0.32f)
            val antennaTipY = cy - (h * 0.44f)
            drawLine(
                color = outlineColor,
                start = Offset(cx, antennaBaseY),
                end = Offset(cx, antennaTipY),
                strokeWidth = w * 0.045f,
                cap = StrokeCap.Round,
            )
            // Antenna Bulb / LED
            drawCircle(
                color = eyeGlowColor.copy(alpha = 0.85f),
                radius = w * 0.055f * antennaPulse,
                center = Offset(cx, antennaTipY),
            )

            // 3. Chassis Capsule Body
            val bodyWidth = w * 0.74f
            val bodyHeight = h * 0.68f
            val bodyLeft = (w - bodyWidth) / 2f
            val bodyTop = (h - bodyHeight) / 2f + (h * 0.02f)
            val bodyCornerRadius = CornerRadius(bodyWidth * 0.42f, bodyHeight * 0.42f)

            // Chassis gradient fill
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(surfaceHigh, surfaceLow),
                    startY = bodyTop,
                    endY = bodyTop + bodyHeight,
                ),
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = bodyCornerRadius,
            )

            // Chassis outline
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = bodyCornerRadius,
                style = Stroke(width = w * 0.035f),
            )

            // 4. Glossy Visor Screen
            val visorWidth = bodyWidth * 0.78f
            val visorHeight = bodyHeight * 0.58f
            val visorLeft = cx - (visorWidth / 2f)
            val visorTop = cy - (visorHeight / 2f) + (h * 0.02f)
            val visorCorner = CornerRadius(visorWidth * 0.28f, visorHeight * 0.28f)

            drawRoundRect(
                color = Color(0xFF0F172A), // Deep obsidian visor
                topLeft = Offset(visorLeft, visorTop),
                size = Size(visorWidth, visorHeight),
                cornerRadius = visorCorner,
            )

            // Visor Glass Specular Shine
            val shinePath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(
                            left = visorLeft + (visorWidth * 0.08f),
                            top = visorTop + (visorHeight * 0.08f),
                            right = visorLeft + (visorWidth * 0.92f),
                            bottom = visorTop + (visorHeight * 0.42f),
                        ),
                        cornerRadius = CornerRadius(visorWidth * 0.16f, visorHeight * 0.16f),
                    ),
                )
            }
            drawPath(
                path = shinePath,
                color = Color.White.copy(alpha = 0.08f),
                style = Fill,
            )

            // 5. Expressive Cyber Eyes & Mood Face
            val eyeSpacing = visorWidth * 0.26f
            val leftEyeCenterX = cx - eyeSpacing
            val rightEyeCenterX = cx + eyeSpacing
            val eyeCenterY = visorTop + (visorHeight * 0.52f)
            val baseEyeWidth = visorWidth * 0.20f
            val baseEyeHeight = visorHeight * 0.36f * blinkProgress

            when (mood) {
                MascotMood.HAPPY, MascotMood.CELEBRATING -> {
                    // Joyful upward curving smiling arcs: ^ ^
                    val arcHeight = visorHeight * 0.24f
                    val arcWidth = visorWidth * 0.22f
                    val leftArcPath = Path().apply {
                        moveTo(leftEyeCenterX - (arcWidth / 2f), eyeCenterY + (arcHeight * 0.3f))
                        quadraticTo(
                            leftEyeCenterX,
                            eyeCenterY - arcHeight,
                            leftEyeCenterX + (arcWidth / 2f),
                            eyeCenterY + (arcHeight * 0.3f),
                        )
                    }
                    val rightArcPath = Path().apply {
                        moveTo(rightEyeCenterX - (arcWidth / 2f), eyeCenterY + (arcHeight * 0.3f))
                        quadraticTo(
                            rightEyeCenterX,
                            eyeCenterY - arcHeight,
                            rightEyeCenterX + (arcWidth / 2f),
                            eyeCenterY + (arcHeight * 0.3f),
                        )
                    }
                    drawPath(
                        path = leftArcPath,
                        color = eyeGlowColor,
                        style = Stroke(width = w * 0.055f, cap = StrokeCap.Round),
                    )
                    drawPath(
                        path = rightArcPath,
                        color = eyeGlowColor,
                        style = Stroke(width = w * 0.055f, cap = StrokeCap.Round),
                    )
                }

                MascotMood.FASTING_ZEN -> {
                    // Meditative calm curved lines: - -
                    val zenWidth = visorWidth * 0.22f
                    drawLine(
                        color = eyeGlowColor,
                        start = Offset(leftEyeCenterX - (zenWidth / 2f), eyeCenterY),
                        end = Offset(leftEyeCenterX + (zenWidth / 2f), eyeCenterY),
                        strokeWidth = w * 0.05f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = eyeGlowColor,
                        start = Offset(rightEyeCenterX - (zenWidth / 2f), eyeCenterY),
                        end = Offset(rightEyeCenterX + (zenWidth / 2f), eyeCenterY),
                        strokeWidth = w * 0.05f,
                        cap = StrokeCap.Round,
                    )
                }

                MascotMood.SLEEPY -> {
                    // Relaxed droopy downward arcs: u u
                    val droopWidth = visorWidth * 0.20f
                    val droopHeight = visorHeight * 0.16f
                    val leftDroopPath = Path().apply {
                        moveTo(leftEyeCenterX - (droopWidth / 2f), eyeCenterY - (droopHeight * 0.2f))
                        quadraticTo(
                            leftEyeCenterX,
                            eyeCenterY + droopHeight,
                            leftEyeCenterX + (droopWidth / 2f),
                            eyeCenterY - (droopHeight * 0.2f),
                        )
                    }
                    val rightDroopPath = Path().apply {
                        moveTo(rightEyeCenterX - (droopWidth / 2f), eyeCenterY - (droopHeight * 0.2f))
                        quadraticTo(
                            rightEyeCenterX,
                            eyeCenterY + droopHeight,
                            rightEyeCenterX + (droopWidth / 2f),
                            eyeCenterY - (droopHeight * 0.2f),
                        )
                    }
                    drawPath(
                        path = leftDroopPath,
                        color = eyeGlowColor.copy(alpha = 0.75f),
                        style = Stroke(width = w * 0.045f, cap = StrokeCap.Round),
                    )
                    drawPath(
                        path = rightDroopPath,
                        color = eyeGlowColor.copy(alpha = 0.75f),
                        style = Stroke(width = w * 0.045f, cap = StrokeCap.Round),
                    )
                }

                else -> {
                    // IDLE, THIRSTY, THINKING: Glowing pill eyes with specular catchlight
                    if (baseEyeHeight > 1.5f) {
                        // Left Eye
                        drawRoundRect(
                            color = eyeGlowColor,
                            topLeft = Offset(leftEyeCenterX - (baseEyeWidth / 2f), eyeCenterY - (baseEyeHeight / 2f)),
                            size = Size(baseEyeWidth, baseEyeHeight),
                            cornerRadius = CornerRadius(baseEyeWidth / 2f, baseEyeWidth / 2f),
                        )
                        // Right Eye
                        drawRoundRect(
                            color = eyeGlowColor,
                            topLeft = Offset(rightEyeCenterX - (baseEyeWidth / 2f), eyeCenterY - (baseEyeHeight / 2f)),
                            size = Size(baseEyeWidth, baseEyeHeight),
                            cornerRadius = CornerRadius(baseEyeWidth / 2f, baseEyeWidth / 2f),
                        )

                        // Catchlight shine on pupils (only when eyes are open)
                        if (blinkProgress > 0.6f) {
                            val shineRadius = baseEyeWidth * 0.22f
                            drawCircle(
                                color = Color.White.copy(alpha = 0.9f),
                                radius = shineRadius,
                                center = Offset(
                                    leftEyeCenterX - (baseEyeWidth * 0.16f),
                                    eyeCenterY - (baseEyeHeight * 0.20f),
                                ),
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.9f),
                                radius = shineRadius,
                                center = Offset(
                                    rightEyeCenterX - (baseEyeWidth * 0.16f),
                                    eyeCenterY - (baseEyeHeight * 0.20f),
                                ),
                            )
                        }
                    }
                }
            }

            // 6. Cute Rosy Cheeks
            val cheekRadius = visorWidth * 0.09f
            val cheekY = visorTop + (visorHeight * 0.75f)
            val cheekColor = Color(0xFFF43F5E).copy(alpha = 0.28f)
            drawCircle(
                color = cheekColor,
                radius = cheekRadius,
                center = Offset(leftEyeCenterX - (visorWidth * 0.08f), cheekY),
            )
            drawCircle(
                color = cheekColor,
                radius = cheekRadius,
                center = Offset(rightEyeCenterX + (visorWidth * 0.08f), cheekY),
            )

            // 7. Thirsty Mood Water Droplet Marker
            if (mood == MascotMood.THIRSTY) {
                val dropCx = visorLeft + visorWidth - (w * 0.02f)
                val dropCy = visorTop + (visorHeight * 0.26f)
                val dropRadius = w * 0.048f
                drawCircle(
                    color = waterColor,
                    radius = dropRadius,
                    center = Offset(dropCx, dropCy),
                )
            }
        }
    }
}
