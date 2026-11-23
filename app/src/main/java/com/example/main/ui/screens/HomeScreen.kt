package com.example.main.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.main.R
import com.example.main.statemgmt.AppViewModel

/**
 * REVERSE ENGINEERING LERNEINHEIT: ANLEITUNG & BEDIENUNG
 * 
 * Der HomeScreen dient als didaktische Übersicht und erklärt den Studierenden
 * die 3 Hauptgesten für die KI-Steuerung.
 */
@Composable
fun HomeScreen(viewModel: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Willkommens-Karte
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_welcome_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.home_welcome_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text(
            text = "Gesten-Steuerung im Überblick:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // Geste 1: Pinch
        GestureExplanationCard(
            title = stringResource(R.string.gesture_pinch_title),
            description = stringResource(R.string.gesture_pinch_desc),
            icon = Icons.Filled.TouchApp,
            badgeColor = MaterialTheme.colorScheme.primary
        )

        // Geste 2: Faust
        GestureExplanationCard(
            title = stringResource(R.string.gesture_fist_title),
            description = stringResource(R.string.gesture_fist_desc),
            icon = Icons.Filled.BackHand,
            badgeColor = MaterialTheme.colorScheme.tertiary
        )

        // Geste 3: Offene Hand
        GestureExplanationCard(
            title = stringResource(R.string.gesture_open_title),
            description = stringResource(R.string.gesture_open_desc),
            icon = Icons.Filled.PanTool,
            badgeColor = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Start Button
        Button(
            onClick = { viewModel.navigateTo(Screen.Cube) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.btn_start_cube),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GestureExplanationCard(
    title: String,
    description: String,
    icon: ImageVector,
    badgeColor: androidx.compose.ui.graphics.Color
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = badgeColor,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
