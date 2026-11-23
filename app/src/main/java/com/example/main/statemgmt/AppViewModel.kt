package com.example.main.statemgmt

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.main.repositories.GestureData
import com.example.main.repositories.GestureRepository
import com.example.main.repositories.HandGestureMode
import com.example.main.ui.screens.Screen
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * ZENTRALES VIEWMODEL (Steuerzentrale der App).
 */
class AppViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)

    // REPOSITORY FÜR LOKALE KI (MEDIAPIPE):
    val gestureRepository = GestureRepository(application)

    // --- 1. UI STATE ---
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // --- 2. PERSISTENT STATE ---
    val persistentUiState: StateFlow<PersistentUiState> = dataStoreManager.persistentUiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PersistentUiState()
        )

    init {
        observeGestureEvents()
    }

    private fun observeGestureEvents() {
        viewModelScope.launch {
            gestureRepository.gestureState.collect { gestureData ->
                processGestureData(gestureData)
            }
        }
    }

    private fun processGestureData(gesture: GestureData) {
        _uiState.update { currentState ->
            when (gesture.mode) {
                HandGestureMode.PINCH -> {
                    // MODUS 1: PINCH (GREIFEN & VERSCHIEBEN)
                    // Würfel folgt der Fingerposition, 3D-Rotation bleibt unverändert
                    currentState.copy(
                        cubePositionX = gesture.pinchPositionX,
                        cubePositionY = gesture.pinchPositionY,
                        isCubeGrabbed = true,
                        gestureMode = HandGestureMode.PINCH,
                        pinchX = gesture.pinchPositionX,
                        pinchY = gesture.pinchPositionY,
                        tiltX = gesture.tiltX,
                        tiltY = gesture.tiltY,
                        rawDistance = gesture.rawDistance,
                        handLandmarks = gesture.landmarks,
                        isHandDetected = true
                    )
                }
                HandGestureMode.FIST -> {
                    // MODUS 2: FAUST (DREHEN AN DER STELLE)
                    // Würfel dreht sich basierend auf der Faust-Orientierung, Position bleibt unverändert
                    val targetRotX = gesture.tiltX
                    val targetRotY = gesture.tiltY

                    val smoothRotX = currentState.cubeRotationX + (targetRotX - currentState.cubeRotationX) * 0.2f
                    val smoothRotY = currentState.cubeRotationY + (targetRotY - currentState.cubeRotationY) * 0.2f

                    currentState.copy(
                        cubeRotationX = smoothRotX,
                        cubeRotationY = smoothRotY,
                        isCubeGrabbed = false,
                        gestureMode = HandGestureMode.FIST,
                        pinchX = gesture.pinchPositionX,
                        pinchY = gesture.pinchPositionY,
                        tiltX = gesture.tiltX,
                        tiltY = gesture.tiltY,
                        rawDistance = gesture.rawDistance,
                        handLandmarks = gesture.landmarks,
                        isHandDetected = true
                    )
                }
                HandGestureMode.OPEN_HAND -> {
                    // MODUS 3: OFFENE HAND (RUHEZUSTAND / INAKTIV)
                    // Keine Transformationen des Würfels, um versehentliche Auslösungen zu vermeiden
                    currentState.copy(
                        isCubeGrabbed = false,
                        gestureMode = HandGestureMode.OPEN_HAND,
                        pinchX = gesture.pinchPositionX,
                        pinchY = gesture.pinchPositionY,
                        tiltX = gesture.tiltX,
                        tiltY = gesture.tiltY,
                        rawDistance = gesture.rawDistance,
                        handLandmarks = gesture.landmarks,
                        isHandDetected = true
                    )
                }
                HandGestureMode.NONE -> {
                    // KEINE HAND ERKANNT
                    currentState.copy(
                        isCubeGrabbed = false,
                        gestureMode = HandGestureMode.NONE,
                        handLandmarks = emptyList(),
                        isHandDetected = false
                    )
                }
            }
        }
    }

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
