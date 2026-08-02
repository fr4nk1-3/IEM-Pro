package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose

@Composable
fun MeterBar(
    level: Float, // 0.0 .. 1.0
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    width: Dp = 8.dp,
    segmentCount: Int = 16,
    showPeakHold: Boolean = true
) {
    val targetLevel = level.coerceIn(0f, 1f)
    
    // Fast attack, smooth ballistic decay animation for realtime audio responsiveness
    val currentLevel by animateFloatAsState(
        targetValue = targetLevel,
        animationSpec = tween(
            durationMillis = if (targetLevel > 0.3f) 40 else 100
        ),
        label = "meterBallistic"
    )

    // Peak hold tracking
    var peakLevel by remember { mutableFloatStateOf(0f) }
    var peakHoldTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(targetLevel) {
        val now = System.currentTimeMillis()
        if (targetLevel >= peakLevel) {
            peakLevel = targetLevel
            peakHoldTime = now
        } else if (now - peakHoldTime > 350L) {
            peakLevel = (peakLevel - 0.06f).coerceAtLeast(targetLevel)
        }
    }

    val emeraldColor = NeonEmerald
    val amberColor = NeonAmber
    val roseColor = NeonRose

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF090D14))
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalHeight = size.height
            val totalWidth = size.width

            // Color stops defining standard audio meter thresholds:
            // 0.0 .. 0.12 = Red (0 dB / Clip)
            // 0.15 .. 0.35 = Amber (-12 dB to -3 dB)
            // 0.38 .. 1.0 = Emerald (Signal Present)
            val meterColorStops = arrayOf(
                0.0f to roseColor,
                0.12f to roseColor,
                0.15f to amberColor,
                0.35f to amberColor,
                0.38f to emeraldColor,
                1.0f to emeraldColor
            )

            val dimColorStops = arrayOf(
                0.0f to roseColor.copy(alpha = 0.15f),
                0.12f to roseColor.copy(alpha = 0.15f),
                0.15f to amberColor.copy(alpha = 0.15f),
                0.35f to amberColor.copy(alpha = 0.15f),
                0.38f to emeraldColor.copy(alpha = 0.15f),
                1.0f to emeraldColor.copy(alpha = 0.15f)
            )

            // Draw dim background track gradient
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colorStops = dimColorStops,
                    startY = 0f,
                    endY = totalHeight
                ),
                topLeft = Offset(0f, 0f),
                size = Size(totalWidth, totalHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // Draw solid continuous active level bar line (from bottom up)
            if (currentLevel > 0.005f) {
                val fillHeight = totalHeight * currentLevel
                val topY = totalHeight - fillHeight

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colorStops = meterColorStops,
                        startY = 0f,
                        endY = totalHeight
                    ),
                    topLeft = Offset(0f, topY),
                    size = Size(totalWidth, fillHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            // Draw peak hold marker line
            if (showPeakHold && peakLevel > 0.01f) {
                val peakY = (totalHeight - (totalHeight * peakLevel)).coerceIn(0f, totalHeight - 2f)
                val peakColor = when {
                    peakLevel > 0.88f -> roseColor
                    peakLevel > 0.65f -> amberColor
                    else -> emeraldColor
                }

                drawRect(
                    color = peakColor,
                    topLeft = Offset(0f, peakY),
                    size = Size(totalWidth, 2.dp.toPx())
                )
            }
        }
    }
}

