package com.example.main.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.main.statemgmt.AppViewModel
import com.example.main.statemgmt.ConnectionState
import com.example.main.statemgmt.DataSource

/**
 * The Home Screen template.
 * Displays data source selection (HTTP vs BLE), Live Data streaming switch, and Cloud Sync controls.
 */
@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 0. DATA SOURCE SELECTOR (HTTP vs. BLE) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Livestream Data Source", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.setDataSource(DataSource.HTTP) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.dataSource == DataSource.HTTP) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (uiState.dataSource == DataSource.HTTP)
                                MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("HTTP Internet")
                    }
                    Button(
                        onClick = { viewModel.setDataSource(DataSource.BLE) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.dataSource == DataSource.BLE) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (uiState.dataSource == DataSource.BLE)
                                MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("ESP32 BLE")
                    }
                }
                if (uiState.dataSource == DataSource.BLE) {
                    Text(
                        text = if (uiState.connectionState == ConnectionState.Connected) 
                            "Status: Connected to ESP32" 
                        else "Status: Not connected (Go to ESP32 BLE tab to connect)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.connectionState == ConnectionState.Connected) 
                            MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // --- 1. LIVE DATA SWITCH ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isPollingEnabled) 
                    MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.isPollingEnabled) "Live Streaming: ON" else "Live Streaming: OFF",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (uiState.dataSource == DataSource.HTTP) 
                            "Streaming via Internet (Ktor)" 
                        else "Streaming via Bluetooth (ESP32)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = uiState.isPollingEnabled,
                    onCheckedChange = { viewModel.setPollingEnabled(it) }
                )
            }
        }

        // --- 2. EXPLICIT CLOUD SYNC CARD (Visible only when authenticated) ---
        if (uiState.isAuthenticated) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Cloud Synchronization", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Download your historical sessions from Firebase Firestore.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = { viewModel.syncFromCloud() },
                        modifier = Modifier.align(Alignment.End),
                        enabled = !uiState.isLoading
                    ) {
                        Text(if (uiState.isLoading) "Syncing..." else "Sync with Cloud")
                    }
                }
            }
        }
    }
}
