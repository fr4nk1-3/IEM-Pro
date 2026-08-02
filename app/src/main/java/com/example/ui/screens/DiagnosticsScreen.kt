package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.network.OscMessage
import com.example.ui.IemViewModel
import com.example.ui.theme.*

@Composable
fun DiagnosticsScreen(
    viewModel: IemViewModel,
    onBackToDashboard: () -> Unit
) {
    val connectionInfo by viewModel.connectionState.collectAsState()
    val oscClient = viewModel.oscClient

    var lastPingResult by remember { mutableStateOf("Ready") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToDashboard) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "NETWORK DIAGNOSTICS & OSC LOG",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
        }

        HorizontalDivider(color = DarkBorder)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Network Connection Specs
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NetworkCheck, contentDescription = "Network", tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active Socket Parameters", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DiagnosticItem("Mixer Model", connectionInfo.model)
                    DiagnosticItem("Target IP Address", connectionInfo.ip)
                    DiagnosticItem("UDP Port", connectionInfo.port.toString())
                    DiagnosticItem("Firmware Version", connectionInfo.firmware)
                    DiagnosticItem("Connection Status", connectionInfo.status.name)
                    DiagnosticItem("Latency Ping", "${connectionInfo.latencyMs} ms")

                    if (connectionInfo.status == ConnectionStatus.DISCONNECTED) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.reconnectToMixer() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonRose),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Reconnect to ${connectionInfo.ip}", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Card 2: Packet rx / tx metrics
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, contentDescription = "Stats", tint = NeonAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("OSC Packet Traffic Counters", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DiagnosticItem("OSC Packets Sent", oscClient.packetsSent.get().toString())
                    DiagnosticItem("OSC Packets Received", oscClient.packetsReceived.get().toString())
                    DiagnosticItem("Simulation Engine", if (oscClient.isSimulatorActive) "ACTIVE (Internal)" else "OFF (Live Socket)")
                }
            }

            // Card 3: Test Ping Action
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Manual Network OSC Test", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NeonEmerald)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Send '/xinfo' diagnostic probe and log latency response.", fontSize = 12.sp, color = TextSecondary)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Result: $lastPingResult", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                        Button(
                            onClick = {
                                val start = System.currentTimeMillis()
                                oscClient.sendOscMessage(OscMessage("/xinfo"))
                                val duration = System.currentTimeMillis() - start
                                lastPingResult = "Sent /xinfo packet in $duration ms"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
                        ) {
                            Text("Send Test Ping", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
