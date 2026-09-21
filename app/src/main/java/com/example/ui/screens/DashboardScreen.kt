package com.example.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChannelState
import com.example.ui.IemViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.util.HapticFeedbackHelper

@Composable
fun DashboardScreen(
    viewModel: IemViewModel,
    onOpenDiscovery: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenEngineer: () -> Unit,
    onOpenDiagnostics: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isFaderExpanded by remember { mutableStateOf(false) }

    val mixerInfo by viewModel.connectionState.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val appThemeMode by viewModel.appThemeMode.collectAsState()
    val activeBusIndex by viewModel.activeBusIndex.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val buses by viewModel.buses.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val groupLevels by viewModel.groupLevels.collectAsState()
    val customGroups by viewModel.customGroups.collectAsState()

    var selectedChannelId by remember { mutableStateOf<Int?>(1) }
    var selectedGroupTag by remember { mutableStateOf<String?>(null) }
    var isMasterSelected by remember { mutableStateOf(false) }

    val channelListState = rememberLazyListState()
    val groupListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val activeBus = buses.getOrNull(activeBusIndex)
    val busName = activeBus?.name ?: "Bus ${activeBusIndex + 1}"

    val isGroupsEnabled = activeProfile?.groupsEnabled ?: true

    val categories = remember(isGroupsEnabled) {
        if (isGroupsEnabled) {
            listOf("All", "Groups")
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
        if (isFaderExpanded || isLandscape) {
            // Sleek minimal top bar in expanded/landscape mode
            Surface(
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isFaderExpanded = !isFaderExpanded }
                    .padding(vertical = 4.dp, horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isFaderExpanded || isLandscape) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Toggle Faders View",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MIXBUS ${activeBusIndex + 1}: $busName • ${mixerInfo.model.uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                    }
                    Text(
                        text = if (isFaderExpanded) "TAP TO COLLAPSE HEADER" else "FULL CONSOLE MODE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )
                }
            }
            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
        } else {
            // Top Connection & Profile Header
            ConnectionHeader(
                mixerInfo = mixerInfo,
                assignedBusName = busName,
                profileName = activeProfile?.profileName ?: "Musician",
                role = userRole,
                buses = buses,
                activeBusIndex = activeBusIndex,
                appThemeMode = appThemeMode,
                onSelectBus = { busIdx -> viewModel.setAssignedBus(busIdx + 1) },
                onSelectThemeMode = { mode -> viewModel.setAppThemeMode(mode) },
                onOpenDiscovery = onOpenDiscovery,
                onOpenProfiles = onOpenProfiles,
                onToggleEngineer = onOpenEngineer,
                onOpenDiagnostics = onOpenDiagnostics,
                onSyncMixer = { viewModel.pullChannelsAndBusesFromMixer() },
                onReconnect = { viewModel.reconnectToMixer() }
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
                    // Locked Master MixBus Badge (Switching restricted to Profile Settings)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onOpenProfiles,
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.6f)),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "MixBus Locked",
                                    tint = NeonAmber,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "MIXBUS ${activeBusIndex + 1}: ${activeBus?.name ?: "MixBus"}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonAmber,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Change in Settings",
                                    tint = TextMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(verticalArrangement = Arrangement.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (activeBus?.masterMute == true) "MUTED" else activeBus?.getMasterDbString() ?: "0 dB",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeBus?.masterMute == true) NeonRose else NeonCyan
                                )
                                if (activeBus != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = activeBus.getPeakDbString(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (activeBus.masterMute) Color(0xFF64748B) else if (activeBus.peakMeter >= 0.88f) NeonRose else TextMuted
                                    )
                                }
                            }
                            if (activeBus != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                HorizontalMeterBar(
                                    level = activeBus.peakMeter,
                                    isMuted = activeBus.masterMute,
                                    height = 4.dp,
                                    modifier = Modifier.width(64.dp)
                                )
                            }
                        }
                    }

                    if (isLandscape) {
                        Surface(
                            onClick = { isFaderExpanded = true },
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.5.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Swipe up to expand",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SWIPE UP TO EXPAND",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                            }
                        }
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
                            color = if (isSelected) Color.White else TextPrimary
                        )
                    }
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
            val masterCardWidth = if (totalWidth < 380.dp) 64.dp else if (totalWidth < 600.dp) 85.dp else if (totalWidth < 900.dp) 105.dp else 125.dp
            val availableWidthForChannels = (totalWidth - (if (activeBus != null) masterCardWidth else 0.dp) - 6.dp).coerceAtLeast(0.dp)
            val minCardWidth = if (totalWidth < 380.dp) 58.dp else if (totalWidth < 600.dp) 72.dp else 85.dp
            val maxVisibleFaders = (availableWidthForChannels / minCardWidth).toInt().coerceAtLeast(1)
            val channelCardWidth = (availableWidthForChannels / maxVisibleFaders).coerceIn(58.dp, 130.dp)

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
                                items(customGroups.keys.toList()) { groupTag ->
                                    val currentLvl = groupLevels[groupTag] ?: 0.8f
                                    val groupChIds = customGroups[groupTag]
                                    val groupChannels = if (groupChIds != null) channels.filter { groupChIds.contains(it.id) } else channels.filter { it.groupTag.equals(groupTag, ignoreCase = true) }
                                    val matchingChs = groupChannels.size
                                    val isGroupMuted = groupChannels.isNotEmpty() && groupChannels.all { it.busSendMutes.getOrElse(activeBusIndex) { false } }

                                    GroupFaderCard(
                                        groupName = groupTag,
                                        channelCount = matchingChs,
                                        groupLevel = currentLvl,
                                        peakMeter = if (groupChannels.isEmpty()) 0f else (groupChannels.map { ch -> ch.peakMeter * ch.busSendLevels.getOrElse(activeBusIndex) { 0.75f } }.average().toFloat() * currentLvl).coerceIn(0f, 1f),
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
                                        cardWidth = channelCardWidth,
                                        isSelected = (selectedGroupTag == groupTag),
                                        onSelect = {
                                            selectedGroupTag = if (selectedGroupTag == groupTag) null else groupTag
                                            selectedChannelId = null
                                            isMasterSelected = false
                                        }
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
                                        cardWidth = channelCardWidth,
                                        isSelected = (selectedChannelId == channel.id),
                                        onSelect = {
                                            selectedChannelId = if (selectedChannelId == channel.id) null else channel.id
                                            selectedGroupTag = null
                                            isMasterSelected = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Fader Bank Navigation Bar (Groups of 4)
                    val activeListState = if (selectedCategory == "Groups") groupListState else channelListState
                    val totalCount = if (selectedCategory == "Groups") customGroups.size else filteredChannels.size
                    val visibleCount = activeListState.layoutInfo.visibleItemsInfo.size
                    val firstVisible = activeListState.firstVisibleItemIndex
                    val lastVisibleIndex = activeListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: firstVisible
                    val maxScrollIndex = (totalCount - 1).coerceAtLeast(0)
                    val canScrollNext = lastVisibleIndex < totalCount - 1 || firstVisible < maxScrollIndex

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
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Previous bank / item button
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val prev = (firstVisible - 4).coerceAtLeast(0)
                                            activeListState.animateScrollToItem(prev)
                                        }
                                    },
                                    enabled = firstVisible > 0,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Scroll Left",
                                        tint = if (firstVisible > 0) NeonCyan else TextMuted,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                // Next bank / item button
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val next = (firstVisible + 4).coerceAtMost(maxScrollIndex)
                                            activeListState.animateScrollToItem(next)
                                        }
                                    },
                                    enabled = canScrollNext,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Scroll Right",
                                        tint = if (canScrollNext) NeonCyan else TextMuted,
                                        modifier = Modifier.size(28.dp)
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
                            HapticFeedbackHelper.triggerBusMuteFeedback(context, haptic)
                            viewModel.toggleMasterBusMute(activeBusIndex)
                        },
                        cardWidth = masterCardWidth,
                        isSelected = isMasterSelected,
                        onSelect = {
                            isMasterSelected = !isMasterSelected
                            if (isMasterSelected) {
                                selectedChannelId = null
                                selectedGroupTag = null
                            }
                        },
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        }
    }
}
