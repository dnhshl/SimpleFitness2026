package com.example.main.statemgmt

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.main.db.AppDatabase
import com.example.main.repositories.HttpRepository
import com.example.main.repositories.LocalFitnessRepository
import com.example.main.ui.screens.Screen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * The Central Control Center (ViewModel) of your App.
 */
class AppViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)
    private val httpRepository = HttpRepository(application)
    
    // LEARNING POINT: Database Initialization
    private val database = AppDatabase.getDatabase(application)
    private val localRepository = LocalFitnessRepository(database.fitnessDao())

    // --- SESSION ID FOR ROOM DB FILTERING ---
    var currentSessionId: String = generateNewSessionId()
        private set

    private fun generateNewSessionId(): String {
        return "Session " + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").format(OffsetDateTime.now())
    }

    val availableSessions: StateFlow<List<String>> = localRepository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 1. UI STATE (Memory only) ---
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        if (_uiState.value.isPollingEnabled) {
            startPollingInternal()
        }
    }

    fun setSelectedSessionId(sessionId: String?) {
        _uiState.update { it.copy(selectedSessionId = sessionId) }
    }

    // --- 2. PERSISTENT STATE (Disk storage) ---
    val persistentUiState: StateFlow<PersistentUiState> = dataStoreManager.persistentUiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PersistentUiState()
        )

    // --- 3. DATABASE FILTERING & TRANSFORMATIONS ---
    
    /**
     * LEARNING POINT: Reaktive Datenverarbeitung & Flow-Kombination
     * 
     * `fitnessHistoryLog` beobachtet den UI-Zustand (Filter wie Puls, Zeit und SessionID) 
     * und lädt Daten automatisch reaktiv aus der Room Database.
     * 
     * 1. map: Extrahiert die relevanten Filterkriterien als Tripel.
     * 2. distinctUntilChanged: Verhindert unnötige DB-Abfragen, wenn sich die Filter nicht geändert haben.
     * 3. flatMapLatest: Bricht bei einer Filteränderung die alte DB-Abfrage ab und startet sofort die neue.
     * 4. stateIn: Wandelt den Flow in einen Hot StateFlow um, den die UI beobachten kann.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val fitnessHistoryLog = uiState
        .map { Triple(it.minBpmFilter, it.timeFilterMinutes, it.selectedSessionId) }
        .distinctUntilChanged()
        .flatMapLatest { (bpm, mins, sessionId) ->
            val timestamp = if (mins > 0) {
                OffsetDateTime.now(ZoneOffset.UTC)
                    .minusMinutes(mins.toLong())
                    .format(DateTimeFormatter.ISO_DATE_TIME)
            } else {
                "" 
            }
            val activeSession = if (sessionId.isNullOrBlank()) currentSessionId else sessionId
            localRepository.getFilteredDataBySession(activeSession, bpm, timestamp)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * LEARNING POINT: Datenquellen-Umschaltung (Live vs. Historisch)
     * 
     * `activeGraphData` entscheidet reaktiv, welche Daten an den Graphen (`GraphScreen`) übergeben werden:
     * - Im Live-Modus (`showLiveGraph = true`): Die flüchtigen Live-Daten aus dem Arbeitsspeicher (`uiState.fitnessHistory`).
     * - Im historischen Modus (`showLiveGraph = false`): Die aus der Room-Datenbank gefilterten und konvertierten Datensätze.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val activeGraphData = uiState
        .map { it.showLiveGraph }
        .distinctUntilChanged()
        .flatMapLatest { isLive ->
            if (isLive) {
                uiState.map { it.fitnessHistory }
            } else {
                fitnessHistoryLog.map { list ->
                    list.map { FitnessData(it.fitnessScore, it.timestamp, it.heartRate) }
                        .reversed() 
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 4. DATA POLLING ENGINE ---
    private var pollingJob: Job? = null

    fun setMinBpmFilter(value: Int) { _uiState.update { it.copy(minBpmFilter = value) } }
    fun setTimeFilter(minutes: Int) { _uiState.update { it.copy(timeFilterMinutes = minutes) } }
    fun setGraphSource(isLive: Boolean) { _uiState.update { it.copy(showLiveGraph = isLive) } }
    
    /**
     * Master Switch: Toggles the background data collection.
     * Erzeugt jedes Mal eine neue SessionID, wenn Live-Streaming eingeschaltet wird.
     */
    fun setPollingEnabled(enabled: Boolean) { 
        _uiState.update { it.copy(isPollingEnabled = enabled) } 
        if (enabled) {
            currentSessionId = generateNewSessionId()
            setSelectedSessionId(currentSessionId)
            startPollingInternal()
        } else {
            stopPollingInternal()
        }
    }

    private fun startPollingInternal() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            try {
                while (true) {
                    performHttpRequest()
                    delay(2000L)
                }
            } catch (e: Exception) { /* Cancelled */ }
        }
    }

    private fun stopPollingInternal() {
        pollingJob?.cancel()
        pollingJob = null
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
                    val newHistory = (state.fitnessHistory + data).takeLast(100)
                    state.copy(
                        lastReceivedData = data,
                        fitnessHistory = newHistory,
                        rawResponse = raw,
                        isLoading = false,
                        errorMessage = null
                    )
                }

                // Automatic Persistence with Session ID
                localRepository.saveReading(
                    timestamp = data.isotimestamp,
                    bpm = data.puls,
                    fitness = data.fitness,
                    sessionId = currentSessionId
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    errorMessage = "Network Error: ${e.localizedMessage}",
                    isLoading = false
                )}
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            localRepository.clearHistory()
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
