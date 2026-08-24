package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.ui.IemViewModel
import com.example.ui.theme.*

data class MixerPreset(
    val id: String,
    val manufacturer: String,
    val modelName: String,
    val backendPort: Int,
    val description: String,
    val channelCount: String = "32-Ch"
)

data class BrandInfo(
    val name: String,
    val subtitle: String
)

val BRAND_LIST = listOf(
    BrandInfo("Behringer", "WING / X32 / XR18"),
    BrandInfo("Midas", "M32 / MR18"),
    BrandInfo("Allen & Heath", "SQ / Qu / Avantis"),
    BrandInfo("Yamaha", "TF / CL / QL / DM3"),
    BrandInfo("Soundcraft", "Ui24R / Ui16 / Ui12"),
    BrandInfo("Generic / Custom", "Custom OSC")
)

val SUPPORTED_MIXER_PRESETS = listOf(
    // Behringer
    MixerPreset("wing", "Behringer", "WING Series (WING / Compact / Rack)", 10023, "48-Ch Flagship Console (OSC Port 10023 / 2222)", "48-Ch"),
    MixerPreset("x32", "Behringer", "X32 Series (X32 / Compact / Producer / Rack)", 10023, "32-Ch Industry Standard Console (OSC Port 10023)", "32-Ch"),
    MixerPreset("xair", "Behringer", "X-Air Series (XR18 / XR16 / XR12)", 8900, "18/16/12-Ch Stagebox Mixer (OSC Port 8900)", "18-Ch"),

    // Midas
    MixerPreset("m32", "Midas", "M32 Series (M32 / M32R / M32C)", 10023, "32-Ch Premium Digital Console (OSC Port 10023)", "32-Ch"),
    MixerPreset("mr18", "Midas", "MR Series (MR18 / MR12)", 8900, "18/12-Ch Stagebox Mixer (OSC Port 8900)", "18-Ch"),

    // Allen & Heath
    MixerPreset("sq", "Allen & Heath", "SQ Series (SQ5 / SQ6 / SQ7)", 51325, "48-Ch Digital Console (OSC Port 51325)", "48-Ch"),
    MixerPreset("qu", "Allen & Heath", "Qu Series (Qu16 / Qu24 / Qu32 / Qu-PAC)", 51326, "Digital Mixer (OSC Port 51326)", "32-Ch"),
    MixerPreset("dlive", "Allen & Heath", "Avantis / dLive Series", 51325, "Flagship Audio Console (OSC Port 51325)", "64-Ch"),

    // Yamaha
    MixerPreset("yamaha_tf", "Yamaha", "TF Series (TF1 / TF3 / TF5 / TF-Rack)", 49280, "Digital Console (OSC Port 49280)", "48-Ch"),
    MixerPreset("yamaha_clql", "Yamaha", "CL / QL Series (CL1 / CL5 / QL1 / QL5)", 49280, "Commercial Console (OSC Port 49280)", "64-Ch"),
    MixerPreset("yamaha_dm3", "Yamaha", "DM3 Series", 49280, "Compact Digital Console (OSC Port 49280)", "22-Ch"),

    // Soundcraft
    MixerPreset("soundcraft", "Soundcraft", "Ui Series (Ui24R / Ui16 / Ui12)", 80, "Web/OSC Rack Mixer (Port 80)", "24-Ch"),

    // Generic
    MixerPreset("custom", "Generic / Custom", "Custom / Standard OSC Console", 10023, "Custom OSC Hardware Device (Port 10023)", "Custom")
)

@Composable
fun getBrandAccentColor(brandName: String): Color {
    return when {
        brandName.contains("Behringer", ignoreCase = true) -> NeonCyan
        brandName.contains("Midas", ignoreCase = true) -> NeonAmber
        brandName.contains("Allen", ignoreCase = true) -> NeonPurple
        brandName.contains("Yamaha", ignoreCase = true) -> NeonBlue
        brandName.contains("Soundcraft", ignoreCase = true) -> NeonRose
        else -> NeonEmerald
    }
}

