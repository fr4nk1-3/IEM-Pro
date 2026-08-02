package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChannelState
import com.example.ui.theme.*

@Composable
fun GroupFaderCard(
    groupName: String, // e.g. "Drums Kit", "Vocals"
    channelCount: Int,
    groupLevel: Float, // 0.0 .. 1.0
    onGroupLevelChange: (Float) -> Unit,
    isGroupMuted: Boolean = false,
    onGroupMuteToggle: (() -> Unit)? = null,
    accentColor: Color = NeonAmber,
    peakMeter: Float = -1f,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 120.dp
) {
    val haptic = LocalHapticFeedback.current
    val isCompact = cardWidth < 85.dp

    Card(
        modifier = modifier
            .width(cardWidth)
            .fillMaxHeight()
            .padding(horizontal = 1.dp, vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(if (isCompact) 4.dp else 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = if (isCompact) 3.dp else 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = "Group",
                    tint = accentColor,
                    modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = groupName,
                    fontSize = if (isCompact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "$channelCount chs",
                fontSize = if (isCompact) 9.sp else 10.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Group dB Display Box (matching Bus Master style)
            val groupDbString = remember(groupLevel) { ChannelState.faderToDbString(groupLevel) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(DarkBackground)
                    .border(0.5.dp, DarkBorder, RoundedCornerShape(4.dp))
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isGroupMuted) "MUTED" else groupDbString,
                    fontSize = if (isCompact) 10.sp else 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isGroupMuted) NeonRose else NeonCyan
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            val groupPeakMeter = if (isGroupMuted) 0.0f else if (peakMeter >= 0f) peakMeter else (groupLevel * 0.85f).coerceIn(0f, 1f)

            LargeTouchFader(
                value = groupLevel,
                onValueChange = onGroupLevelChange,
                peakMeter = groupPeakMeter,
                faderColor = if (isGroupMuted) DarkBorder else accentColor,
                width = if (isCompact) 36.dp else 50.dp,
                showMeter = true,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (onGroupMuteToggle != null) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onGroupMuteToggle()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGroupMuted) NeonRose else DarkSurfaceVariant,
                        contentColor = if (isGroupMuted) Color.White else TextSecondary
                    ),
                    contentPadding = PaddingValues(vertical = 1.dp, horizontal = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                ) {
                    Text(
                        text = if (isGroupMuted) "MUTED" else "MUTE",
                        fontSize = if (isCompact) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "SUB-MIX",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
        }
    }
}
