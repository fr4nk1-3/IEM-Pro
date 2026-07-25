package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChannelState
import com.example.ui.IemViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: IemViewModel,
    onOpenDiscovery: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenEngineer: () -> Unit,
    onOpenDiagnostics: () -> Unit
) {
    val mixerInfo by viewModel.connectionState.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val activeBusIndex by viewModel.activeBusIndex.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val buses by viewModel.buses.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val groupLevels by viewModel.groupLevels.collectAsState()
    val customGroups by viewModel.customGroups.collectAsState()

    var showBusDropdown by remember { mutableStateOf(false) }

    val channelListState = rememberLazyListState()
    val groupListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current

    val activeBus = buses.getOrNull(activeBusIndex)
    val busName = activeBus?.name ?: "Bus ${activeBusIndex + 1}"

    val isGroupsEnabled = activeProfile?.groupsEnabled ?: true

    val categories = remember(customGroups, isGroupsEnabled) {
        if (isGroupsEnabled) {
            listOf("All", "Groups") + customGroups.keys.toList()
        } else {
            listOf("All")
        }
    }

    LaunchedEffect(isGroupsEnabled) {
        if (!isGroupsEnabled && selectedCategory != "All") {
            viewModel.setCategoryFilter("All")
        }
    }

    // Filter channels based on category
    val filteredChannels = remember(channels, selectedCategory, customGroups) {
        channels.filter { ch ->
            when (selectedCategory) {
                "All" -> true
                "Groups" -> false // Group cards shown separately
                else -> {
                    val groupChIds = customGroups[selectedCategory]
                    if (groupChIds != null) {
                        groupChIds.contains(ch.id)
                    } else {
                        ch.groupTag.equals(selectedCategory, ignoreCase = true)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Connection & Profile Header
        ConnectionHeader(
            mixerInfo = mixerInfo,
            assignedBusName = busName,
            profileName = activeProfile?.profileName ?: "Musician",
            role = userRole,
            buses = buses,
            activeBusIndex = activeBusIndex,
            onSelectBus = { busIdx -> viewModel.setAssignedBus(busIdx + 1) },
            onOpenDiscovery = onOpenDiscovery,
            onOpenProfiles = onOpenProfiles,
            onToggleEngineer = onOpenEngineer,
            onOpenDiagnostics = onOpenDiagnostics,
            onSyncMixer = { viewModel.pullChannelsAndBusesFromMixer() }
        )

        // Master Monitor Bar & Quick Presets
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Master Bus Selector, Volume / Mute
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Surface(
                            onClick = { showBusDropdown = true },
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.6f)),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MIXBUS:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Bus ${activeBusIndex + 1}: ${activeBus?.name ?: "MixBus"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Select MixBus",
                                    tint = NeonAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showBusDropdown,
                            onDismissRequest = { showBusDropdown = false },
                            modifier = Modifier
                                .width(220.dp)
                                .background(DarkSurfaceVariant)
                        ) {
                            buses.forEachIndexed { index, bus ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Bus ${bus.id}: ${bus.name}",
                                                fontSize = 12.sp,
                                                fontWeight = if (index == activeBusIndex) FontWeight.Bold else FontWeight.Normal,
                                                color = if (index == activeBusIndex) NeonCyan else TextPrimary
                                            )
                                            if (index == activeBusIndex) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = NeonCyan,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.setAssignedBus(bus.id)
                                        showBusDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = activeBus?.getMasterDbString() ?: "0 dB",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
            }
        }

        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)

        // Category Filter Tabs Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) NeonCyan else DarkSurfaceVariant)
                        .clickable { viewModel.setCategoryFilter(category) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = category,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Main Channel Strip Content Area
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 2.dp)
        ) {
            val totalWidth = maxWidth
            val masterCardWidth = if (totalWidth < 400.dp) 68.dp else if (totalWidth < 700.dp) 95.dp else 115.dp
            val availableWidthForChannels = (totalWidth - (if (activeBus != null) masterCardWidth else 0.dp) - 8.dp).coerceAtLeast(0.dp)
            val maxVisibleFaders = (availableWidthForChannels / 70.dp).toInt().coerceAtLeast(3)
            val channelCardWidth = (availableWidthForChannels / maxVisibleFaders).coerceIn(65.dp, 115.dp)

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (selectedCategory == "Groups") {
                            // Render Group MCA Submix Cards
                            LazyRow(
                                state = groupListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(groupLevels.entries.toList()) { entry ->
                                    val groupTag = entry.key
                                    val currentLvl = entry.value
                                    val groupChIds = customGroups[groupTag]
                                    val groupChannels = if (groupChIds != null) channels.filter { groupChIds.contains(it.id) } else channels.filter { it.groupTag.equals(groupTag, ignoreCase = true) }
                                    val matchingChs = groupChannels.size
                                    val isGroupMuted = groupChannels.isNotEmpty() && groupChannels.all { it.busSendMutes.getOrElse(activeBusIndex) { false } }

                                    GroupFaderCard(
                                        groupName = groupTag,
                                        channelCount = matchingChs,
                                        groupLevel = currentLvl,
                                        onGroupLevelChange = { newLvl ->
                                            viewModel.updateGroupSubmixLevel(groupTag, newLvl)
                                        },
                                        isGroupMuted = isGroupMuted,
                                        onGroupMuteToggle = {
                                            viewModel.toggleGroupMute(groupTag)
                                        },
                                        accentColor = when (groupTag) {
                                            "Drums" -> NeonCyan
                                            "Vocals" -> NeonMagenta
                                            "Guitars" -> NeonAmber
                                            else -> NeonEmerald
                                        },
                                        cardWidth = channelCardWidth
                                    )
                                }
                            }
                        } else if (filteredChannels.isEmpty()) {
                            // Empty state
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.FilterAltOff,
                                    contentDescription = "No channels",
                                    tint = TextMuted,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No channels match category '$selectedCategory'",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { viewModel.setCategoryFilter("All") }) {
                                    Text("Show All Channels")
                                }
                            }
                        } else {
                            // Horizontal Scrollable Channel Strip Rack (Ideal for stage touch control)
                            LazyRow(
                                state = channelListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(filteredChannels, key = { it.id }) { channel ->
                                    ChannelStripCard(
                                        channel = channel,
                                        activeBusIndex = activeBusIndex,
                                        onLevelChange = { newLvl ->
                                            viewModel.updateChannelBusLevel(channel.id, activeBusIndex, newLvl)
                                        },
                                        onMuteToggle = {
                                            viewModel.toggleChannelBusMute(channel.id, activeBusIndex)
                                        },
                                        cardWidth = channelCardWidth
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Fader Bank Navigation Bar (Groups of 4)
                    val activeListState = if (selectedCategory == "Groups") groupListState else channelListState
                    val totalCount = if (selectedCategory == "Groups") groupLevels.size else filteredChannels.size
                    val visibleCount = activeListState.layoutInfo.visibleItemsInfo.size
                    val maxScrollIndex = (totalCount - visibleCount).coerceAtLeast(0)
                    val firstVisible = activeListState.firstVisibleItemIndex

                    if (totalCount > 0) {
                        Surface(
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.5.dp, DarkBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous bank / item button
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val prev = (activeListState.firstVisibleItemIndex - 4).coerceAtLeast(0)
                                            activeListState.animateScrollToItem(prev)
                                        }
                                    },
                                    enabled = firstVisible > 0,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Bank Left",
                                        tint = if (firstVisible > 0) NeonCyan else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Text(
                                    text = "BANKS:",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    modifier = Modifier.padding(end = 4.dp)
                                )

                                // Bank Buttons (Groups of 4) in a scrollable Row
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val bankSize = 4
                                    val numBanks = (totalCount + bankSize - 1) / bankSize
                                    for (b in 0 until numBanks) {
                                        val startCh = b * bankSize + 1
                                        val endCh = minOf((b + 1) * bankSize, totalCount)
                                        val targetIdx = b * bankSize
                                        val isBankActive = firstVisible in (b * bankSize) until minOf((b + 1) * bankSize, totalCount)

                                        Surface(
                                            onClick = {
                                                coroutineScope.launch {
                                                    activeListState.animateScrollToItem(targetIdx)
                                                }
                                            },
                                            color = if (isBankActive) NeonCyan else DarkSurface,
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(0.5.dp, if (isBankActive) NeonCyan else DarkBorder),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text(
                                                text = if (selectedCategory == "Groups") "G$startCh-$endCh" else "$startCh-$endCh",
                                                fontSize = 10.sp,
                                                fontWeight = if (isBankActive) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isBankActive) Color.Black else TextSecondary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Next bank / item button
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val next = (activeListState.firstVisibleItemIndex + 4).coerceAtMost(maxScrollIndex)
                                            activeListState.animateScrollToItem(next)
                                        }
                                    },
                                    enabled = firstVisible < maxScrollIndex,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Bank Right",
                                        tint = if (firstVisible < maxScrollIndex) NeonCyan else TextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Active position indicator
                                Surface(
                                    color = DarkSurface,
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.5.dp, DarkBorder),
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    val lastVisible = (firstVisible + visibleCount).coerceAtMost(totalCount)
                                    val labelText = if (selectedCategory == "Groups") {
                                        "GRP ${firstVisible + 1}-$lastVisible / $totalCount"
                                    } else if (visibleCount >= totalCount) {
                                        "ALL $totalCount"
                                    } else {
                                        "CH ${firstVisible + 1}-$lastVisible / $totalCount"
                                    }
                                    Text(
                                        text = labelText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeonCyan,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Pinned Bus Master Fader Strip
                if (activeBus != null) {
                    VerticalDivider(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 1.dp),
                        color = DarkBorder,
                        thickness = 1.dp
                    )

                    BusMasterFaderCard(
                        bus = activeBus,
                        onMasterLevelChange = { newLvl ->
                            viewModel.updateMasterBusLevel(activeBusIndex, newLvl)
                        },
                        onMasterMuteToggle = {
                            viewModel.toggleMasterBusMute(activeBusIndex)
                        },
                        cardWidth = masterCardWidth,
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        }
    }
}
