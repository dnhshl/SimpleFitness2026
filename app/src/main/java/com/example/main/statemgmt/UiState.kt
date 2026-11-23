package com.example.main.statemgmt

import kotlinx.serialization.Serializable

/**
 * REVERSE ENGINEERING LERNEINHEIT: LOKALE KI & GESTENSTEUERUNG
 *
 * 1. UI STATE (Flüchtiger Haupt-Zustand im Arbeitsspeicher)
 *
 * Enthält alle Daten, die zur Darstellung des 3D-Würfels, des Kamera-Overlays
 * und der KI-Erkennungssymbole auf dem Screen benötigt werden.
 */
data class UiState(
    // WÜRFEL-TRANSFORMATIONEN:
    val cubePositionX: Float = 0.5f,  // Normiert 0.0 (links) bis 1.0 (rechts)
    val cubePositionY: Float = 0.4f,  // Normiert 0.0 (oben) bis 1.0 (unten)
    val cubeRotationX: Float = 15f,   // Neigung um X-Achse in Grad
    val cubeRotationY: Float = 45f,   // Neigung um Y-Achse in Grad
    val isCubeGrabbed: Boolean = false, // True wenn Pinch-Geste aktiv ist

    // ECHTZEIT FINGER-LANDMARK TRACKER (0.0 .. 1.0):
    val pinchX: Float = 0.5f,
    val pinchY: Float = 0.5f,
    val rawDistance: Float = 1.0f,

    // STATUS & FEHLERMELDUNGEN:
    val gestureStatus: String = "Starte Kamera...",
    val isCameraPermissionGranted: Boolean = false
)

/**
 * 2. PERSISTENT UI STATE (Speicherung auf Festplatte/DataStore)
 */
@Serializable
data class PersistentUiState(
    val lastUsedTimestamp: Long = 0L
)
