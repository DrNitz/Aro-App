package com.drnitz.aro.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// ARO CLINICAL & MACRO TOKENS
// ==========================================
val AroEmeraldPrimary = Color(0xFF10B981)
val AroEmeraldLight = Color(0xFF34D399)
val AroEmeraldDark = Color(0xFF059669)

// Macro Spectrum
val AroColorCalories = Color(0xFFF43F5E) // Vibrant Rose / Red
val AroColorCaloriesBg = Color(0x24F43F5E)
val AroColorProtein = Color(0xFF8B5CF6)  // Violet
val AroColorProteinBg = Color(0x248B5CF6)
val AroColorCarbs = Color(0xFFF59E0B)    // Amber / Gold
val AroColorCarbsBg = Color(0x24F59E0B)
val AroColorFat = Color(0xFF3B82F6)      // Cyan / Royal Blue
val AroColorFatBg = Color(0x243B82F6)
val AroColorFiber = Color(0xFF10B981)    // Emerald Green
val AroColorFiberBg = Color(0x2410B981)
val AroColorWater = Color(0xFF06B6D4)    // Electric Cyan
val AroColorWaterBg = Color(0x2406B6D4)
val AroColorCardio = Color(0xFFEC4899)   // Hot Pink
val AroColorFasting = Color(0xFF6366F1)  // Indigo

// Clinical Alert Tones
val AroClinicalOptimal = Color(0xFF10B981)
val AroClinicalModerate = Color(0xFFF59E0B)
val AroClinicalHighRisk = Color(0xFFF43F5E)
val AroClinicalCeiling = Color(0xFF8B5CF6)

// Dark / OLED Surface Palette
val AroDarkBackground = Color(0xFF080C14)
val AroDarkSurface = Color(0xFF0C111C)
val AroDarkSurfaceContainerLowest = Color(0xFF05080E)
val AroDarkSurfaceContainerLow = Color(0xFF0E1422)
val AroDarkSurfaceContainer = Color(0xFF121826)
val AroDarkSurfaceContainerHigh = Color(0xFF182032)
val AroDarkSurfaceContainerHighest = Color(0xFF1F2942)
val AroDarkOutline = Color(0x26FFFFFF)
val AroDarkOutlineVariant = Color(0x14FFFFFF)

// Light Surface Palette
val AroLightBackground = Color(0xFFF4F6FA)
val AroLightSurface = Color(0xFFFFFFFF)
val AroLightSurfaceContainerLowest = Color(0xFFFFFFFF)
val AroLightSurfaceContainerLow = Color(0xFFF8FAFC)
val AroLightSurfaceContainer = Color(0xFFEDF2F7)
val AroLightSurfaceContainerHigh = Color(0xFFE2E8F0)
val AroLightSurfaceContainerHighest = Color(0xFFCBD5E1)
val AroLightOutline = Color(0x1A000000)
val AroLightOutlineVariant = Color(0x0F000000)

// ==========================================
// MATERIAL 3 COLOR SCHEMES
// ==========================================
val AroDarkColorScheme: ColorScheme = darkColorScheme(
    primary = AroEmeraldLight,
    onPrimary = Color(0xFF003822),
    primaryContainer = Color(0xFF005234),
    onPrimaryContainer = Color(0xFF66FFA6),
    secondary = AroColorFat,
    onSecondary = Color(0xFF002B73),
    secondaryContainer = Color(0xFF0040A1),
    onSecondaryContainer = Color(0xFFD6E3FF),
    tertiary = AroColorProtein,
    onTertiary = Color(0xFF2E006A),
    tertiaryContainer = Color(0xFF470B99),
    onTertiaryContainer = Color(0xFFEADBFF),
    error = AroColorCalories,
    onError = Color(0xFF600010),
    errorContainer = Color(0xFF8C001C),
    onErrorContainer = Color(0xFFFFDAD9),
    background = AroDarkBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = AroDarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = AroDarkSurfaceContainerHigh,
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = AroDarkSurfaceContainerLowest,
    surfaceContainerLow = AroDarkSurfaceContainerLow,
    surfaceContainer = AroDarkSurfaceContainer,
    surfaceContainerHigh = AroDarkSurfaceContainerHigh,
    surfaceContainerHighest = AroDarkSurfaceContainerHighest,
    outline = AroDarkOutline,
    outlineVariant = AroDarkOutlineVariant
)

val AroLightColorScheme: ColorScheme = lightColorScheme(
    primary = AroEmeraldDark,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF66FFA6),
    onPrimaryContainer = Color(0xFF002113),
    secondary = AroColorFat,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E3FF),
    onSecondaryContainer = Color(0xFF001B4D),
    tertiary = AroColorProtein,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEADBFF),
    onTertiaryContainer = Color(0xFF240054),
    error = AroColorCalories,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD9),
    onErrorContainer = Color(0xFF410008),
    background = AroLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = AroLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = AroLightSurfaceContainerHigh,
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = AroLightSurfaceContainerLowest,
    surfaceContainerLow = AroLightSurfaceContainerLow,
    surfaceContainer = AroLightSurfaceContainer,
    surfaceContainerHigh = AroLightSurfaceContainerHigh,
    surfaceContainerHighest = AroLightSurfaceContainerHighest,
    outline = AroLightOutline,
    outlineVariant = AroLightOutlineVariant
)

// Custom Semantic Macro Extension Tokens
@Immutable
data class AroMacroColors(
    val calories: Color = AroColorCalories,
    val caloriesBg: Color = AroColorCaloriesBg,
    val protein: Color = AroColorProtein,
    val proteinBg: Color = AroColorProteinBg,
    val carbs: Color = AroColorCarbs,
    val carbsBg: Color = AroColorCarbsBg,
    val fat: Color = AroColorFat,
    val fatBg: Color = AroColorFatBg,
    val fiber: Color = AroColorFiber,
    val fiberBg: Color = AroColorFiberBg,
    val water: Color = AroColorWater,
    val waterBg: Color = AroColorWaterBg,
    val cardio: Color = AroColorCardio,
    val fasting: Color = AroColorFasting,
    val clinicalOptimal: Color = AroClinicalOptimal,
    val clinicalModerate: Color = AroClinicalModerate,
    val clinicalHighRisk: Color = AroClinicalHighRisk,
    val clinicalCeiling: Color = AroClinicalCeiling
)

val LocalAroMacroColors = staticCompositionLocalOf { AroMacroColors() }
