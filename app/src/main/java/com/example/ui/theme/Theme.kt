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
            primaryContainer = Color(0xFF003643),
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(appColors.background)
            ) {
                if (themeMode == AppThemeMode.CQ_MIXPAD) {
                    // Dark Metallic Grey Technical Background Canvas
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        // Subtle metallic steel top-left radial glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x20525E75), Color.Transparent),
                                center = Offset(w * 0.10f, h * 0.15f),
                                radius = w * 0.6f
                            )
                        )
                        // Metallic gunmetal bottom-right radial glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0x18373E4D), Color.Transparent),
                                center = Offset(w * 0.90f, h * 0.85f),
                                radius = w * 0.6f
                            )
                        )
                    }
                }
                content()
            }
        }
    }
}
