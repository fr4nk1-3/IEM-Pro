package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.rgbGlowBorder(
    isSelected: Boolean,
    borderWidth: Dp = 1.5.dp,
    shape: Shape = RoundedCornerShape(10.dp)
): Modifier = this.then(
    if (isSelected) {
        Modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                spotColor = Color(0xFF00E5FF),
                ambientColor = Color(0xFFFF0055)
            )
            .composed {
                val infiniteTransition = rememberInfiniteTransition(label = "rgbGlow")
                val phase by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rgbPhase"
                )

                val baseRgbColors = remember {
                    listOf(
                        Color(0xFFFF0055), // Neon Red/Pink
                        Color(0xFFFF9900), // Vibrant Orange
                        Color(0xFFFFEE00), // Electric Yellow
                        Color(0xFF00FF66), // Neon Green
                        Color(0xFF00E5FF), // Electric Cyan
                        Color(0xFF0066FF), // Neon Blue
                        Color(0xFF9D00FF), // Deep Purple
                        Color(0xFFFF0055)  // Loop Red
                    )
                }

                val count = baseRgbColors.size - 1
                val shiftedColors = List(baseRgbColors.size) { i ->
                    val offset = (i.toFloat() / count - phase + 1f) % 1f
                    val scaled = offset * count
                    val idx1 = scaled.toInt().coerceIn(0, count - 1)
                    val idx2 = (idx1 + 1) % count
                    val frac = scaled - idx1
                    lerp(baseRgbColors[idx1], baseRgbColors[idx2], frac)
                }

                val brush = Brush.linearGradient(
                    colors = shiftedColors,
                    start = Offset(0f, 0f),
                    end = Offset(200f, 600f)
                )

                Modifier.border(
                    width = borderWidth,
                    brush = brush,
                    shape = shape
                )
            }
    } else Modifier
)
