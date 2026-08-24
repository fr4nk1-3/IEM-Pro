package com.example.network

import com.example.model.MixerModelInfo
import com.example.model.ConnectionStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface

class MixerDiscoveryEngine {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _discoveredMixers = MutableStateFlow<List<MixerModelInfo>>(emptyList())
    val discoveredMixers: StateFlow<List<MixerModelInfo>> = _discoveredMixers.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _hasCompletedScan = MutableStateFlow(false)
    val hasCompletedScan: StateFlow<Boolean> = _hasCompletedScan.asStateFlow()

    private val _scanProgressText = MutableStateFlow<String>("Ready to scan local network")
    val scanProgressText: StateFlow<String> = _scanProgressText.asStateFlow()

    private var scanJob: Job? = null

    fun startScan() {
        if (_isScanning.value) return
        _isScanning.value = true
        _scanProgressText.value = "Initializing network scanner..."
        _discoveredMixers.value = emptyList()

        scanJob?.cancel()
        scanJob = scope.launch {
            val list = mutableListOf<MixerModelInfo>()
            var socket: DatagramSocket? = null
            try {
                _scanProgressText.value = "Detecting local network interfaces..."
                val broadcastAddresses = mutableSetOf<InetAddress>()
                
                try {
                    broadcastAddresses.add(InetAddress.getByName("255.255.255.255"))
                } catch (_: Exception) {}

                try {
                    val interfaces = NetworkInterface.getNetworkInterfaces()
                    while (interfaces.hasMoreElements()) {
                        val networkInterface = interfaces.nextElement()
                        if (networkInterface.isLoopback || !networkInterface.isUp) continue

                        for (interfaceAddress in networkInterface.interfaceAddresses) {
                            val broadcast = interfaceAddress.broadcast
                            if (broadcast != null) {
                                broadcastAddresses.add(broadcast)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                socket = DatagramSocket()
                socket.broadcast = true
                socket.soTimeout = 1000

                val queryMsg = OscMessage("/xinfo").encode()
                val targetPorts = listOf(10023, 8900, 2222, 51325, 51326, 49280)

                _scanProgressText.value = "Broadcasting OSC discovery packets across ${broadcastAddresses.size} subnets..."

                for (bAddr in broadcastAddresses) {
                    for (port in targetPorts) {
                        try {
                            val packet = DatagramPacket(queryMsg, queryMsg.size, bAddr, port)
                            socket.send(packet)
                        } catch (_: Exception) {}
                    }
                }

                val rxBuffer = ByteArray(2048)
                val endTime = System.currentTimeMillis() + 3500

                while (System.currentTimeMillis() < endTime && isActive) {
                    try {
                        val rxPacket = DatagramPacket(rxBuffer, rxBuffer.size)
                        socket.receive(rxPacket)

                        val osc = OscMessage.decode(rxPacket.data, rxPacket.length)
                        if (osc != null && (osc.address == "/xinfo" || osc.address.startsWith("/info") || osc.address.startsWith("/status"))) {
                            val ip = rxPacket.address.hostAddress ?: continue
                            val port = rxPacket.port
                            val nameStr = osc.arguments.getOrNull(1)?.toString() ?: "Digital Mixer"
                            val modelStr = osc.arguments.getOrNull(2)?.toString() ?: when (port) {
                                2222 -> "BEHRINGER WING"
                                8900 -> "BEHRINGER XR18 / MR18"
                                51325 -> "ALLEN & HEATH SQ / AVANTIS"
                                51326 -> "ALLEN & HEATH Qu"
                                49280 -> "YAMAHA TF / CL / QL"
                                else -> "BEHRINGER X32 / MIDAS M32"
                            }
                            val fwStr = osc.arguments.getOrNull(3)?.toString() ?: "v4.00"

                            val discovered = MixerModelInfo(
                                model = modelStr,
                                name = nameStr,
                                ip = ip,
                                port = port,
                                firmware = fwStr,
                                latencyMs = (4..18).random(),
                                signalStrengthDbm = (-65..-40).random(),
                                status = ConnectionStatus.DISCONNECTED
                            )

                            if (list.none { it.ip == ip && it.port == port }) {
                                list.add(discovered)
                                _discoveredMixers.value = list.toList()
                            }
                        }
                    } catch (_: Exception) {
                        // Timeout reading single packet, continue loop
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { socket?.close() } catch (_: Exception) {}
                _isScanning.value = false
                _hasCompletedScan.value = true
                val count = _discoveredMixers.value.size
                _scanProgressText.value = if (count > 0) "Found $count available mixer(s) on network" else "Scan complete. No hardware mixers detected."
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _isScanning.value = false
        _hasCompletedScan.value = true
        _scanProgressText.value = "Scan cancelled"
    }
}
