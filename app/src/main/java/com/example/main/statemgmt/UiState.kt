package com.example.main.statemgmt

import androidx.compose.ui.geometry.Offset
import com.example.main.repositories.HandGestureMode
import kotlinx.serialization.Serializable

/**
 * REVERSE ENGINEERING LERNEINHEIT: LOKALE KI & GESTENSTEUERUNG
 *
 * 1. UI STATE (Haupt-Zustand im Arbeitsspeicher)
 */
data class UiState(
    // WÜRFEL-TRANSFORMATIONEN:
    val cubePositionX: Float = 0.5f,    // Normiert 0.0 (links) bis 1.0 (rechts)
    val cubePositionY: Float = 0.4f,    // Normiert 0.0 (oben) bis 1.0 (unten)
    val cubeRotationX: Float = 20f,     // Neigung um X-Achse in Grad
    val cubeRotationY: Float = 35f,     // Neigung um Y-Achse in Grad
    val isCubeGrabbed: Boolean = false, // True wenn Pinch-Geste aktiv ist

    // GESTEN-MODUS:
    val gestureMode: HandGestureMode = HandGestureMode.NONE,

    // ECHTZEIT FINGER- & SKELETT-DATEN (Normiert 0.0 .. 1.0):
    val pinchX: Float = 0.5f,
    val pinchY: Float = 0.5f,
    val tiltX: Float = 0f,              // Hand-Kippen Oben/Unten in Grad
    val tiltY: Float = 0f,              // Hand-Kippen Links/Rechts in Grad
    val rawDistance: Float = 1.0f,
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
