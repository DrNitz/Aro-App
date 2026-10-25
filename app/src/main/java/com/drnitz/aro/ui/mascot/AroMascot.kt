package com.drnitz.aro.ui.mascot

import androidx.annotation.RawRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.drnitz.aro.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Expressive Animated Health Companion Mascot ("Aro the Finch").
 *
 * Renders vector Lottie animations at 60-120 FPS matching the user's
 * real-time nutrition, fasting, workout, and hydration vitals:
 * - Top Bar Peeking: Peeks playfully over the top bar header.
 * - Beast Mode: Enthusiastically pumps mini dumbbells after workouts.
 * - Healthy Fueling: Munches crisp apple and flexes after logging meals.
 * - Hydration: Drinks refreshing water after logging hydration.
 * - Fasting Zen: Floats serenely in the clouds during active fasts.
 * - Celebration: Confetti dance when daily goals are reached.
 * - Sleep: Slumbers cozily in nightcap and pillow late at night.
 * - Interactive Taps: Tap to trigger cheer jump / clapping and bouncy spring physics.
 */
@Composable
fun AroMascot(
    mood: MascotMood,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isTopBar: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = "Aro Animated Mascot",
) {
    val coroutineScope = rememberCoroutineScope()
    val bounceScale = remember { Animatable(1f) }
    val bounceRotation = remember { Animatable(0f) }

    // Ephemeral reaction animation triggered by tapping
    var reactionRes by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(reactionRes) {
        if (reactionRes != null) {
            delay(2800)
            reactionRes = null
        }
    }

    val targetRes = reactionRes ?: getMascotRawRes(mood = mood, isTopBar = isTopBar)
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(targetRes))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = if (reactionRes != null) 1 else LottieConstants.IterateForever,
        speed = 1.0f,
    )

    val clickModifier = if (onClick != null || !isTopBar) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Button,
        ) {
            coroutineScope.launch {
                // Playful interactive squash & stretch physics
                bounceRotation.animateTo(if (Random.nextBoolean()) 10f else -10f, tween(60))
                bounceScale.animateTo(0.85f, tween(60))
                bounceRotation.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = 400f))
                bounceScale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 420f))
            }

            // In dialog or full view, trigger an enthusiastic reaction loop on tap
            if (!isTopBar && reactionRes == null) {
                reactionRes = if (Random.nextBoolean()) {
                    R.raw.finch_happy_jump
                } else {
                    R.raw.finch_celebrate_clap
                }
            }

            onClick?.invoke()
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(bounceScale.value)
            .rotate(bounceRotation.value)
            .then(clickModifier)
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = "$contentDescription: ${mood.label}"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * Resolves the Finch Lottie raw animation resource for the given mood and context.
 */
@RawRes
fun getMascotRawRes(mood: MascotMood, isTopBar: Boolean = false): Int {
    if (isTopBar && mood == MascotMood.HANGING_CURIOUS) {
        return R.raw.finch_peeking
    }
    return when (mood) {
        MascotMood.HANGING_CURIOUS -> if (isTopBar) R.raw.finch_peeking else R.raw.finch_idle_avatar
        MascotMood.WELCOMING -> R.raw.finch_heart_hug
        MascotMood.WORKOUT_BEAST -> R.raw.finch_lift_weight
        MascotMood.EATING_HEALTHY -> R.raw.finch_nutrition_apple
        MascotMood.DRINKING_WATER, MascotMood.THIRSTY -> R.raw.finch_drink_water
        MascotMood.CELEBRATING -> R.raw.finch_celebrate
        MascotMood.FASTING_ZEN -> R.raw.finch_happy_float
        MascotMood.SLEEPY -> R.raw.finch_sleep_cap
    }
}
