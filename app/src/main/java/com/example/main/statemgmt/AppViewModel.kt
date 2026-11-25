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

    // REPOSITORY FÜR LOKALE KI (MEDIAPIPE HAND LANDMARKER):
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

    /**
     * REVERSE ENGINEERING LERNEINHEIT: STATE MACHINE FÜR REHA-WIEDERHOLUNGSZÄHLER
     *
     * 1. Erfasst den aktuellen PIP-Gelenkwinkel aus dem Repository.
     * 2. Prüft Beugung (Finger gebeugt <= 105°).
     * 3. Erkennt vollständige Streckung (Finger gestreckt >= 155°) nach einer Beugung -> Repetition +1!
     */
    private fun processGestureData(gesture: GestureData) {
        if (!gesture.isHandDetected) {
            _uiState.update {
                it.copy(
                    isHandDetected = false,
                    handLandmarks = emptyList(),
                    gestureMode = HandGestureMode.NONE
                )
            }
            return
        }

        val angle = gesture.fingerPipAngle
        val currentlyFlexed = _uiState.value.isFingerFlexed
        var newRepCount = _uiState.value.rehabRepCount
        var nextFlexedState = currentlyFlexed

        // STATE MACHINE LOGIK:
        if (angle <= 105.0) {
            // Zustand 1: Finger wird gebeugt
            nextFlexedState = true
        } else if (angle >= 155.0 && currentlyFlexed) {
            // Zustand 2: Finger wird wieder vollständig gestreckt nach einer Beugung -> Zyklen +1
            nextFlexedState = false
            newRepCount += 1
        }

        _uiState.update { currentState ->
            currentState.copy(
                fingerPipAngle = angle,
                rehabRepCount = newRepCount,
                isFingerFlexed = nextFlexedState,
                gestureMode = gesture.mode,
                handLandmarks = gesture.landmarks,
                isHandDetected = true
            )
        }
    }

    fun resetRehabRepCount() {
        _uiState.update {
            it.copy(
                rehabRepCount = 0,
                isFingerFlexed = false
            )
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
