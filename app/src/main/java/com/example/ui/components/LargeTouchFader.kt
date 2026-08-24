package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun LargeTouchFader(
    value: Float, // 0.0 .. 1.0
    onValueChange: (Float) -> Unit,
    peakMeter: Float = 0f,
    peakMeterL: Float? = null,
    peakMeterR: Float? = null,
    isMuted: Boolean = false,
    faderColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 180.dp,
    width: Dp = 52.dp,
    showMeter: Boolean = true,
    onDragStateChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var totalHeightPx by remember { mutableFloatStateOf(1f) }
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier
    ) {
        val actualHeight = if (maxHeight != Dp.Unspecified && maxHeight > 0.dp && maxHeight < Dp.Infinity) {
            maxHeight.coerceIn(120.dp, 600.dp)
        } else {
            height
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight().padding(vertical = 2.dp)
        ) {
            if (showMeter) {
                if (peakMeterL != null && peakMeterR != null) {
                    MasterStereoMeterBar(
                        levelLeft = peakMeterL,
                        levelRight = peakMeterR,
                        isMuted = isMuted,
                        height = actualHeight,
                        barWidth = if (width < 32.dp) 3.5.dp else if (width < 42.dp) 4.5.dp else 5.5.dp,
                        showLabels = true,
                        showTicks = (width >= 44.dp),
                        modifier = Modifier.padding(end = if (width >= 40.dp) 2.dp else 3.dp)
                    )
                } else {
                    MeterBar(
                        level = peakMeter,
                        isMuted = isMuted,
                        height = actualHeight,
                        width = if (width < 32.dp) 6.dp else 7.dp,
                        showTicks = (width >= 40.dp),
                        modifier = Modifier.padding(end = if (width >= 40.dp) 2.dp else 3.dp)
                    )
                }
            }

            var lastTapTime by remember { mutableLongStateOf(0L) }

            Box(
                modifier = Modifier
                    .width(width)
                    .height(actualHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .onGloballyPositioned { coordinates ->
                        totalHeightPx = coordinates.size.height.toFloat().coerceAtLeast(1f)
                    }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            onDragStateChange?.invoke(true)
                            try {
                                down.consume()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                val now = System.currentTimeMillis()
                                if (now - lastTapTime < 300L) {
                                    // Double tap -> reset to 0 dB (0.75f)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onValueChange(0.75f)
                                    lastTapTime = 0L
                                } else {
                                    val newFraction = (1f - (down.position.y / totalHeightPx)).coerceIn(0f, 1f)
                                    onValueChange(newFraction)
                                    lastTapTime = now
                                }

                                var previousVal = value
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    change.consume()
                                    val newFraction = (1f - (change.position.y / totalHeightPx)).coerceIn(0f, 1f)
                                    
                                    val crossedZeroDb = (previousVal < 0.75f && newFraction >= 0.75f) || (previousVal > 0.75f && newFraction <= 0.75f)
                                    if (crossedZeroDb) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        previousVal = newFraction
                                    } else if (kotlin.math.abs(newFraction - previousVal) >= 0.05f) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        previousVal = newFraction
                                    }

                                    onValueChange(newFraction)
                                }
                            } finally {
                                onDragStateChange?.invoke(false)
                            }
                        }
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                // Track Groove line
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(Color(0xFF0D131D))
                )

                // Muted Dark Metallic Fill Bar following slider level
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(value.coerceIn(0f, 1f))
                        .background(Color(0xFF33425B).copy(alpha = 0.35f))
                )

                // 0 dB Line Indicator (at 0.75 fraction)
                val lineBottomPadding = ((actualHeight * 0.75f) - 1.dp).coerceAtLeast(0.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = lineBottomPadding)
                        .height(2.dp)
                        .background(TextPrimary.copy(alpha = 0.5f))
                )

                // Fader Handle / Cap (3D Textured Metallic Silver, 20% longer: 44.dp height)
                val handleHeight = 44.dp
                val handleBottomPadding = ((actualHeight - handleHeight).coerceAtLeast(0.dp) * value.coerceIn(0f, 1f))
                Box(
                    modifier = Modifier
                        .padding(bottom = handleBottomPadding)
                        .width((width - 6.dp).coerceAtLeast(18.dp))
                        .height(handleHeight)
                        .shadow(6.dp, RoundedCornerShape(6.dp), spotColor = Color.Black)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF8FAFC), // Specular Top Metallic Reflection
                                    Color(0xFFE2E8F0), // Brushed Silver Top
                                    Color(0xFFCBD5E1), // Metallic Silver Mid
                                    Color(0xFF94A3B8), // Brushed Steel Tone
                                    Color(0xFF64748B), // Metallic Shadow Tone
                                    Color(0xFF475569)  // Deep Metallic Bottom Bevel
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF), // Top Specular Bevel
                                    Color(0xFFCBD5E1), // Mid Rim Silver
                                    Color(0xFF334155)  // Bottom Bevel Inset Shadow
                                )
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(vertical = 4.dp)
                    ) {
                        // 3D Engraved Ribbed Metal Grooves
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            repeat(4) {
                                Column {
                                    // Dark Engraved Shadow Line
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .height(1.dp)
                                            .background(Color(0xFF1E293B).copy(alpha = 0.75f))
                                    )
                                    // Light Specular Highlight Line
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .height(1.dp)
                                            .background(Color(0xFFFFFFFF).copy(alpha = 0.85f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
