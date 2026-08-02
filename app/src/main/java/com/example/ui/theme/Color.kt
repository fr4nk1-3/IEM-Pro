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
    val themeMode: AppThemeMode = AppThemeMode.GLASSMORPHISM
)

// Allen & Heath CQ MixPad Palette (Deep Slate Console with Vivid Cyan & Amber Accents)
val CQMixPadAppColors = AppColors(
    background = Color(0xFF10141D),     // Allen & Heath CQ Console Deep Slate Surface
    surface = Color(0xFF1A212E),        // CQ Dark Tactical Card Container
    surfaceVariant = Color(0xFF242E40), // CQ High-Contrast Control Strip
    border = Color(0xFF33425B),         // Precision Console Border
    textPrimary = Color(0xFFF8FAFC),    // Crisp White Readout
    textSecondary = Color(0xFF94A3B8),  // CQ Slate Label
    textMuted = Color(0xFF64748B),      // CQ Secondary Caption
    cyan = Color(0xFF00D2FF),           // Allen & Heath Electric Cyan
    blue = Color(0xFF29B6F6),           // CQ Electric Blue
    emerald = Color(0xFF00E676),        // CQ Signal Green
    amber = Color(0xFFFFB703),          // CQ Amber / Solo Active
    rose = Color(0xFFFF1744),           // CQ Red / Mute Active
    magenta = Color(0xFFE040FB),        // CQ Magenta
    purple = Color(0xFF7C4DFF),         // CQ Deep Purple
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

// Glassmorphism Palette (Translucent Frosted Glass with Glowing Neon Borders)
val GlassmorphismAppColors = AppColors(
    background = Color(0xFF070B14),  // Deep obsidian with ambient glow background
    surface = Color(0x331E293B),     // Frosted 20% slate glass
    surfaceVariant = Color(0x40334155), // Translucent 25% glass variant
    border = Color(0x5538BDF8),      // Glowing frosted cyan edge
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFCBD5E1),
    textMuted = Color(0xFF94A3B8),
    cyan = Color(0xFF38BDF8),        // Electric Cyan
    blue = Color(0xFF60A5FA),
    emerald = Color(0xFF34D399),
    amber = Color(0xFFFBBF24),
    rose = Color(0xFFF43F5E),
    magenta = Color(0xFFF472B6),
    purple = Color(0xFFA78BFA),
    isGlass = true,
    themeMode = AppThemeMode.GLASSMORPHISM
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
