package com.example.main.statemgmt

import androidx.compose.ui.geometry.Offset
import com.example.main.repositories.HandGestureMode
import kotlinx.serialization.Serializable

/**
 * REVERSE ENGINEERING LERNEINHEIT: LOKALE KI & HAND-GONIOMETRIE
 *
 * 1. UI STATE (Haupt-Zustand im Arbeitsspeicher)
 */
data class UiState(
    // BIOMECHANISCHE MESSWERTE (GONIOMETRIE & REHA-TRAINER):
    val fingerPipAngle: Double = 180.0,   // Zeigefinger PIP-Gelenkwinkel in Grad
    val rehabRepCount: Int = 0,           // Beuge-/Streckungs-Zähler
    val isFingerFlexed: Boolean = false,  // State-Machine Zustand

    // HAND-ZUSTAND:
    val gestureMode: HandGestureMode = HandGestureMode.NONE,

    // ECHTZEIT FINGER- & SKELETT-DATEN (Normiert 0.0 .. 1.0):
    val handLandmarks: List<Offset> = emptyList(), // Alle 21 Handgelenke für das Skelett-Overlay
    val isHandDetected: Boolean = false,

    // BERECHTIGUNGEN:
    val isCameraPermissionGranted: Boolean = false
)

/**
 * 2. PERSISTENT UI STATE (Speicherung auf Festplatte/DataStore)
 */
@Serializable
data class PersistentUiState(
    val lastUsedTimestamp: Long = 0L
)
