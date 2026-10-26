//
// BodyViewStyle.kt
//
// Visual styling presets for BodyView with Aro theme support.
//

package com.drnitz.aro.ui.bodymap

import androidx.compose.ui.graphics.Color

/** Configuration for the visual appearance of a [BodyView]. */
data class BodyViewStyle(
    val defaultFillColor: Color = Color(0xFFC7C7C7),
    val strokeColor: Color = Color.Transparent,
    val strokeWidth: Float = 0f,
    val selectionColor: Color = Color(0xFF4CAF50),
    val selectionStrokeColor: Color = Color(0xFF4CAF50),
    val selectionStrokeWidth: Float = 2f,
    val headColor: Color = Color(0xFFBFBFBF),
    val hairColor: Color = Color(0xFF404040),
) {
    companion object {
        /** Default style with gray fill and green selection. */
        val Default = BodyViewStyle()

        /** Aro Dark Mode: obsidian/slate body with vibrant amber selection and subtle boundary strokes. */
        val AroDark = BodyViewStyle(
            defaultFillColor = Color(0xFF242730),
            strokeColor = Color(0xFF373C48),
            strokeWidth = 1f,
            selectionColor = Color(0xFFFFB300), // Aro Vibrant Amber
            selectionStrokeColor = Color(0xFFFFC107),
            selectionStrokeWidth = 2.5f,
            headColor = Color(0xFF2E323E),
            hairColor = Color(0xFF1B1D24),
        )

        /** Aro Emerald: dark theme with vibrant emerald selection for recovery/ready states. */
        val AroEmerald = BodyViewStyle(
            defaultFillColor = Color(0xFF242730),
            strokeColor = Color(0xFF373C48),
            strokeWidth = 1f,
            selectionColor = Color(0xFF10B981), // Emerald
            selectionStrokeColor = Color(0xFF34D399),
            selectionStrokeWidth = 2.5f,
            headColor = Color(0xFF2E323E),
            hairColor = Color(0xFF1B1D24),
        )

        /** Minimal style with thin strokes and subtle fill. */
        val Minimal = BodyViewStyle(
            defaultFillColor = Color(0xFFE0E0E0),
            strokeColor = Color(0xFFB3B3B3),
            strokeWidth = 0.5f,
            selectionStrokeWidth = 1.5f,
        )

        /** Neon style with dark background tones and a cyan selection. */
        val Neon = BodyViewStyle(
            defaultFillColor = Color(0xFF262626),
            strokeColor = Color(0xFF4D4D4D),
            strokeWidth = 0.5f,
            selectionColor = Color(0xFF00F4FE),
            selectionStrokeColor = Color(0xFF00F4FE),
            selectionStrokeWidth = 2f,
            headColor = Color(0xFF333333),
            hairColor = Color(0xFF1A1A1A),
        )

        /** Medical/clinical style. */
        val Medical = BodyViewStyle(
            defaultFillColor = Color(0xFFE6EBF2),
            strokeColor = Color(0xFFB3BFCC),
            strokeWidth = 0.5f,
            selectionColor = Color(0xFF007AFF),
            selectionStrokeColor = Color(0xFF007AFF),
            selectionStrokeWidth = 2f,
            headColor = Color(0xFFD9DEE6),
            hairColor = Color(0xFF4D5259),
        )
    }
}
