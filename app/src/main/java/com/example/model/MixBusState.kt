package com.example.model

enum class BusTapMode(val label: String, val description: String) {
    PRE_FADER("Pre-Fader", "Independent IEM mix unaffected by main FOH fader"),
    POST_FADER("Post-Fader", "Follows main fader level (Aux/FX)"),
    PRE_EQ("Pre-EQ", "Raw channel tap before EQ processing"),
    SUBGROUP("Subgroup", "Fixed unity gain group routing")
}

data class BusEqBand(
    val id: Int, // 1..6
    val label: String, // e.g. "Low Cut", "Low Mid", "Mid", "High Mid", "High", "High Cut"
    var freqHz: Float, // Frequency in Hz
    var gainDb: Float = 0f, // -15.0f to +15.0f dB
    var qFactor: Float = 1.0f // 0.3 to 10.0
)

fun defaultBusEqBands(): List<BusEqBand> = listOf(
    BusEqBand(1, "Low Cut", 80f, 0f, 0.7f),
    BusEqBand(2, "Low Mid", 250f, 0f, 1.0f),
    BusEqBand(3, "Mid", 800f, 0f, 1.0f),
    BusEqBand(4, "High Mid", 2500f, 0f, 1.0f),
    BusEqBand(5, "High", 8000f, 0f, 1.0f),
    BusEqBand(6, "High Cut", 12000f, 0f, 0.7f)
)

data class MixBusState(
    val id: Int, // 1..16
    var name: String,
    var color: X32Color = X32Color.CYAN,
    var masterLevel: Float = 0.8f,
    var masterMute: Boolean = false,
    var isStereoLinked: Boolean = false,
    var linkedBusId: Int? = null,
    var limiterActive: Boolean = true,
    var limiterThresholdDb: Float = -6f, // -24dB to 0dB
    var eqActive: Boolean = true,
    var eqBands: List<BusEqBand> = defaultBusEqBands(),
    var outputDelayMs: Float = 0f, // 0..50 ms
    var phaseInverted: Boolean = false,
    var sendTapMode: BusTapMode = BusTapMode.PRE_FADER,
    var talkbackActive: Boolean = false,
    var iconType: String = "HEADPHONES",
    var peakMeter: Float = 0.0f,
    var peakMeterL: Float = 0.0f,
    var peakMeterR: Float = 0.0f
) {
    fun getMasterDbString(): String {
        return ChannelState.faderToDbString(masterLevel)
    }

    fun getPeakDbString(): String {
        val peak = maxOf(peakMeter, maxOf(peakMeterL, peakMeterR))
        if (peak <= 0.01f) return "-inf dB"
        val db = (peak - 0.75f) * 40f
        return String.format(java.util.Locale.US, "%+.1f dBFS", db)
    }
}

