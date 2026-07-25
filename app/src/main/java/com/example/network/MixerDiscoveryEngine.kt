package com.example.network

import com.example.model.MixerModelInfo
import com.example.model.ConnectionStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MixerDiscoveryEngine {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _discoveredMixers = MutableStateFlow<List<MixerModelInfo>>(emptyList())
    val discoveredMixers: StateFlow<List<MixerModelInfo>> = _discoveredMixers

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private var scanJob: Job? = null

    fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true

        scanJob?.cancel()
        scanJob = scope.launch {
            val list = mutableListOf<MixerModelInfo>()

            // Add default mixer to discovery list
            list.add(
                MixerModelInfo(
                    model = "BEHRINGER X32 LIVE",
                    name = "Offline Console",
                    ip = "127.0.0.1",
                    port = 10023,
                    firmware = "v4.09",
                    latencyMs = 2,
                    signalStrengthDbm = -35,
                    status = ConnectionStatus.SIMULATION
                )
            )
            _discoveredMixers.value = list.toList()

            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true
                socket.soTimeout = 1200

                val queryMsg = OscMessage("/xinfo").encode()
                val broadcastAddr = InetAddress.getByName("255.255.255.255")

                // Send broadcast to X32/M32 port 10023, X-Air port 8900, WING port 2222
                val p10023 = DatagramPacket(queryMsg, queryMsg.size, broadcastAddr, 10023)
                val p8900 = DatagramPacket(queryMsg, queryMsg.size, broadcastAddr, 8900)
                val p2222 = DatagramPacket(queryMsg, queryMsg.size, broadcastAddr, 2222)

                socket.send(p10023)
                socket.send(p8900)
                socket.send(p2222)

                val rxBuffer = ByteArray(1024)
                val endTime = System.currentTimeMillis() + 3000

                while (System.currentTimeMillis() < endTime && isActive) {
                    try {
                        val rxPacket = DatagramPacket(rxBuffer, rxBuffer.size)
                        socket.receive(rxPacket)

                        val osc = OscMessage.decode(rxPacket.data, rxPacket.length)
                        if (osc != null && osc.address == "/xinfo") {
                            val ip = rxPacket.address.hostAddress ?: continue
                            val port = rxPacket.port
                            val nameStr = osc.arguments.getOrNull(1)?.toString() ?: "Digital Mixer"
                            val modelStr = osc.arguments.getOrNull(2)?.toString() ?: when (port) {
                                2222 -> "BEHRINGER WING"
                                8900 -> "XR18 / MR18"
                                else -> "X32 / M32"
                            }
                            val fwStr = osc.arguments.getOrNull(3)?.toString() ?: "v4.00"

                            val discovered = MixerModelInfo(
                                model = modelStr,
                                name = nameStr,
                                ip = ip,
                                port = port,
                                firmware = fwStr,
                                latencyMs = 12,
                                signalStrengthDbm = -55,
                                status = ConnectionStatus.DISCONNECTED
                            )

                            if (list.none { it.ip == ip && it.port == port }) {
                                list.add(discovered)
                                _discoveredMixers.value = list.toList()
                            }
                        }
                    } catch (_: Exception) {
                        // Timeout reading packet
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { socket?.close() } catch (_: Exception) {}
                _isScanning.value = false
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _isScanning.value = false
    }
}
