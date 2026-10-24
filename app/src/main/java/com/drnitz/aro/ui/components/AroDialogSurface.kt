package com.drnitz.aro.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Medium window threshold for full-screen dialog scaffolds. At or above this
 * width the dialog switches from the full-bleed phone layout to a centered,
 * width-capped card so forms stay readable on tablets and foldables.
 */
val AroDialogWideThreshold = 600.dp

/** Same threshold as [AroDialogWideThreshold] for non-composable width checks. */
const val AroDialogWideMinScreenWidthDp = 600

/**
 * The maximum content width for dialogs on medium/expanded screens. Material
 * guidance keeps primary interaction surfaces narrow so forms, search fields,
 * and action rows do not stretch across a poster-sized window.
 */
private val MaxDialogCardWidth = 640.dp

/**
 * Shared root container for the app's full-screen custom dialogs.
 *
 * Layout policy:
 * - [floatingCard] = false ("sheet card"):
 *   - Compact (< [AroDialogWideThreshold]): fills the window and peeks below a
 *     28dp top gap with rounded top corners, exactly like the hand-rolled
 *     Surface used by LogWorkout/PlanPickers/DayPlanSheet before.
 *   - Wide (>= [AroDialogWideThreshold]): centered card capped at
 *     [MaxDialogCardWidth] wide and 94% height, fully rounded.
 * - [floatingCard] = true (centered card, e.g. NotificationSettings, food
 *   detail):
 *   - Compact: 95% window width.
 *   - Wide: centered card capped at [MaxDialogCardWidth].
 *
 * IME insets are applied here so every dialog built on this scaffold keeps
 * its inputs visible above the keyboard; call sites must not add their own
 * [imePadding].
 *
 * @param modifier extra constraints applied to the card itself, after the
 * scaffold's own sizing (e.g. `Modifier.fillMaxHeight(0.92f)`).
 * @param floatingCard whether the card floats center-screen instead of
 * anchoring like a bottom sheet.
 * @param content dialog body; fills the card.
 */
@Composable
fun AroDialogSurface(
    modifier: Modifier = Modifier,
    floatingCard: Boolean = false,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val wide = maxWidth >= AroDialogWideThreshold
        val shape =
            if (!floatingCard && !wide) {
                // Sheet card on compact phones: rounded top corners only.
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            } else {
                RoundedCornerShape(24.dp)
            }
        val cardModifier = when {
            !floatingCard && !wide ->
                Modifier
                    .fillMaxSize()
                    .padding(top = 28.dp)
            !floatingCard && wide ->
                Modifier
                    .widthIn(max = MaxDialogCardWidth)
                    .fillMaxHeight(0.94f)
            floatingCard && wide ->
                Modifier.widthIn(max = MaxDialogCardWidth)
            else ->
                Modifier.fillMaxWidth(0.95f)
        }

        Surface(
            modifier = cardModifier
                .then(modifier)
                .imePadding(),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = if (floatingCard) 4.dp else 0.dp,
        ) {
            content()
        }
    }
}
