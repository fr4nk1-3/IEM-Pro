package com.example.network

import com.example.model.ConnectionStatus
import com.example.model.MixerModelInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicLong

class OscSocketClient {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var socket: DatagramSocket? = null
    private var targetAddress: InetAddress? = null
    private var targetPort: Int = 10023

    private val _connectionState = MutableStateFlow<MixerModelInfo>(
        MixerModelInfo(
            model = "NO MIXER CONNECTED",
            name = "Disconnected",
            ip = "192.168.1.100",
            port = 10023,
            firmware = "N/A",
            latencyMs = 0,
            signalStrengthDbm = -100,
            status = ConnectionStatus.DISCONNECTED
        )
    )
    val connectionState: StateFlow<MixerModelInfo> = _connectionState

    private val _incomingMessages = MutableSharedFlow<OscMessage>(extraBufferCapacity = 256)
    val incomingMessages: SharedFlow<OscMessage> = _incomingMessages

    val simulator = VirtualMixerSimulator()
    var isSimulatorActive: Boolean = false
        private set

    private var receiveJob: Job? = null
    private var heartbeatJob: Job? = null
    private var pingJob: Job? = null

    val packetsSent = AtomicLong(0)
    val packetsReceived = AtomicLong(0)

    val notificationEvent = MutableSharedFlow<String>(extraBufferCapacity = 8)

    init {
        // Forward simulator messages if in simulation mode
        scope.launch {
            simulator.incomingOscFlow.collect { msg ->
                if (isSimulatorActive) {
                    _incomingMessages.emit(msg)
                }
            }
        }
    }

    fun startSimulatorMode() {
        disconnectSocket()
        isSimulatorActive = true
        _connectionState.value = MixerModelInfo(
            model = "BEHRINGER X32 LIVE",
            name = "Offline Console",
            ip = "127.0.0.1",
            port = 10023,
            firmware = "v4.09",
            latencyMs = 2,
            signalStrengthDbm = -38,
            status = ConnectionStatus.SIMULATION
        )
    }

    fun connectToMixer(ip: String, port: Int = 10023) {
        scope.launch {
            try {
                disconnectSocket()
                isSimulatorActive = false

                _connectionState.value = MixerModelInfo(
                    model = if (port == 8900) "XR18 / MR18" else "X32 / M32",
                    name = "Connecting...",
                    ip = ip,
                    port = port,
                    firmware = "...",
                    latencyMs = 0,
                    signalStrengthDbm = -50,
                    status = ConnectionStatus.CONNECTING
                )

                val inetAddr = InetAddress.getByName(ip)
                val ds = DatagramSocket()
                ds.soTimeout = 2500
                socket = ds
                targetAddress = inetAddr
                targetPort = port

                var hasReceivedResponse = false

                startListening {
                    hasReceivedResponse = true
                }
                startHeartbeat()
                startPingLoop()

                // Request initial info from mixer
                sendOscMessage(OscMessage("/xinfo"))
                sendOscMessage(OscMessage("/xremote"))

                // Wait up to 2.5s for real hardware response
                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < 2500 && !hasReceivedResponse) {
                    delay(100)
                }

                if (hasReceivedResponse) {
                    try {
                        socket?.soTimeout = 4000
                    } catch (_: Exception) {}
                    _connectionState.value = _connectionState.value.copy(
                        status = ConnectionStatus.CONNECTED
                    )
                    notificationEvent.emit("Connected to mixer at $ip:$port")
                } else {
                    // Real mixer not found! Close socket and set DISCONNECTED state without fallback
                    disconnectSocket()
                    isSimulatorActive = false
                    _connectionState.value = MixerModelInfo(
                        model = "NO MIXER FOUND",
                        name = "Disconnected",
                        ip = ip,
                        port = port,
                        firmware = "N/A",
                        latencyMs = 0,
                        signalStrengthDbm = -100,
                        status = ConnectionStatus.DISCONNECTED
                    )
                    notificationEvent.emit("Mixer not found at $ip:$port. Check network and IP.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                disconnectSocket()
                isSimulatorActive = false
                _connectionState.value = MixerModelInfo(
                    model = "CONNECTION FAILED",
                    name = "Disconnected",
                    ip = ip,
                    port = port,
                    firmware = "N/A",
                    latencyMs = 0,
                    signalStrengthDbm = -100,
                    status = ConnectionStatus.DISCONNECTED
                )
                notificationEvent.emit("Mixer not found at $ip:$port (${e.localizedMessage ?: "Unreachable"})")
            }
        }
    }

    fun sendOscMessage(message: OscMessage) {
        if (isSimulatorActive) {
            simulator.handleOutgoingOsc(message)
            return
        }

        val ds = socket ?: return
        val addr = targetAddress ?: return
        val bytes = message.encode()

        scope.launch {
            try {
                val packet = DatagramPacket(bytes, bytes.size, addr, targetPort)
                ds.send(packet)
                packetsSent.incrementAndGet()
            } catch (e: Exception) {
                e.printStackTrace()
                handleConnectionLoss()
            }
        }
    }

    private fun startListening(onPacketReceived: (() -> Unit)? = null) {
        receiveJob?.cancel()
        receiveJob = scope.launch {
            val buffer = ByteArray(2048)
            while (isActive) {
                val ds = socket ?: break
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    ds.receive(packet)
                    packetsReceived.incrementAndGet()
                    onPacketReceived?.invoke()
                    val decoded = OscMessage.decode(packet.data, packet.length)
                    if (decoded != null) {
                        if (decoded.address == "/xinfo" && decoded.arguments.size >= 2) {
                            val modelStr = decoded.arguments.getOrNull(1)?.toString() ?: "Mixer"
                            val fwStr = decoded.arguments.getOrNull(3)?.toString() ?: "v4.0"
                            _connectionState.value = _connectionState.value.copy(
                                model = modelStr,
                                firmware = fwStr,
                                status = ConnectionStatus.CONNECTED
                            )
                        } else if (_connectionState.value.status == ConnectionStatus.CONNECTING) {
                            _connectionState.value = _connectionState.value.copy(
                                status = ConnectionStatus.CONNECTED
                            )
                        }
                        _incomingMessages.emit(decoded)
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    // Socket timeout on read is expected when no data is sent for 4s - stay active and loop
                    continue
                } catch (e: Exception) {
                    if (!isActive) break
                }
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(2500) // Send /xremote or /renew every 2.5 seconds to maintain active OSC subscription
                if (!isSimulatorActive && socket != null) {
                    sendOscMessage(OscMessage("/xremote"))
                }
            }
        }
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                delay(5000)
                if (!isSimulatorActive && socket != null) {
                    val start = System.currentTimeMillis()
                    sendOscMessage(OscMessage("/xinfo"))
                    delay(200)
                    val roundTrip = System.currentTimeMillis() - start
                    _connectionState.value = _connectionState.value.copy(
                        latencyMs = roundTrip.toInt().coerceIn(1, 999)
                    )
                }
            }
        }
    }

    private fun handleConnectionLoss() {
        if (_connectionState.value.status == ConnectionStatus.CONNECTED) {
            _connectionState.value = _connectionState.value.copy(
                status = ConnectionStatus.RECONNECTING
            )
            scope.launch {
                delay(3000)
                connectToMixer(_connectionState.value.ip, _connectionState.value.port)
            }
        }
    }

    fun disconnectSocket() {
        receiveJob?.cancel()
        heartbeatJob?.cancel()
        pingJob?.cancel()
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }

    fun release() {
        disconnectSocket()
        simulator.stop()
    }
}
