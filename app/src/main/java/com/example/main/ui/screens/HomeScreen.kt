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

/**
 * The Home Screen template.
 * Displays the Live Data streaming switch and explicit Cloud Sync controls when authenticated.
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
                        text = "Enable to auto-update Speed and Graph screens.",
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
