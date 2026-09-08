package com.example.main.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.main.R
import com.example.main.statemgmt.AppViewModel
import com.example.main.statemgmt.TimeHelper
import com.example.main.statemgmt.UiState

/**
 * The Home Screen of App #3.
 * 
 * This screen demonstrates how to trigger an HTTP request and display the 
 * results, both as a structured UI and as raw JSON.
 */
@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    
    // LEARNING POINT: Responsive Design
    // We check the orientation here and swap the entire layout function.
    // This is cleaner than having dozens of 'if (isLandscape)' inside one big column.
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        HomeScreenLandscape(viewModel, uiState)
    } else {
        HomeScreenPortrait(viewModel, uiState)
    }
}

@Composable
fun HomeScreenPortrait(viewModel: AppViewModel, uiState: UiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HomeControls(viewModel, uiState)
        ParsedDataCard(uiState)
        RawDataCard(uiState)
    }
}

@Composable
fun HomeScreenLandscape(viewModel: AppViewModel, uiState: UiState) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Since the global TopBar is hidden in Landscape, we provide our own local title here.
        Text(
            text = "Fitness Data Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // LEARNING POINT: The "Side-by-Side" pattern
        // In landscape, we use a 'Row' to place the controls and the data next to each other.
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HomeControls(viewModel, uiState)
                ParsedDataCard(uiState)
            }
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                RawDataCard(uiState, modifier = Modifier.fillMaxHeight())
            }
        }
    }
}

@Composable
fun HomeControls(viewModel: AppViewModel, uiState: UiState) {
    Column(
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
    }
}

@Composable
fun ParsedDataCard(uiState: UiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Parsed Data",
            style = MaterialTheme.typography.titleLarge
        )
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                uiState.lastReceivedData?.let { data ->
                    DataRow(label = "Fitness Value:", value = "%.2f".format(data.fitness))
                    DataRow(label = "Heart Rate (Puls):", value = "${data.puls} bpm")
                    DataRow(
                        label = "Timestamp:", 
                        value = TimeHelper.formatIsoTimestamp(data.isotimestamp)
                    )
                } ?: Text("No parsed data available. Click the button.")
            }
        }
    }
}

@Composable
fun RawDataCard(uiState: UiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.label_response_data),
            style = MaterialTheme.typography.titleLarge
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = uiState.rawResponse.ifEmpty { "No raw data yet." },
                modifier = Modifier
                    .padding(16.dp)
                    .heightIn(max = 400.dp) // Limit height so it doesn't push everything off screen
                    .verticalScroll(rememberScrollState()),
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
