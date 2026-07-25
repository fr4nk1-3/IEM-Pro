package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonRose

@Composable
fun MeterBar(
    level: Float, // 0.0 .. 1.0
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    width: Dp = 6.dp
) {
    val animatedLevel by animateFloatAsState(
        targetValue = level.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 60),
        label = "meterAnim"
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF111827))
            .border(0.5.dp, Color(0xFF374151), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Meter Fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedLevel)
                .background(
                    when {
                        animatedLevel > 0.88f -> NeonRose
                        animatedLevel > 0.70f -> NeonAmber
                        else -> NeonEmerald
                    }
                )
        )
    }
}
