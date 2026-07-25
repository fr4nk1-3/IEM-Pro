package com.example.network

import com.example.model.ChannelState
import com.example.model.MixBusState
import com.example.model.X32Color
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class VirtualMixerSimulator {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    private val _incomingOscFlow = MutableSharedFlow<OscMessage>(extraBufferCapacity = 128)
    val incomingOscFlow: SharedFlow<OscMessage> = _incomingOscFlow

    val channels = MutableList(32) { index ->
        val chNum = index + 1
        val (defaultName, color, icon) = getDefaultChannelInfo(chNum)
        ChannelState(
            id = chNum,
            name = defaultName,
            color = color,
            iconType = icon,
            level = 0.75f - (index % 5) * 0.05f,
            pan = 0.5f,
            isMuted = false,
            isSolo = false,
            peakMeter = 0.2f + Random.nextFloat() * 0.4f,
            busSendLevels = MutableList(16) { 0.7f - (index % 4) * 0.1f },
            busSendPans = MutableList(16) { 0.5f },
            busSendMutes = MutableList(16) { false },
            isFavorite = chNum in listOf(1, 2, 5, 8, 10),
            groupTag = when (chNum) {
                in 1..4 -> "Drums"
                5 -> "Bass"
                6, 7 -> "Guitars"
                8, 9 -> "Keys"
                10, 11, 12 -> "Vocals"
                in 13..14 -> "Horns"
                else -> "FX / Aux"
            },
            inputGain = 0.5f,
            phantomPower = chNum in listOf(1, 2, 10, 11),
            eqActive = true,
            gateActive = chNum in 1..4,
            compActive = true
        )
    }

    val buses = MutableList(16) { index ->
        val busNum = index + 1
        MixBusState(
            id = busNum,
            name = when (busNum) {
                1 -> "Bus 1 (Drums IEM)"
                2 -> "Bus 2 (Bass IEM)"
                3 -> "Bus 3 (Keys IEM)"
                4 -> "Bus 4 (Gtr IEM)"
                5 -> "Bus 5 (Lead Vox IEM)"
                6 -> "Bus 6 (BG Vox IEM)"
                7 -> "Bus 7 (Horn IEM)"
                8 -> "Bus 8 (Guest IEM)"
                in 9..12 -> "Bus $busNum (Wedge $busNum)"
                13 -> "FX 1 (Rev Verb)"
                14 -> "FX 2 (Delay)"
                else -> "Aux Bus $busNum"
            },
            color = when (busNum % 6) {
                1 -> X32Color.CYAN
                2 -> X32Color.BLUE
                3 -> X32Color.MAGENTA
                4 -> X32Color.YELLOW
                5 -> X32Color.GREEN
                else -> X32Color.RED
            },
            masterLevel = 0.8f,
            masterMute = false,
            isStereoLinked = (busNum % 2 == 1 && busNum <= 8),
            linkedBusId = if (busNum % 2 == 1 && busNum <= 8) busNum + 1 else null
        )
    }

    private var meterJob: Job? = null
    private var simulatedTick = 0L

    init {
        startMeterSimulation()
    }

    private fun startMeterSimulation() {
        meterJob?.cancel()
        meterJob = scope.launch {
            while (isActive) {
                delay(80) // 12.5 FPS meter updates
                simulatedTick++
                val timeSec = simulatedTick * 0.08
                
                for (ch in channels) {
                    val beat = abs(sin(timeSec * 2.5 + ch.id)).toFloat()
                    val noise = Random.nextFloat() * 0.15f
                    val rawMeter = if (ch.isMuted) 0f else (beat * 0.7f + noise).coerceIn(0f, 1f)
                    ch.peakMeter = rawMeter
                }
            }
        }
    }

    fun handleOutgoingOsc(msg: OscMessage) {
        val addr = msg.address
        when {
            addr == "/xinfo" -> {
                scope.launch {
                    _incomingOscFlow.emit(
                        OscMessage(
                            "/xinfo",
                            listOf("192.168.1.100", "BEHRINGER X32 LIVE", "X32 Console", "V4.09")
                        )
                    )
                }
            }
            addr.startsWith("/ch/") -> {
                val parts = addr.split("/")
                if (parts.size >= 3) {
                    val chIdx = parts[2].toIntOrNull()?.minus(1) ?: return
                    if (chIdx in 0 until 32) {
                        val ch = channels[chIdx]
                        if (addr.contains("/mix/") && addr.contains("/level")) {
                            val busIdx = (parts[4].toIntOrNull()?.minus(1) ?: 0).coerceIn(0, 15)
                            val floatVal = (msg.arguments.firstOrNull() as? Float)
                                ?: (msg.arguments.firstOrNull() as? Number)?.toFloat()
                                ?: 0f
                            val newLevels = ch.busSendLevels.toMutableList()
                            newLevels[busIdx] = floatVal
                            channels[chIdx] = ch.copy(busSendLevels = newLevels)
                        } else if (addr.contains("/mix/fader")) {
                            val floatVal = (msg.arguments.firstOrNull() as? Float) ?: 0f
                            ch.level = floatVal
                        } else if (addr.contains("/mix/on")) {
                            val intVal = (msg.arguments.firstOrNull() as? Number)?.toInt() ?: 1
                            ch.isMuted = (intVal == 0)
                        } else if (addr.contains("/mix/") && addr.contains("/pan")) {
                            val busIdx = (parts[4].toIntOrNull()?.minus(1) ?: 0).coerceIn(0, 15)
                            val floatVal = (msg.arguments.firstOrNull() as? Float) ?: 0.5f
                            val newPans = ch.busSendPans.toMutableList()
                            newPans[busIdx] = floatVal
                            channels[chIdx] = ch.copy(busSendPans = newPans)
                        }
                    }
                }
            }
            addr.startsWith("/bus/") -> {
                val parts = addr.split("/")
                if (parts.size >= 3) {
                    val busIdx = parts[2].toIntOrNull()?.minus(1) ?: return
                    if (busIdx in 0 until 16) {
                        val bus = buses[busIdx]
                        if (addr.endsWith("/mix/fader")) {
                            val floatVal = (msg.arguments.firstOrNull() as? Float) ?: 0.8f
                            bus.masterLevel = floatVal
                        } else if (addr.endsWith("/mix/on")) {
                            val intVal = (msg.arguments.firstOrNull() as? Number)?.toInt() ?: 1
                            bus.masterMute = (intVal == 0)
                        }
                    }
                }
            }
        }
    }

    private fun getDefaultChannelInfo(chNum: Int): Triple<String, X32Color, String> {
        return when (chNum) {
            1 -> Triple("Kick In", X32Color.CYAN, "DRUM")
            2 -> Triple("Kick Out", X32Color.CYAN, "DRUM")
            3 -> Triple("Snare Top", X32Color.CYAN, "DRUM")
            4 -> Triple("Snare Bot", X32Color.CYAN, "DRUM")
            5 -> Triple("Hi-Hat", X32Color.CYAN, "DRUM")
            6 -> Triple("Rack Tom 1", X32Color.CYAN, "DRUM")
            7 -> Triple("Rack Tom 2", X32Color.CYAN, "DRUM")
            8 -> Triple("Floor Tom", X32Color.CYAN, "DRUM")
            9 -> Triple("Overhead L", X32Color.CYAN, "DRUM")
            10 -> Triple("Overhead R", X32Color.CYAN, "DRUM")
            11 -> Triple("Bass Line", X32Color.BLUE, "BASS")
            12 -> Triple("Bass Mic", X32Color.BLUE, "BASS")
            13 -> Triple("Acoustic Gtr", X32Color.YELLOW, "GUITAR")
            14 -> Triple("Electric Gtr L", X32Color.YELLOW, "GUITAR")
            15 -> Triple("Electric Gtr R", X32Color.YELLOW, "GUITAR")
            16 -> Triple("Keys Piano L", X32Color.MAGENTA, "KEYBOARD")
            17 -> Triple("Keys Piano R", X32Color.MAGENTA, "KEYBOARD")
            18 -> Triple("Synth Pad", X32Color.MAGENTA, "KEYBOARD")
            19 -> Triple("Organ", X32Color.MAGENTA, "KEYBOARD")
            20 -> Triple("Brass Trpt", X32Color.RED, "HORN")
            21 -> Triple("Saxophone", X32Color.RED, "HORN")
            22 -> Triple("Lead Vox", X32Color.WHITE, "MIC")
            23 -> Triple("BG Vox High", X32Color.WHITE, "MIC")
            24 -> Triple("BG Vox Mid", X32Color.WHITE, "MIC")
            25 -> Triple("BG Vox Low", X32Color.WHITE, "MIC")
            26 -> Triple("Pastor / Talk", X32Color.WHITE, "MIC")
            27 -> Triple("Click Track", X32Color.GREEN, "AUDIO")
            28 -> Triple("Guide Track", X32Color.GREEN, "AUDIO")
            29 -> Triple("Ambient Mic L", X32Color.WHITE, "MIC")
            30 -> Triple("Ambient Mic R", X32Color.WHITE, "MIC")
            31 -> Triple("Reverb Return", X32Color.YELLOW, "FX")
            32 -> Triple("Delay Return", X32Color.YELLOW, "FX")
            else -> Triple("Ch $chNum", X32Color.OFF, "CHANNEL")
        }
    }

    fun stop() {
        meterJob?.cancel()
    }
}
