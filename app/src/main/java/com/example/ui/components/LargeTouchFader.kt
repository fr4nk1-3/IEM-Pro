package com.example.ui.components

import androidx.compose.foundation.background
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
    faderColor: Color = MaterialTheme.colorScheme.primary,
    height: Dp = 180.dp,
    width: Dp = 52.dp,
    showMeter: Boolean = true,
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
                MeterBar(
                    level = peakMeter,
                    height = actualHeight,
                    width = 6.dp,
                    modifier = Modifier.padding(end = 4.dp)
                )
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

                // Fill Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(value.coerceIn(0f, 1f))
                        .background(faderColor.copy(alpha = 0.25f))
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

                // Fader Handle / Cap
                val handleBottomPadding = ((actualHeight - 36.dp).coerceAtLeast(0.dp) * value.coerceIn(0f, 1f))
                Box(
                    modifier = Modifier
                        .padding(bottom = handleBottomPadding)
                        .width((width - 8.dp).coerceAtLeast(16.dp))
                        .height(36.dp)
                        .shadow(6.dp, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(faderColor),
                    contentAlignment = Alignment.Center
                ) {
                    // Fader grip lines
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(modifier = Modifier.width(20.dp).height(2.dp).background(Color.Black.copy(alpha = 0.4f)))
                        Box(modifier = Modifier.width(20.dp).height(2.dp).background(Color.Black.copy(alpha = 0.4f)))
                    }
                }
            }
        }
    }
}
