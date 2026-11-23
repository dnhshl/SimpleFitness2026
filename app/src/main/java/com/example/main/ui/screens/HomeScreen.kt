package com.example.main.ui.screens

import androidx.compose.runtime.Composable
import com.example.main.statemgmt.AppViewModel

/**
 * REVERSE ENGINEERING LERNEINHEIT: LOKALE KI GESTENSTEUERUNG
 * 
 * Der HomeScreen dient als Haupteinstiegspunkt für die Gestensteuerung
 * und bettet die `CubeScreen` Komponente ein.
 */
@Composable
fun HomeScreen(viewModel: AppViewModel) {
    CubeScreen(viewModel = viewModel)
}
