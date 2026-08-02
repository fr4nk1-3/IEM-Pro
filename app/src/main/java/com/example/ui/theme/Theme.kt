package com.example.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.model.AppThemeMode

@Composable
fun IemMixerTheme(
    themeMode: AppThemeMode = AppThemeMode.CQ_MIXPAD,
    content: @Composable () -> Unit
) {
    val appColors = when (themeMode) {
        AppThemeMode.CQ_MIXPAD -> CQMixPadAppColors
        AppThemeMode.LIGHT -> LightAppColors
        AppThemeMode.GLASSMORPHISM -> GlassmorphismAppColors
    }

    val colorScheme = if (themeMode == AppThemeMode.LIGHT) {
        lightColorScheme(
            primary = appColors.cyan,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0F2FE),
            onPrimaryContainer = appColors.cyan,
            secondary = appColors.emerald,
            onSecondary = Color.White,
            tertiary = appColors.amber,
            background = appColors.background,
            onBackground = appColors.textPrimary,
            surface = appColors.surface,
            onSurface = appColors.textPrimary,
            surfaceVariant = appColors.surfaceVariant,
            onSurfaceVariant = appColors.textSecondary,
            outline = appColors.border,
            error = appColors.rose,
            onError = Color.White
        )
    } else {
        darkColorScheme(
            primary = appColors.cyan,
            onPrimary = Color.Black,
            primaryContainer = if (themeMode == AppThemeMode.GLASSMORPHISM) Color(0x400284C7) else Color(0xFF003643),
            onPrimaryContainer = appColors.cyan,
            secondary = appColors.emerald,
            onSecondary = Color.Black,
            tertiary = appColors.amber,
            background = appColors.background,
            onBackground = appColors.textPrimary,
            surface = appColors.surface,
            onSurface = appColors.textPrimary,
            surfaceVariant = appColors.surfaceVariant,
            onSurfaceVariant = appColors.textSecondary,
            outline = appColors.border,
            error = appColors.rose,
            onError = Color.White
        )
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography
        ) {
            if (themeMode == AppThemeMode.CQ_MIXPAD) {
                Box(modifier = Modifier.fillMaxSize().background(appColors.background)) {
                    // Allen & Heath CQ Console Technical Background Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        // Subtle cyan console top-left radial glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x2800D2FF), Color.Transparent),
                                center = Offset(w * 0.10f, h * 0.15f),
                                radius = w * 0.6f
                            )
                        )
                        // Amber console bottom-right radial glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x20FFB703), Color.Transparent),
                                center = Offset(w * 0.90f, h * 0.85f),
                                radius = w * 0.6f
                            )
                        )
                    }
                    content()
                }
            } else if (themeMode == AppThemeMode.GLASSMORPHISM) {
                Box(modifier = Modifier.fillMaxSize().background(appColors.background)) {
                    // Glassmorphism ambient glow background canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        // Ambient cyan glow top-left
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x3506B6D4), Color.Transparent),
                                center = Offset(w * 0.15f, h * 0.2f),
                                radius = w * 0.65f
                            )
                        )
                        // Ambient purple glow bottom-right
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x308B5CF6), Color.Transparent),
                                center = Offset(w * 0.85f, h * 0.8f),
                                radius = w * 0.7f
                            )
                        )
                        // Ambient emerald glow bottom-left
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x2510B981), Color.Transparent),
                                center = Offset(w * 0.2f, h * 0.85f),
                                radius = w * 0.55f
                            )
                        )
                    }
                    content()
                }
            } else {
                content()
            }
        }
    }
}
