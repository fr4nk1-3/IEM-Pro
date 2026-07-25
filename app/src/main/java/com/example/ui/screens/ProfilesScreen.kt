package com.example.ui.screens

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
import com.example.data.UserProfileEntity
import com.example.model.ConnectionStatus
import com.example.ui.IemViewModel
import com.example.ui.components.getInstrumentIcon
import com.example.ui.theme.*

@Composable
fun ProfilesScreen(
    viewModel: IemViewModel,
    onBackToDashboard: () -> Unit,
    onOpenDiscovery: () -> Unit = {}
) {
    val connectionInfo by viewModel.connectionState.collectAsState()
    val isConnected = connectionInfo.status == ConnectionStatus.CONNECTED || connectionInfo.status == ConnectionStatus.SIMULATION

    val profiles by viewModel.profiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val activeBusIndex by viewModel.activeBusIndex.collectAsState()
    val buses by viewModel.buses.collectAsState()
    val currentPresets by viewModel.currentPresets.collectAsState()
    val customGroups by viewModel.customGroups.collectAsState()
    val channels by viewModel.channels.collectAsState()

    var newPresetName by remember { mutableStateOf("") }
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<UserProfileEntity?>(null) }
    var newProfileName by remember { mutableStateOf("") }
    var selectedBusId by remember { mutableStateOf(1) }
    var selectedIcon by remember { mutableStateOf("MIC") }
    var groupsEnabledInDialog by remember { mutableStateOf(true) }
    var showBusDropdownInDialog by remember { mutableStateOf(false) }

    var showGroupDialog by remember { mutableStateOf(false) }
    var editingGroupName by remember { mutableStateOf<String?>(null) }
    var groupNameInput by remember { mutableStateOf("") }
    var selectedGroupChannelIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    var isProfilesExpanded by remember { mutableStateOf(false) }
    var isMixBusExpanded by remember { mutableStateOf(false) }
    var isGroupsExpanded by remember { mutableStateOf(false) }
    var isPresetsExpanded by remember { mutableStateOf(false) }

    fun toggleProfilesSection() {
        val next = !isProfilesExpanded
        isProfilesExpanded = next
        if (next) {
            isMixBusExpanded = false
            isGroupsExpanded = false
            isPresetsExpanded = false
        }
    }

    fun toggleMixBusSection() {
        val next = !isMixBusExpanded
        isMixBusExpanded = next
        if (next) {
            isProfilesExpanded = false
            isGroupsExpanded = false
            isPresetsExpanded = false
        }
    }

    fun toggleGroupsSection() {
        val next = !isGroupsExpanded
        isGroupsExpanded = next
        if (next) {
            isProfilesExpanded = false
            isMixBusExpanded = false
            isPresetsExpanded = false
        }
    }

    fun togglePresetsSection() {
        val next = !isPresetsExpanded
        isPresetsExpanded = next
        if (next) {
            isProfilesExpanded = false
            isMixBusExpanded = false
            isGroupsExpanded = false
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
                    text = "LOCAL MUSICIAN PROFILES",
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Connection Guard Banner if Mixer Not Connected
            if (!isConnected) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NeonRose.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, NeonRose),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Connection Warning",
                                    tint = NeonRose,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "MIXER CONNECTION REQUIRED",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonRose
                                    )
                                    Text(
                                        text = "Current Status: ${connectionInfo.status.name}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "A live mixer or Virtual Offline Console connection must be established before setting up or modifying musician profiles and channel assignments.",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onOpenDiscovery,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Icon(Icons.Default.Router, contentDescription = "Connect Mixer", tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CONNECT TO MIXER BEFORE SETUP",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
            // Section 1: Musician Profiles Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggleProfilesSection() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Profiles",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "MUSICIAN PROFILES",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                    if (!isProfilesExpanded) {
                                        Text(
                                            text = "Active: ${activeProfile?.profileName ?: "None"} (Bus ${activeProfile?.assignedBusId ?: 1}) • ${profiles.size} profiles",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        editingProfile = null
                                        newProfileName = ""
                                        selectedBusId = (activeBusIndex + 1).coerceIn(1, 16)
                                        selectedIcon = "MIC"
                                        groupsEnabledInDialog = true
                                        showAddProfileDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Profile",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "NEW PROFILE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { toggleProfilesSection() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isProfilesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isProfilesExpanded) "Collapse" else "Expand",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (isProfilesExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                profiles.forEach { prof ->
                                    val isSelected = activeProfile?.id == prof.id
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) DarkSurfaceVariant else DarkBackground)
                                            .border(1.dp, if (isSelected) NeonCyan else DarkBorder, RoundedCornerShape(10.dp))
                                            .clickable {
                                                viewModel.selectProfile(prof)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
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
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSelected) NeonCyan else DarkSurfaceVariant),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = getInstrumentIcon(prof.instrumentIcon),
                                                        contentDescription = prof.instrumentIcon,
                                                        tint = if (isSelected) Color.Black else TextPrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(prof.profileName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                    Text("Assigned to Bus ${prof.assignedBusId} • Groups: ${if (prof.groupsEnabled) "ON" else "OFF"}", fontSize = 11.sp, color = NeonCyan)
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(NeonCyan)
                                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        editingProfile = prof
                                                        newProfileName = prof.profileName
                                                        selectedBusId = prof.assignedBusId
                                                        selectedIcon = prof.instrumentIcon
                                                        groupsEnabledInDialog = prof.groupsEnabled
                                                        showAddProfileDialog = true
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        contentDescription = "Edit Profile",
                                                        tint = NeonCyan,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                if (profiles.size > 1) {
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    IconButton(
                                                        onClick = { viewModel.deleteProfile(prof) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = "Delete Profile",
                                                            tint = NeonRose.copy(alpha = 0.8f),
                                                            modifier = Modifier.size(16.dp)
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

            // Section 2: Select MixBus for Active Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggleMixBusSection() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = "Mix Bus",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "SELECT MONITOR MIX BUS (${activeProfile?.profileName ?: "Musician"})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                    if (!isMixBusExpanded) {
                                        val currentBusObj = buses.getOrNull(activeBusIndex)
                                        val currentBusName = currentBusObj?.name ?: "Bus ${activeBusIndex + 1}"
                                        Text(
                                            text = "Active: Bus ${activeBusIndex + 1} (${currentBusName})",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(NeonCyan)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Bus ${activeBusIndex + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { toggleMixBusSection() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMixBusExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isMixBusExpanded) "Collapse" else "Expand",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (isMixBusExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Grid of 16 MixBuses (2 columns)
                            val busRows = (0 until 16).chunked(2)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                busRows.forEach { rowIndices ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowIndices.forEach { busIdx ->
                                            val busNum = busIdx + 1
                                            val busObj = buses.getOrNull(busIdx)
                                            val busName = busObj?.name ?: "Bus $busNum"
                                            val isSelected = activeBusIndex == busIdx

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isSelected) DarkSurfaceVariant else DarkBackground)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) NeonCyan else DarkBorder,
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.setAssignedBus(busNum)
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "BUS $busNum",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) NeonCyan else TextMuted
                                                        )
                                                        Text(
                                                            text = busName,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary,
                                                            maxLines = 1
                                                        )
                                                    }
                                                    if (isSelected) {
                                                        Icon(
                                                            Icons.Default.CheckCircle,
                                                            contentDescription = "Selected",
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
                        }
                    }
                }
            }

            // Section 3: Profile Channel Groups Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggleGroupsSection() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = "Channel Groups",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "PROFILE CHANNEL GROUPS (${activeProfile?.profileName ?: "Musician"})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan
                                    )
                                    if (!isGroupsExpanded) {
                                        Text(
                                            text = if (customGroups.isEmpty()) "No custom groups" else "${customGroups.size} groups: ${customGroups.keys.joinToString(", ")}",
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        editingGroupName = null
                                        groupNameInput = ""
                                        selectedGroupChannelIds = emptySet()
                                        showGroupDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Add Group",
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "NEW GROUP",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { toggleGroupsSection() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isGroupsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isGroupsExpanded) "Collapse" else "Expand",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Group Enable / Disable Switch Card
                        val isGroupsOn = activeProfile?.groupsEnabled ?: true
                        Surface(
                            color = if (isGroupsOn) DarkSurfaceVariant else DarkBackground,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isGroupsOn) NeonCyan else DarkBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isGroupsOn) Icons.Default.FolderCopy else Icons.Default.FolderOff,
                                        contentDescription = "Enable Groups",
                                        tint = if (isGroupsOn) NeonCyan else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Enable Channel Groups",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (isGroupsOn)
                                                "Active: Group MCA submix faders & tabs enabled"
                                            else
                                                "Disabled: Dashboard shows full channel strip grid directly",
                                            fontSize = 10.sp,
                                            color = if (isGroupsOn) NeonCyan else TextMuted
                                        )
                                    }
                                }
                                Switch(
                                    checked = isGroupsOn,
                                    onCheckedChange = { viewModel.toggleGroupsEnabled() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = NeonCyan,
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = DarkSurface
                                    )
                                )
                            }
                        }

                        if (isGroupsExpanded) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Configure custom channel groups for your profile dashboard. A channel selected in one group cannot be added to another.",
                                fontSize = 11.sp,
                                color = TextMuted
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (customGroups.isEmpty()) {
                                Text(
                                    text = "No custom groups defined.",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    customGroups.forEach { (gName, chIds) ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(DarkBackground)
                                                .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = gName,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(DarkSurfaceVariant)
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = "${chIds.size} Chs",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = NeonCyan
                                                            )
                                                        }
                                                    }
                                                    val summary = if (chIds.isEmpty()) "No channels assigned" else {
                                                        chIds.mapNotNull { id -> channels.find { it.id == id }?.let { "Ch $id (${it.name})" } }
                                                            .take(5)
                                                            .joinToString(", ") + if (chIds.size > 5) "..." else ""
                                                    }
                                                    Text(
                                                        text = summary,
                                                        fontSize = 11.sp,
                                                        color = TextMuted,
                                                        maxLines = 1
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = {
                                                            editingGroupName = gName
                                                            groupNameInput = gName
                                                            selectedGroupChannelIds = chIds.toSet()
                                                            showGroupDialog = true
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Edit,
                                                            contentDescription = "Edit Group",
                                                            tint = NeonCyan,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            val updated = customGroups.toMutableMap().apply { remove(gName) }
                                                            viewModel.updateProfileGroups(updated)
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = "Delete Group",
                                                            tint = NeonRose.copy(alpha = 0.8f),
                                                            modifier = Modifier.size(16.dp)
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

            // Section 4: CTA to Proceed to Dashboard
            item {
                val currentBusObj = buses.getOrNull(activeBusIndex)
                val currentBusName = currentBusObj?.name ?: "Bus ${activeBusIndex + 1}"
                Button(
                    onClick = onBackToDashboard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "CONTINUE TO DASHBOARD (BUS ${activeBusIndex + 1}: ${currentBusName.uppercase()})",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Go to Dashboard",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Section 5: Presets & Snapshots Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { togglePresetsSection() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    Icons.Default.Bookmark,
                                    contentDescription = "Snapshots",
                                    tint = NeonAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "PRESETS & SNAPSHOTS (${activeProfile?.profileName ?: "Musician"})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonAmber
                                    )
                                    if (!isPresetsExpanded) {
                                        Text(
                                            text = "${currentPresets.size} snapshots saved for ${activeProfile?.profileName ?: "this profile"}",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurfaceVariant)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${currentPresets.size} Saved",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { togglePresetsSection() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPresetsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (isPresetsExpanded) "Collapse" else "Expand",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (isPresetsExpanded) {
                            Spacer(modifier = Modifier.height(10.dp))

                            if (currentPresets.isEmpty()) {
                                Text(
                                    text = "No saved presets for ${activeProfile?.profileName ?: "this profile"} yet.",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    currentPresets.forEach { preset ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(DarkSurfaceVariant)
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(preset.presetName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                                Text("Bus ${preset.busId}", fontSize = 11.sp, color = NeonCyan)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Button(
                                                    onClick = { viewModel.applyPreset(preset) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(26.dp)
                                                ) {
                                                    Text("LOAD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { viewModel.deletePreset(preset) },
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeonRose, modifier = Modifier.size(15.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Save Current Mix as New Snapshot:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = newPresetName,
                                    onValueChange = { newPresetName = it },
                                    placeholder = { Text("e.g. Acoustic Set, Vocal Boost", fontSize = 12.sp, color = TextMuted) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonAmber,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedContainerColor = DarkBackground,
                                        unfocusedContainerColor = DarkBackground
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (newPresetName.isNotBlank()) {
                                            viewModel.savePreset(newPresetName)
                                            newPresetName = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonAmber),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Text("SAVE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddProfileDialog) {
        val availableIcons = listOf(
            "MIC" to "Vocal/Mic",
            "GUITAR" to "Guitar",
            "BASS" to "Bass",
            "KEYBOARD" to "Keys",
            "DRUM" to "Drums",
            "HORN" to "Horns",
            "FX" to "FX / Aux"
        )

        AlertDialog(
            onDismissRequest = { showAddProfileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (editingProfile != null) Icons.Default.Edit else Icons.Default.PersonAdd,
                        contentDescription = "Profile Dialog",
                        tint = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (editingProfile != null) "Edit Musician Profile" else "Add Musician Profile",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Profile Name:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        placeholder = { Text("e.g. Percussion, Lead Sax, Guest Vocal", fontSize = 12.sp, color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        )
                    )

                    Text("Assigned Monitor MixBus:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Box {
                        Surface(
                            onClick = { showBusDropdownInDialog = true },
                            color = DarkBackground,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, NeonAmber),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val currentBusName = buses.getOrNull(selectedBusId - 1)?.name ?: "Bus $selectedBusId"
                                Text(
                                    text = "Bus $selectedBusId: $currentBusName",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Bus", tint = NeonAmber)
                            }
                        }

                        DropdownMenu(
                            expanded = showBusDropdownInDialog,
                            onDismissRequest = { showBusDropdownInDialog = false },
                            modifier = Modifier
                                .width(240.dp)
                                .heightIn(max = 240.dp)
                                .background(DarkSurfaceVariant)
                        ) {
                            buses.forEach { bus ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Bus ${bus.id}: ${bus.name}",
                                            fontSize = 12.sp,
                                            fontWeight = if (bus.id == selectedBusId) FontWeight.Bold else FontWeight.Normal,
                                            color = if (bus.id == selectedBusId) NeonCyan else TextPrimary
                                        )
                                    },
                                    onClick = {
                                        selectedBusId = bus.id
                                        showBusDropdownInDialog = false
                                    }
                                )
                            }
                        }
                    }

                    Text("Instrument Icon:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableIcons.forEach { (iconKey, label) ->
                            val isSelectedIcon = selectedIcon == iconKey
                            Surface(
                                onClick = { selectedIcon = iconKey },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelectedIcon) NeonCyan else DarkBackground,
                                border = BorderStroke(1.dp, if (isSelectedIcon) NeonCyan else DarkBorder),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getInstrumentIcon(iconKey),
                                        contentDescription = label,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelectedIcon) Color.Black else TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelectedIcon) Color.Black else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Enable / Disable Groups Switch in Dialog
                    Surface(
                        color = DarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Channel Groups",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (groupsEnabledInDialog) "Groups enabled (MCA cards active)" else "Groups disabled (Direct channels only)",
                                    fontSize = 10.sp,
                                    color = if (groupsEnabledInDialog) NeonCyan else TextMuted
                                )
                            }
                            Switch(
                                checked = groupsEnabledInDialog,
                                onCheckedChange = { groupsEnabledInDialog = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = NeonCyan,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = DarkSurfaceVariant
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            if (editingProfile != null) {
                                viewModel.updateProfile(
                                    profile = editingProfile!!,
                                    name = newProfileName,
                                    busId = selectedBusId,
                                    instrumentIcon = selectedIcon,
                                    groupsEnabled = groupsEnabledInDialog
                                )
                            } else {
                                viewModel.createProfile(
                                    name = newProfileName,
                                    busId = selectedBusId,
                                    instrumentIcon = selectedIcon,
                                    groupsEnabled = groupsEnabledInDialog
                                )
                            }
                            showAddProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text(
                        if (editingProfile != null) "SAVE CHANGES" else "CREATE PROFILE",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProfileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    if (showGroupDialog) {
        val assignedInOtherGroups = remember(customGroups, editingGroupName) {
            val map = mutableMapOf<Int, String>()
            customGroups.forEach { (gName, ids) ->
                if (gName != editingGroupName) {
                    ids.forEach { chId -> map[chId] = gName }
                }
            }
            map
        }

        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = if (editingGroupName == null) "CREATE NEW GROUP" else "EDIT GROUP: ${editingGroupName?.uppercase()}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = { Text("Group Name (e.g. Drums, Vocals, Rhythm)", fontSize = 11.sp, color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "SELECT CHANNELS FOR THIS GROUP:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "Channels assigned to another group cannot be selected.",
                        fontSize = 10.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground)
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(channels.sortedBy { it.id }) { ch ->
                                val inOtherGroup = assignedInOtherGroups[ch.id]
                                val isSelectedInThisGroup = selectedGroupChannelIds.contains(ch.id)
                                val isAvailable = inOtherGroup == null

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when {
                                                isSelectedInThisGroup -> NeonCyan.copy(alpha = 0.15f)
                                                !isAvailable -> DarkSurface.copy(alpha = 0.3f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable(enabled = isAvailable) {
                                            if (isSelectedInThisGroup) {
                                                selectedGroupChannelIds = selectedGroupChannelIds - ch.id
                                            } else {
                                                selectedGroupChannelIds = selectedGroupChannelIds + ch.id
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = isSelectedInThisGroup,
                                            onCheckedChange = if (isAvailable) {
                                                { checked ->
                                                    if (checked) selectedGroupChannelIds = selectedGroupChannelIds + ch.id
                                                    else selectedGroupChannelIds = selectedGroupChannelIds - ch.id
                                                }
                                            } else null,
                                            enabled = isAvailable,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = NeonCyan,
                                                uncheckedColor = TextMuted,
                                                disabledCheckedColor = TextMuted,
                                                disabledUncheckedColor = DarkBorder
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Ch ${ch.id}: ${ch.name}",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelectedInThisGroup) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isAvailable) TextPrimary else TextMuted
                                        )
                                    }

                                    if (!isAvailable && inOtherGroup != null) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(DarkSurfaceVariant)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "In $inOtherGroup",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonAmber
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = groupNameInput.trim()
                        if (name.isNotEmpty()) {
                            val updated = customGroups.toMutableMap()
                            if (editingGroupName != null && editingGroupName != name) {
                                updated.remove(editingGroupName)
                            }
                            updated[name] = selectedGroupChannelIds.toList().sorted()
                            viewModel.updateProfileGroups(updated)
                            showGroupDialog = false
                        }
                    },
                    enabled = groupNameInput.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("SAVE GROUP", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGroupDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}
