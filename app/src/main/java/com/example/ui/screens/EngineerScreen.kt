package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.X32Color
import com.example.ui.IemViewModel
import com.example.ui.theme.*

@Composable
fun EngineerScreen(
    viewModel: IemViewModel,
    onBackToDashboard: () -> Unit
) {
    val userRole by viewModel.userRole.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val buses by viewModel.buses.collectAsState()

    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var selectedChannelId by remember { mutableIntStateOf(1) }
    val selectedChannel = channels.firstOrNull { it.id == selectedChannelId } ?: channels.firstOrNull()

    var editName by remember { mutableStateOf(selectedChannel?.name ?: "") }
    var editColor by remember { mutableStateOf(selectedChannel?.color ?: X32Color.CYAN) }
    var editIcon by remember { mutableStateOf(selectedChannel?.iconType ?: "MIC") }

    LaunchedEffect(selectedChannelId) {
        selectedChannel?.let {
            editName = it.name
            editColor = it.color
            editIcon = it.iconType
        }
    }

    if (!userRole.isUnlocked) {
        // Locked Screen Dialog
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .width(320.dp)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = NeonAmber,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Engineer Mode Lock",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter local PIN to access input gain, phantom power +48V, channel processing & bus routing.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            pinError = false
                        },
                        label = { Text("PIN Code (Default: 1234)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (pinError) {
                        Text(
                            text = "Incorrect PIN! Default is 1234",
                            fontSize = 11.sp,
                            color = NeonRose,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

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
                            Text("Unlock", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        // Unlocked Engineer Console View
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
                        text = "ENGINEER CONSOLE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonAmber
                    )
                }

                Button(
                    onClick = { viewModel.lockEngineerMode() },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Lock", tint = NeonAmber, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Lock Console", fontSize = 12.sp, color = TextPrimary)
                }
            }

            HorizontalDivider(color = DarkBorder)

            // Split View Layout
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Panel: Channel Selector List
                LazyColumn(
                    modifier = Modifier
                        .width(140.dp)
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
                                .padding(horizontal = 10.dp, vertical = 10.dp),
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

                // Right Panel: Channel Processing & Edit Controls
                if (selectedChannel != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "CHANNEL ${selectedChannel.id}: ${selectedChannel.name}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Hardware Input Processing (+48V Phantom Power & Gain)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("HEADAMP / INPUT GAIN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonAmber)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("+48V Phantom Power:", fontSize = 12.sp, color = TextPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = selectedChannel.phantomPower,
                                            onCheckedChange = { viewModel.toggleEngineerPhantomPower(selectedChannel.id) },
                                            colors = SwitchDefaults.colors(checkedThumbColor = NeonRose)
                                        )
                                    }

                                    Text(
                                        text = "+${(selectedChannel.inputGain * 60).toInt()} dB",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Slider(
                                    value = selectedChannel.inputGain,
                                    onValueChange = { newGain ->
                                        viewModel.updateEngineerChannelGain(selectedChannel.id, newGain)
                                    },
                                    valueRange = 0f..1f
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Channel Metadata Override (Name, Color Palette, Icon)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("CHANNEL METADATA & COLOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("Channel Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Select Channel Color:", fontSize = 11.sp, color = TextSecondary)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    X32Color.entries.forEach { xColor ->
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(xColor.composeColor)
                                                .clickable { editColor = xColor }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        viewModel.updateChannelDetails(selectedChannel.id, editName, editColor, editIcon)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Save Channel", color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
