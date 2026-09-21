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
            groupTag = "All",
            inputGain = 0.5f,
            phantomPower = chNum in listOf(1, 2, 10, 11),
            lowCutActive = chNum !in listOf(1, 11), // Low cut active on all instruments/vocals except Sub Kick and Bass
            lowCutFreq = when (chNum) {
                in 22..26 -> 120f // Vocals HPF 120Hz
                in 13..16 -> 100f // Guitars HPF 100Hz
                3, 4 -> 80f // Snare HPF 80Hz
                else -> 80f
            },
            eqActive = true,
            eqBands = getPresetChannelEqBands(chNum),
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
            masterLevel = 0.60f, // Default -6dBFS limiter cap
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

                    val dynamicLevel: Float = when (ch.id) {
                        in 1..8 -> { // Drums
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
                        in 9..10 -> { // Bass: Steady rhythmic bass line
                            val bassPulse = (sin(timeSec * 4.0).toFloat() * 0.35f + 0.5f) + (Random.nextFloat() * 0.08f)
                            bassPulse.coerceIn(0.15f, 0.82f)
                        }
                        in 19..24 -> { // Vocals: Dynamic swells and pauses
                            val phrase = (sin(timeSec * 0.8 + ch.id).toFloat() * 0.5f + 0.4f)
                            if (phrase > 0.15f) {
                                val vibrato = abs(sin(timeSec * 6.0)).toFloat() * 0.2f
                                (phrase * 0.7f + vibrato + Random.nextFloat() * 0.1f).coerceIn(0.1f, 0.88f)
                            } else {
                                0.02f // Breathing pause
                            }
                        }
                        in 11..14 -> { // Guitars: Strumming chords
                            val strum = abs(sin(timeSec * 2.5 + ch.id * 0.5)).toFloat() * 0.55f + 0.2f
                            (strum + Random.nextFloat() * 0.08f).coerceIn(0.1f, 0.85f)
                        }
                        in 15..18 -> { // Keys: Piano / Synth pads
                            val pad = (sin(timeSec * 1.5 + ch.id).toFloat() * 0.3f + 0.45f)
                            (pad + Random.nextFloat() * 0.06f).coerceIn(0.1f, 0.78f)
                        }
                        in 25..28 -> { // Horns: Brass stabs
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
                        } else if (addr.endsWith("/preamp/hpon") || addr.endsWith("/config/hpon")) {
                            val pVal = (msg.arguments.firstOrNull() as? Number)?.toInt() ?: 0
                            ch.lowCutActive = (pVal == 1)
                        } else if (addr.endsWith("/preamp/hpf") || addr.endsWith("/config/hpf")) {
                            val fVal = (msg.arguments.firstOrNull() as? Number)?.toFloat() ?: 80f
                            ch.lowCutFreq = fVal
                        } else if (addr.endsWith("/eq/on")) {
                            val eqVal = (msg.arguments.firstOrNull() as? Number)?.toInt() ?: 1
                            ch.eqActive = (eqVal == 1)
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
                        } else if (addr.endsWith("/hpon")) {
                            val pVal = (msg.arguments.firstOrNull() as? Number)?.toInt() ?: 0
                            ch.lowCutActive = (pVal == 1)
                        } else if (addr.endsWith("/hpf")) {
                            val fVal = (msg.arguments.firstOrNull() as? Number)?.toFloat() ?: 80f
                            ch.lowCutFreq = fVal
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
                            bus.masterLevel = if (bus.limiterActive) floatVal.coerceAtMost(bus.getMaxFaderLevel()) else floatVal
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
                            if (bus.limiterActive) {
                                bus.masterLevel = bus.masterLevel.coerceAtMost(bus.getMaxFaderLevel())
                            }
                        } else if (addr.endsWith("/dyn/thresh")) {
                            val thresh = (msg.arguments.firstOrNull() as? Number)?.toFloat() ?: -6f
                            bus.limiterThresholdDb = thresh
                            if (bus.limiterActive) {
                                bus.masterLevel = bus.masterLevel.coerceAtMost(bus.getMaxFaderLevel())
                            }
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

    private fun getPresetChannelEqBands(chNum: Int): List<com.example.model.BusEqBand> {
        return when (chNum) {
            1, 2 -> listOf( // Kick Drum: Low boost, mid scoop, high beater click
                com.example.model.BusEqBand(1, "Low Shelf", 60f, 4.5f, 0.8f),
                com.example.model.BusEqBand(2, "Low Mid", 350f, -6.0f, 1.8f),
                com.example.model.BusEqBand(3, "High Mid", 3200f, 3.5f, 1.4f),
                com.example.model.BusEqBand(4, "High Shelf", 9000f, -2.0f, 0.7f)
            )
            3, 4 -> listOf( // Snare: Body punch, boxy dip, sizzle boost
                com.example.model.BusEqBand(1, "Low Shelf", 150f, 2.0f, 0.9f),
                com.example.model.BusEqBand(2, "Low Mid", 600f, -3.5f, 1.5f),
                com.example.model.BusEqBand(3, "High Mid", 4500f, 4.0f, 1.2f),
                com.example.model.BusEqBand(4, "High Shelf", 10000f, 2.5f, 0.7f)
            )
            11, 12 -> listOf( // Bass Guitar: Sub warmth, mid cut, attack
                com.example.model.BusEqBand(1, "Low Shelf", 90f, 3.0f, 0.8f),
                com.example.model.BusEqBand(2, "Low Mid", 400f, -4.5f, 1.6f),
                com.example.model.BusEqBand(3, "High Mid", 1800f, 2.5f, 1.5f),
                com.example.model.BusEqBand(4, "High Shelf", 6000f, -5.0f, 0.7f)
            )
            13, 14, 15 -> listOf( // Guitars: Low cut, bite, sparkle
                com.example.model.BusEqBand(1, "Low Shelf", 120f, -2.0f, 0.7f),
                com.example.model.BusEqBand(2, "Low Mid", 750f, -2.5f, 1.2f),
                com.example.model.BusEqBand(3, "High Mid", 2800f, 3.0f, 1.3f),
                com.example.model.BusEqBand(4, "High Shelf", 8500f, 1.5f, 0.7f)
            )
            22, 23, 24, 25, 26 -> listOf( // Vocals: High-pass warmth, mud cleanup, presence, air
                com.example.model.BusEqBand(1, "Low Shelf", 100f, -3.0f, 0.7f),
                com.example.model.BusEqBand(2, "Low Mid", 450f, -2.5f, 1.4f),
                com.example.model.BusEqBand(3, "High Mid", 3500f, 3.5f, 1.2f),
                com.example.model.BusEqBand(4, "High Shelf", 11000f, 4.0f, 0.7f)
            )
            else -> listOf(
                com.example.model.BusEqBand(1, "Low Cut", 80f, 0f, 0.7f),
                com.example.model.BusEqBand(2, "Low Mid", 250f, 0f, 1.0f),
                com.example.model.BusEqBand(3, "High Mid", 2500f, 0f, 1.0f),
                com.example.model.BusEqBand(4, "High", 8000f, 0f, 0.7f)
            )
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
