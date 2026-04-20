package com.example.main.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.main.R
import com.example.main.statemgmt.AppViewModel
import com.example.main.statemgmt.TimeHelper

/**
 * The Home Screen of App #3.
 * 
 * This screen demonstrates how to trigger an HTTP request and display the 
 * results, both as a structured UI and as raw JSON.
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
        // 1. Trigger Button
        Button(
            onClick = { viewModel.performHttpRequest() },
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.btn_fetch_data))
        }

        // 2. Loading Indicator
        if (uiState.isLoading) {
            CircularProgressIndicator()
        }

        // 3. Error Message
        uiState.errorMessage?.let { error ->
            Text(
                text = stringResource(R.string.error_prefix, error),
                color = MaterialTheme.colorScheme.error
            )
        }

        // 4. Structured Data Display
        Text(
            text = "Parsed Data",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                uiState.lastReceivedData?.let { data ->
                    DataRow(label = "Fitness Value:", value = "%.2f".format(data.fitness))
                    DataRow(label = "Heart Rate (Puls):", value = "${data.puls} bpm")
                    
                    // We use our new TimeHelper to make the date pretty!
                    DataRow(
                        label = "Timestamp:", 
                        value = TimeHelper.formatIsoTimestamp(data.isotimestamp)
                    )
                } ?: Text("No parsed data available. Click the button above.")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 5. Raw JSON Display
        Text(
            text = stringResource(R.string.label_response_data),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.Start)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = uiState.rawResponse.ifEmpty { "No raw data yet." },
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Bold)
        Text(text = value)
    }
}
