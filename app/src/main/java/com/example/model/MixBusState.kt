package com.example.model

data class MixBusState(
    val id: Int, // 1..16
    var name: String,
    var color: X32Color = X32Color.CYAN,
    var masterLevel: Float = 0.8f,
    var masterMute: Boolean = false,
    var isStereoLinked: Boolean = false,
    var linkedBusId: Int? = null,
    var limiterActive: Boolean = false,
    var eqActive: Boolean = true
) {
    fun getMasterDbString(): String {
        return ChannelState.faderToDbString(masterLevel)
    }
}
