package com.example.main.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.main.statemgmt.AppViewModel
import com.example.main.statemgmt.TimeHelper

/**
 * LEARNING POINT: The Training History with Filtering
 * This screen demonstrates how to read and filter data from the Room database.
 */
@Composable
fun HistoryScreen(viewModel: AppViewModel) {
    // 1. Observe the data and the central UI State from the ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.fitnessHistoryLog.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- FILTER UI SECTION ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = "Filters", style = MaterialTheme.typography.titleMedium)
                
                Spacer(modifier = Modifier.height(8.dp))

                // BPM Filter Slider (now reading from uiState)
                Text(text = "Min Heart Rate: ${uiState.minBpmFilter} BPM", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = uiState.minBpmFilter.toFloat(),
                    onValueChange = { viewModel.setMinBpmFilter(it.toInt()) },
                    valueRange = 0f..200f,
                    steps = 20
                )

                // Time Filter Selection (now reading from uiState)
                Text(text = "Show records from:", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterButton(label = "All", isSelected = uiState.timeFilterMinutes == 0) { viewModel.setTimeFilter(0) }
                    FilterButton(label = "5m", isSelected = uiState.timeFilterMinutes == 5) { viewModel.setTimeFilter(5) }
                    FilterButton(label = "1h", isSelected = uiState.timeFilterMinutes == 60) { viewModel.setTimeFilter(60) }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Clear History Button
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.Red)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- RESULTS LIST ---
        Text(
            text = "Found ${history.size} records",
            style = MaterialTheme.typography.labelLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (history.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No data matches your filters.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { entry ->
                    HistoryItem(
                        time = TimeHelper.formatIsoTimestamp(entry.timestamp),
                        bpm = entry.heartRate,
                        fitness = entry.fitnessScore
                    )
                }
            }
        }
    }
}

@Composable
fun FilterButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun HistoryItem(time: String, bpm: Int, fitness: Double) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = time, style = MaterialTheme.typography.bodySmall)
                Text(text = "Heart Rate: $bpm BPM", style = MaterialTheme.typography.bodyLarge)
            }
            // LEARNING TASK: Students could add color coding here (e.g. red if high BPM)
            Text(
                text = "%.1f".format(fitness),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
