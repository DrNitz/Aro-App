package com.drnitz.aro.ui.mascot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
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
import kotlin.math.sin
import kotlin.random.Random

/**
 * Expressive Native Cartoon Mascot ("Aro the Pocket Companion").
 *
 * Runs at 60-120 FPS using pure Jetpack Compose Canvas vector rendering.
 * Hangs playfully from the top bar, waves, pumps mini dumbbells, sips water,
 * nibbles healthy apples, meditates in fasting flow, and celebrates milestones!
 */
@Composable
fun AroMascot(
    mood: MascotMood,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = "Aro Cartoon Companion",
) {
    val coroutineScope = rememberCoroutineScope()
    val bounceScale = remember { Animatable(1f) }
    val bounceRotation = remember { Animatable(0f) }

    // Natural involuntary blinking physics
    var blinkProgress by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2600, 4400))
            animate(1f, 0.05f, animationSpec = tween(85, easing = FastOutLinearInEasing)) { value, _ ->
                blinkProgress = value
            }
            animate(0.05f, 1f, animationSpec = tween(100, easing = LinearOutSlowInEasing)) { value, _ ->
                blinkProgress = value
            }
        }
    }

    // Infinite breathing and micro-motion loops
    val infiniteTransition = rememberInfiniteTransition(label = "AroCartoonMotion")

    // Gentle pendulum hanging sway
    val hangingSway by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hangingSway",
    )

    // Breathing tummy expansion
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathingScale",
    )

    // Energetic waving paw motion (for WELCOMING)
    val wavingAngle by infiniteTransition.animateFloat(
        initialValue = -18f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wavingAngle",
    )

    // Dumbbell curl pumping motion (for WORKOUT_BEAST)
    val dumbbellPump by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dumbbellPump",
    )

    // Zen floating hover (for FASTING_ZEN)
    val zenHover by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "zenHover",
    )

    // Continuous particle rotation (for CELEBRATING confetti)
    val particleSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "particleSpin",
    )

    // Dynamic theme colors (pre-evaluated outside DrawScope)
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val waterColor = AroTheme.macroColors.water
    val fastingColor = AroTheme.macroColors.fasting
    val calorieColor = AroTheme.macroColors.calories
    val cardioColor = AroTheme.macroColors.cardio

    // Character body colors
    val furMain = Color(0xFFF8FAFC) // Pearlescent soft cream coat
    val furShading = Color(0xFFE2E8F0) // Subtle shadow
    val furHighlight = Color.White
    val innerEarColor = Color(0xFFFDA4AF) // Soft blush pink
    val cheekBlush = Color(0xFFFB7185).copy(alpha = 0.55f)
    val darkOutline = Color(0xFF1E293B) // Rich espresso outline
    val bellyPatch = Color(0xFFE0F2FE) // Soft sky/mint belly
    val tongueColor = Color(0xFFF43F5E)

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Button,
        ) {
            coroutineScope.launch {
                // Interactive spring squish and celebratory wobble
                bounceRotation.animateTo(if (Random.nextBoolean()) 14f else -14f, tween(70))
                bounceScale.animateTo(0.82f, tween(60))
                bounceRotation.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 420f))
                bounceScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 460f))
            }
            onClick()
        }
    } else {
        Modifier
    }

    val finalSway = if (mood == MascotMood.FASTING_ZEN) 0f else hangingSway

    Box(
        modifier = modifier
            .size(size)
            .scale(breathingScale * bounceScale.value)
            .rotate(finalSway + bounceRotation.value)
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
            val cy = h / 2f + (if (mood == MascotMood.FASTING_ZEN) zenHover else 0f)

            // -------------------------------------------------------------
            // 1. BACKGROUND PARTICLES & AURAS (Confetti, Zen Halo, Nightcap Zzz)
            // -------------------------------------------------------------
            when (mood) {
                MascotMood.FASTING_ZEN -> {
                    // Soft glowing zen aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(fastingColor.copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = w * 0.52f,
                        ),
                        radius = w * 0.52f,
                        center = Offset(cx, cy),
                    )
                    // Golden Zen Halo ring above head
                    val haloY = cy - (h * 0.38f)
                    drawOval(
                        color = Color(0xFFF59E0B).copy(alpha = 0.85f),
                        topLeft = Offset(cx - (w * 0.22f), haloY - (h * 0.05f)),
                        size = Size(w * 0.44f, h * 0.10f),
                        style = Stroke(width = w * 0.04f),
                    )
                }

                MascotMood.CELEBRATING -> {
                    // Exploding victory confetti stars and diamonds
                    val confettiColors = listOf(
                        calorieColor,
                        waterColor,
                        cardioColor,
                        Color(0xFFFBBF24),
                        Color(0xFFA855F7),
                    )
                    for (i in 0 until 6) {
                        val angle = Math.toRadians((particleSpin + i * 60.0)).toFloat()
                        val dist = w * 0.44f
                        val px = cx + (dist * kotlin.math.cos(angle))
                        val py = cy + (dist * kotlin.math.sin(angle))
                        drawCircle(
                            color = confettiColors[i % confettiColors.size],
                            radius = w * 0.035f,
                            center = Offset(px, py),
                        )
                    }
                }

                MascotMood.SLEEPY -> {
                    // Floating Zzz indicators
                    val zColor = primaryColor.copy(alpha = 0.75f)
                    val zPath = Path().apply {
                        val zx = cx + (w * 0.32f)
                        val zy = cy - (h * 0.28f)
                        moveTo(zx, zy)
                        lineTo(zx + (w * 0.08f), zy)
                        lineTo(zx, zy + (h * 0.08f))
                        lineTo(zx + (w * 0.08f), zy + (h * 0.08f))
                    }
                    drawPath(path = zPath, color = zColor, style = Stroke(width = w * 0.035f))
                }

                else -> {}
            }

            // -------------------------------------------------------------
            // 2. CUTE ROUNDED EARS (with soft inner ear blush)
            // -------------------------------------------------------------
            val earRadius = w * 0.16f
            val earY = cy - (h * 0.28f)
            val leftEarX = cx - (w * 0.26f)
            val rightEarX = cx + (w * 0.26f)

            // Left Ear
            drawCircle(color = furMain, radius = earRadius, center = Offset(leftEarX, earY))
            drawCircle(color = darkOutline, radius = earRadius, center = Offset(leftEarX, earY), style = Stroke(w * 0.035f))
            drawCircle(color = innerEarColor, radius = earRadius * 0.62f, center = Offset(leftEarX, earY))

            // Right Ear
            drawCircle(color = furMain, radius = earRadius, center = Offset(rightEarX, earY))
            drawCircle(color = darkOutline, radius = earRadius, center = Offset(rightEarX, earY), style = Stroke(w * 0.035f))
            drawCircle(color = innerEarColor, radius = earRadius * 0.62f, center = Offset(rightEarX, earY))

            // -------------------------------------------------------------
            // 3. MAIN CARTOON BODY & HEAD (chubby, friendly silhouette)
            // -------------------------------------------------------------
            val headWidth = w * 0.72f
            val headHeight = h * 0.64f
            val headLeft = cx - (headWidth / 2f)
            val headTop = cy - (headHeight / 2f) + (h * 0.04f)

            // Body shadow
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(furHighlight, furShading),
                    startY = headTop,
                    endY = headTop + headHeight,
                ),
                topLeft = Offset(headLeft, headTop),
                size = Size(headWidth, headHeight),
                cornerRadius = CornerRadius(headWidth * 0.44f, headHeight * 0.44f),
            )
            // Body Outline
            drawRoundRect(
                color = darkOutline,
                topLeft = Offset(headLeft, headTop),
                size = Size(headWidth, headHeight),
                cornerRadius = CornerRadius(headWidth * 0.44f, headHeight * 0.44f),
                style = Stroke(width = w * 0.04f),
            )

            // Soft belly patch
            val bellyWidth = headWidth * 0.60f
            val bellyHeight = headHeight * 0.38f
            drawRoundRect(
                color = bellyPatch.copy(alpha = 0.75f),
                topLeft = Offset(cx - (bellyWidth / 2f), headTop + (headHeight * 0.58f)),
                size = Size(bellyWidth, bellyHeight),
                cornerRadius = CornerRadius(bellyWidth * 0.38f, bellyHeight * 0.38f),
            )

            // Cute Sprout / Tuft on top of head
            val sproutY = headTop - (h * 0.06f)
            val sproutPath = Path().apply {
                moveTo(cx, headTop + (h * 0.02f))
                quadraticTo(cx - (w * 0.06f), sproutY, cx, sproutY)
                quadraticTo(cx + (w * 0.06f), sproutY, cx, headTop + (h * 0.02f))
            }
            drawPath(path = sproutPath, color = primaryColor)
            drawPath(path = sproutPath, color = darkOutline, style = Stroke(w * 0.025f))

            // -------------------------------------------------------------
            // 4. BIG EXPRESSIVE CARTOON EYES & EMOTIONS
            // -------------------------------------------------------------
            val eyeSpacing = headWidth * 0.24f
            val leftEyeX = cx - eyeSpacing
            val rightEyeX = cx + eyeSpacing
            val eyeY = cy - (h * 0.02f)
            val eyeRadius = w * 0.09f
            val currentBlink = blinkProgress

            when (mood) {
                MascotMood.FASTING_ZEN -> {
                    // Peaceful serene sleeping crescents: ︶ ︶
                    val zenArcW = w * 0.14f
                    val leftZen = Path().apply {
                        moveTo(leftEyeX - (zenArcW / 2f), eyeY)
                        quadraticTo(leftEyeX, eyeY + (h * 0.05f), leftEyeX + (zenArcW / 2f), eyeY)
                    }
                    val rightZen = Path().apply {
                        moveTo(rightEyeX - (zenArcW / 2f), eyeY)
                        quadraticTo(rightEyeX, eyeY + (h * 0.05f), rightEyeX + (zenArcW / 2f), eyeY)
                    }
                    drawPath(leftZen, darkOutline, style = Stroke(width = w * 0.045f, cap = StrokeCap.Round))
                    drawPath(rightZen, darkOutline, style = Stroke(width = w * 0.045f, cap = StrokeCap.Round))
                }

                MascotMood.EATING_HEALTHY, MascotMood.CELEBRATING -> {
                    // Joyful upside-down smiling arcs: ⌒ ⌒
                    val joyArcW = w * 0.14f
                    val leftJoy = Path().apply {
                        moveTo(leftEyeX - (joyArcW / 2f), eyeY + (h * 0.02f))
                        quadraticTo(leftEyeX, eyeY - (h * 0.05f), leftEyeX + (joyArcW / 2f), eyeY + (h * 0.02f))
                    }
                    val rightJoy = Path().apply {
                        moveTo(rightEyeX - (joyArcW / 2f), eyeY + (h * 0.02f))
                        quadraticTo(rightEyeX, eyeY - (h * 0.05f), rightEyeX + (joyArcW / 2f), eyeY + (h * 0.02f))
                    }
                    drawPath(leftJoy, darkOutline, style = Stroke(width = w * 0.05f, cap = StrokeCap.Round))
                    drawPath(rightJoy, darkOutline, style = Stroke(width = w * 0.05f, cap = StrokeCap.Round))
                }

                MascotMood.SLEEPY -> {
                    // Droopy sleepy eyelids: ◡ ◡
                    val sleepW = w * 0.13f
                    val leftSleep = Path().apply {
                        moveTo(leftEyeX - (sleepW / 2f), eyeY)
                        quadraticTo(leftEyeX, eyeY + (h * 0.035f), leftEyeX + (sleepW / 2f), eyeY)
                    }
                    val rightSleep = Path().apply {
                        moveTo(rightEyeX - (sleepW / 2f), eyeY)
                        quadraticTo(rightEyeX, eyeY + (h * 0.035f), rightEyeX + (sleepW / 2f), eyeY)
                    }
                    drawPath(leftSleep, darkOutline, style = Stroke(width = w * 0.04f, cap = StrokeCap.Round))
                    drawPath(rightSleep, darkOutline, style = Stroke(width = w * 0.04f, cap = StrokeCap.Round))
                }

                else -> {
                    // Open, big, glossy cartoon eyes with catchlights
                    if (currentBlink > 0.15f) {
                        val verticalScale = currentBlink
                        // Left Eye Sclera/Pupil
                        drawOval(
                            color = darkOutline,
                            topLeft = Offset(leftEyeX - eyeRadius, eyeY - (eyeRadius * verticalScale)),
                            size = Size(eyeRadius * 2f, eyeRadius * 2f * verticalScale),
                        )
                        // Right Eye Sclera/Pupil
                        drawOval(
                            color = darkOutline,
                            topLeft = Offset(rightEyeX - eyeRadius, eyeY - (eyeRadius * verticalScale)),
                            size = Size(eyeRadius * 2f, eyeRadius * 2f * verticalScale),
                        )

                        // Catchlight Sparkles
                        if (currentBlink > 0.5f) {
                            // Primary catchlight
                            drawCircle(
                                color = Color.White,
                                radius = eyeRadius * 0.38f,
                                center = Offset(leftEyeX - (eyeRadius * 0.28f), eyeY - (eyeRadius * 0.28f)),
                            )
                            drawCircle(
                                color = Color.White,
                                radius = eyeRadius * 0.38f,
                                center = Offset(rightEyeX - (eyeRadius * 0.28f), eyeY - (eyeRadius * 0.28f)),
                            )
                            // Minor sparkle
                            drawCircle(
                                color = Color.White,
                                radius = eyeRadius * 0.18f,
                                center = Offset(leftEyeX + (eyeRadius * 0.32f), eyeY + (eyeRadius * 0.25f)),
                            )
                            drawCircle(
                                color = Color.White,
                                radius = eyeRadius * 0.18f,
                                center = Offset(rightEyeX + (eyeRadius * 0.32f), eyeY + (eyeRadius * 0.25f)),
                            )
                        }
                    } else {
                        // Blink slit
                        drawLine(
                            color = darkOutline,
                            start = Offset(leftEyeX - eyeRadius, eyeY),
                            end = Offset(leftEyeX + eyeRadius, eyeY),
                            strokeWidth = w * 0.04f,
                            cap = StrokeCap.Round,
                        )
                        drawLine(
                            color = darkOutline,
                            start = Offset(rightEyeX - eyeRadius, eyeY),
                            end = Offset(rightEyeX + eyeRadius, eyeY),
                            strokeWidth = w * 0.04f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 5. CUTE SNOUT, NOSE & MOUTH
            // -------------------------------------------------------------
            val noseY = eyeY + (h * 0.08f)
            // Button nose
            val nosePath = Path().apply {
                moveTo(cx, noseY)
                lineTo(cx - (w * 0.032f), noseY - (h * 0.025f))
                lineTo(cx + (w * 0.032f), noseY - (h * 0.025f))
                close()
            }
            drawPath(nosePath, color = darkOutline)

            // Cartoon mouth
            val mouthY = noseY + (h * 0.03f)
            when (mood) {
                MascotMood.WELCOMING, MascotMood.CELEBRATING, MascotMood.EATING_HEALTHY -> {
                    // Open happy mouth showing pink tongue: D
                    val mouthW = w * 0.14f
                    val mouthH = h * 0.08f
                    val mouthPath = Path().apply {
                        moveTo(cx - (mouthW / 2f), mouthY)
                        quadraticTo(cx, mouthY + mouthH, cx + (mouthW / 2f), mouthY)
                        close()
                    }
                    drawPath(mouthPath, color = darkOutline)
                    // Tongue inside
                    drawCircle(
                        color = tongueColor,
                        radius = mouthW * 0.28f,
                        center = Offset(cx, mouthY + (mouthH * 0.65f)),
                    )
                }

                else -> {
                    // Cute cat/bear mouth: w
                    val mw = w * 0.05f
                    val mh = h * 0.03f
                    val mouthLeft = Path().apply {
                        moveTo(cx, mouthY)
                        quadraticTo(cx - (mw / 2f), mouthY + mh, cx - mw, mouthY)
                    }
                    val mouthRight = Path().apply {
                        moveTo(cx, mouthY)
                        quadraticTo(cx + (mw / 2f), mouthY + mh, cx + mw, mouthY)
                    }
                    drawPath(mouthLeft, darkOutline, style = Stroke(w * 0.035f, cap = StrokeCap.Round))
                    drawPath(mouthRight, darkOutline, style = Stroke(w * 0.035f, cap = StrokeCap.Round))
                }
            }

            // -------------------------------------------------------------
            // 6. BLUSHING ROSY CHEEKS
            // -------------------------------------------------------------
            val cheekY = eyeY + (h * 0.08f)
            val cheekRadius = w * 0.075f
            drawCircle(
                color = cheekBlush,
                radius = cheekRadius,
                center = Offset(leftEyeX - (w * 0.07f), cheekY),
            )
            drawCircle(
                color = cheekBlush,
                radius = cheekRadius,
                center = Offset(rightEyeX + (w * 0.07f), cheekY),
            )

            // -------------------------------------------------------------
            // 7. ACTIVITY PROPS & ACCESSORIES
            // -------------------------------------------------------------
            when (mood) {
                MascotMood.WORKOUT_BEAST -> {
                    // Red athletic sweatband across upper forehead
                    val bandY = headTop + (headHeight * 0.05f)
                    val bandH = headHeight * 0.11f
                    drawRoundRect(
                        color = Color(0xFFEF4444),
                        topLeft = Offset(headLeft + (headWidth * 0.06f), bandY),
                        size = Size(headWidth * 0.88f, bandH),
                        cornerRadius = CornerRadius(bandH * 0.4f, bandH * 0.4f),
                    )
                    // White athletic stripe
                    drawLine(
                        color = Color.White,
                        start = Offset(headLeft + (headWidth * 0.10f), bandY + (bandH / 2f)),
                        end = Offset(headLeft + (headWidth * 0.80f), bandY + (bandH / 2f)),
                        strokeWidth = w * 0.02f,
                    )
                    // Sweat droplet
                    val dropX = rightEyeX + (w * 0.12f)
                    val dropY = bandY + bandH + (h * 0.015f)
                    drawCircle(color = waterColor, radius = w * 0.028f, center = Offset(dropX, dropY))

                    // Pumping Mini Silver Dumbbell beside right arm!
                    val bellY = cy + (h * 0.18f) - (dumbbellPump * h * 0.07f)
                    val bellX = cx + (w * 0.38f)
                    // Bar
                    drawLine(
                        color = Color(0xFF94A3B8),
                        start = Offset(bellX, bellY - (h * 0.07f)),
                        end = Offset(bellX, bellY + (h * 0.07f)),
                        strokeWidth = w * 0.03f,
                    )
                    // Top plate
                    drawRoundRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(bellX - (w * 0.055f), bellY - (h * 0.08f)),
                        size = Size(w * 0.11f, h * 0.035f),
                        cornerRadius = CornerRadius(2f, 2f),
                    )
                    // Bottom plate
                    drawRoundRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(bellX - (w * 0.055f), bellY + (h * 0.045f)),
                        size = Size(w * 0.11f, h * 0.035f),
                        cornerRadius = CornerRadius(2f, 2f),
                    )
                }

                MascotMood.DRINKING_WATER, MascotMood.THIRSTY -> {
                    // Cute Water Glass with Straw
                    val glassX = cx + (w * 0.14f)
                    val glassY = cy + (h * 0.12f)
                    val glassW = w * 0.18f
                    val glassH = h * 0.20f
                    // Glass body
                    drawRoundRect(
                        color = waterColor.copy(alpha = 0.5f),
                        topLeft = Offset(glassX, glassY),
                        size = Size(glassW, glassH),
                        cornerRadius = CornerRadius(glassW * 0.2f, glassW * 0.2f),
                    )
                    // Water inside
                    drawRoundRect(
                        color = waterColor.copy(alpha = 0.85f),
                        topLeft = Offset(glassX + (glassW * 0.1f), glassY + (glassH * 0.3f)),
                        size = Size(glassW * 0.8f, glassH * 0.65f),
                        cornerRadius = CornerRadius(glassW * 0.15f, glassW * 0.15f),
                    )
                    // Striped Straw
                    drawLine(
                        color = Color(0xFFF43F5E),
                        start = Offset(glassX + (glassW * 0.5f), glassY + (glassH * 0.8f)),
                        end = Offset(glassX + (glassW * 0.85f), glassY - (glassH * 0.35f)),
                        strokeWidth = w * 0.025f,
                        cap = StrokeCap.Round,
                    )
                }

                MascotMood.EATING_HEALTHY -> {
                    // Crisp Healthy Red Apple in paws
                    val appleX = cx - (w * 0.14f)
                    val appleY = cy + (h * 0.16f)
                    val appleR = w * 0.09f
                    // Apple body
                    drawCircle(color = Color(0xFFE11D48), radius = appleR, center = Offset(appleX, appleY))
                    // Stem
                    drawLine(
                        color = Color(0xFF78350F),
                        start = Offset(appleX, appleY - appleR),
                        end = Offset(appleX + (w * 0.02f), appleY - appleR - (h * 0.04f)),
                        strokeWidth = w * 0.02f,
                    )
                    // Green Leaf
                    val leafPath = Path().apply {
                        moveTo(appleX + (w * 0.02f), appleY - appleR - (h * 0.02f))
                        quadraticTo(
                            appleX + (w * 0.08f),
                            appleY - appleR - (h * 0.04f),
                            appleX + (w * 0.06f),
                            appleY - appleR,
                        )
                        close()
                    }
                    drawPath(leafPath, color = Color(0xFF22C55E))
                }

                MascotMood.SLEEPY -> {
                    // Slouching Nightcap on head drooping to the left
                    val capPath = Path().apply {
                        val capBaseLeft = headLeft + (headWidth * 0.18f)
                        val capBaseRight = headLeft + (headWidth * 0.65f)
                        val capBaseY = headTop + (headHeight * 0.10f)
                        moveTo(capBaseLeft, capBaseY)
                        quadraticTo(cx - (w * 0.1f), headTop - (h * 0.15f), headLeft - (w * 0.14f), headTop + (h * 0.08f))
                        quadraticTo(cx, headTop - (h * 0.06f), capBaseRight, capBaseY)
                        close()
                    }
                    drawPath(capPath, color = Color(0xFF3B82F6))
                    // Fluffy white pom-pom on cap tip
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.05f,
                        center = Offset(headLeft - (w * 0.14f), headTop + (h * 0.08f)),
                    )
                }

                else -> {}
            }

            // -------------------------------------------------------------
            // 8. HANGING / WAVING PAWS (The iconic "hanging from top bar" feature!)
            // -------------------------------------------------------------
            val pawRadius = w * 0.07f
            val pawColor = furMain
            val pawPadColor = innerEarColor.copy(alpha = 0.8f)

            when (mood) {
                MascotMood.WELCOMING -> {
                    // Left paw grips ledge, right paw waves enthusiastically!
                    // Left gripping paw
                    drawCircle(pawColor, pawRadius, Offset(cx - (w * 0.24f), headTop + (h * 0.02f)))
                    drawCircle(darkOutline, pawRadius, Offset(cx - (w * 0.24f), headTop + (h * 0.02f)), style = Stroke(w * 0.03f))

                    // Right Waving Paw (animated wave angle)
                    val waveCenter = Offset(cx + (w * 0.32f), cy - (h * 0.08f))
                    val waveOffset = Offset(
                        waveCenter.x + (w * 0.14f * sin(Math.toRadians(wavingAngle.toDouble())).toFloat()),
                        waveCenter.y - (h * 0.12f),
                    )
                    drawCircle(pawColor, pawRadius * 1.1f, waveOffset)
                    drawCircle(darkOutline, pawRadius * 1.1f, waveOffset, style = Stroke(w * 0.035f))
                    // Pink paw pad
                    drawCircle(pawPadColor, pawRadius * 0.45f, waveOffset)
                }

                MascotMood.CELEBRATING -> {
                    // Both paws raised high in the air: \o/
                    val leftHighPaw = Offset(headLeft - (w * 0.04f), headTop - (h * 0.06f))
                    val rightHighPaw = Offset(headLeft + headWidth + (w * 0.04f), headTop - (h * 0.06f))
                    drawCircle(pawColor, pawRadius * 1.05f, leftHighPaw)
                    drawCircle(darkOutline, pawRadius * 1.05f, leftHighPaw, style = Stroke(w * 0.035f))
                    drawCircle(pawPadColor, pawRadius * 0.4f, leftHighPaw)

                    drawCircle(pawColor, pawRadius * 1.05f, rightHighPaw)
                    drawCircle(darkOutline, pawRadius * 1.05f, rightHighPaw, style = Stroke(w * 0.035f))
                    drawCircle(pawPadColor, pawRadius * 0.4f, rightHighPaw)
                }

                else -> {
                    // Hanging Paws: Clutching the top bar ledge!
                    val leftPawX = cx - (w * 0.22f)
                    val rightPawX = cx + (w * 0.22f)
                    val pawY = headTop + (h * 0.02f)

                    // Left Paw
                    drawCircle(pawColor, pawRadius, Offset(leftPawX, pawY))
                    drawCircle(darkOutline, pawRadius, Offset(leftPawX, pawY), style = Stroke(w * 0.03f))
                    drawCircle(pawPadColor, pawRadius * 0.4f, Offset(leftPawX, pawY))

                    // Right Paw
                    drawCircle(pawColor, pawRadius, Offset(rightPawX, pawY))
                    drawCircle(darkOutline, pawRadius, Offset(rightPawX, pawY), style = Stroke(w * 0.03f))
                    drawCircle(pawPadColor, pawRadius * 0.4f, Offset(rightPawX, pawY))

                    // Cute little feet dangling below
                    val feetY = headTop + headHeight + (h * 0.01f)
                    drawOval(
                        pawColor,
                        topLeft = Offset(cx - (w * 0.22f), feetY - (h * 0.04f)),
                        size = Size(w * 0.16f, h * 0.08f),
                    )
                    drawOval(
                        darkOutline,
                        topLeft = Offset(cx - (w * 0.22f), feetY - (h * 0.04f)),
                        size = Size(w * 0.16f, h * 0.08f),
                        style = Stroke(w * 0.03f),
                    )
                    drawOval(
                        pawColor,
                        topLeft = Offset(cx + (w * 0.06f), feetY - (h * 0.04f)),
                        size = Size(w * 0.16f, h * 0.08f),
                    )
                    drawOval(
                        darkOutline,
                        topLeft = Offset(cx + (w * 0.06f), feetY - (h * 0.04f)),
                        size = Size(w * 0.16f, h * 0.08f),
                        style = Stroke(w * 0.03f),
                    )
                }
            }
        }
    }
}
