package com.example.model

import kotlin.math.log10
import kotlin.math.pow

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
    var eqActive: Boolean = true,
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
        fun faderToDbString(valFloat: Float): String {
            if (valFloat <= 0.001f) return "-∞ dB"
            // X32 fader scale approximation: 0.75 -> 0 dB, 1.0 -> +10 dB, 0.5 -> -10 dB, 0.25 -> -30 dB
            val db = when {
                valFloat >= 0.75f -> (valFloat - 0.75f) / 0.25f * 10f
                valFloat >= 0.50f -> -10f + (valFloat - 0.50f) / 0.25f * 10f
                valFloat >= 0.25f -> -30f + (valFloat - 0.25f) / 0.25f * 20f
                else -> -60f + (valFloat / 0.25f) * 30f
            }
            return if (db > 0f) String.format("+%.1f dB", db) else String.format("%.1f dB", db)
        }
    }
}
