package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.model.MixerModelInfo
import com.example.ui.IemViewModel
import com.example.ui.theme.*

data class MixerPreset(
    val id: String,
    val manufacturer: String,
    val modelName: String,
    val backendPort: Int,
    val description: String
)

val MANUFACTURERS = listOf(
    "Behringer",
    "Midas",
    "Allen & Heath",
    "Soundcraft",
    "Yamaha",
    "Generic / Custom"
)

val SUPPORTED_MIXER_PRESETS = listOf(
    // Behringer
    MixerPreset("wing", "Behringer", "WING Series (WING / Compact / Rack)", 10023, "48-Ch Flagship Console (OSC Port 10023 / 2222)"),
    MixerPreset("x32", "Behringer", "X32 Series (X32 / Compact / Producer / Rack)", 10023, "32-Ch Industry Standard Console (OSC Port 10023)"),
    MixerPreset("xair", "Behringer", "X-Air Series (XR18 / XR16 / XR12)", 8900, "18/16/12-Ch Stagebox Mixer (OSC Port 8900)"),

    // Midas
    MixerPreset("m32", "Midas", "M32 Series (M32 / M32R / M32C)", 10023, "32-Ch Premium Digital Console (OSC Port 10023)"),
    MixerPreset("mr18", "Midas", "MR Series (MR18 / MR12)", 8900, "18/12-Ch Stagebox Mixer (OSC Port 8900)"),

    // Allen & Heath
    MixerPreset("sq", "Allen & Heath", "SQ Series (SQ5 / SQ6 / SQ7)", 51325, "48-Ch Digital Console (OSC Port 51325)"),
    MixerPreset("qu", "Allen & Heath", "Qu Series (Qu16 / Qu24 / Qu32 / Qu-PAC)", 51326, "Digital Mixer (OSC Port 51326)"),
    MixerPreset("dlive", "Allen & Heath", "Avantis / dLive Series", 51325, "Flagship Audio Console (OSC Port 51325)"),

    // Soundcraft
    MixerPreset("soundcraft", "Soundcraft", "Ui Series (Ui24R / Ui16 / Ui12)", 80, "Web/OSC Rack Mixer (Port 80)"),

    // Yamaha
    MixerPreset("yamaha_tf", "Yamaha", "TF Series (TF1 / TF3 / TF5 / TF-Rack)", 49280, "Digital Console (OSC Port 49280)"),
    MixerPreset("yamaha_clql", "Yamaha", "CL / QL Series (CL1 / CL5 / QL1 / QL5)", 49280, "Commercial Console (OSC Port 49280)"),
    MixerPreset("yamaha_dm3", "Yamaha", "DM3 Series", 49280, "Compact Digital Console (OSC Port 49280)"),

    // Generic
    MixerPreset("custom", "Generic / Custom", "Custom / Standard OSC Console", 10023, "Custom OSC Hardware Device (Port 10023)")
)

