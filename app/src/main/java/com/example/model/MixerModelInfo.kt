package com.example.model

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    SIMULATION
}

data class MixerModelInfo(
    val model: String,
    val name: String,
    val ip: String,
    val port: Int,
    val firmware: String,
    val latencyMs: Int,
    val signalStrengthDbm: Int,
    val status: ConnectionStatus
)