@Composable
fun DiscoveryScreen(
    viewModel: IemViewModel,
    onBackToDashboard: () -> Unit,
    onNavigateToProfiles: () -> Unit
) {
    val discoveredMixers by viewModel.discoveredMixers.collectAsState()
    val currentConnection by viewModel.connectionState.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val hasCompletedScan by viewModel.hasCompletedScan.collectAsState()
    val scanProgressText by viewModel.scanProgressText.collectAsState()
    val autoScanOnOpen by viewModel.autoScanOnOpen.collectAsState()

    var selectedManufacturer by remember { mutableStateOf("Behringer") }
    var selectedMixerPreset by remember { mutableStateOf(SUPPORTED_MIXER_PRESETS.first { it.manufacturer == "Behringer" }) }
    var isModelListExpanded by remember { mutableStateOf(false) }

    var showManualDetails by remember { mutableStateOf(true) }

    val savedManualIp by viewModel.savedManualIp.collectAsState()
    var manualIp by remember(savedManualIp) { mutableStateOf(savedManualIp) }
    var userInitiatedConnect by remember { mutableStateOf(false) }

    val currentBrandColor = getBrandAccentColor(selectedManufacturer)

    val showManualFallback = hasCompletedScan && !isScanning && discoveredMixers.isEmpty()

    // Auto-scan on screen launch if option is enabled
    LaunchedEffect(Unit) {
        if (autoScanOnOpen && !isScanning) {
            viewModel.startNetworkScan()
        }
    }

    LaunchedEffect(currentConnection.status) {
        if (userInitiatedConnect) {
            if (currentConnection.status == ConnectionStatus.CONNECTED || currentConnection.status == ConnectionStatus.SIMULATION) {
                userInitiatedConnect = false
                onNavigateToProfiles()
            } else if (currentConnection.status == ConnectionStatus.DISCONNECTED) {
                userInitiatedConnect = false
            }
        }
    }

    // Animation for radar scanner sweep
    val infiniteTransition = rememberInfiniteTransition(label = "RadarScan")
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAngle"
    )
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadarPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToDashboard) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "CONNECT TO MIXER",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )

                    // Status Badge inline with CONNECT TO MIXER text
                    Surface(
                        color = when (currentConnection.status) {
                            ConnectionStatus.CONNECTED -> NeonEmerald.copy(alpha = 0.2f)
                            ConnectionStatus.SIMULATION -> NeonCyan.copy(alpha = 0.2f)
                            ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> NeonAmber.copy(alpha = 0.2f)
                            ConnectionStatus.DISCONNECTED -> DarkBorder.copy(alpha = 0.5f)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            when (currentConnection.status) {
                                ConnectionStatus.CONNECTED -> NeonEmerald
                                ConnectionStatus.SIMULATION -> NeonCyan
                                ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> NeonAmber
                                ConnectionStatus.DISCONNECTED -> DarkBorder
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (currentConnection.status) {
                                            ConnectionStatus.CONNECTED -> NeonEmerald
                                            ConnectionStatus.SIMULATION -> NeonCyan
                                            ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> NeonAmber
                                            ConnectionStatus.DISCONNECTED -> NeonRose
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = currentConnection.status.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Auto-scan network or configure manual console IP",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        HorizontalDivider(color = DarkBorder)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: Virtual Offline Console Quick Access (Moved to Top)
            item {
                Button(
                    onClick = {
                        userInitiatedConnect = true
                        viewModel.startSimulatorMode()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Computer,
                            contentDescription = "Virtual Offline Console",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "START VIRTUAL OFFLINE CONSOLE",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // SECTION 2: Automatic Network Scanner Card (Shown initially or while scanning or when mixers found)
            if (!showManualFallback) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isScanning) NeonCyan else DarkBorder)
                    ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        // Title Row with Icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isScanning) NeonCyan.copy(alpha = 0.2f) else DarkSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isScanning) Icons.Default.WifiTethering else Icons.Default.Router,
                                        contentDescription = "Scanner",
                                        tint = if (isScanning) NeonCyan else NeonAmber,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .then(if (isScanning) Modifier.scale(radarPulse) else Modifier)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "AUTOMATIC NETWORK SCANNER",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Auto-discovers consoles via UDP /xinfo broadcasts",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Radar Banner when scanning
                        if (isScanning) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = DarkSurfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(NeonCyan.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Radar,
                                            contentDescription = "Scanning",
                                            tint = NeonCyan,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .rotate(radarRotation)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "SCANNING LOCAL NETWORK...",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan
                                        )
                                        Text(
                                            text = scanProgressText,
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Action Buttons: Scan Now / Stop Scan
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (isScanning) {
                                        viewModel.cancelNetworkScan()
                                    } else {
                                        viewModel.startNetworkScan()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isScanning) NeonRose else NeonCyan
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isScanning) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "CANCEL SCAN",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = "Scan",
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "SCAN FOR MIXERS",
                                            color = Color.Black,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Switch Option: Automatically scan on screen open
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                                .clickable { viewModel.setAutoScanOnOpen(!autoScanOnOpen) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoMode,
                                    contentDescription = "Auto Scan",
                                    tint = if (autoScanOnOpen) NeonCyan else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Auto-scan network whenever opening screen",
                                    fontSize = 11.sp,
                                    color = TextPrimary
                                )
                            }
                            Switch(
                                checked = autoScanOnOpen,
                                onCheckedChange = { viewModel.setAutoScanOnOpen(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NeonCyan,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkSurface
                                ),
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }
                }
            }
        }

            // SECTION 3: Discovered Mixers List
            if (discoveredMixers.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AVAILABLE MIXERS ON NETWORK",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonEmerald
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = NeonEmerald.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${discoveredMixers.size} DETECTED",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonEmerald
                                )
                            }
                        }

                        Text(
                            text = "Tap to connect",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                items(discoveredMixers) { mixer ->
                    val isCurrent = (currentConnection.ip == mixer.ip && (currentConnection.status == ConnectionStatus.CONNECTED || currentConnection.status == ConnectionStatus.SIMULATION))
                    val brandColor = getBrandAccentColor(mixer.model)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                userInitiatedConnect = true
                                if (mixer.status == ConnectionStatus.SIMULATION) {
                                    viewModel.startSimulatorMode()
                                } else {
                                    viewModel.connectToMixer(mixer.ip, mixer.port)
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) DarkSurfaceVariant else DarkSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) NeonEmerald else brandColor.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(brandColor.copy(alpha = 0.15f))
                                        .border(1.dp, brandColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (mixer.status == ConnectionStatus.SIMULATION) Icons.Default.Computer else Icons.Default.Tune,
                                        contentDescription = "Mixer",
                                        tint = brandColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = mixer.model,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (mixer.name.isNotBlank()) {
                                        Text(
                                            text = mixer.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = brandColor
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${mixer.ip}:${mixer.port}",
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(text = " • ", fontSize = 10.sp, color = TextMuted)
                                        Text(
                                            text = mixer.firmware,
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                        Text(text = " • ", fontSize = 10.sp, color = TextMuted)
                                        Icon(
                                            Icons.Default.NetworkPing,
                                            contentDescription = "Latency",
                                            tint = NeonEmerald,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${mixer.latencyMs}ms",
                                            fontSize = 10.sp,
                                            color = NeonEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (isCurrent) {
                                Surface(
                                    color = NeonEmerald,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Connected",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "CONNECTED",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        userInitiatedConnect = true
                                        if (mixer.status == ConnectionStatus.SIMULATION) {
                                            viewModel.startSimulatorMode()
                                        } else {
                                            viewModel.connectToMixer(mixer.ip, mixer.port)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = "CONNECT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (brandColor == NeonCyan || brandColor == NeonAmber) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 4: Extended Manual Mixer Brand, Model & IP Selection (Replaces scanner when NO mixer found)
            if (showManualFallback) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, currentBrandColor.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Section Header with Rescan Action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(NeonAmber.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = "Manual Setup",
                                            tint = NeonAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "MANUAL IP & CONSOLE SELECTION",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "No mixers found on network — connect via static IP",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                // Rescan Network Button
                                Button(
                                    onClick = { viewModel.startNetworkScan() },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Rescan",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "RESCAN",
                                            color = NeonCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // STEP 1: SELECT MIXER BRAND (Always visible in collapsed section)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "1. SELECT MIXER BRAND",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = currentBrandColor
                                )
                                Text(
                                    text = "${BRAND_LIST.size} BRANDS AVAILABLE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Horizontal Brand Selection Row with Rich Cards (Always visible)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(BRAND_LIST) { brand ->
                                    val isSelected = selectedManufacturer == brand.name
                                    val brandColor = getBrandAccentColor(brand.name)
                                    Surface(
                                        modifier = Modifier
                                            .width(132.dp)
                                            .clickable {
                                                selectedManufacturer = brand.name
                                                val firstPreset = SUPPORTED_MIXER_PRESETS.firstOrNull { it.manufacturer == brand.name }
                                                    ?: SUPPORTED_MIXER_PRESETS.first()
                                                selectedMixerPreset = firstPreset
                                                isModelListExpanded = false
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) brandColor.copy(alpha = 0.15f) else DarkSurfaceVariant.copy(alpha = 0.7f),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) brandColor else DarkBorder
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = brand.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) brandColor else TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(brandColor)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = brand.subtitle,
                                                fontSize = 9.sp,
                                                color = if (isSelected) TextPrimary.copy(alpha = 0.8f) else TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Toggle Expand / Collapse Bar
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showManualDetails = !showManualDetails },
                                shape = RoundedCornerShape(8.dp),
                                color = if (showManualDetails) DarkSurfaceVariant.copy(alpha = 0.4f) else currentBrandColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, if (showManualDetails) DarkBorder else currentBrandColor.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (showManualDetails) Icons.Default.ExpandLess else Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = currentBrandColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (showManualDetails) "HIDE MODEL & IP CONFIG" else "CONFIG ${selectedManufacturer.uppercase()}: ${selectedMixerPreset.modelName}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = currentBrandColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = if (showManualDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = currentBrandColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Collapsible Details: Model Selection, IP & Port Input, Connect Button
                            AnimatedVisibility(visible = showManualDetails) {
                                Column(modifier = Modifier.padding(top = 16.dp)) {

                                    // STEP 2: SELECT SPECIFIC MODEL
                                    val availableModels = SUPPORTED_MIXER_PRESETS.filter { it.manufacturer == selectedManufacturer }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "2. CONSOLE MODEL ($selectedManufacturer)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = currentBrandColor
                                        )
                                        if (availableModels.size > 1) {
                                            TextButton(
                                                onClick = { isModelListExpanded = !isModelListExpanded },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text(
                                                    text = if (isModelListExpanded) "COLLAPSE" else "CHANGE (${availableModels.size} MODELS)",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isModelListExpanded) TextMuted else currentBrandColor
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Model Selection: Collapsed (Single Selected Model) vs Expanded (All Models)
                                    if (!isModelListExpanded) {
                                        // Display ONLY the selected model card
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = availableModels.size > 1) {
                                                    isModelListExpanded = true
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            color = currentBrandColor.copy(alpha = 0.12f),
                                            border = BorderStroke(
                                                width = 1.5.dp,
                                                color = currentBrandColor
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(currentBrandColor),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = if (currentBrandColor == NeonCyan || currentBrandColor == NeonAmber) Color.Black else Color.White,
                                                            modifier = Modifier.size(13.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = selectedMixerPreset.modelName,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary
                                                        )
                                                        Text(
                                                            text = selectedMixerPreset.description,
                                                            fontSize = 10.sp,
                                                            color = TextSecondary
                                                        )
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        color = currentBrandColor.copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(6.dp),
                                                        border = BorderStroke(1.dp, currentBrandColor.copy(alpha = 0.5f))
                                                    ) {
                                                        Text(
                                                            text = selectedMixerPreset.channelCount,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = currentBrandColor,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    if (availableModels.size > 1) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.SwapVert,
                                                            contentDescription = "Change model",
                                                            tint = currentBrandColor.copy(alpha = 0.7f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // Expanded List of Models - selecting any collapses back immediately
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            availableModels.forEach { preset ->
                                                val isSelected = selectedMixerPreset.id == preset.id
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedMixerPreset = preset
                                                            isModelListExpanded = false
                                                        },
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (isSelected) currentBrandColor.copy(alpha = 0.15f) else DarkSurfaceVariant.copy(alpha = 0.5f),
                                                    border = BorderStroke(
                                                        width = if (isSelected) 1.5.dp else 1.dp,
                                                        color = if (isSelected) currentBrandColor else DarkBorder
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            RadioButton(
                                                                selected = isSelected,
                                                                onClick = {
                                                                    selectedMixerPreset = preset
                                                                    isModelListExpanded = false
                                                                },
                                                                colors = RadioButtonDefaults.colors(
                                                                    selectedColor = currentBrandColor,
                                                                    unselectedColor = TextMuted
                                                                ),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                            Column {
                                                                Text(
                                                                    text = preset.modelName,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isSelected) TextPrimary else TextSecondary
                                                                )
                                                                Text(
                                                                    text = preset.description,
                                                                    fontSize = 10.sp,
                                                                    color = TextMuted
                                                                )
                                                            }
                                                        }

                                                        Surface(
                                                            color = if (isSelected) currentBrandColor.copy(alpha = 0.2f) else DarkBackground,
                                                            shape = RoundedCornerShape(6.dp),
                                                            border = BorderStroke(1.dp, if (isSelected) currentBrandColor.copy(alpha = 0.5f) else DarkBorder)
                                                        ) {
                                                            Text(
                                                                text = preset.channelCount,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isSelected) currentBrandColor else TextSecondary,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // STEP 3: CONSOLE IP ADDRESS
                                    Text(
                                        text = "3. CONSOLE IP ADDRESS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = currentBrandColor
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedTextField(
                                        value = manualIp,
                                        onValueChange = {
                                            manualIp = it
                                            viewModel.setManualIp(it)
                                        },
                                        label = { Text("IP Address", fontSize = 11.sp) },
                                        placeholder = { Text("192.168.1.100", fontSize = 11.sp, color = TextMuted) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = currentBrandColor,
                                            unfocusedBorderColor = DarkBorder,
                                            focusedLabelColor = currentBrandColor,
                                            unfocusedLabelColor = TextSecondary,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Quick IP Presets
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Presets:",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                        listOf("192.168.1.", "192.168.0.", "10.0.0.", "172.16.1.").forEach { prefix ->
                                            Surface(
                                                modifier = Modifier.clickable {
                                                    manualIp = "${prefix}100"
                                                    viewModel.setManualIp(manualIp)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                color = DarkSurfaceVariant,
                                                border = BorderStroke(1.dp, DarkBorder)
                                            ) {
                                                Text(
                                                    text = "${prefix}xxx",
                                                    fontSize = 10.sp,
                                                    color = NeonCyan,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    // Connect Action Button
                                    Button(
                                        onClick = {
                                            userInitiatedConnect = true
                                            viewModel.connectToMixer(manualIp, selectedMixerPreset.backendPort)
                                        },
                                        enabled = currentConnection.status != ConnectionStatus.CONNECTING && manualIp.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = currentBrandColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (currentConnection.status == ConnectionStatus.CONNECTING) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = if (currentBrandColor == NeonCyan || currentBrandColor == NeonAmber) Color.Black else Color.White,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "CONNECTING TO MIXER...",
                                                color = if (currentBrandColor == NeonCyan || currentBrandColor == NeonAmber) Color.Black else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Power,
                                                contentDescription = "Connect",
                                                tint = if (currentBrandColor == NeonCyan || currentBrandColor == NeonAmber) Color.Black else Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "CONNECT TO ${selectedManufacturer.uppercase()} (${selectedMixerPreset.channelCount})",
                                                color = if (currentBrandColor == NeonCyan || currentBrandColor == NeonAmber) Color.Black else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
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
    }
}
