package com.example.model

import kotlin.math.log10
import kotlin.math.pow

fun defaultChannelEqBands(): List<BusEqBand> = listOf(
    BusEqBand(1, "Low", 100f, 0f, 0.7f),
    BusEqBand(2, "Low Mid", 400f, 0f, 1.0f),
    BusEqBand(3, "High Mid", 2500f, 0f, 1.0f),
    BusEqBand(4, "High", 10000f, 0f, 0.7f)
)

data class ChannelState(
    val id: Int, // 1..32
    var name: String,
    var color: X32Color = X32Color.CYAN,
    var iconType: String = "MIC",
    var level: Float = 0.75f, // Main fader level 0.0 .. 1.0
    var pan: Float = 0.5f, // 0.0 (100% L) .. 0.5 (Center) .. 1.0 (100% R)
    var isMuted: Boolean = false,
    var isSolo: Boolean = false,
    var peakMeter: Float = 0.0f, // 0.0 .. 1.0
    val busSendLevels: List<Float> = List(16) { 0.75f }, // Per bus send levels
    val busSendPans: List<Float> = List(16) { 0.5f }, // Per bus send pans
    val busSendMutes: List<Boolean> = List(16) { false }, // Per bus send mutes
    var isFavorite: Boolean = false,
    var groupTag: String = "All", // Custom MCA/group tag e.g. "Drums", "Vocals"
    
    // Engineer Processing Parameters
    var inputGain: Float = 0.5f, // 0.0 .. 1.0
    var phantomPower: Boolean = false, // +48V
    var lowCutActive: Boolean = false, // Dedicated Console Preamp Low Cut / HPF filter
    var lowCutFreq: Float = 80f, // Dedicated Low Cut cutoff frequency (20Hz .. 400Hz)
    var eqActive: Boolean = true,
    var eqBands: List<BusEqBand> = defaultChannelEqBands(),
    var gateActive: Boolean = false,
    var compActive: Boolean = true,
    var mainLrSend: Boolean = true
) {
    // Utility for dB display calculation
    fun getSendDbLevel(busIndex: Int): String {
        val faderVal = busSendLevels.getOrElse(busIndex) { 0.75f }
        return faderToDbString(faderVal)
    }

    companion object {
        fun faderToDb(valFloat: Float): Float {
            if (valFloat <= 0.001f) return -90f
            // X32 fader scale approximation: 0.75 -> 0 dB, 1.0 -> +10 dB, 0.5 -> -10 dB, 0.25 -> -30 dB
            return when {
                valFloat >= 0.75f -> (valFloat - 0.75f) / 0.25f * 10f
                valFloat >= 0.50f -> -10f + (valFloat - 0.50f) / 0.25f * 10f
                valFloat >= 0.25f -> -30f + (valFloat - 0.25f) / 0.25f * 20f
                else -> -60f + (valFloat / 0.25f) * 30f
            }
        }

        fun dbToFader(db: Float): Float {
            return when {
                db >= 0f -> (0.75f + (db / 10f) * 0.25f).coerceIn(0f, 1f)
                db >= -10f -> (0.50f + ((db + 10f) / 10f) * 0.25f).coerceIn(0f, 1f)
                db >= -30f -> (0.25f + ((db + 30f) / 20f) * 0.25f).coerceIn(0f, 1f)
                db >= -60f -> (((db + 60f) / 30f) * 0.25f).coerceIn(0f, 1f)
                else -> 0f
            }
        }

        fun faderToDbString(valFloat: Float): String {
            if (valFloat <= 0.001f) return "-∞ dB"
            val db = faderToDb(valFloat)
            return if (db > 0f) String.format("+%.1f dB", db) else String.format("%.1f dB", db)
        }
    }
}
