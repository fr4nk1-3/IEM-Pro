package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextSecondary

@Composable
fun PanControl(
    pan: Float, // 0.0 (Left) .. 0.5 (Center) .. 1.0 (Right)
    onPanChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var widthPx by remember { mutableFloatStateOf(1f) }

    val panLabel = remember(pan) {
        val pct = ((pan - 0.5f) * 200).toInt()
        when {
            pct < -2 -> "L${-pct}%"
            pct > 2 -> "R${pct}%"
            else -> "C"
        }
    }

    var lastTapTime by remember { mutableLongStateOf(0L) }

    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("PAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Text(panLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
        }

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1A212D))
                .onGloballyPositioned { coordinates ->
                    widthPx = coordinates.size.width.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                        val now = System.currentTimeMillis()
                        if (now - lastTapTime < 300L) {
                            // Center pan on double tap
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPanChange(0.5f)
                            lastTapTime = 0L
                        } else {
                            val newPan = (down.position.x / widthPx).coerceIn(0f, 1f)
                            onPanChange(newPan)
                            lastTapTime = now
                        }

                        var previousPan = pan
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            val newPan = (change.position.x / widthPx).coerceIn(0f, 1f)

                            val crossedCenter = (previousPan < 0.5f && newPan >= 0.5f) || (previousPan > 0.5f && newPan <= 0.5f)
                            if (crossedCenter) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                previousPan = newPan
                            } else if (kotlin.math.abs(newPan - previousPan) >= 0.08f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                previousPan = newPan
                            }

                            onPanChange(newPan)
                        }
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Center detent line
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.3f))
            )

            // Pan Thumb Indicator - Safely positioned using BiasAlignment
            val safePan = pan.coerceIn(0f, 1f)
            val horizontalBias = (safePan * 2f) - 1f // Maps 0.0..1.0 to -1.0..1.0

            Box(
                modifier = Modifier
                    .align(BiasAlignment(horizontalBias = horizontalBias, verticalBias = 0f))
                    .width(16.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonCyan)
            )
        }
    }
}
