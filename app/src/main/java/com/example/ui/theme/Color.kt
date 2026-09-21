package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.model.AppThemeMode

data class AppColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val cyan: Color,
    val blue: Color,
    val emerald: Color,
    val amber: Color,
    val rose: Color,
    val magenta: Color,
    val purple: Color,
    val isGlass: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.CQ_MIXPAD
)

// Dark Metallic Grey Console Palette
val CQMixPadAppColors = AppColors(
    background = Color(0xFF16181C),     // Dark Metallic Charcoal Surface
    surface = Color(0xFF22252B),        // Dark Metallic Grey Card Container
    surfaceVariant = Color(0xFF2E323B), // Metallic Gunmetal Control Strip
    border = Color(0xFF444A56),         // Metallic Steel Console Border
    textPrimary = Color(0xFFF1F5F9),    // Crisp Metallic White Readout
    textSecondary = Color(0xFFA0AAB8),  // Metallic Slate Label
    textMuted = Color(0xFF6C7685),      // Dark Slate Caption
    cyan = Color(0xFF90A4AE),           // Metallic Steel Grey Accent
    blue = Color(0xFF78909C),           // Dark Metallic Slate Blue
    emerald = Color(0xFF10B981),        // Signal Green
    amber = Color(0xFFF59E0B),          // Active Amber
    rose = Color(0xFFEF4444),           // Active Mute Red
    magenta = Color(0xFFD946EF),        // Active Magenta
    purple = Color(0xFF8B5CF6),         // Active Purple
    isGlass = false,
    themeMode = AppThemeMode.CQ_MIXPAD
)

// Light Studio Palette (High-Contrast Outdoor Daylight Mode)
val LightAppColors = AppColors(
    background = Color(0xFFF1F5F9), // Slate 100 canvas
    surface = Color(0xFFFFFFFF),     // Pure white card container
    surfaceVariant = Color(0xFFE2E8F0), // Slate 200
    border = Color(0xFF94A3B8),      // Slate 400 for crisp card borders
    textPrimary = Color(0xFF0F172A), // Dark slate 900 for maximum readability
    textSecondary = Color(0xFF334155), // Dark slate 700 for high contrast secondary labels
    textMuted = Color(0xFF475569),   // Dark slate 600 for clear muted captions
    cyan = Color(0xFF0284C7),        // Sky/Cyan 600
    blue = Color(0xFF2563EB),        // Blue 600
    emerald = Color(0xFF047857),     // Emerald 700
    amber = Color(0xFFB45309),       // Amber 700
    rose = Color(0xFFB91C1C),        // Red 700
    magenta = Color(0xFFBE185D),     // Pink 700
    purple = Color(0xFF6D28D9),      // Purple 700
    isGlass = false,
    themeMode = AppThemeMode.LIGHT
)

val LocalAppColors = staticCompositionLocalOf { CQMixPadAppColors }

// Composable Color Getters dynamically resolving to the current theme
val DarkBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.background

val DarkSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.surface

val DarkSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.surfaceVariant

val DarkBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.border

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.textSecondary

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.textMuted

val NeonCyan: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.cyan

val NeonBlue: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.blue

val NeonEmerald: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.emerald

val NeonAmber: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.amber

val NeonRose: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.rose

val NeonMagenta: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.magenta

val NeonPurple: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current.purple
