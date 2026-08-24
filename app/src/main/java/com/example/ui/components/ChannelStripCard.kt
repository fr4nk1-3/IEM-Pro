package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
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
import com.example.model.ChannelState
import com.example.ui.theme.*

@Composable
fun ChannelStripCard(
    channel: ChannelState,
    activeBusIndex: Int, // 0..15
    onLevelChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 120.dp,
    onPanChange: ((Float) -> Unit)? = null,
    isSelected: Boolean = false,
    onSelect: (() -> Unit)? = null
) {
    var isAdjusting by remember { mutableStateOf(false) }
    val effectiveSelected = isSelected || isAdjusting

    val sendLevel = channel.busSendLevels.getOrElse(activeBusIndex) { 0.75f }
    val isSendMuted = channel.busSendMutes.getOrElse(activeBusIndex) { false }
    val colorAccent = channel.color.composeColor
    val haptic = LocalHapticFeedback.current

    val isUltraCompact = cardWidth < 72.dp
    val isCompact = cardWidth < 90.dp

    Card(
        modifier = modifier
            .width(cardWidth)
            .fillMaxHeight()
            .padding(horizontal = 1.dp, vertical = 2.dp)
            .rgbGlowBorder(isSelected = effectiveSelected, borderWidth = 2.dp, shape = RoundedCornerShape(10.dp))
            .then(if (onSelect != null) Modifier.clickable { onSelect() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = if (!effectiveSelected) BorderStroke(1.dp, DarkBorder) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (effectiveSelected) 6.dp else 2.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(if (isUltraCompact) 3.dp else if (isCompact) 4.dp else 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Color Accent Strip + Channel Number + Real-time SIG/CLIP LED
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(colorAccent.copy(alpha = 0.2f))
                    .padding(horizontal = if (isUltraCompact) 2.dp else if (isCompact) 4.dp else 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(if (isUltraCompact) 5.dp else if (isCompact) 6.dp else 8.dp)
                            .clip(CircleShape)
                            .background(colorAccent)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "CH ${channel.id.toString().padStart(2, '0')}",
                        fontSize = if (isUltraCompact) 8.sp else if (isCompact) 9.sp else 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                }

                // Real-time Input Signal / Clip LED indicator
                val isClipping = channel.peakMeter >= 0.88f
                val hasSignal = channel.peakMeter >= 0.05f
                val sigColor = when {
                    isClipping -> NeonRose
                    hasSignal -> NeonEmerald
                    else -> Color(0xFF1E293B)
                }

                Box(
                    modifier = Modifier
                        .size(if (isUltraCompact) 5.dp else 7.dp)
                        .clip(CircleShape)
                        .background(sigColor)
                        .border(0.5.dp, if (isClipping || hasSignal) sigColor else Color(0xFF334155), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Channel Name & Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = getInstrumentIcon(channel.iconType),
                    contentDescription = channel.iconType,
                    tint = colorAccent,
                    modifier = Modifier.size(if (isUltraCompact) 10.dp else if (isCompact) 12.dp else 14.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = channel.name,
                    fontSize = if (isUltraCompact) 9.sp else if (isCompact) 10.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Channel dB Display Box (matching Bus Master style)
            val sendDbString = remember(sendLevel) { ChannelState.faderToDbString(sendLevel) }
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
                    text = if (isSendMuted) "MUTED" else sendDbString,
                    fontSize = if (isUltraCompact) 9.sp else if (isCompact) 10.sp else 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSendMuted) NeonRose else NeonCyan
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Large Touch Fader + Peak Meter - filling remaining vertical space
            val activeMeterLevel = if (isSendMuted || channel.isMuted) {
                0.0f
            } else {
                (channel.peakMeter * (0.25f + sendLevel * 0.75f)).coerceIn(0f, 1f)
            }

            LargeTouchFader(
                value = sendLevel,
                onValueChange = onLevelChange,
                peakMeter = activeMeterLevel,
                faderColor = if (isSendMuted) DarkBorder else colorAccent,
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

            // Mute Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onMuteToggle()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSendMuted) NeonRose else DarkSurfaceVariant,
                    contentColor = if (isSendMuted) Color.White else TextSecondary
                ),
                contentPadding = PaddingValues(vertical = 1.dp, horizontal = 2.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isUltraCompact) 24.dp else 28.dp)
            ) {
                Text(
                    text = if (isSendMuted) "MUTED" else "MUTE",
                    fontSize = if (isUltraCompact) 8.sp else if (isCompact) 9.sp else 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun getInstrumentIcon(iconType: String) = when (iconType.uppercase()) {
    "DRUM" -> Icons.Default.GraphicEq
    "BASS" -> Icons.Default.AudioFile
    "GUITAR" -> Icons.Default.MusicNote
    "KEYBOARD" -> Icons.Default.Piano
    "MIC" -> Icons.Default.Mic
    "HORN" -> Icons.AutoMirrored.Filled.VolumeUp
    "FX" -> Icons.Default.SurroundSound
    else -> Icons.Default.Equalizer
}
