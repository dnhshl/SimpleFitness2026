package com.example.main.statemgmt

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.main.repositories.GestureData
import com.example.main.repositories.GestureRepository
import com.example.main.ui.screens.Screen
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * ZENTRALES VIEWMODEL (Steuerzentrale der App).
 *
 * Das ViewModel verbindet das `GestureRepository` mit dem `UiState`:
 * 1. Es sammelt kontinuierlich die KI-Ergebnisse (`GestureData`) ein.
 * 2. Wenn eine Pinch-Geste erkannt wird (`isPinching == true`), aktualisiert es die Würfel-Position
 *    und -Rotation im `UiState`.
 */
class AppViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)

    // REPOSITORY FÜR LOKALE KI (MEDIAPIPE):
    val gestureRepository = GestureRepository(application)

    // --- 1. UI STATE (Arbeitsspeicher) ---
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // --- 2. PERSISTENT STATE (Disk Storage) ---
    val persistentUiState: StateFlow<PersistentUiState> = dataStoreManager.persistentUiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PersistentUiState()
        )

    init {
        observeGestureEvents()
    }

    /**
     * Beobachtet die vom GestureRepository emittierten KI-Daten.
     */
    private fun observeGestureEvents() {
        viewModelScope.launch {
            gestureRepository.gestureState.collect { gestureData ->
                processGestureData(gestureData)
            }
        }
    }

    /**
     * Verarbeitet die rohen KI-Daten und entscheidet über Würfelbewegung & Status.
     */
    private fun processGestureData(gesture: GestureData) {
        _uiState.update { currentState ->
            if (gesture.isPinching) {
                // VERHALTEN BEIM GREIFEN:
                // Der Würfel folgt direkt den Koordinaten der zusammengeführten Fingerkuppen
                val targetX = gesture.pinchPositionX
                val targetY = gesture.pinchPositionY

                // Sanfte Würfeldrehung basierend auf Handneigung & Bewegung
                val newRotX = (currentState.cubeRotationX + (targetY - currentState.cubePositionY) * 50f) % 360f
                val newRotY = (currentState.cubeRotationY + (targetX - currentState.cubePositionX) * 50f) % 360f

                currentState.copy(
                    cubePositionX = targetX,
                    cubePositionY = targetY,
                    cubeRotationX = newRotX,
                    cubeRotationY = newRotY,
                    isCubeGrabbed = true,
                    pinchX = gesture.pinchPositionX,
                    pinchY = gesture.pinchPositionY,
                    rawDistance = gesture.rawDistance,
                    gestureStatus = gesture.gestureName
                )
            } else {
                // VERHALTEN BEIM LOSLASSEN:
                // Würfel verbleibt an seiner letzten Position, wird als nicht gegriffen markiert
                currentState.copy(
                    isCubeGrabbed = false,
                    pinchX = gesture.pinchPositionX,
                    pinchY = gesture.pinchPositionY,
                    rawDistance = gesture.rawDistance,
                    gestureStatus = gesture.gestureName
                )
            }
        }
    }

    /**
     * Wird von PermissionWrapper aufgerufen, wenn Kamera-Berechtigung gewährt wird.
     */
    fun onCameraPermissionGranted(isGranted: Boolean) {
        _uiState.update { it.copy(isCameraPermissionGranted = isGranted) }
    }

    override fun onCleared() {
        super.onCleared()
        gestureRepository.close()
    }

    // =========================================================================
    // BOILERPLATE AREA: NAVIGATION HANDLING
    // =========================================================================

    private val savedBackStack: List<Screen>? = savedStateHandle.get<String>("backstack_keys")?.let {
        try { Json.decodeFromString<List<Screen>>(it) } catch (e: Exception) { null }
    }
    
    val backStack = mutableStateListOf<Screen>().apply {
        addAll(savedBackStack ?: listOf(Screen.Home))
    }

    private fun saveBackStack() {
        savedStateHandle["backstack_keys"] = Json.encodeToString(backStack.toList())
    }

    fun navigateTo(screen: Screen) {
        if (backStack.lastOrNull() != screen) {
            backStack.clear()
            backStack.add(screen)
            saveBackStack()
        }
    }

    fun popBackStack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
            saveBackStack()
        }
    }
}
