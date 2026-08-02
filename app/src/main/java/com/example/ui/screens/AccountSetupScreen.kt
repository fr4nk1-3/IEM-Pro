package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProfileEntity
import com.example.model.ConnectionStatus
import com.example.ui.IemViewModel
import com.example.ui.components.getInstrumentIcon
import com.example.ui.theme.*

@Composable
fun AccountSetupScreen(
    viewModel: IemViewModel,
    onProceedToMixbusSelection: () -> Unit,
    onBackToDiscovery: () -> Unit
) {
    val connectionInfo by viewModel.connectionState.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()

    var selectedProfileId by remember(activeProfile) { mutableStateOf(activeProfile?.id ?: "") }
    var isCreatingNewAccount by remember { mutableStateOf(profiles.isEmpty()) }

    var newAccountName by remember { mutableStateOf("") }
    var selectedInstrumentIcon by remember { mutableStateOf("MIC") }
    var selectedAccentTheme by remember { mutableStateOf("CYAN") }

    val instrumentIcons = listOf("MIC", "DRUM", "GUITAR", "BASS", "KEYBOARD", "HORN")
    val accentThemes = listOf("CYAN", "AMBER", "EMERALD", "MAGENTA", "VIOLET")

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToDiscovery) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Console Connection",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "STEP 2: LOCAL MUSICIAN ACCOUNT",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                    Text(
                        text = "Connected to ${connectionInfo.model} (${connectionInfo.ip})",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            Surface(
                color = if (connectionInfo.status == ConnectionStatus.CONNECTED) NeonEmerald.copy(alpha = 0.2f) else NeonAmber.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (connectionInfo.status == ConnectionStatus.CONNECTED) NeonEmerald else NeonAmber)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (connectionInfo.status == ConnectionStatus.CONNECTED) NeonEmerald else NeonAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (connectionInfo.status == ConnectionStatus.CONNECTED) "ONLINE" else "SIMULATION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (connectionInfo.status == ConnectionStatus.CONNECTED) NeonEmerald else NeonAmber
                    )
                }
            }
        }

        HorizontalDivider(color = DarkBorder)

        // Main Content Area
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Explanatory Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Account Setup",
                        tint = NeonCyan,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "LOCAL ACCOUNT PROFILE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Set up your local musician account to save your channel favorites, submix groups, and monitor settings locally on this device.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Choose or Create Toggle
            if (profiles.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isCreatingNewAccount,
                        onClick = { isCreatingNewAccount = false },
                        label = { Text("SELECT EXISTING ACCOUNT", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurface,
                            labelColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isCreatingNewAccount,
                        onClick = { isCreatingNewAccount = true },
                        label = { Text("+ CREATE NEW ACCOUNT", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurface,
                            labelColor = TextPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!isCreatingNewAccount && profiles.isNotEmpty()) {
                Text(
                    text = "SELECT YOUR MUSICIAN ACCOUNT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                profiles.forEach { profile ->
                    val isSelected = profile.id == selectedProfileId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable {
                                selectedProfileId = profile.id
                                viewModel.selectProfile(profile)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) NeonCyan.copy(alpha = 0.15f) else DarkSurface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 0.5.dp,
                            color = if (isSelected) NeonCyan else DarkBorder
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = if (isSelected) NeonCyan else DarkSurfaceVariant,
                                    shape = CircleShape,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = getInstrumentIcon(profile.instrumentIcon),
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else NeonCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = profile.profileName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) NeonCyan else TextPrimary
                                    )
                                    Text(
                                        text = "Assigned MixBus: Bus ${profile.assignedBusId}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedProfileId = profile.id
                                    viewModel.selectProfile(profile)
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = NeonCyan)
                            )
                        }
                    }
                }
            } else {
                // Create New Account Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "CREATE LOCAL MUSICIAN ACCOUNT",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = newAccountName,
                            onValueChange = { newAccountName = it },
                            label = { Text("Musician / Role Name") },
                            placeholder = { Text("e.g. Lead Vocalist, Alex - Drums") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedLabelColor = NeonCyan
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "INSTRUMENT / ROLE ICON",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(instrumentIcons) { iconName ->
                                val isSelected = selectedInstrumentIcon == iconName
                                Surface(
                                    onClick = { selectedInstrumentIcon = iconName },
                                    color = if (isSelected) NeonCyan else DarkSurfaceVariant,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isSelected) NeonCyan else DarkBorder),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = getInstrumentIcon(iconName),
                                            contentDescription = iconName,
                                            tint = if (isSelected) Color.White else TextPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "PREFERRED ACCENT COLOR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(accentThemes) { theme ->
                                val isSelected = selectedAccentTheme == theme
                                val chipColor = when (theme) {
                                    "CYAN" -> NeonCyan
                                    "AMBER" -> NeonAmber
                                    "EMERALD" -> NeonEmerald
                                    "MAGENTA" -> NeonMagenta
                                    else -> NeonPurple
                                }
                                Surface(
                                    onClick = { selectedAccentTheme = theme },
                                    color = chipColor.copy(alpha = if (isSelected) 0.3f else 0.1f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, chipColor)
                                ) {
                                    Text(
                                        text = theme,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = chipColor,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = DarkBorder)

        // Bottom Action Bar
        Surface(
            color = DarkSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ACCOUNT STATUS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = if (isCreatingNewAccount) {
                            newAccountName.ifBlank { "New Musician Account" }
                        } else {
                            activeProfile?.profileName ?: "Musician Account"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Button(
                    onClick = {
                        if (isCreatingNewAccount) {
                            val accountName = newAccountName.ifBlank { "Musician" }
                            val defaultBus = activeProfile?.assignedBusId ?: 1
                            viewModel.createProfile(
                                name = accountName,
                                busId = defaultBus,
                                instrumentIcon = selectedInstrumentIcon,
                                accentTheme = selectedAccentTheme
                            )
                        } else {
                            val currentSelected = profiles.find { it.id == selectedProfileId }
                            if (currentSelected != null) {
                                viewModel.selectProfile(currentSelected)
                            }
                        }
                        onProceedToMixbusSelection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PROCEED TO MIXBUS SELECTION",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
