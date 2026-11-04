package com.example.main.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.main.R
import com.example.main.statemgmt.AppViewModel
import com.example.main.statemgmt.ConnectionState
import com.example.main.statemgmt.DiscoveredDevice
import com.example.main.ui.components.PermissionWrapper

/**
 * The BLE Screen for scanning and connecting to ESP32 Bluetooth sensors.
 */
@Composable
fun BleScreen(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    PermissionWrapper {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Connection Status Card
            ConnectionStatusCard(uiState.connectionState, onDisconnect = { viewModel.disconnect() })

            // 2. Scan Controls & Last Device Connect (Point a, c)
            val persistentState by viewModel.persistentUiState.collectAsState()
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (uiState.connectionState == ConnectionState.Disconnected) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (uiState.isScanning) viewModel.stopScanning() else viewModel.startScanning()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(if (uiState.isScanning) R.string.btn_stop_scan else R.string.btn_scan))
                        }

                        Button(
                            onClick = { viewModel.connectToLastDevice() },
                            modifier = Modifier.weight(1f),
                            enabled = persistentState.lastUsedDeviceAddress.isNotBlank()
                        ) {
                            Text("Connect Last")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Last Device", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (persistentState.lastUsedDeviceAddress.isNotBlank()) persistentState.lastUsedDeviceAddress else "None saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // 3. Device List or Raw Data
            if (uiState.connectionState == ConnectionState.Connected) {
                RawDataDisplay(uiState.rawResponse)
            } else {
                DeviceList(
                    devices = uiState.foundDevices,
                    isScanning = uiState.isScanning,
                    onDeviceClick = { viewModel.connectToDevice(it.address) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(state: ConnectionState, onDisconnect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (state) {
                ConnectionState.Connected -> Color(0xFFE8F5E9)
                ConnectionState.Disconnected -> MaterialTheme.colorScheme.surfaceVariant
                else -> Color(0xFFFFF3E0)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Status", style = MaterialTheme.typography.labelMedium)
                Text(state.name, style = MaterialTheme.typography.headlineSmall)
            }
            if (state == ConnectionState.Connected) {
                Button(onClick = onDisconnect) {
                    Text(stringResource(R.string.btn_disconnect))
                }
            }
        }
    }
}

@Composable
fun DeviceList(
    devices: List<DiscoveredDevice>,
    isScanning: Boolean,
    onDeviceClick: (DiscoveredDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (isScanning) "Scanning for devices..." else "Available Devices",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices) { device ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeviceClick(device) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = device.name ?: "Unknown", fontWeight = FontWeight.Bold)
                        Text(text = device.address, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun RawDataDisplay(rawJson: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.label_response_data),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                text = rawJson.ifEmpty { "Waiting for data..." },
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
