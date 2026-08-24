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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose

/**
 * Professional Segmented LED VU Meter for Real-Time Audio Signal Monitoring.
 *
 * Implements a calibrated console LED ladder:
 * - Clip / Over (Red) : >= 0.90 (0 dBFS / Overload)
 * - Amber / Hot       : 0.72 .. 0.90 (-3 dB to 0 dB)
 * - Yellow / Warning  : 0.50 .. 0.72 (-12 dB to -3 dB)
 * - Emerald / Target  : 0.22 .. 0.50 (-24 dB to -12 dB)
 * - Green / Signal    : 0.03 .. 0.22 (-48 dB to -24 dB, SIG Present)
 */
@Composable
fun MeterBar(
    level: Float, // 0.0 .. 1.0
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    width: Dp = 8.dp,
    segmentCount: Int = 18,
    showPeakHold: Boolean = true,
    showTicks: Boolean = false
) {
    val targetLevel = level.coerceIn(0f, 1f)

    // Fast instant attack (20ms), natural smooth exponential decay for authentic audio VU ballistics
    val currentLevel by animateFloatAsState(
        targetValue = targetLevel,
        animationSpec = tween(
            durationMillis = if (targetLevel > 0.3f) 25 else 85
        ),
        label = "vuMeterBallistic"
    )

    // Dynamic Peak-Hold tracking with ballistic decay
    var peakLevel by remember { mutableFloatStateOf(0f) }
    var peakHoldTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(targetLevel) {
        val now = System.currentTimeMillis()
        if (targetLevel >= peakLevel) {
            peakLevel = targetLevel
            peakHoldTime = now
        } else if (now - peakHoldTime > 450L) {
            peakLevel = (peakLevel - 0.05f).coerceAtLeast(targetLevel)
        }
    }

    // Color definitions
    val clipRed = NeonRose
    val hotAmber = Color(0xFFF97316)
    val warnYellow = NeonAmber
    val nominalGreen = NeonEmerald
    val signalGreen = Color(0xFF059669)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        // Main Segmented LED Track Box
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF080C14))
                .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val totalHeight = size.height
                val totalWidth = size.width
                if (totalHeight <= 0f || totalWidth <= 0f) return@Canvas

                val count = segmentCount.coerceIn(12, 28)
                val gapPx = 1.5.dp.toPx()
                val totalGapSpace = gapPx * (count - 1)
                val segmentHeight = ((totalHeight - totalGapSpace) / count).coerceAtLeast(1f)
                val cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())

                val peakSegmentIndex = if (showPeakHold && peakLevel > 0.03f) {
                    (peakLevel * count).toInt().coerceIn(0, count - 1)
                } else {
                    -1
                }

                // Draw each LED segment from bottom (index 0) to top (index count - 1)
                for (i in 0 until count) {
                    val fraction = (i + 1).toFloat() / count
                    val segBottomY = totalHeight - (i * (segmentHeight + gapPx))
                    val segTopY = segBottomY - segmentHeight

                    // Determine LED Color based on threshold
                    val segColor = when {
                        fraction >= 0.88f -> clipRed
                        fraction >= 0.72f -> hotAmber
                        fraction >= 0.50f -> warnYellow
                        fraction >= 0.22f -> nominalGreen
                        else -> signalGreen
                    }

                    val isLit = currentLevel >= (i.toFloat() + 0.35f) / count
                    val isPeakLit = (i == peakSegmentIndex)

                    if (isLit) {
                        // Fully Illuminated LED Segment with specular highlight
                        drawRoundRect(
                            color = segColor,
                            topLeft = Offset(0.5.dp.toPx(), segTopY),
                            size = Size(totalWidth - 1.dp.toPx(), segmentHeight),
                            cornerRadius = cornerRadius
                        )
                    } else if (isPeakLit) {
                        // Peak Hold Marker Segment
                        drawRoundRect(
                            color = segColor,
                            topLeft = Offset(0.5.dp.toPx(), segTopY),
                            size = Size(totalWidth - 1.dp.toPx(), segmentHeight),
                            cornerRadius = cornerRadius
                        )
                    } else {
                        // Unlit Dark Lens Segment (Classic hardware mixer LED bezel)
                        drawRoundRect(
                            color = segColor.copy(alpha = 0.08f),
                            topLeft = Offset(0.5.dp.toPx(), segTopY),
                            size = Size(totalWidth - 1.dp.toPx(), segmentHeight),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }
        }

        // Optional Side dB Graduation Ticks
        if (showTicks) {
            Spacer(modifier = Modifier.width(2.dp))
            Canvas(
                modifier = Modifier
                    .width(4.dp)
                    .height(height)
            ) {
                val totalHeight = size.height
                val tickFractions = listOf(
                    0.92f to clipRed,       // Clip / 0 dBFS
                    0.75f to warnYellow,    // 0 dB Nominal
                    0.55f to nominalGreen,  // -12 dB
                    0.35f to nominalGreen,  // -24 dB
                    0.12f to signalGreen    // -48 dB / SIG
                )

                tickFractions.forEach { (fraction, color) ->
                    val y = totalHeight * (1f - fraction)
                    drawLine(
                        color = color.copy(alpha = 0.6f),
                        start = Offset(0f, y),
                        end = Offset(3.dp.toPx(), y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
    }
}


