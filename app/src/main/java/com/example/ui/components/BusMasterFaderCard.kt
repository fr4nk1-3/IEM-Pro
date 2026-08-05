package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MixBusState
import com.example.ui.theme.*

@Composable
fun BusMasterFaderCard(
    bus: MixBusState,
    onMasterLevelChange: (Float) -> Unit,
    onMasterMuteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 120.dp,
    isSelected: Boolean = false,
    onSelect: (() -> Unit)? = null
) {
    var isAdjusting by remember { mutableStateOf(false) }
    val effectiveSelected = isSelected || isAdjusting

    val haptic = LocalHapticFeedback.current
    val accentColor = if (bus.masterMute) NeonRose else NeonAmber
    val isUltraCompact = cardWidth < 72.dp
    val isCompact = cardWidth < 90.dp

    Card(
        modifier = modifier
            .width(cardWidth)
            .fillMaxHeight()
            .padding(horizontal = 1.dp, vertical = 2.dp)
            .rgbGlowBorder(isSelected = effectiveSelected, borderWidth = 2.dp, shape = RoundedCornerShape(10.dp))
            .then(
                if (!effectiveSelected) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = if (bus.masterMute) NeonRose.copy(alpha = 0.6f) else NeonAmber.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp)
                    )
                } else Modifier
            )
            .then(if (onSelect != null) Modifier.clickable { onSelect() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (effectiveSelected) 6.dp else 4.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(if (isUltraCompact) 3.dp else if (isCompact) 4.dp else 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header: Bus Master Label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = if (isUltraCompact) 2.dp else if (isCompact) 4.dp else 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Bus Master",
                        tint = accentColor,
                        modifier = Modifier.size(if (isUltraCompact) 10.dp else if (isCompact) 12.dp else 14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "BUS ${bus.id}",
                        fontSize = if (isUltraCompact) 8.sp else if (isCompact) 9.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                if (bus.isStereoLinked) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(NeonCyan.copy(alpha = 0.3f))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "ST",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NeonCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Bus Name & Output Readout
            Text(
                text = bus.name,
                fontSize = if (isUltraCompact) 9.sp else if (isCompact) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Master dB Display
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
                    text = if (bus.masterMute) "MUTED" else bus.getMasterDbString(),
                    fontSize = if (isUltraCompact) 9.sp else if (isCompact) 10.sp else 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (bus.masterMute) NeonRose else NeonCyan
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Master Fader
            LargeTouchFader(
                value = bus.masterLevel,
                onValueChange = onMasterLevelChange,
                peakMeter = if (bus.masterMute) 0.0f else bus.peakMeter,
                faderColor = accentColor,
                width = if (isUltraCompact) 28.dp else if (isCompact) 36.dp else 50.dp,
                showMeter = true,
                onDragStateChange = { dragging ->
                    isAdjusting = dragging
                    if (dragging) {
                        onSelect?.invoke()
                    }
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Bus Master Mute Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMasterMuteToggle()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (bus.masterMute) NeonRose else DarkSurfaceVariant,
                    contentColor = if (bus.masterMute) Color.White else TextPrimary
                ),
                contentPadding = PaddingValues(vertical = 1.dp, horizontal = 2.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isUltraCompact) 24.dp else 28.dp)
            ) {
                Text(
                    text = if (isCompact) (if (bus.masterMute) "MUTED" else "MUTE") else (if (bus.masterMute) "MASTER MUTED" else "MUTE MASTER"),
                    fontSize = if (isUltraCompact) 8.sp else if (isCompact) 9.sp else 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
