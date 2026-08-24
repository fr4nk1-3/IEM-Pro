package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppThemeMode
import com.example.model.BusTapMode
import com.example.model.MixBusState
import com.example.model.X32Color
import com.example.ui.IemViewModel
import com.example.ui.components.HorizontalMeterBar
import com.example.ui.theme.*

@Composable
fun EngineerScreen(
    viewModel: IemViewModel,
    onBackToDashboard: () -> Unit
) {
    val userRole by viewModel.userRole.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val buses by viewModel.buses.collectAsState()
    val appThemeMode by viewModel.appThemeMode.collectAsState()
    val talkbackGain by viewModel.talkbackMicGain.collectAsState()
    val isTalkbackEngaged by viewModel.isTalkbackEngaged.collectAsState()

    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var activeTab by remember { mutableIntStateOf(1) } // Default to Mixbuses Tab

    // Channel Selection State
    var selectedChannelId by remember { mutableIntStateOf(1) }
    val selectedChannel = channels.firstOrNull { it.id == selectedChannelId } ?: channels.firstOrNull()

    var editChName by remember { mutableStateOf(selectedChannel?.name ?: "") }
    var editChColor by remember { mutableStateOf(selectedChannel?.color ?: X32Color.CYAN) }

    LaunchedEffect(selectedChannelId) {
        selectedChannel?.let {
            editChName = it.name
            editChColor = it.color
        }
    }

    // Mixbus Selection State
    var selectedBusId by remember { mutableIntStateOf(1) }
    val selectedBus = buses.firstOrNull { it.id == selectedBusId } ?: buses.firstOrNull()

    var editBusName by remember { mutableStateOf(selectedBus?.name ?: "") }
    var editBusColor by remember { mutableStateOf(selectedBus?.color ?: X32Color.CYAN) }

    LaunchedEffect(selectedBusId) {
        selectedBus?.let {
            editBusName = it.name
            editBusColor = it.color
        }
    }

    if (!userRole.isUnlocked) {
        // Engineer Authentication PIN Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .width(360.dp)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = NeonAmber,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "ENGINEER CONSOLE LOCK",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Enter local PIN to access input gain, phantom power +48V, mixbus limiters, EQ & routing.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            pinError = false
                        },
                        label = { Text("Enter Passcode") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError) {
                        Text(
                            text = "Incorrect Passcode!",
                            fontSize = 11.sp,
                            color = NeonRose,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onBackToDashboard) {
                            Text("Back", color = TextSecondary)
                        }
                        Button(
                            onClick = {
                                val success = viewModel.unlockEngineerMode(pinInput)
                                if (!success) {
                                    pinError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonAmber)
                        ) {
                            Text("Unlock", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        // Unlocked Engineer Console View with Adaptive Layout Scaling
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackToDashboard) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonAmber)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "CQ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ENGINEER CONSOLE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Button(
                    onClick = { viewModel.lockEngineerMode() },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock", tint = NeonAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lock Console", fontSize = 11.sp, color = TextPrimary)
                }
            }

            HorizontalDivider(color = DarkBorder)

            // Scrollable Tab Navigation Bar for Scaling across Small and Large screens
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = DarkSurface,
                contentColor = NeonCyan,
                edgePadding = 8.dp,
                divider = { HorizontalDivider(color = DarkBorder) }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CHANNELS (1-32)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MIXBUSES & EQ (1-16)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TALKBACK & ROUTING", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                )
            }

            // Main Console Container with Responsive Layout Scaling
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val screenWidth = this.maxWidth
                val isCompact = screenWidth < 600.dp

                when (activeTab) {
                    0 -> {
                        // CHANNELS TAB
                        if (isCompact) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkSurfaceVariant)
                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(channels) { ch ->
                                        val isSelected = ch.id == selectedChannelId
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedChannelId = ch.id },
                                            label = { Text("CH ${ch.id}: ${ch.name}", fontSize = 11.sp, maxLines = 1) },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(ch.color.composeColor)
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = NeonCyan,
                                                selectedLabelColor = Color.White,
                                                containerColor = DarkSurface,
                                                labelColor = TextPrimary
                                            )
                                        )
                                    }
                                }

                                if (selectedChannel != null) {
                                    ChannelDetailControls(
                                        channel = selectedChannel,
                                        viewModel = viewModel,
                                        editChName = editChName,
                                        onNameChange = { editChName = it },
                                        editChColor = editChColor,
                                        onColorChange = { editChColor = it },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxSize()) {
                                val leftWidth = (screenWidth * 0.25f).coerceIn(120.dp, 180.dp)
                                LazyColumn(
                                    modifier = Modifier
                                        .width(leftWidth)
                                        .fillMaxHeight()
                                        .background(DarkSurfaceVariant)
                                        .padding(vertical = 4.dp)
                                ) {
                                    items(channels) { ch ->
                                        val isSelected = ch.id == selectedChannelId
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isSelected) DarkSurface else Color.Transparent)
                                                .clickable { selectedChannelId = ch.id }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(ch.color.composeColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(
                                                    text = "CH ${ch.id.toString().padStart(2, '0')}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) NeonCyan else TextSecondary
                                                )
                                                Text(
                                                    text = ch.name,
                                                    fontSize = 12.sp,
                                                    color = TextPrimary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                                    }
                                }

                                if (selectedChannel != null) {
                                    ChannelDetailControls(
                                        channel = selectedChannel,
                                        viewModel = viewModel,
                                        editChName = editChName,
                                        onNameChange = { editChName = it },
                                        editChColor = editChColor,
                                        onColorChange = { editChColor = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // MIXBUSES & EQ TAB
                        if (isCompact) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkSurfaceVariant)
                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(buses) { bus ->
                                        val isSelected = bus.id == selectedBusId
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedBusId = bus.id },
                                            label = { Text("BUS ${bus.id}: ${bus.name}", fontSize = 11.sp, maxLines = 1) },
                                            leadingIcon = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(bus.color.composeColor)
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = NeonCyan,
                                                selectedLabelColor = Color.White,
                                                containerColor = DarkSurface,
                                                labelColor = TextPrimary
                                            )
                                        )
                                    }
                                }

                                if (selectedBus != null) {
                                    MixbusDetailControls(
                                        bus = selectedBus,
                                        viewModel = viewModel,
                                        editBusName = editBusName,
                                        onNameChange = { editBusName = it },
                                        editBusColor = editBusColor,
                                        onColorChange = { editBusColor = it },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxSize()) {
                                val leftWidth = (screenWidth * 0.25f).coerceIn(130.dp, 190.dp)
                                LazyColumn(
                                    modifier = Modifier
                                        .width(leftWidth)
                                        .fillMaxHeight()
                                        .background(DarkSurfaceVariant)
                                        .padding(vertical = 4.dp)
                                ) {
                                    items(buses) { bus ->
                                        val isSelected = bus.id == selectedBusId
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isSelected) DarkSurface else Color.Transparent)
                                                .clickable { selectedBusId = bus.id }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(bus.color.composeColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "BUS ${bus.id.toString().padStart(2, '0')}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) NeonCyan else TextSecondary
                                                    )
                                                    if (bus.isStereoLinked) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("ST", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = NeonEmerald)
                                                    }
                                                }
                                                Text(
                                                    text = bus.name,
                                                    fontSize = 11.sp,
                                                    color = TextPrimary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                                    }
                                }

                                if (selectedBus != null) {
                                    MixbusDetailControls(
                                        bus = selectedBus,
                                        viewModel = viewModel,
                                        editBusName = editBusName,
                                        onNameChange = { editBusName = it },
                                        editBusColor = editBusColor,
                                        onColorChange = { editBusColor = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // TALKBACK & MATRIX ROUTING TAB
                        TalkbackRoutingConsole(
                            buses = buses,
                            viewModel = viewModel,
                            talkbackGain = talkbackGain,
                            isTalkbackEngaged = isTalkbackEngaged,
                            appThemeMode = appThemeMode,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelDetailControls(
    channel: com.example.model.ChannelState,
    viewModel: IemViewModel,
    editChName: String,
    onNameChange: (String) -> Unit,
    editChColor: X32Color,
    onColorChange: (X32Color) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "CHANNEL ${channel.id}: ${channel.name}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Headamp Gain & Phantom Power
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("HEADAMP / INPUT GAIN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("+48V Phantom Power:", fontSize = 11.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = channel.phantomPower,
                            onCheckedChange = { viewModel.toggleEngineerPhantomPower(channel.id) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonRose)
                        )
                    }

                    Text(
                        text = "+${(channel.inputGain * 60).toInt()} dB",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = channel.inputGain,
                    onValueChange = { newGain ->
                        viewModel.updateEngineerChannelGain(channel.id, newGain)
                    },
                    valueRange = 0f..1f
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Channel Metadata
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("CHANNEL METADATA & COLOR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editChName,
                    onValueChange = onNameChange,
                    label = { Text("Channel Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Select Channel Color:", fontSize = 11.sp, color = TextSecondary)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(X32Color.entries.toTypedArray()) { xColor ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(xColor.composeColor)
                                .clickable { onColorChange(xColor) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (editChColor == xColor) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.updateChannelDetails(channel.id, editChName, editChColor, channel.iconType)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Channel", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MixbusDetailControls(
    bus: MixBusState,
    viewModel: IemViewModel,
    editBusName: String,
    onNameChange: (String) -> Unit,
    editBusColor: X32Color,
    onColorChange: (X32Color) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header for selected bus
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(bus.color.composeColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BUS ${bus.id}: ${bus.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Button(
                onClick = { viewModel.toggleBusStereoLink(bus.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (bus.isStereoLinked) NeonEmerald.copy(alpha = 0.2f) else DarkSurfaceVariant
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = if (bus.isStereoLinked) NeonEmerald else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (bus.isStereoLinked) "Stereo (Bus ${bus.id}+${bus.linkedBusId})" else "Link Stereo",
                    fontSize = 10.sp,
                    color = if (bus.isStereoLinked) NeonEmerald else TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Master Level & Mute Card with Live Audio Meter Output
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("BUS MASTER OUTPUT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                        Spacer(modifier = Modifier.width(6.dp))
                        val isClipping = !bus.masterMute && (bus.peakMeter >= 0.88f || bus.peakMeterL >= 0.88f || bus.peakMeterR >= 0.88f)
                        val hasSignal = !bus.masterMute && (bus.peakMeter >= 0.04f || bus.peakMeterL >= 0.04f || bus.peakMeterR >= 0.04f)
                        val sigColor = when {
                            isClipping -> NeonRose
                            hasSignal -> NeonEmerald
                            else -> Color(0xFF1E293B)
                        }
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(sigColor)
                                .border(0.5.dp, if (isClipping || hasSignal) sigColor else Color(0xFF334155), androidx.compose.foundation.shape.CircleShape)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bus.getPeakDbString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (bus.masterMute) Color(0xFF64748B) else if (bus.peakMeter >= 0.88f) NeonRose else TextMuted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (bus.masterMute) "MUTED" else bus.getMasterDbString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bus.masterMute) NeonRose else NeonCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Real-time Master Output Audio Meter Bar
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("L", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(12.dp))
                        HorizontalMeterBar(
                            level = bus.peakMeterL,
                            isMuted = bus.masterMute,
                            modifier = Modifier.weight(1f),
                            height = 6.dp
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("R", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(12.dp))
                        HorizontalMeterBar(
                            level = bus.peakMeterR,
                            isMuted = bus.masterMute,
                            modifier = Modifier.weight(1f),
                            height = 6.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.toggleMasterBusMute(bus.id - 1) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bus.masterMute) NeonRose else DarkSurfaceVariant
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (bus.masterMute) "MUTED" else "MUTE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Slider(
                        value = bus.masterLevel,
                        onValueChange = { newLvl ->
                            viewModel.updateMasterBusLevel(bus.id - 1, newLvl)
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Interactive 6-Band Parametric Bus EQ Console Card
        MixbusEqControlCard(bus = bus, viewModel = viewModel)

        Spacer(modifier = Modifier.height(12.dp))

        // IEM Ear Protection Limiter
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = if (bus.limiterActive) NeonRose else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "EAR-PROTECTION LIMITER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bus.limiterActive) NeonRose else TextSecondary
                        )
                    }

                    Switch(
                        checked = bus.limiterActive,
                        onCheckedChange = { viewModel.toggleBusLimiter(bus.id) },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonRose)
                    )
                }

                Text(
                    text = "Protects musician ears from feedback or transient spikes",
                    fontSize = 10.sp,
                    color = TextSecondary
                )

                if (bus.limiterActive) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Limiter Threshold:", fontSize = 10.sp, color = TextPrimary)
                        Text(
                            text = "${bus.limiterThresholdDb.toInt()} dBFS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonRose
                        )
                    }

                    Slider(
                        value = bus.limiterThresholdDb,
                        onValueChange = { newThresh ->
                            viewModel.updateBusLimiterThreshold(bus.id, newThresh)
                        },
                        valueRange = -24f..0f
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Time Alignment Delay & Output Phase Processing
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("TIME ALIGNMENT & PHASE INVERT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonEmerald)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = bus.phaseInverted,
                        onClick = { viewModel.toggleBusPhaseInvert(bus.id) },
                        label = { Text(if (bus.phaseInverted) "180° INVERTED" else "0° NORMAL", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonRose,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextPrimary
                        )
                    )

                    val distMeters = (bus.outputDelayMs * 0.343f)
                    Text(
                        text = String.format("%.1f ms (%.1f m)", bus.outputDelayMs, distMeters),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = bus.outputDelayMs,
                    onValueChange = { newDelay ->
                        viewModel.updateBusOutputDelay(bus.id, newDelay)
                    },
                    valueRange = 0f..50f
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bus Tap Point Config with LazyRow for scaling
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("BUS TAP POINT CONFIGURATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(BusTapMode.entries.toTypedArray()) { mode ->
                        val isSelected = bus.sendTapMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateBusTapMode(bus.id, mode) },
                            label = { Text(mode.label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.White,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextPrimary
                            )
                        )
                    }
                }

                Text(
                    text = bus.sendTapMode.description,
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bus Name & Palette Color
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("RENAME BUS & COLOR PALETTE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editBusName,
                    onValueChange = onNameChange,
                    label = { Text("Mixbus Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Bus Palette Color:", fontSize = 11.sp, color = TextSecondary)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(X32Color.entries.toTypedArray()) { xColor ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(xColor.composeColor)
                                .clickable { onColorChange(xColor) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (editBusColor == xColor) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.updateBusDetails(bus.id, editBusName, editBusColor)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Save Bus Settings", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MixbusEqControlCard(
    bus: MixBusState,
    viewModel: IemViewModel,
    modifier: Modifier = Modifier
) {
    var selectedBandIndex by remember { mutableIntStateOf(0) }
    val activeBand = bus.eqBands.getOrNull(selectedBandIndex) ?: bus.eqBands.firstOrNull()

    val cEmerald = NeonEmerald
    val cSecondary = TextSecondary
    val cAmber = NeonAmber
    val cPrimary = TextPrimary
    val cCyan = NeonCyan
    val bandColors = listOf(NeonCyan, NeonAmber, NeonEmerald, NeonPurple, NeonMagenta, NeonRose)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = if (bus.eqActive) cEmerald else cSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "6-BAND PARAMETRIC BUS EQ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bus.eqActive) cEmerald else cSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = { viewModel.resetBusEq(bus.id) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Reset Flat", fontSize = 10.sp, color = cAmber)
                    }

                    FilterChip(
                        selected = bus.eqActive,
                        onClick = { viewModel.toggleBusEq(bus.id) },
                        label = { Text(if (bus.eqActive) "EQ ON" else "BYPASS", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cEmerald,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceVariant,
                            labelColor = cPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Frequency Response Curve Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBackground)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    if (w <= 0f || h <= 0f) return@Canvas
                    val zeroY = h / 2f

                    // Gridlines (+15dB, 0dB, -15dB)
                    drawLine(Color(0xFF2A2E3D), Offset(0f, 0.15f * h), Offset(w, 0.15f * h), strokeWidth = 1f)
                    drawLine(Color(0xFF3A4050), Offset(0f, zeroY), Offset(w, zeroY), strokeWidth = 1.5f)
                    drawLine(Color(0xFF2A2E3D), Offset(0f, 0.85f * h), Offset(w, 0.85f * h), strokeWidth = 1f)

                    // Frequency Gridlines (100Hz, 1kHz, 10kHz)
                    listOf(100f, 1000f, 10000f).forEach { freq ->
                        val logFrac = (Math.log10(freq.toDouble() / 20.0) / Math.log10(20000.0 / 20.0)).toFloat()
                        val x = logFrac * w
                        drawLine(Color(0xFF2A2E3D), Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                    }

                    if (bus.eqActive && bus.eqBands.isNotEmpty()) {
                        val path = Path()
                        val pointsCount = 100
                        for (i in 0..pointsCount) {
                            val frac = i / pointsCount.toFloat()
                            val freq = 20f * Math.pow(1000.0, frac.toDouble()).toFloat()

                            var totalGainDb = 0f
                            bus.eqBands.forEach { b ->
                                val fRatio = freq / b.freqHz.coerceAtLeast(10f)
                                val q = b.qFactor.coerceAtLeast(0.1f)
                                val gainResponse = b.gainDb / (1f + q * q * (fRatio - 1f / fRatio) * (fRatio - 1f / fRatio))
                                totalGainDb += gainResponse
                            }

                            val y = zeroY - (totalGainDb / 15f) * (h * 0.35f)
                            val x = frac * w

                            if (i == 0) path.moveTo(x, y.coerceIn(0f, h))
                            else path.lineTo(x, y.coerceIn(0f, h))
                        }

                        drawPath(
                            path = path,
                            color = cEmerald,
                            style = Stroke(width = 3f)
                        )

                        // Draw band marker dots
                        bus.eqBands.forEachIndexed { idx, band ->
                            val logFrac = (Math.log10(band.freqHz.toDouble() / 20.0) / Math.log10(20000.0 / 20.0)).toFloat()
                            val dotX = logFrac * w
                            val dotY = zeroY - (band.gainDb / 15f) * (h * 0.35f)
                            val color = bandColors.getOrElse(idx) { cCyan }
                            val isSel = idx == selectedBandIndex

                            drawCircle(
                                color = if (isSel) Color.White else color,
                                radius = if (isSel) 9f else 5f,
                                center = Offset(dotX.coerceIn(8f, w - 8f), dotY.coerceIn(8f, h - 8f))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6-Band Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(bus.eqBands) { idx, band ->
                    val isSel = idx == selectedBandIndex
                    val bColor = bandColors.getOrElse(idx) { cCyan }
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedBandIndex = idx },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(bColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "B${band.id} ${if (band.gainDb >= 0) "+${band.gainDb.toInt()}" else "${band.gainDb.toInt()}"}dB",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = bColor,
                            selectedLabelColor = Color.Black,
                            containerColor = DarkSurfaceVariant,
                            labelColor = cPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Active Band Fine Parameter Sliders
            if (activeBand != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BAND ${activeBand.id}: ${activeBand.label}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = bandColors.getOrElse(selectedBandIndex) { cCyan }
                            )

                            Text(
                                text = "${String.format("%.1f", activeBand.gainDb)} dB @ ${if (activeBand.freqHz >= 1000) String.format("%.1fkHz", activeBand.freqHz / 1000f) else "${activeBand.freqHz.toInt()}Hz"} (Q=${String.format("%.1f", activeBand.qFactor)})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = cPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Gain Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Gain:", fontSize = 10.sp, color = cSecondary, modifier = Modifier.width(40.dp))
                            Slider(
                                value = activeBand.gainDb,
                                onValueChange = { newG ->
                                    viewModel.updateBusEqBandGain(bus.id, selectedBandIndex, newG)
                                },
                                valueRange = -15f..15f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Frequency Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Freq:", fontSize = 10.sp, color = cSecondary, modifier = Modifier.width(40.dp))
                            val logVal = (Math.log10(activeBand.freqHz.toDouble() / 20.0) / Math.log10(20000.0 / 20.0)).toFloat()
                            Slider(
                                value = logVal,
                                onValueChange = { frac ->
                                    val newF = (20f * Math.pow(1000.0, frac.toDouble())).toFloat()
                                    viewModel.updateBusEqBandFreq(bus.id, selectedBandIndex, newF)
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Q-Factor Slider
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Q-Fact:", fontSize = 10.sp, color = cSecondary, modifier = Modifier.width(40.dp))
                            Slider(
                                value = activeBand.qFactor,
                                onValueChange = { newQ ->
                                    viewModel.updateBusEqBandQ(bus.id, selectedBandIndex, newQ)
                                },
                                valueRange = 0.3f..8f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Graphic Band Gain Sliders
            Text("GRAPHIC BAND GAIN FADERS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = cSecondary)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                bus.eqBands.forEachIndexed { idx, band ->
                    val bColor = bandColors.getOrElse(idx) { cCyan }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (band.gainDb >= 0) "+${band.gainDb.toInt()}" else "${band.gainDb.toInt()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (band.gainDb != 0f) bColor else cSecondary
                        )

                        Slider(
                            value = band.gainDb,
                            onValueChange = { g ->
                                viewModel.updateBusEqBandGain(bus.id, idx, g)
                            },
                            valueRange = -15f..15f,
                            modifier = Modifier.height(70.dp)
                        )

                        Text(
                            text = "B${idx + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = bColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TalkbackRoutingConsole(
    buses: List<MixBusState>,
    viewModel: IemViewModel,
    talkbackGain: Float,
    isTalkbackEngaged: Boolean,
    appThemeMode: AppThemeMode,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "ENGINEER TALKBACK & MATRIX ROUTING",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Talkback Mic Control Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = if (isTalkbackEngaged) NeonRose else NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("ENGINEER TALKBACK MIC", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text = if (isTalkbackEngaged) "LIVE ON AIR" else "STANDBY / MUTED",
                                fontSize = 10.sp,
                                color = if (isTalkbackEngaged) NeonRose else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.toggleTalkbackEngaged() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTalkbackEngaged) NeonRose else NeonCyan
                        )
                    ) {
                        Text(
                            text = if (isTalkbackEngaged) "TALKBACK LIVE" else "ENGAGE TALK",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Talkback Mic Gain:", fontSize = 11.sp, color = TextPrimary)
                    Text("${(talkbackGain * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                }

                Slider(
                    value = talkbackGain,
                    onValueChange = { viewModel.setTalkbackMicGain(it) },
                    valueRange = 0f..1f
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Talkback Bus Matrix Routing Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TALKBACK DESTINATION MATRIX", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                    TextButton(
                        onClick = {
                            val allActive = buses.all { it.talkbackActive }
                            buses.forEach { bus ->
                                if (bus.talkbackActive == allActive) {
                                    viewModel.toggleBusTalkback(bus.id)
                                }
                            }
                        }
                    ) {
                        Text("TOGGLE ALL", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier.height(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(buses) { bus ->
                        val isRouted = bus.talkbackActive
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleBusTalkback(bus.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRouted) NeonAmber.copy(alpha = 0.2f) else DarkSurfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(bus.color.composeColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("BUS ${bus.id}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Text(bus.name, fontSize = 11.sp, color = TextPrimary, maxLines = 1)
                                }
                                Icon(
                                    imageVector = if (isRouted) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = null,
                                    tint = if (isRouted) NeonAmber else TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // App Theme Switcher Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("UI DISPLAY THEME", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonEmerald)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Switch visual style between Light Studio and Glassmorphism", fontSize = 10.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AppThemeMode.entries.toTypedArray()) { mode ->
                        val isSelected = appThemeMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setAppThemeMode(mode) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (mode) {
                                            AppThemeMode.CQ_MIXPAD -> Icons.Default.Equalizer
                                            AppThemeMode.LIGHT -> Icons.Default.LightMode
                                            AppThemeMode.GLASSMORPHISM -> Icons.Default.AutoAwesome
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(mode.displayName, fontSize = 11.sp)
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextPrimary,
                                iconColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}
