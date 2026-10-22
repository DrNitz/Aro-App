package com.drnitz.aro.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// MD3 Fully Rounded Shapes System
val AroShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set default to false to preserve Aro's signature medical macro palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AroDarkColorScheme
        else -> AroLightColorScheme
    }

    val macroColors = AroMacroColors()

    CompositionLocalProvider(
        LocalAroMacroColors provides macroColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AroTypography,
            shapes = AroShapes,
            content = content
        )
    }
}

// Convenient Accessor for Aro Theme Tokens
object AroTheme {
    val macroColors: AroMacroColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAroMacroColors.current
}