@Composable
fun DiscoveryScreen(
    viewModel: IemViewModel,
    onBackToDashboard: () -> Unit,
    onNavigateToProfiles: () -> Unit
) {
    val discoveredMixers by viewModel.discoveredMixers.collectAsState()
    val currentConnection by viewModel.connectionState.collectAsState()

    var selectedManufacturer by remember { mutableStateOf("Behringer") }
    var selectedMixerPreset by remember { mutableStateOf(SUPPORTED_MIXER_PRESETS.first { it.manufacturer == "Behringer" }) }

    var isManufacturerDropdownExpanded by remember { mutableStateOf(false) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }
    var manualIp by remember { mutableStateOf("192.168.1.100") }
    var userInitiatedConnect by remember { mutableStateOf(false) }

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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToDashboard) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SELECT MIXER & CONNECTION",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
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
            // Section 1: Virtual Offline Console Button
            item {
                Button(
                    onClick = {
                        userInitiatedConnect = true
                        viewModel.startSimulatorMode()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Computer,
                            contentDescription = "Virtual Offline Console",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "VIRTUAL OFFLINE CONSOLE",
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Section 2: Mixer Model Selection & IP Entry Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "1. SELECT MANUFACTURER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Manufacturer Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedManufacturer,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Manufacturer / Brand") },
                                leadingIcon = {
                                    Icon(Icons.Default.Business, contentDescription = null, tint = NeonCyan)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isManufacturerDropdownExpanded = !isManufacturerDropdownExpanded }) {
                                        Icon(
                                            imageVector = if (isManufacturerDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Manufacturer",
                                            tint = NeonCyan
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isManufacturerDropdownExpanded = !isManufacturerDropdownExpanded },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedLabelColor = NeonCyan
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { isManufacturerDropdownExpanded = !isManufacturerDropdownExpanded }
                            )

                            DropdownMenu(
                                expanded = isManufacturerDropdownExpanded,
                                onDismissRequest = { isManufacturerDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(DarkSurfaceVariant)
                            ) {
                                MANUFACTURERS.forEach { brand ->
                                    val isSelected = selectedManufacturer == brand
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = brand,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) NeonCyan else TextPrimary
                                            )
                                        },
                                        onClick = {
                                            selectedManufacturer = brand
                                            val firstPreset = SUPPORTED_MIXER_PRESETS.firstOrNull { it.manufacturer == brand }
                                                ?: SUPPORTED_MIXER_PRESETS.first()
                                            selectedMixerPreset = firstPreset
                                            isManufacturerDropdownExpanded = false
                                        },
                                        modifier = Modifier.background(
                                            if (isSelected) DarkBorder.copy(alpha = 0.5f) else Color.Transparent
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "2. SELECT MIXER MODEL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Mixer Model Dropdown
                        val availableModels = SUPPORTED_MIXER_PRESETS.filter { it.manufacturer == selectedManufacturer }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedMixerPreset.modelName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mixer Model ($selectedManufacturer)") },
                                leadingIcon = {
                                    Icon(Icons.Default.Tune, contentDescription = null, tint = NeonCyan)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isModelDropdownExpanded = !isModelDropdownExpanded }) {
                                        Icon(
                                            imageVector = if (isModelDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Mixer Model",
                                            tint = NeonCyan
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isModelDropdownExpanded = !isModelDropdownExpanded },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedLabelColor = NeonCyan
                                )
                            )

                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { isModelDropdownExpanded = !isModelDropdownExpanded }
                            )

                            DropdownMenu(
                                expanded = isModelDropdownExpanded,
                                onDismissRequest = { isModelDropdownExpanded = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(DarkSurfaceVariant)
                            ) {
                                availableModels.forEach { preset ->
                                    val isSelected = selectedMixerPreset.id == preset.id
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = preset.modelName,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) NeonCyan else TextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = preset.description,
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedMixerPreset = preset
                                            isModelDropdownExpanded = false
                                        },
                                        modifier = Modifier.background(
                                            if (isSelected) DarkBorder.copy(alpha = 0.5f) else Color.Transparent
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "3. ENTER MIXER IP ADDRESS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonAmber
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = manualIp,
                            onValueChange = { manualIp = it },
                            label = { Text("IP Address") },
                            placeholder = { Text("192.168.1.100") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Router, contentDescription = null, tint = NeonAmber)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonAmber,
                                unfocusedBorderColor = DarkBorder,
                                focusedLabelColor = NeonAmber
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (currentConnection.status == ConnectionStatus.DISCONNECTED && currentConnection.model == "NO MIXER FOUND") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = NeonRose.copy(alpha = 0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonRose)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = "Error",
                                        tint = NeonRose,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "MIXER NOT FOUND",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonRose
                                        )
                                        Text(
                                            text = "No hardware response from ${currentConnection.ip}:${currentConnection.port}. Check IP address and Wi-Fi connection.",
                                            fontSize = 11.sp,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                userInitiatedConnect = true
                                viewModel.connectToMixer(manualIp, selectedMixerPreset.backendPort)
                            },
                            enabled = currentConnection.status != ConnectionStatus.CONNECTING,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (currentConnection.status == ConnectionStatus.CONNECTING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SEARCHING FOR MIXER...",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            } else {
                                Text(
                                    text = "CONNECT TO ${selectedMixerPreset.modelName.uppercase()}",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Discovered Mixers List (If available on local network)
            if (discoveredMixers.isNotEmpty()) {
                item {
                    Text(
                        text = "DISCOVERED MIXERS ON LOCAL NETWORK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }

                items(discoveredMixers) { mixer ->
                    val isCurrent = currentConnection.ip == mixer.ip && currentConnection.status == ConnectionStatus.CONNECTED
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
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Router,
                                    contentDescription = "Mixer",
                                    tint = if (isCurrent) NeonEmerald else NeonCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(mixer.model, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("${mixer.ip} • Firmware ${mixer.firmware}", fontSize = 11.sp, color = TextSecondary)
                                }
                            }

                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(NeonEmerald)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("CONNECTED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        userInitiatedConnect = true
                                        if (mixer.status == ConnectionStatus.SIMULATION) {
                                            viewModel.startSimulatorMode()
                                        } else {
                                            viewModel.connectToMixer(mixer.ip, mixer.port)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("CONNECT", fontSize = 11.sp, color = NeonCyan)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

