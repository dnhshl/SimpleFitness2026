package com.example.main.statemgmt

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.main.repositories.HttpRepository
import com.example.main.ui.screens.Screen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The Central Control Center (ViewModel) of your App.
 */
class AppViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)
    private val httpRepository = HttpRepository(application)

    // --- 1. UI STATE (Memory only) ---
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // --- 2. PERSISTENT STATE (Disk storage) ---
    val persistantUiState: StateFlow<PersistantUiState> = dataStoreManager.persistantUiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PersistantUiState()
        )

    // --- 3. DATA POLLING ---
    private var pollingJob: Job? = null
    private var activePollingRequests = 0 // Track how many screens want polling

    /**
     * Starts fetching data from the API every X milliseconds.
     */
    fun startPolling(intervalMs: Long = 2000L) {
        activePollingRequests++
        
        if (pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            try {
                while (true) {
                    performHttpRequest()
                    delay(intervalMs)
                }
            } catch (e: Exception) {
                // Task was cancelled
            }
        }
    }

    /**
     * Stops the background data fetching.
     */
    fun stopPolling() {
        activePollingRequests--
        if (activePollingRequests <= 0) {
            pollingJob?.cancel()
            pollingJob = null
            activePollingRequests = 0
        }
    }

    /**
     * Performs an HTTP request to get the latest fitness data.
     */
    fun performHttpRequest() {
        viewModelScope.launch {
            // Show loading indicator only for the very first request
            if (_uiState.value.lastReceivedData == null) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            
            try {
                val data = httpRepository.fetchFitnessData()
                val raw = httpRepository.fetchRawJson()
                
                _uiState.update { state -> 
                    // Add the new FitnessData to our history (max 100 entries)
                    val newHistory = (state.fitnessHistory + data).takeLast(100)
                    
                    state.copy(
                        lastReceivedData = data,
                        fitnessHistory = newHistory,
                        rawResponse = raw,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    errorMessage = "Network Error: ${e.localizedMessage}",
                    isLoading = false
                )}
            }
        }
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
