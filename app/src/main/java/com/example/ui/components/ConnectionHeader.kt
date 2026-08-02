package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppThemeMode
import com.example.model.ConnectionStatus
import com.example.model.MixBusState
import com.example.model.MixerModelInfo
import com.example.model.UserRole
import com.example.ui.theme.*

@Composable
fun ConnectionHeader(
    mixerInfo: MixerModelInfo,
    assignedBusName: String,
    profileName: String,
    role: UserRole,
    buses: List<MixBusState> = emptyList(),
    activeBusIndex: Int = 0,
    appThemeMode: AppThemeMode = AppThemeMode.GLASSMORPHISM,
    onSelectBus: (Int) -> Unit = {},
    onSelectThemeMode: (AppThemeMode) -> Unit = {},
    onOpenDiscovery: () -> Unit,
    onOpenProfiles: () -> Unit,
    onToggleEngineer: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onSyncMixer: () -> Unit = {},
    onReconnect: () -> Unit = {}
) {
    var showThemeMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(bottom = 6.dp)
    ) {
        // Top Toolbar Row: App Branding & Utility Icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // App Title / Branding
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Mixer Engine",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "IEM MIXER PRO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Right side tools: Profile Switcher, Theme Switcher, Diagnostics, Engineer Lock
            Row(verticalAlignment = Alignment.CenterVertically) {
                // User Profile Switcher
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { onOpenProfiles() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = profileName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Theme Mode Switcher
                Box {
                    IconButton(
                        onClick = { showThemeMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = when (appThemeMode) {
                                AppThemeMode.CQ_MIXPAD -> Icons.Default.Equalizer
                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                AppThemeMode.GLASSMORPHISM -> Icons.Default.AutoAwesome
                            },
                            contentDescription = "Theme Mode",
                            tint = if (appThemeMode == AppThemeMode.CQ_MIXPAD || appThemeMode == AppThemeMode.GLASSMORPHISM) NeonCyan else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        AppThemeMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when (mode) {
                                                AppThemeMode.CQ_MIXPAD -> Icons.Default.Equalizer
                                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                                AppThemeMode.GLASSMORPHISM -> Icons.Default.AutoAwesome
                                            },
                                            contentDescription = null,
                                            tint = if (appThemeMode == mode) NeonCyan else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = mode.displayName,
                                                fontSize = 12.sp,
                                                fontWeight = if (appThemeMode == mode) FontWeight.Bold else FontWeight.Normal,
                                                color = if (appThemeMode == mode) NeonCyan else TextPrimary
                                            )
                                            Text(
                                                text = mode.description,
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectThemeMode(mode)
                                    showThemeMenu = false
                                }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onOpenDiagnostics,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = "Diagnostics",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onToggleEngineer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (role.isUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = "Engineer Lock",
                        tint = if (role.isUnlocked) NeonRose else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Full-Width Connected Mixer Selector Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (mixerInfo.status == ConnectionStatus.DISCONNECTED) NeonRose.copy(alpha = 0.6f) else DarkBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenDiscovery() }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    when (mixerInfo.status) {
                                        ConnectionStatus.CONNECTED -> NeonEmerald
                                        ConnectionStatus.SIMULATION -> NeonCyan
                                        ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> NeonAmber
                                        ConnectionStatus.DISCONNECTED -> NeonRose
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CONNECTED MIXER",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Text(
                                text = "${mixerInfo.model.uppercase()} (${if (mixerInfo.status == ConnectionStatus.CONNECTED || mixerInfo.status == ConnectionStatus.SIMULATION) "CONNECTED" else mixerInfo.status.name})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (mixerInfo.status == ConnectionStatus.DISCONNECTED) {
                            Surface(
                                onClick = onReconnect,
                                color = NeonRose,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Reconnect",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "RECONNECT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        } else if (mixerInfo.status == ConnectionStatus.CONNECTING || mixerInfo.status == ConnectionStatus.RECONNECTING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = NeonAmber,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Row(
                            modifier = Modifier.clickable { onOpenDiscovery() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CHANGE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Open Mixer Settings",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
