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
                in 1..8 -> "Drums"
                in 9..10 -> "Bass"
                in 11..14 -> "Guitars"
                in 15..18 -> "Keys"
                in 19..24 -> "Vocals"
                in 25..28 -> "Horns"
                else -> "FX"
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
                delay(50) // 20 FPS responsive real-time VU meter updates
                simulatedTick++
                val timeSec = simulatedTick * 0.05
                val beat4 = (timeSec * 2.0).rem(4.0) // 120 BPM tempo

                for (ch in channels) {
                    if (ch.isMuted) {
                        ch.peakMeter = 0f
                        continue
                    }

                    val dynamicLevel: Float = when (ch.groupTag) {
                        "Drums" -> {
                            when (ch.id) {
                                1 -> { // Kick Drum: Strong pulse on beats 1 & 3
                                    val kickBeat = (beat4 % 2.0)
                                    if (kickBeat < 0.25) (0.85f - (kickBeat * 2.5f).toFloat()).coerceIn(0.1f, 0.88f) else 0.08f
                                }
                                2 -> { // Snare: Sharp snap on beats 2 & 4
                                    val snareBeat = ((beat4 + 1.0) % 2.0)
                                    if (snareBeat < 0.2) (0.92f - (snareBeat * 3.5f).toFloat()).coerceIn(0.08f, 0.95f) else 0.05f
                                }
                                3, 4 -> { // Hi-Hat / Ride: 8th/16th groove
                                    val hat = abs(sin(timeSec * 12.0)).toFloat() * 0.45f + 0.15f
                                    hat + Random.nextFloat() * 0.1f
                                }
                                else -> { // Toms / Overhead
                                    val tom = abs(sin(timeSec * 3.0 + ch.id)).toFloat() * 0.55f + 0.1f
                                    tom + Random.nextFloat() * 0.1f
                                }
                            }
                        }
                        "Bass" -> { // Steady rhythmic bass line
                            val bassPulse = (sin(timeSec * 4.0).toFloat() * 0.35f + 0.5f) + (Random.nextFloat() * 0.08f)
                            bassPulse.coerceIn(0.15f, 0.82f)
                        }
                        "Vocals" -> { // Vocal phrasing with dynamic swells and pauses
                            val phrase = (sin(timeSec * 0.8 + ch.id).toFloat() * 0.5f + 0.4f)
                            if (phrase > 0.15f) {
                                val vibrato = abs(sin(timeSec * 6.0)).toFloat() * 0.2f
                                (phrase * 0.7f + vibrato + Random.nextFloat() * 0.1f).coerceIn(0.1f, 0.88f)
                            } else {
                                0.02f // Breathing pause
                            }
                        }
                        "Guitars" -> { // Strumming chords
                            val strum = abs(sin(timeSec * 2.5 + ch.id * 0.5)).toFloat() * 0.55f + 0.2f
                            (strum + Random.nextFloat() * 0.08f).coerceIn(0.1f, 0.85f)
                        }
                        "Keys" -> { // Piano / Synth pads
                            val pad = (sin(timeSec * 1.5 + ch.id).toFloat() * 0.3f + 0.45f)
                            (pad + Random.nextFloat() * 0.06f).coerceIn(0.1f, 0.78f)
                        }
                        "Horns" -> { // Brass stabs
                            val stab = abs(sin(timeSec * 1.8 + ch.id)).toFloat()
                            if (stab > 0.6f) (stab * 0.85f + Random.nextFloat() * 0.1f).coerceIn(0.2f, 0.90f) else 0.05f
                        }
                        else -> { // FX / Aux
                            val ambient = abs(sin(timeSec * 1.0 + ch.id)).toFloat() * 0.4f + 0.1f
                            ambient.coerceIn(0.05f, 0.65f)
                        }
                    }

                    // Apply input gain scale
                    val gainedLevel = (dynamicLevel * (0.5f + ch.inputGain * 0.8f)).coerceIn(0f, 1f)
                    ch.peakMeter = gainedLevel
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
                        } else if (addr.endsWith("/config/name")) {
                            val newName = (msg.arguments.firstOrNull() as? String) ?: ch.name
                            ch.name = newName
                        } else if (addr.endsWith("/config/color")) {
                            val cId = (msg.arguments.firstOrNull() as? Number)?.toInt() ?: 1
                            ch.color = X32Color.entries.find { it.id == cId } ?: ch.color
                        } else if (addr.endsWith("/config/icon")) {
                            val newIcon = (msg.arguments.firstOrNull() as? String) ?: ch.iconType
                            ch.iconType = newIcon
                        }
                    }
                }
            }
            addr.startsWith("/headamp/") -> {
                val parts = addr.split("/")
                if (parts.size >= 3) {
                    val chIdx = parts[2].toIntOrNull()?.minus(1) ?: return
                    if (chIdx in 0 until 32) {
                        val ch = channels[chIdx]
                        if (addr.endsWith("/gain")) {
                            val gVal = (msg.arguments.firstOrNull() as? Number)?.toFloat() ?: 0.5f
                            ch.inputGain = gVal
                        } else if (addr.endsWith("/phantom")) {
                            val pVal = (msg.arguments.firstOrNull() as? Number)?.toInt() ?: 0
                            ch.phantomPower = (pVal == 1)
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
                        } else if (addr.endsWith("/config/name")) {
                            val newName = (msg.arguments.firstOrNull() as? String) ?: bus.name
                            bus.name = newName
                        } else if (addr.endsWith("/config/color")) {
                            val cId = (msg.arguments.firstOrNull() as? Number)?.toInt() ?: 1
                            bus.color = X32Color.entries.find { it.id == cId } ?: bus.color
                        } else if (addr.endsWith("/config/tap")) {
                            val tapIdx = (msg.arguments.firstOrNull() as? Number)?.toInt() ?: 0
                            bus.sendTapMode = com.example.model.BusTapMode.entries.getOrElse(tapIdx) { bus.sendTapMode }
                        } else if (addr.endsWith("/config/link")) {
                            val linkVal = (msg.arguments.firstOrNull() as? Number)?.toInt() ?: 0
                            bus.isStereoLinked = (linkVal == 1)
                        } else if (addr.endsWith("/eq/on")) {
                            val eqVal = (msg.arguments.firstOrNull() as? Number)?.toInt() ?: 1
                            bus.eqActive = (eqVal == 1)
                        } else if (addr.contains("/eq/")) {
                            val bandNum = parts.getOrNull(4)?.toIntOrNull()
                            if (bandNum != null && bandNum in 1..6) {
                                val bIdx = bandNum - 1
                                val bands = bus.eqBands.toMutableList()
                                if (bIdx in bands.indices) {
                                    val paramVal = (msg.arguments.firstOrNull() as? Number)?.toFloat() ?: 0f
                                    val currentB = bands[bIdx]
                                    if (addr.endsWith("/g")) {
                                        bands[bIdx] = currentB.copy(gainDb = paramVal)
                                    } else if (addr.endsWith("/f")) {
                                        bands[bIdx] = currentB.copy(freqHz = paramVal)
                                    } else if (addr.endsWith("/q")) {
                                        bands[bIdx] = currentB.copy(qFactor = paramVal)
                                    }
                                    bus.eqBands = bands
                                }
                            }
                        } else if (addr.endsWith("/dyn/on")) {
                            val limVal = (msg.arguments.firstOrNull() as? Number)?.toInt() ?: 1
                            bus.limiterActive = (limVal == 1)
                        } else if (addr.endsWith("/dyn/thresh")) {
                            val thresh = (msg.arguments.firstOrNull() as? Number)?.toFloat() ?: -6f
                            bus.limiterThresholdDb = thresh
                        } else if (addr.endsWith("/delay/time")) {
                            val delMs = (msg.arguments.firstOrNull() as? Number)?.toFloat() ?: 0f
                            bus.outputDelayMs = delMs
                        } else if (addr.endsWith("/preamp/invert")) {
                            val invVal = (msg.arguments.firstOrNull() as? Number)?.toInt() ?: 0
                            bus.phaseInverted = (invVal == 1)
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
