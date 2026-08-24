package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonRose

/**
 * Midas M32-Edit style high-resolution Audio Signal Level Meter (VU Meter).
 * Features smooth continuous gradient with fine micro-segment slits,
 * fast attack / natural ballistic decay, and transient peak-hold tracking.
 */
@Composable
fun MeterBar(
    level: Float, // 0.0 .. 1.0
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    height: Dp = 180.dp,
    width: Dp = 6.dp,
    showPeakHold: Boolean = true,
    showTicks: Boolean = false
) {
    val targetLevel = level.coerceIn(0f, 1f)

    // Fast instant attack, smooth exponential decay for authentic console VU ballistics
    val currentLevel by animateFloatAsState(
        targetValue = targetLevel,
        animationSpec = tween(
            durationMillis = if (targetLevel > 0.3f) 20 else 80
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
        } else if (now - peakHoldTime > 400L) {
            peakLevel = (peakLevel - 0.05f).coerceAtLeast(targetLevel)
        }
    }

    // Midas M32 / X32 calibrated meter color gradient:
    // When muted: Decolorized greyscale/muted slate gradient
    // When active: Signal Green -> Nominal Emerald -> Yellow/Warm -> Amber -> Clip Red
    val meterColorStops = remember(isMuted) {
        if (isMuted) {
            arrayOf(
                0.00f to Color(0xFF94A3B8), // Muted clip grey
                0.10f to Color(0xFF64748B),
                0.18f to Color(0xFF475569),
                0.32f to Color(0xFF334155),
                0.50f to Color(0xFF334155),
                0.70f to Color(0xFF1E293B),
                0.85f to Color(0xFF1E293B),
                1.00f to Color(0xFF0F172A)
            )
        } else {
            arrayOf(
                0.00f to Color(0xFFEF4444), // Clip (0 dBFS)
                0.10f to Color(0xFFF43F5E), // +3 dB
                0.18f to Color(0xFFF97316), // 0 dB Nominal Hot
                0.32f to Color(0xFFEAB308), // -6 dB Warning
                0.50f to Color(0xFF84CC16), // -12 dB
                0.70f to Color(0xFF10B981), // -18 dB
                1.00f to Color(0xFF059669)  // -48 dB / Signal Present
            )
        }
    }

    val dimMeterColorStops = remember(isMuted) {
        if (isMuted) {
            arrayOf(
                0.00f to Color(0xFF475569).copy(alpha = 0.10f),
                0.50f to Color(0xFF334155).copy(alpha = 0.08f),
                1.00f to Color(0xFF1E293B).copy(alpha = 0.06f)
            )
        } else {
            arrayOf(
                0.00f to Color(0xFFEF4444).copy(alpha = 0.12f),
                0.10f to Color(0xFFF43F5E).copy(alpha = 0.12f),
                0.18f to Color(0xFFF97316).copy(alpha = 0.12f),
                0.32f to Color(0xFFEAB308).copy(alpha = 0.12f),
                0.50f to Color(0xFF84CC16).copy(alpha = 0.12f),
                0.70f to Color(0xFF10B981).copy(alpha = 0.12f),
                1.00f to Color(0xFF059669).copy(alpha = 0.12f)
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        // Main Meter Track Box
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF090D14))
                .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val totalHeight = size.height
                val totalWidth = size.width
                if (totalHeight <= 0f || totalWidth <= 0f) return@Canvas

                // 1. Draw dim background gradient track
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colorStops = dimMeterColorStops,
                        startY = 0f,
                        endY = totalHeight
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(totalWidth, totalHeight),
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                )

                // 2. Draw solid active signal bar (continuous gradient filled from bottom)
                if (currentLevel > 0.01f) {
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
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                    )
                }

                // 3. Draw fine M32-Edit small segmentation slits (subtle horizontal micro-gaps every 2.5dp)
                val slitIntervalPx = 2.5.dp.toPx()
                val slitHeightPx = 0.6.dp.toPx()
                var slitY = totalHeight - slitIntervalPx
                while (slitY > 0f) {
                    drawRect(
                        color = Color(0xFF090D14).copy(alpha = 0.85f),
                        topLeft = Offset(0f, slitY),
                        size = Size(totalWidth, slitHeightPx)
                    )
                    slitY -= slitIntervalPx
                }

                // 4. Draw Peak Hold Indicator
                if (showPeakHold && peakLevel > 0.02f) {
                    val peakY = (totalHeight - (totalHeight * peakLevel)).coerceIn(0f, totalHeight - 2.dp.toPx())
                    val peakColor = if (isMuted) {
                        Color(0xFF94A3B8)
                    } else {
                        when {
                            peakLevel >= 0.88f -> Color(0xFFEF4444)
                            peakLevel >= 0.70f -> Color(0xFFF97316)
                            peakLevel >= 0.50f -> Color(0xFFEAB308)
                            else -> Color(0xFF10B981)
                        }
                    }

                    drawRect(
                        color = peakColor,
                        topLeft = Offset(0f, peakY),
                        size = Size(totalWidth, 1.8.dp.toPx())
                    )
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
                    0.92f to Color(0xFFEF4444), // Clip
                    0.75f to Color(0xFFF97316), // 0 dB
                    0.55f to Color(0xFFEAB308), // -6 dB
                    0.35f to Color(0xFF10B981), // -18 dB
                    0.12f to Color(0xFF059669)  // -48 dB
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

/**
 * High-precision Master Stereo VU Meter Bar (Dual L / R) for Master Bus Output Monitoring.
 * Features independent left/right ballistic meters, center dB graduation markers,
 * peak-hold, and dual clip/signal presence indicators.
 */
@Composable
fun MasterStereoMeterBar(
    levelLeft: Float,
    levelRight: Float,
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    height: Dp = 180.dp,
    barWidth: Dp = 5.dp,
    showLabels: Boolean = true,
    showTicks: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        if (showLabels) {
            Row(
                modifier = Modifier.width(barWidth * 2 + (if (showTicks) 18.dp else 4.dp)),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "L",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMuted) Color(0xFF64748B) else if (levelLeft >= 0.88f) Color(0xFFEF4444) else if (levelLeft >= 0.05f) Color(0xFF10B981) else Color(0xFF64748B)
                )
                if (showTicks) {
                    Text(
                        text = "dB",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }
                Text(
                    text = "R",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMuted) Color(0xFF64748B) else if (levelRight >= 0.88f) Color(0xFFEF4444) else if (levelRight >= 0.05f) Color(0xFF10B981) else Color(0xFF64748B)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Left Channel Meter
            MeterBar(
                level = levelLeft,
                isMuted = isMuted,
                height = height,
                width = barWidth,
                showTicks = false,
                showPeakHold = true
            )

            if (showTicks) {
                // Center dB Calibration Scale
                Column(
                    modifier = Modifier
                        .height(height)
                        .padding(horizontal = 2.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CLIP", fontSize = 6.sp, fontWeight = FontWeight.ExtraBold, color = if (isMuted) Color(0xFF64748B) else Color(0xFFEF4444))
                    Text("0", fontSize = 6.sp, fontWeight = FontWeight.Bold, color = if (isMuted) Color(0xFF64748B) else Color(0xFFF97316))
                    Text("-6", fontSize = 6.sp, fontWeight = FontWeight.Normal, color = if (isMuted) Color(0xFF475569) else Color(0xFFEAB308))
                    Text("-18", fontSize = 6.sp, fontWeight = FontWeight.Normal, color = if (isMuted) Color(0xFF475569) else Color(0xFF10B981))
                    Text("-48", fontSize = 6.sp, fontWeight = FontWeight.Normal, color = if (isMuted) Color(0xFF334155) else Color(0xFF059669))
                }
            } else {
                Spacer(modifier = Modifier.width(2.dp))
            }

            // Right Channel Meter
            MeterBar(
                level = levelRight,
                isMuted = isMuted,
                height = height,
                width = barWidth,
                showTicks = false,
                showPeakHold = true
            )
        }
    }
}

/**
 * Horizontal Audio Level Meter for landscape bars, bus headers, and monitor cards.
 */
@Composable
fun HorizontalMeterBar(
    level: Float,
    modifier: Modifier = Modifier,
    isMuted: Boolean = false,
    height: Dp = 8.dp,
    showPeakHold: Boolean = true
) {
    val targetLevel = level.coerceIn(0f, 1f)
    val currentLevel by animateFloatAsState(
        targetValue = targetLevel,
        animationSpec = tween(durationMillis = if (targetLevel > 0.3f) 20 else 80),
        label = "hMeterBallistic"
    )

    var peakLevel by remember { mutableFloatStateOf(0f) }
    var peakHoldTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(targetLevel) {
        val now = System.currentTimeMillis()
        if (targetLevel >= peakLevel) {
            peakLevel = targetLevel
            peakHoldTime = now
        } else if (now - peakHoldTime > 400L) {
            peakLevel = (peakLevel - 0.05f).coerceAtLeast(targetLevel)
        }
    }

    val meterColorStops = remember(isMuted) {
        if (isMuted) {
            arrayOf(
                0.00f to Color(0xFF0F172A),
                0.35f to Color(0xFF1E293B),
                0.55f to Color(0xFF334155),
                0.72f to Color(0xFF475569),
                0.85f to Color(0xFF64748B),
                1.00f to Color(0xFF94A3B8)
            )
        } else {
            arrayOf(
                0.00f to Color(0xFF059669), // -48 dB / Signal Present
                0.35f to Color(0xFF10B981), // -18 dB
                0.55f to Color(0xFF84CC16), // -12 dB
                0.72f to Color(0xFFEAB308), // -6 dB Warning
                0.85f to Color(0xFFF97316), // 0 dB Nominal Hot
                0.92f to Color(0xFFF43F5E), // +3 dB
                1.00f to Color(0xFFEF4444)  // Clip (0 dBFS)
            )
        }
    }

    val dimMeterColorStops = remember(isMuted) {
        if (isMuted) {
            arrayOf(
                0.00f to Color(0xFF1E293B).copy(alpha = 0.06f),
                0.50f to Color(0xFF334155).copy(alpha = 0.08f),
                1.00f to Color(0xFF475569).copy(alpha = 0.10f)
            )
        } else {
            arrayOf(
                0.00f to Color(0xFF059669).copy(alpha = 0.12f),
                0.35f to Color(0xFF10B981).copy(alpha = 0.12f),
                0.55f to Color(0xFF84CC16).copy(alpha = 0.12f),
                0.72f to Color(0xFFEAB308).copy(alpha = 0.12f),
                0.85f to Color(0xFFF97316).copy(alpha = 0.12f),
                0.92f to Color(0xFFF43F5E).copy(alpha = 0.12f),
                1.00f to Color(0xFFEF4444).copy(alpha = 0.12f)
            )
        }
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF090D14))
            .border(0.5.dp, Color(0xFF1E293B), RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalWidth = size.width
            val totalHeight = size.height
            if (totalWidth <= 0f || totalHeight <= 0f) return@Canvas

            // 1. Dim background
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colorStops = dimMeterColorStops,
                    startX = 0f,
                    endX = totalWidth
                ),
                topLeft = Offset(0f, 0f),
                size = Size(totalWidth, totalHeight),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )

            // 2. Active level fill
            if (currentLevel > 0.01f) {
                val fillWidth = totalWidth * currentLevel
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colorStops = meterColorStops,
                        startX = 0f,
                        endX = totalWidth
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(fillWidth, totalHeight),
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
                )
            }

            // 3. Micro slit dividers
            val slitIntervalPx = 4.dp.toPx()
            var slitX = slitIntervalPx
            while (slitX < totalWidth) {
                drawRect(
                    color = Color(0xFF090D14).copy(alpha = 0.85f),
                    topLeft = Offset(slitX, 0f),
                    size = Size(0.8.dp.toPx(), totalHeight)
                )
                slitX += slitIntervalPx
            }

            // 4. Peak hold marker
            if (showPeakHold && peakLevel > 0.02f) {
                val peakX = (totalWidth * peakLevel).coerceIn(0f, totalWidth - 2.dp.toPx())
                val peakColor = if (isMuted) {
                    Color(0xFF94A3B8)
                } else {
                    when {
                        peakLevel >= 0.88f -> Color(0xFFEF4444)
                        peakLevel >= 0.70f -> Color(0xFFF97316)
                        peakLevel >= 0.50f -> Color(0xFFEAB308)
                        else -> Color(0xFF10B981)
                    }
                }

                drawRect(
                    color = peakColor,
                    topLeft = Offset(peakX, 0f),
                    size = Size(1.8.dp.toPx(), totalHeight)
                )
            }
        }
    }
}



