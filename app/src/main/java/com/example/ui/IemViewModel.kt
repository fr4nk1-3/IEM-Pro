package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.model.*
import com.example.network.MixerDiscoveryEngine
import com.example.network.OscMessage
import com.example.network.OscSocketClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class IemViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = IemRepository(db)

    val oscClient = OscSocketClient()
    val discoveryEngine = MixerDiscoveryEngine()

    // Connection & Mixer Info State
    val connectionState: StateFlow<MixerModelInfo> = oscClient.connectionState
    val discoveredMixers: StateFlow<List<MixerModelInfo>> = discoveryEngine.discoveredMixers
    val isScanning: StateFlow<Boolean> = discoveryEngine.isScanning
    val hasCompletedScan: StateFlow<Boolean> = discoveryEngine.hasCompletedScan
    val scanProgressText: StateFlow<String> = discoveryEngine.scanProgressText

    private val _autoScanOnOpen = MutableStateFlow(true)
    val autoScanOnOpen: StateFlow<Boolean> = _autoScanOnOpen.asStateFlow()

    fun setAutoScanOnOpen(enabled: Boolean) {
        _autoScanOnOpen.value = enabled
    }

    // Profiles & Presets State
    val profiles: StateFlow<List<UserProfileEntity>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _activeProfile = MutableStateFlow<UserProfileEntity?>(null)
    val activeProfile: StateFlow<UserProfileEntity?> = _activeProfile.asStateFlow()

    private val _activeBusIndex = MutableStateFlow(0) // 0..15 (Bus 1..16)
    val activeBusIndex: StateFlow<Int> = _activeBusIndex.asStateFlow()

    // Channels & Buses Live State
    private val _channels = MutableStateFlow<List<ChannelState>>(emptyList())
    val channels: StateFlow<List<ChannelState>> = _channels.asStateFlow()

    private val _buses = MutableStateFlow<List<MixBusState>>(emptyList())
    val buses: StateFlow<List<MixBusState>> = _buses.asStateFlow()

    // UI Category Filter ("All", "Favorites", "Groups", "Drums", "Vocals", etc.)
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // App UI Theme Mode State (CQ MixPad, Glassmorphism, Light)
    private val _appThemeMode = MutableStateFlow(AppThemeMode.CQ_MIXPAD)
    val appThemeMode: StateFlow<AppThemeMode> = _appThemeMode.asStateFlow()

    fun setAppThemeMode(mode: AppThemeMode) {
        _appThemeMode.value = mode
    }

    // User Role State (Musician vs Engineer)
    private val _userRole = MutableStateFlow(UserRole(RoleType.MUSICIAN, isUnlocked = false))
    val userRole: StateFlow<UserRole> = _userRole.asStateFlow()

    // Group Fader Multiplier State
    private val _groupLevels = MutableStateFlow(
        mutableMapOf("Drums" to 0.8f, "Vocals" to 0.8f, "Guitars" to 0.8f, "Keys" to 0.8f)
    )
    val groupLevels: StateFlow<Map<String, Float>> = _groupLevels.asStateFlow()

    // Base fader ratio levels set by the user in the faders section: Pair(channelId, busIndex) -> Baseline Level (0.0..1.0)
    private val channelSendBaselines = mutableMapOf<Pair<Int, Int>, Float>()

    // Custom groups for active profile
    private val _customGroups = MutableStateFlow<Map<String, List<Int>>>(emptyMap())
    val customGroups: StateFlow<Map<String, List<Int>>> = _customGroups.asStateFlow()

    // Active Presets for current profile
    private val _currentPresets = MutableStateFlow<List<MixPresetEntity>>(emptyList())
    val currentPresets: StateFlow<List<MixPresetEntity>> = _currentPresets.asStateFlow()

    // Notification message flow
    private val _notificationMessage = MutableStateFlow<String?>(null)
    val notificationMessage: StateFlow<String?> = _notificationMessage.asStateFlow()

    fun showNotification(msg: String) {
        _notificationMessage.value = msg
    }

    fun clearNotification() {
        _notificationMessage.value = null
    }

    init {
        viewModelScope.launch {
            repository.ensureDefaultProfiles()
            
            // Set initial channels and buses from simulator
            _channels.value = oscClient.simulator.channels.map { it.copy() }
            _buses.value = oscClient.simulator.buses.map { it.copy() }

            // Set default active profile
            profiles.collect { list ->
                if (list.isNotEmpty() && _activeProfile.value == null) {
                    val defaultProf = list.firstOrNull { it.isDefault } ?: list.first()
                    selectProfile(defaultProf)
                }
            }
        }

        // Listen for socket notification events
        viewModelScope.launch {
            oscClient.notificationEvent.collect { msg ->
                _notificationMessage.value = msg
            }
        }

        // Listen for incoming OSC packets
        viewModelScope.launch {
            oscClient.incomingMessages.collect { msg ->
                parseIncomingOsc(msg)
            }
        }

        // Auto-pull channels and mixbuses when mixer connects
        viewModelScope.launch {
            connectionState.collect { info ->
                if (info.status == ConnectionStatus.CONNECTED || info.status == ConnectionStatus.SIMULATION) {
                    pullChannelsAndBusesFromMixer()
                }
            }
        }

        // Periodically refresh live channel peak meters and compute bus master meters
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(50)
                val chs = _channels.value
                val buses = _buses.value

                var chsChanged = false
                var busesChanged = false

                val newChs = chs.toMutableList()
                if (oscClient.isSimulatorActive) {
                    val simChs = oscClient.simulator.channels
                    for (i in newChs.indices) {
                        if (i in simChs.indices) {
                            val simVal = simChs[i].peakMeter
                            if (kotlin.math.abs(newChs[i].peakMeter - simVal) > 0.005f) {
                                newChs[i] = newChs[i].copy(peakMeter = simVal)
                                chsChanged = true
                            }
                        }
                    }
                }

                val newBuses = buses.toMutableList()
                for (bIdx in newBuses.indices) {
                    val bus = newBuses[bIdx]
                    var sendSumL = 0f
                    var sendSumR = 0f
                    var activeSources = 0
                    for (ch in newChs) {
                        if (!ch.isMuted && !ch.busSendMutes.getOrElse(bIdx) { false }) {
                            val sendLvl = ch.busSendLevels.getOrElse(bIdx) { 0.75f }
                            val sendPan = ch.busSendPans.getOrElse(bIdx) { 0.5f }
                            val chAudio = (ch.peakMeter * (0.2f + sendLvl * 0.8f)).coerceIn(0f, 1f)
                            
                            // Equal power / linear stereo panning to bus master
                            val leftFactor = (1.0f - sendPan) * 1.3f
                            val rightFactor = sendPan * 1.3f
                            sendSumL += chAudio * leftFactor.coerceIn(0.2f, 1.0f)
                            sendSumR += chAudio * rightFactor.coerceIn(0.2f, 1.0f)
                            activeSources++
                        }
                    }
                    val normalizationDivisor = if (activeSources > 0) (kotlin.math.sqrt(activeSources.toFloat()) * 1.2f).coerceAtLeast(2.0f) else 2.5f
                    val rawBusAudioL = (sendSumL / normalizationDivisor).coerceIn(0f, 1.3f)
                    val rawBusAudioR = (sendSumR / normalizationDivisor).coerceIn(0f, 1.3f)

                    var calcBusMeterL = (rawBusAudioL * (if (bus.masterLevel <= 0.01f) 0.5f else bus.masterLevel)).coerceIn(0f, 1f)
                    var calcBusMeterR = (rawBusAudioR * (if (bus.masterLevel <= 0.01f) 0.5f else bus.masterLevel)).coerceIn(0f, 1f)

                    // Apply brickwall limiter if bus limiter is active
                    if (bus.limiterActive) {
                        val limitCeiling = (0.75f + (bus.limiterThresholdDb / 40f)).coerceIn(0.6f, 0.95f)
                        if (calcBusMeterL > limitCeiling) {
                            calcBusMeterL = limitCeiling + (calcBusMeterL - limitCeiling) * 0.15f
                        }
                        if (calcBusMeterR > limitCeiling) {
                            calcBusMeterR = limitCeiling + (calcBusMeterR - limitCeiling) * 0.15f
                        }
                    }

                    val calcBusMeter = maxOf(calcBusMeterL, calcBusMeterR)

                    if (kotlin.math.abs(bus.peakMeter - calcBusMeter) > 0.005f ||
                        kotlin.math.abs(bus.peakMeterL - calcBusMeterL) > 0.005f ||
                        kotlin.math.abs(bus.peakMeterR - calcBusMeterR) > 0.005f) {
                        newBuses[bIdx] = bus.copy(
                            peakMeter = calcBusMeter,
                            peakMeterL = calcBusMeterL,
                            peakMeterR = calcBusMeterR
                        )
                        busesChanged = true
                    }
                }

                if (chsChanged) _channels.value = newChs
                if (busesChanged) _buses.value = newBuses
            }
        }
    }

    fun selectProfile(profile: UserProfileEntity) {
        _activeProfile.value = profile
        _activeBusIndex.value = (profile.assignedBusId - 1).coerceIn(0, 15)

        applyProfileGroups(profile)

        // Parse favorite channel IDs
        val favIds = profile.favoriteChannelIdsCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()

        _channels.value = _channels.value.map { ch ->
            ch.copy(isFavorite = ch.id in favIds)
        }

        // Load presets for profile and persist default active profile
        viewModelScope.launch {
            repository.setActiveDefaultProfile(profile.id)
            repository.getPresetsForProfile(profile.id).collect { list ->
                _currentPresets.value = list
            }
        }
    }

    // Saved manual IP state so user IP is never lost
    private val _savedManualIp = MutableStateFlow("192.168.1.100")
    val savedManualIp: StateFlow<String> = _savedManualIp.asStateFlow()

    fun setManualIp(ip: String) {
        if (ip.isNotBlank()) {
            _savedManualIp.value = ip
        }
    }

    private fun applyProfileGroups(profile: UserProfileEntity) {
        val groups = CustomGroupParser.parseGroups(profile.customGroupsJson)
        _customGroups.value = groups

        val channelGroupMap = mutableMapOf<Int, String>()
        groups.forEach { (gName, chIds) ->
            chIds.forEach { channelGroupMap[it] = gName }
        }

        _channels.value = _channels.value.map { ch ->
            val tag = channelGroupMap[ch.id] ?: "All"
            ch.copy(groupTag = tag)
        }

        val newGroupLevels = mutableMapOf<String, Float>()
        groups.keys.forEach { gName ->
            newGroupLevels[gName] = _groupLevels.value[gName] ?: 0.8f
        }
        _groupLevels.value = newGroupLevels

        if (_selectedCategory.value != "All" && _selectedCategory.value != "Groups" && !groups.containsKey(_selectedCategory.value)) {
            _selectedCategory.value = "All"
        }
    }

    fun toggleGroupsEnabled() {
        val currentProfile = _activeProfile.value ?: return
        val newStatus = !currentProfile.groupsEnabled
        setGroupsEnabled(newStatus)
    }

    fun setGroupsEnabled(enabled: Boolean) {
        val currentProfile = _activeProfile.value ?: return
        val updated = currentProfile.copy(groupsEnabled = enabled)
        _activeProfile.value = updated
        if (!enabled && _selectedCategory.value != "All") {
            _selectedCategory.value = "All"
        }
        viewModelScope.launch {
            repository.saveProfile(updated)
        }
    }

    fun updateProfileGroups(newGroups: Map<String, List<Int>>) {
        val currentProfile = _activeProfile.value ?: return
        val jsonStr = CustomGroupParser.toJson(newGroups)
        val updated = currentProfile.copy(customGroupsJson = jsonStr)
        _activeProfile.value = updated
        applyProfileGroups(updated)
        viewModelScope.launch {
            repository.saveProfile(updated)
        }
    }

    fun setAssignedBus(busId: Int) {
        _activeBusIndex.value = (busId - 1).coerceIn(0, 15)
        _activeProfile.value?.let { current ->
            val updated = current.copy(assignedBusId = busId)
            _activeProfile.value = updated
            viewModelScope.launch { repository.saveProfile(updated) }
        }
    }

    fun updateProfile(
        profile: UserProfileEntity,
        name: String,
        busId: Int,
        instrumentIcon: String,
        groupsEnabled: Boolean = profile.groupsEnabled
    ) {
        val updated = profile.copy(
            profileName = name.ifBlank { profile.profileName },
            assignedBusId = busId.coerceIn(1, 16),
            instrumentIcon = instrumentIcon,
            groupsEnabled = groupsEnabled
        )
        viewModelScope.launch {
            repository.saveProfile(updated)
            if (_activeProfile.value?.id == profile.id) {
                _activeProfile.value = updated
                _activeBusIndex.value = (busId - 1).coerceIn(0, 15)
                if (!groupsEnabled && _selectedCategory.value != "All") {
                    _selectedCategory.value = "All"
                }
            }
        }
    }

    fun createProfile(
        name: String,
        busId: Int,
        instrumentIcon: String = "MIC",
        accentTheme: String = "CYAN",
        groupsEnabled: Boolean = false
    ) {
        val newId = "prof_" + System.currentTimeMillis()
        val newProf = UserProfileEntity(
            id = newId,
            profileName = name.ifBlank { "Musician" },
            instrumentIcon = instrumentIcon,
            assignedBusId = busId.coerceIn(1, 16),
            favoriteChannelIdsCsv = "1,2,5,8,10,13,22",
            groupsEnabled = groupsEnabled,
            preferredThemeAccent = accentTheme,
            isDefault = false
        )
        viewModelScope.launch {
            repository.saveProfile(newProf)
            selectProfile(newProf)
        }
    }

    fun deleteProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            if (_activeProfile.value?.id == profile.id) {
                val remaining = repository.allProfiles.first()
                remaining.firstOrNull { it.id != profile.id }?.let { next ->
                    selectProfile(next)
                }
            }
        }
    }

    fun setCategoryFilter(category: String) {
        _selectedCategory.value = category
    }

    fun updateChannelBusLevel(channelId: Int, busIndex: Int, newLevel: Float) {
        val list = _channels.value.toMutableList()
        val idx = list.indexOfFirst { it.id == channelId }
        if (idx != -1) {
            val oldCh = list[idx]
            val newLevels = oldCh.busSendLevels.toMutableList().apply {
                if (busIndex in indices) this[busIndex] = newLevel
            }
            list[idx] = oldCh.copy(busSendLevels = newLevels)
            _channels.value = list.toList()

            // Update baseline reference for ratio-based group fader scaling
            val groupTag = oldCh.groupTag
            val currentGroupLvl = _groupLevels.value[groupTag] ?: 0.8f
            val baseLevel = if (currentGroupLvl > 0.05f) {
                (newLevel / (currentGroupLvl / 0.8f)).coerceIn(0f, 1f)
            } else {
                newLevel
            }
            channelSendBaselines[Pair(channelId, busIndex)] = baseLevel

            // Send OSC message to mixer
            val chStr = String.format("%02d", channelId)
            val busStr = String.format("%02d", busIndex + 1)
            oscClient.sendOscMessage(OscMessage("/ch/$chStr/mix/$busStr/level", listOf(newLevel)))
        }
    }

    fun updateChannelBusPan(channelId: Int, busIndex: Int, newPan: Float) {
        val list = _channels.value.toMutableList()
        val idx = list.indexOfFirst { it.id == channelId }
        if (idx != -1) {
            val oldCh = list[idx]
            val newPans = oldCh.busSendPans.toMutableList().apply {
                if (busIndex in indices) this[busIndex] = newPan
            }
            list[idx] = oldCh.copy(busSendPans = newPans)
            _channels.value = list.toList()

            val chStr = String.format("%02d", channelId)
            val busStr = String.format("%02d", busIndex + 1)
            oscClient.sendOscMessage(OscMessage("/ch/$chStr/mix/$busStr/pan", listOf(newPan)))
        }
    }

    fun toggleChannelBusMute(channelId: Int, busIndex: Int) {
        val list = _channels.value.toMutableList()
        val idx = list.indexOfFirst { it.id == channelId }
        if (idx != -1) {
            val oldCh = list[idx]
            val currentMute = oldCh.busSendMutes.getOrElse(busIndex) { false }
            val newMute = !currentMute
            val newMutes = oldCh.busSendMutes.toMutableList().apply {
                if (busIndex in indices) this[busIndex] = newMute
            }
            list[idx] = oldCh.copy(busSendMutes = newMutes)
            _channels.value = list.toList()

            val chStr = String.format("%02d", channelId)
            val busStr = String.format("%02d", busIndex + 1)
            // In X32 OSC, 1 is ON (unmuted), 0 is OFF (muted)
            oscClient.sendOscMessage(OscMessage("/ch/$chStr/mix/$busStr/on", listOf(if (newMute) 0 else 1)))
        }
    }

    fun updateMasterBusLevel(busIndex: Int, newLevel: Float) {
        val list = _buses.value.toMutableList()
        if (busIndex in list.indices) {
            val maxLevel = list[busIndex].getMaxFaderLevel()
            val clampedLevel = newLevel.coerceIn(0f, maxLevel)
            val bus = list[busIndex].copy(masterLevel = clampedLevel)
            list[busIndex] = bus
            _buses.value = list

            val busStr = String.format("%02d", busIndex + 1)
            oscClient.sendOscMessage(OscMessage("/bus/$busStr/mix/fader", listOf(clampedLevel)))
        }
    }

    fun toggleMasterBusMute(busIndex: Int) {
        val list = _buses.value.toMutableList()
        if (busIndex in list.indices) {
            val newMute = !list[busIndex].masterMute
            val bus = list[busIndex].copy(masterMute = newMute)
            list[busIndex] = bus
            _buses.value = list

            val busStr = String.format("%02d", busIndex + 1)
            oscClient.sendOscMessage(OscMessage("/bus/$busStr/mix/on", listOf(if (newMute) 0 else 1)))
        }
    }

    // Engineer Talkback & Solo State
    private val _talkbackMicGain = MutableStateFlow(0.75f)
    val talkbackMicGain: StateFlow<Float> = _talkbackMicGain.asStateFlow()

    private val _isTalkbackEngaged = MutableStateFlow(false)
    val isTalkbackEngaged: StateFlow<Boolean> = _isTalkbackEngaged.asStateFlow()

    fun setTalkbackMicGain(gain: Float) {
        _talkbackMicGain.value = gain
    }

    fun toggleTalkbackEngaged() {
        _isTalkbackEngaged.value = !_isTalkbackEngaged.value
        showNotification(if (_isTalkbackEngaged.value) "Talkback Mic ENGAGED" else "Talkback Mic MUTED")
    }

    fun updateBusDetails(busId: Int, name: String, color: X32Color, icon: String = "HEADPHONES") {
        val busIdx = busId - 1
        val list = _buses.value.toMutableList()
        if (busIdx in list.indices) {
            val updated = list[busIdx].copy(name = name, color = color, iconType = icon)
            list[busIdx] = updated
            _buses.value = list

            val busStr = String.format("%02d", busId)
            oscClient.sendOscMessage(OscMessage("/bus/$busStr/config/name", listOf(name)))
            oscClient.sendOscMessage(OscMessage("/bus/$busStr/config/color", listOf(color.id)))
            showNotification("Bus $busId settings saved: $name")
        }
    }

    fun toggleBusStereoLink(busId: Int) {
        val busIdx = busId - 1
        val list = _buses.value.toMutableList()
        if (busIdx in list.indices) {
            val bus = list[busIdx]
            val newLinked = !bus.isStereoLinked
            val partnerId = if (busId % 2 != 0) busId + 1 else busId - 1
            val partnerIdx = partnerId - 1

            list[busIdx] = bus.copy(isStereoLinked = newLinked, linkedBusId = if (newLinked) partnerId else null)
            if (partnerIdx in list.indices) {
                list[partnerIdx] = list[partnerIdx].copy(isStereoLinked = newLinked, linkedBusId = if (newLinked) busId else null)
            }
            _buses.value = list

            val busStr = String.format("%02d", busId)
            oscClient.sendOscMessage(OscMessage("/bus/$busStr/config/link", listOf(if (newLinked) 1 else 0)))

            val statusStr = if (newLinked) "Linked Bus $busId + Bus $partnerId Stereo Pair" else "Unlinked Bus $busId"
            showNotification(statusStr)
        }
    }

    fun toggleBusLimiter(busId: Int) {
        val busIdx = busId - 1
        val list = _buses.value.toMutableList()
        if (busIdx in list.indices) {
            val newLimiter = !list[busIdx].limiterActive
            var bus = list[busIdx].copy(limiterActive = newLimiter)
            val maxLevel = bus.getMaxFaderLevel()
            if (bus.limiterActive && bus.masterLevel > maxLevel) {
                bus = bus.copy(masterLevel = maxLevel)
                val busStr = String.format("%02d", busId)
                oscClient.sendOscMessage(OscMessage("/bus/$busStr/mix/fader", listOf(maxLevel)))
            }
            list[busIdx] = bus
            _buses.value = list

            val busStr = String.format("%02d", busId)
            oscClient.sendOscMessage(OscMessage("/bus/$busStr/dyn/on", listOf(if (newLimiter) 1 else 0)))

            showNotification("Bus $busId Ear Protection Limiter ${if (newLimiter) "ON (Capped at ${bus.limiterThresholdDb.toInt()}dB)" else "OFF"}")
        }
    }

    fun updateBusLimiterThreshold(busId: Int, thresholdDb: Float) {
        val busIdx = busId - 1
        val list = _buses.value.toMutableList()
        if (busIdx in list.indices) {
            var bus = list[busIdx].copy(limiterThresholdDb = thresholdDb)
            val maxLevel = bus.getMaxFaderLevel()
            if (bus.limiterActive && bus.masterLevel > maxLevel) {
                bus = bus.copy(masterLevel = maxLevel)
                val busStr = String.format("%02d", busId)
                oscClient.sendOscMessage(OscMessage("/bus/$busStr/mix/fader", listOf(maxLevel)))
            }
            list[busIdx] = bus
            _buses.value = list

            val busStr = String.format("%02d", busId)
            oscClient.sendOscMessage(OscMessage("/bus/$busStr/dyn/thresh", listOf(thresholdDb)))
        }
    }

    fun toggleChannelEq(channelId: Int) {
        val chIdx = channelId - 1
        val list = _channels.value.toMutableList()
        if (chIdx in list.indices) {
            val newEq = !list[chIdx].eqActive
            list[chIdx] = list[chIdx].copy(eqActive = newEq)
            _channels.value = list

            val chStr = String.format("%02d", channelId)
            oscClient.sendOscMessage(OscMessage("/ch/$chStr/eq/on", listOf(if (newEq) 1 else 0)))

            showNotification("Channel $channelId EQ ${if (newEq) "ACTIVE" else "BYPASSED"}")
        }
    }

    fun toggleBusEq(busId: Int) {
        val busIdx = busId - 1
        val list = _buses.value.toMutableList()
        if (busIdx in list.indices) {
            val newEq = !list[busIdx].eqActive
            list[busIdx] = list[busIdx].copy(eqActive = newEq)
            _buses.value = list

            val busStr = String.format("%02d", busId)
            oscClient.sendOscMessage(OscMessage("/bus/$busStr/eq/on", listOf(if (newEq) 1 else 0)))

            showNotification("Bus $busId 6-Band Parametric EQ ${if (newEq) "ACTIVE" else "BYPASSED"}")
        }
    }

    fun updateBusEqBandGain(busId: Int, bandIndex: Int, gainDb: Float) {
        val busIdx = busId - 1
        val list = _buses.value.toMutableList()
        if (busIdx in list.indices) {
            val bus = list[busIdx]
            val updatedBands = bus.eqBands.toMutableList()
            if (bandIndex in updatedBands.indices) {
                updatedBands[bandIndex] = updatedBands[bandIndex].copy(gainDb = gainDb)
                list[busIdx] = bus.copy(eqBands = updatedBands)
                _buses.value = list
                
                val busStr = String.format("%02d", busId)
                val bandNum = bandIndex + 1
                oscClient.sendOscMessage(OscMessage("/bus/$busStr/eq/$bandNum/g", listOf(gainDb)))
            }
        }
    }

    fun updateBusEqBandFreq(busId: Int, bandIndex: Int, freqHz: Float) {
        val busIdx = busId - 1
        val list = _buses.value.toMutableList()
        if (busIdx in list.indices) {
            val bus = list[busIdx]
            val updatedBands = bus.eqBands.toMutableList()
            if (bandIndex in updatedBands.indices) {
                updatedBands[bandIndex] = updatedBands[bandIndex].copy(freqHz = freqHz)
                list[busIdx] = bus.copy(eqBands = updatedBands)
                _buses.value = list

                val busStr = String.format("%02d", busId)
                val bandNum = bandIndex + 1
                oscClient.sendOscMessage(OscMessage("/bus/$busStr/eq/$bandNum/f", listOf(freqHz)))
            }
        }
    }

    fun updateBusEqBandQ(busId: Int, bandIndex: Int, qFactor: Float) {
        val busIdx = busId - 1
        val list = _buses.value.toMutableList()
        if (busIdx in list.indices) {
            val bus = list[busIdx]
            val updatedBands = bus.eqBands.toMutableList()
            if (bandIndex in updatedBands.indices) {
                updatedBands[bandIndex] = updatedBands[bandIndex].copy(qFactor = qFactor)
                list[busIdx] = bus.copy(eqBands = updatedBands)
                _buses.value = list

                val busStr = String.format("%02d", busId)
                val bandNum = bandIndex + 1
                oscClient.sendOscMessage(OscMessage("/bus/$busStr/eq/$bandNum/q", listOf(qFactor)))
            }
        }
    }

    fun resetBusEq(busId: Int) {
        val busIdx = busId - 1
        val list = _buses.value.toMutableList()
        if (busIdx in list.indices) {
            val bus = list[busIdx]
            val flatBands = defaultBusEqBands()
            list[busIdx] = bus.copy(eqBands = flatBands)
            _buses.value = list

            val busStr = String.format("%02d", busId)
            for (i in 1..6) {
                oscClient.sendOscMessage(OscMessage("/bus/$busStr/eq/$i/g", listOf(0.0f)))
            }
            showNotification("Bus $busId EQ Reset to Flat (0 dB)")
        }
    }

    fun updateBusOutputDelay(busId: Int, delayMs: Float) {
        val busIdx = busId - 1
        val list = _buses.value.toMutableList()
        if (busIdx in list.indices) {
            list[busIdx] = list[busIdx].copy(outputDelayMs = delayMs)
            _buses.value = list

            val busStr = String.format("%02d", busId)
            oscClient.sendOscMessage(OscMessage("/bus/$busStr/delay/time", listOf(delayMs)))
            oscClient.sendOscMessage(OscMessage("/bus/$busStr/delay/on", listOf(if (delayMs > 0f) 1 else 0)))
        }
    }

    fun toggleBusPhaseInvert(busId: Int) {
        val busIdx = busId - 1
        val list = _buses.value.toMutableList()
        if (busIdx in list.indices) {
            val newPhase = !list[busIdx].phaseInverted
            list[busIdx] = list[busIdx].copy(phaseInverted = newPhase)
            _buses.value = list

            val busStr = String.format("%02d", busId)
            oscClient.sendOscMessage(OscMessage("/bus/$busStr/preamp/invert", listOf(if (newPhase) 1 else 0)))

            showNotification("Bus $busId Output Phase ${if (newPhase) "180° INVERTED" else "0° NORMAL"}")
        }
    }

    fun updateBusTapMode(busId: Int, mode: BusTapMode) {
        val busIdx = busId - 1
        val list = _buses.value.toMutableList()
        if (busIdx in list.indices) {
            list[busIdx] = list[busIdx].copy(sendTapMode = mode)
            _buses.value = list

            val busStr = String.format("%02d", busId)
            oscClient.sendOscMessage(OscMessage("/bus/$busStr/config/tap", listOf(mode.ordinal)))

            showNotification("Bus $busId Tap Mode changed to ${mode.label}")
        }
    }

    fun toggleBusTalkback(busId: Int) {
        val busIdx = busId - 1
        val list = _buses.value.toMutableList()
        if (busIdx in list.indices) {
            val newTalk = !list[busIdx].talkbackActive
            list[busIdx] = list[busIdx].copy(talkbackActive = newTalk)
            _buses.value = list
            showNotification("Talkback Feed to Bus $busId ${if (newTalk) "ENABLED" else "DISABLED"}")
        }
    }


    fun updateGroupSubmixLevel(groupTag: String, newGroupLevel: Float) {
        val currentMap = _groupLevels.value.toMutableMap()
        currentMap[groupTag] = newGroupLevel
        _groupLevels.value = currentMap

        val activeBus = _activeBusIndex.value
        val list = _channels.value.toMutableList()
        val groupChIds = _customGroups.value[groupTag]
        val scaleFactor = newGroupLevel / 0.8f // 0.8f is nominal group fader center

        for (i in list.indices) {
            val ch = list[i]
            val isMember = (groupChIds != null && groupChIds.contains(ch.id)) || ch.groupTag.equals(groupTag, ignoreCase = true)
            if (isMember) {
                val key = Pair(ch.id, activeBus)
                val baseLevel = channelSendBaselines.getOrPut(key) {
                    ch.busSendLevels.getOrElse(activeBus) { 0.75f }
                }
                val adjusted = (baseLevel * scaleFactor).coerceIn(0f, 1f)
                val newLevels = ch.busSendLevels.toMutableList().apply {
                    if (activeBus in indices) this[activeBus] = adjusted
                }
                val updatedCh = ch.copy(busSendLevels = newLevels)
                list[i] = updatedCh

                val chStr = String.format("%02d", ch.id)
                val busStr = String.format("%02d", activeBus + 1)
                oscClient.sendOscMessage(OscMessage("/ch/$chStr/mix/$busStr/level", listOf(adjusted)))
            }
        }
        _channels.value = list.toList()
    }

    fun toggleGroupMute(groupTag: String) {
        val activeBus = _activeBusIndex.value
        val list = _channels.value.toMutableList()
        val groupChIds = _customGroups.value[groupTag]
        val isMember: (ChannelState) -> Boolean = { ch ->
            (groupChIds != null && groupChIds.contains(ch.id)) || ch.groupTag.equals(groupTag, ignoreCase = true)
        }
        val groupChannels = list.filter(isMember)
        val anyUnmuted = groupChannels.any { !it.busSendMutes.getOrElse(activeBus) { false } }
        val targetMute = anyUnmuted

        for (i in list.indices) {
            if (isMember(list[i])) {
                val oldCh = list[i]
                val newMutes = oldCh.busSendMutes.toMutableList().apply {
                    if (activeBus in indices) this[activeBus] = targetMute
                }
                list[i] = oldCh.copy(busSendMutes = newMutes)

                val chStr = String.format("%02d", oldCh.id)
                val busStr = String.format("%02d", activeBus + 1)
                oscClient.sendOscMessage(OscMessage("/ch/$chStr/mix/$busStr/on", listOf(if (targetMute) 0 else 1)))
            }
        }
        _channels.value = list.toList()
    }

    // Engineer Mode Actions
    fun unlockEngineerMode(pinInput: String): Boolean {
        if (pinInput == _userRole.value.engineerPin) {
            _userRole.value = _userRole.value.copy(
                type = RoleType.ENGINEER,
                isUnlocked = true
            )
            return true
        }
        return false
    }

    fun lockEngineerMode() {
        _userRole.value = _userRole.value.copy(
            type = RoleType.MUSICIAN,
            isUnlocked = false
        )
    }

    fun updateEngineerChannelGain(channelId: Int, gainVal: Float) {
        val list = _channels.value.toMutableList()
        val idx = list.indexOfFirst { it.id == channelId }
        if (idx != -1) {
            val ch = list[idx].copy(inputGain = gainVal)
            list[idx] = ch
            _channels.value = list

            val chStr = String.format("%02d", channelId)
            oscClient.sendOscMessage(OscMessage("/headamp/$chStr/gain", listOf(gainVal)))
        }
    }

    fun toggleEngineerPhantomPower(channelId: Int) {
        val list = _channels.value.toMutableList()
        val idx = list.indexOfFirst { it.id == channelId }
        if (idx != -1) {
            val new48V = !list[idx].phantomPower
            val ch = list[idx].copy(phantomPower = new48V)
            list[idx] = ch
            _channels.value = list

            val chStr = String.format("%02d", channelId)
            oscClient.sendOscMessage(OscMessage("/headamp/$chStr/phantom", listOf(if (new48V) 1 else 0)))
        }
    }

    fun toggleEngineerLowCut(channelId: Int) {
        val list = _channels.value.toMutableList()
        val idx = list.indexOfFirst { it.id == channelId }
        if (idx != -1) {
            val newActive = !list[idx].lowCutActive
            val ch = list[idx].copy(lowCutActive = newActive)
            list[idx] = ch
            _channels.value = list

            val chStr = String.format("%02d", channelId)
            oscClient.sendOscMessage(OscMessage("/ch/$chStr/preamp/hpon", listOf(if (newActive) 1 else 0)))
            oscClient.sendOscMessage(OscMessage("/ch/$chStr/config/hpon", listOf(if (newActive) 1 else 0)))
            oscClient.sendOscMessage(OscMessage("/headamp/$chStr/hpon", listOf(if (newActive) 1 else 0)))
            showNotification("Channel $channelId Low Cut ${if (newActive) "ON (${ch.lowCutFreq.toInt()}Hz)" else "OFF"}")
        }
    }

    fun updateEngineerLowCutFreq(channelId: Int, freqHz: Float) {
        val list = _channels.value.toMutableList()
        val idx = list.indexOfFirst { it.id == channelId }
        if (idx != -1) {
            val clamped = freqHz.coerceIn(20f, 400f)
            val ch = list[idx].copy(lowCutFreq = clamped)
            list[idx] = ch
            _channels.value = list

            val chStr = String.format("%02d", channelId)
            oscClient.sendOscMessage(OscMessage("/ch/$chStr/preamp/hpf", listOf(clamped)))
            oscClient.sendOscMessage(OscMessage("/ch/$chStr/config/hpf", listOf(clamped)))
            oscClient.sendOscMessage(OscMessage("/headamp/$chStr/hpf", listOf(clamped)))
        }
    }

    fun updateChannelDetails(channelId: Int, name: String, color: X32Color, iconType: String) {
        val list = _channels.value.toMutableList()
        val idx = list.indexOfFirst { it.id == channelId }
        if (idx != -1) {
            val ch = list[idx].copy(name = name, color = color, iconType = iconType)
            list[idx] = ch
            _channels.value = list

            val chStr = String.format("%02d", channelId)
            oscClient.sendOscMessage(OscMessage("/ch/$chStr/config/name", listOf(name)))
            oscClient.sendOscMessage(OscMessage("/ch/$chStr/config/color", listOf(color.id)))
            oscClient.sendOscMessage(OscMessage("/ch/$chStr/config/icon", listOf(mapTypeToMixerIconId(iconType))))
        }
    }

    // Presets
    fun savePreset(presetName: String) {
        val profile = _activeProfile.value ?: return
        val busIdx = _activeBusIndex.value
        val levelsCsv = _channels.value.joinToString(",") { it.busSendLevels[busIdx].toString() }
        val pansCsv = _channels.value.joinToString(",") { it.busSendPans[busIdx].toString() }

        val entity = MixPresetEntity(
            profileId = profile.id,
            busId = busIdx + 1,
            presetName = presetName,
            channelLevelsCsv = levelsCsv,
            channelPansCsv = pansCsv
        )
        viewModelScope.launch { repository.savePreset(entity) }
    }

    fun applyPreset(preset: MixPresetEntity) {
        val levels = preset.channelLevelsCsv.split(",").mapNotNull { it.trim().toFloatOrNull() }
        val pans = preset.channelPansCsv.split(",").mapNotNull { it.trim().toFloatOrNull() }
        val busIdx = _activeBusIndex.value

        val list = _channels.value.toMutableList()
        for (i in list.indices) {
            var ch = list[i]
            if (i in levels.indices) {
                val newLevels = ch.busSendLevels.toMutableList().apply {
                    if (busIdx in indices) this[busIdx] = levels[i]
                }
                ch = ch.copy(busSendLevels = newLevels)
            }
            if (i in pans.indices) {
                val newPans = ch.busSendPans.toMutableList().apply {
                    if (busIdx in indices) this[busIdx] = pans[i]
                }
                ch = ch.copy(busSendPans = newPans)
            }
            list[i] = ch

            val chStr = String.format("%02d", ch.id)
            val busStr = String.format("%02d", busIdx + 1)
            val sendLvl = ch.busSendLevels.getOrElse(busIdx) { 0.75f }
            oscClient.sendOscMessage(OscMessage("/ch/$chStr/mix/$busStr/level", listOf(sendLvl)))
        }
        _channels.value = list.toList()
    }

    fun deletePreset(preset: MixPresetEntity) {
        viewModelScope.launch { repository.deletePreset(preset) }
    }

    // Network & Scanner triggers
    fun startNetworkScan() {
        discoveryEngine.startScan()
    }

    fun cancelNetworkScan() {
        discoveryEngine.cancelScan()
    }

    fun connectToMixer(ip: String, port: Int = 10023) {
        setManualIp(ip)
        oscClient.connectToMixer(ip, port)
        pullChannelsAndBusesFromMixer()
    }

    fun reconnectToMixer() {
        val currentInfo = connectionState.value
        val targetIp = if (currentInfo.ip.isNotBlank() && currentInfo.ip != "127.0.0.1") currentInfo.ip else savedManualIp.value
        showNotification("Reconnecting to mixer at $targetIp:${currentInfo.port}...")
        connectToMixer(targetIp, currentInfo.port)
    }

    fun startSimulatorMode() {
        oscClient.startSimulatorMode()
        pullChannelsAndBusesFromMixer()
    }

    fun pullChannelsAndBusesFromMixer() {
        viewModelScope.launch {
            if (oscClient.isSimulatorActive) {
                _channels.value = oscClient.simulator.channels.map { it.copy() }
                _buses.value = oscClient.simulator.buses.map { it.copy() }
                _activeProfile.value?.let { applyProfileGroups(it) }
            } else {
                // Pre-populate 32 channels if missing so incoming OSC updates aren't ignored
                if (_channels.value.size < 32) {
                    _channels.value = oscClient.simulator.channels.map { it.copy() }
                    _activeProfile.value?.let { applyProfileGroups(it) }
                }
                // Request live data from connected hardware mixer over OSC
                for (chId in 1..32) {
                    val chStr = String.format("%02d", chId)
                    oscClient.sendOscMessage(OscMessage("/ch/$chStr/config/name"))
                    oscClient.sendOscMessage(OscMessage("/ch/$chStr/config/color"))
                    oscClient.sendOscMessage(OscMessage("/ch/$chStr/config/icon"))
                    oscClient.sendOscMessage(OscMessage("/ch/$chStr/mix/fader"))
                    oscClient.sendOscMessage(OscMessage("/ch/$chStr/mix/on"))
                    oscClient.sendOscMessage(OscMessage("/ch/$chStr/eq/on"))
                    oscClient.sendOscMessage(OscMessage("/ch/$chStr/preamp/hpon"))
                    oscClient.sendOscMessage(OscMessage("/ch/$chStr/preamp/hpf"))
                    oscClient.sendOscMessage(OscMessage("/headamp/$chStr/gain"))
                    oscClient.sendOscMessage(OscMessage("/headamp/$chStr/phantom"))
                    for (bId in 1..4) {
                        oscClient.sendOscMessage(OscMessage("/ch/$chStr/eq/$bId/g"))
                        oscClient.sendOscMessage(OscMessage("/ch/$chStr/eq/$bId/f"))
                        oscClient.sendOscMessage(OscMessage("/ch/$chStr/eq/$bId/q"))
                    }
                    for (busId in 1..16) {
                        val busStr = String.format("%02d", busId)
                        oscClient.sendOscMessage(OscMessage("/ch/$chStr/mix/$busStr/level"))
                        oscClient.sendOscMessage(OscMessage("/ch/$chStr/mix/$busStr/on"))
                    }
                }
                for (busId in 1..16) {
                    val busStr = String.format("%02d", busId)
                    oscClient.sendOscMessage(OscMessage("/bus/$busStr/config/name"))
                    oscClient.sendOscMessage(OscMessage("/bus/$busStr/config/color"))
                    oscClient.sendOscMessage(OscMessage("/bus/$busStr/config/icon"))
                    oscClient.sendOscMessage(OscMessage("/bus/$busStr/mix/fader"))
                    oscClient.sendOscMessage(OscMessage("/bus/$busStr/mix/on"))
                }
            }
        }
    }

    private fun parseIncomingOsc(msg: OscMessage) {
        val addr = msg.address
        if (addr.startsWith("/ch/")) {
            val parts = addr.split("/")
            if (parts.size >= 3) {
                val chIdx = parts[2].toIntOrNull()?.minus(1) ?: return
                if (chIdx in _channels.value.indices) {
                    val list = _channels.value.toMutableList()
                    var ch = list[chIdx].copy()

                    if (addr.endsWith("/config/name") && msg.arguments.isNotEmpty()) {
                        ch.name = msg.arguments[0].toString()
                    } else if (addr.endsWith("/config/color") && msg.arguments.isNotEmpty()) {
                        val cId = (msg.arguments[0] as? Number)?.toInt() ?: 6
                        ch.color = X32Color.fromId(cId)
                    } else if (addr.endsWith("/config/icon") && msg.arguments.isNotEmpty()) {
                        ch.iconType = parseMixerIcon(msg.arguments[0])
                    } else if (addr.endsWith("/mix/fader") && msg.arguments.isNotEmpty()) {
                        ch.level = (msg.arguments[0] as? Number)?.toFloat() ?: ch.level
                    } else if (addr.endsWith("/mix/on") && msg.arguments.isNotEmpty()) {
                        val isOn = (msg.arguments[0] as? Number)?.toInt() == 1
                        ch.isMuted = !isOn
                    } else if (addr.endsWith("/eq/on") && msg.arguments.isNotEmpty()) {
                        val eqOn = (msg.arguments[0] as? Number)?.toInt() == 1
                        ch.eqActive = eqOn
                    } else if ((addr.endsWith("/preamp/hpon") || addr.endsWith("/config/hpon")) && msg.arguments.isNotEmpty()) {
                        val hpOn = (msg.arguments[0] as? Number)?.toInt() == 1
                        ch.lowCutActive = hpOn
                    } else if ((addr.endsWith("/preamp/hpf") || addr.endsWith("/config/hpf")) && msg.arguments.isNotEmpty()) {
                        val hpFreq = (msg.arguments[0] as? Number)?.toFloat() ?: 80f
                        ch.lowCutFreq = hpFreq
                    } else if (addr.contains("/eq/") && msg.arguments.isNotEmpty()) {
                        val bandNum = parts.getOrNull(4)?.toIntOrNull()
                        if (bandNum != null && bandNum in 1..4) {
                            val bIdx = bandNum - 1
                            val bands = ch.eqBands.toMutableList()
                            if (bIdx in bands.indices) {
                                val paramVal = (msg.arguments[0] as? Number)?.toFloat() ?: 0f
                                if (addr.endsWith("/g")) bands[bIdx] = bands[bIdx].copy(gainDb = paramVal)
                                else if (addr.endsWith("/f")) bands[bIdx] = bands[bIdx].copy(freqHz = paramVal)
                                else if (addr.endsWith("/q")) bands[bIdx] = bands[bIdx].copy(qFactor = paramVal)
                                ch.eqBands = bands
                            }
                        }
                    } else if (parts.size >= 5 && parts[3] == "mix") {
                        val busIdx = parts[4].toIntOrNull()?.minus(1)
                        if (busIdx != null && busIdx in 0..15) {
                            if (addr.endsWith("/level") && msg.arguments.isNotEmpty()) {
                                val floatVal = (msg.arguments[0] as? Number)?.toFloat() ?: ch.busSendLevels.getOrElse(busIdx) { 0.75f }
                                val newLevels = ch.busSendLevels.toMutableList().apply { if (busIdx in indices) this[busIdx] = floatVal }
                                ch = ch.copy(busSendLevels = newLevels)
                            } else if (addr.endsWith("/on") && msg.arguments.isNotEmpty()) {
                                val isOn = (msg.arguments[0] as? Number)?.toInt() == 1
                                val newMutes = ch.busSendMutes.toMutableList().apply { if (busIdx in indices) this[busIdx] = !isOn }
                                ch = ch.copy(busSendMutes = newMutes)
                            } else if (addr.endsWith("/pan") && msg.arguments.isNotEmpty()) {
                                val floatVal = (msg.arguments[0] as? Number)?.toFloat() ?: ch.busSendPans.getOrElse(busIdx) { 0.5f }
                                val newPans = ch.busSendPans.toMutableList().apply { if (busIdx in indices) this[busIdx] = floatVal }
                                ch = ch.copy(busSendPans = newPans)
                            }
                        }
                    }
                    list[chIdx] = ch
                    _channels.value = list.toList()
                }
            }
        } else if (addr.startsWith("/bus/")) {
            val parts = addr.split("/")
            if (parts.size >= 3) {
                val busIdx = parts[2].toIntOrNull()?.minus(1) ?: return
                if (busIdx in _buses.value.indices) {
                    val list = _buses.value.toMutableList()
                    val bus = list[busIdx].copy()

                    if (addr.endsWith("/config/name") && msg.arguments.isNotEmpty()) {
                        bus.name = msg.arguments[0].toString()
                    } else if (addr.endsWith("/config/color") && msg.arguments.isNotEmpty()) {
                        val cId = (msg.arguments[0] as? Number)?.toInt() ?: 6
                        bus.color = X32Color.fromId(cId)
                    } else if (addr.endsWith("/mix/fader") && msg.arguments.isNotEmpty()) {
                        val fVal = (msg.arguments[0] as? Number)?.toFloat() ?: bus.masterLevel
                        val maxLvl = bus.getMaxFaderLevel()
                        bus.masterLevel = if (bus.limiterActive) fVal.coerceAtMost(maxLvl) else fVal
                    } else if (addr.endsWith("/mix/on") && msg.arguments.isNotEmpty()) {
                        val isOn = (msg.arguments[0] as? Number)?.toInt() == 1
                        bus.masterMute = !isOn
                    } else if (addr.endsWith("/config/tap") && msg.arguments.isNotEmpty()) {
                        val tapIdx = (msg.arguments[0] as? Number)?.toInt() ?: 0
                        bus.sendTapMode = BusTapMode.entries.getOrElse(tapIdx) { bus.sendTapMode }
                    } else if (addr.endsWith("/config/link") && msg.arguments.isNotEmpty()) {
                        val isL = (msg.arguments[0] as? Number)?.toInt() == 1
                        bus.isStereoLinked = isL
                    } else if (addr.endsWith("/eq/on") && msg.arguments.isNotEmpty()) {
                        val eqOn = (msg.arguments[0] as? Number)?.toInt() == 1
                        bus.eqActive = eqOn
                    } else if (addr.contains("/eq/") && msg.arguments.isNotEmpty()) {
                        val bandNum = parts.getOrNull(4)?.toIntOrNull()
                        if (bandNum != null && bandNum in 1..6) {
                            val bIdx = bandNum - 1
                            val bands = bus.eqBands.toMutableList()
                            if (bIdx in bands.indices) {
                                val paramVal = (msg.arguments[0] as? Number)?.toFloat() ?: 0f
                                if (addr.endsWith("/g")) bands[bIdx] = bands[bIdx].copy(gainDb = paramVal)
                                else if (addr.endsWith("/f")) bands[bIdx] = bands[bIdx].copy(freqHz = paramVal)
                                else if (addr.endsWith("/q")) bands[bIdx] = bands[bIdx].copy(qFactor = paramVal)
                                bus.eqBands = bands
                            }
                        }
                    } else if (addr.endsWith("/dyn/on") && msg.arguments.isNotEmpty()) {
                        bus.limiterActive = (msg.arguments[0] as? Number)?.toInt() == 1
                        if (bus.limiterActive) {
                            bus.masterLevel = bus.masterLevel.coerceAtMost(bus.getMaxFaderLevel())
                        }
                    } else if (addr.endsWith("/dyn/thresh") && msg.arguments.isNotEmpty()) {
                        bus.limiterThresholdDb = (msg.arguments[0] as? Number)?.toFloat() ?: bus.limiterThresholdDb
                        if (bus.limiterActive) {
                            bus.masterLevel = bus.masterLevel.coerceAtMost(bus.getMaxFaderLevel())
                        }
                    } else if (addr.endsWith("/delay/time") && msg.arguments.isNotEmpty()) {
                        bus.outputDelayMs = (msg.arguments[0] as? Number)?.toFloat() ?: bus.outputDelayMs
                    } else if (addr.endsWith("/preamp/invert") && msg.arguments.isNotEmpty()) {
                        bus.phaseInverted = (msg.arguments[0] as? Number)?.toInt() == 1
                    }
                    list[busIdx] = bus
                    _buses.value = list
                }
            }
        } else if (addr.startsWith("/headamp/")) {
            val parts = addr.split("/")
            if (parts.size >= 3) {
                val chIdx = parts[2].toIntOrNull()?.minus(1) ?: return
                if (chIdx in _channels.value.indices) {
                    val list = _channels.value.toMutableList()
                    var ch = list[chIdx].copy()
                    if (addr.endsWith("/gain") && msg.arguments.isNotEmpty()) {
                        ch.inputGain = (msg.arguments[0] as? Number)?.toFloat() ?: ch.inputGain
                    } else if (addr.endsWith("/phantom") && msg.arguments.isNotEmpty()) {
                        ch.phantomPower = (msg.arguments[0] as? Number)?.toInt() == 1
                    } else if (addr.endsWith("/hpon") && msg.arguments.isNotEmpty()) {
                        ch.lowCutActive = (msg.arguments[0] as? Number)?.toInt() == 1
                    } else if (addr.endsWith("/hpf") && msg.arguments.isNotEmpty()) {
                        ch.lowCutFreq = (msg.arguments[0] as? Number)?.toFloat() ?: ch.lowCutFreq
                    }
                    list[chIdx] = ch
                    _channels.value = list.toList()
                }
            }
        }
    }

    private fun parseMixerIcon(rawIcon: Any): String {
        return when (rawIcon) {
            is Number -> {
                val id = rawIcon.toInt()
                when (id) {
                    in 1..12, in 50..55 -> "DRUM"
                    in 13..17 -> "BASS"
                    in 18..22, in 56..60 -> "GUITAR"
                    in 23..28, in 61..65 -> "KEYBOARD"
                    in 29..36, in 66..70 -> "MIC"
                    in 37..40 -> "HORN"
                    in 41..45 -> "FX"
                    in 46..49 -> "AUDIO"
                    else -> "MIC"
                }
            }
            else -> {
                val str = rawIcon.toString().uppercase()
                when {
                    str.contains("DRUM") || str.contains("KICK") || str.contains("SNARE") || str.contains("PERC") -> "DRUM"
                    str.contains("BASS") -> "BASS"
                    str.contains("GTR") || str.contains("GUITAR") -> "GUITAR"
                    str.contains("KEY") || str.contains("PIANO") || str.contains("SYNTH") -> "KEYBOARD"
                    str.contains("MIC") || str.contains("VOX") || str.contains("VOCAL") -> "MIC"
                    str.contains("SAX") || str.contains("HORN") || str.contains("BRASS") -> "HORN"
                    str.contains("FX") || str.contains("REV") || str.contains("DEL") -> "FX"
                    else -> "MIC"
                }
            }
        }
    }

    private fun mapTypeToMixerIconId(iconType: String): Int {
        return when (iconType.uppercase()) {
            "DRUM" -> 1
            "BASS" -> 13
            "GUITAR" -> 18
            "KEYBOARD" -> 23
            "MIC" -> 29
            "HORN" -> 37
            "FX" -> 41
            "AUDIO" -> 46
            else -> 29
        }
    }

    override fun onCleared() {
        super.onCleared()
        oscClient.release()
    }
}
