package com.example.main.statemgmt

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.main.db.AppDatabase
import com.example.main.repositories.HttpRepository
import com.example.main.repositories.FirebaseAuthRepository
import com.example.main.repositories.LocalFitnessRepository
import com.example.main.ui.screens.Screen
import com.example.main.R
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
 * 
 * Das ViewModel verbindet die UI (Compose Screens) mit den Datenquellen 
 * (Netzwerk via Ktor und lokale Room Database) und verwaltet den UI-Zustand.
 */
class AppViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    // --- REPOSITORIES & DATA SOURCES ---
    private val dataStoreManager = DataStoreManager(application)
    private val httpRepository = HttpRepository(application)
    private val authRepository = FirebaseAuthRepository()
    
    // LEARNING POINT: Database Initialization
    private val database = AppDatabase.getDatabase(application)
    private val localRepository = LocalFitnessRepository(database.fitnessDao())

    // --- SESSION ID MANAGEMENT ---
    var currentSessionId: String = generateNewSessionId()
        private set

    private fun generateNewSessionId(): String {
        return "Session " + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").format(OffsetDateTime.now())
    }

    val availableSessions: StateFlow<List<String>> = localRepository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- 1. UI STATE & AUTHENTICATION ---
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        updateAuthState()
        if (_uiState.value.isPollingEnabled) {
            startPollingInternal()
        }
    }

    /**
     * Aktualisiert den provider-unabhängigen Authentifizierungsstatus im UI-Zustand.
     */
    private fun updateAuthState() {
        val user = authRepository.currentUser
        _uiState.update { 
            it.copy(
                isAuthenticated = user != null,
                currentUserId = user?.uid,
                currentUserEmail = user?.email,
                currentUserDisplayName = user?.displayName
            )
        }
    }

    /**
     * Meldet einen Benutzer mit E-Mail und Passwort via Firebase Auth an.
     */
    fun signIn(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            showSnackbar(getStringResource(R.string.error_empty_credentials))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signIn(email, pass)
            result.fold(
                onSuccess = { _ ->
                    updateAuthState()
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    showSnackbar(getStringResource(R.string.success_signed_in))
                    navigateTo(Screen.Home)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    showSnackbar(getStringResource(R.string.error_login, error.localizedMessage ?: ""))
                }
            )
        }
    }

    /**
     * Registriert einen neuen Benutzer bei Firebase Auth.
     */
    fun signUp(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            showSnackbar(getStringResource(R.string.error_empty_credentials))
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signUp(email, pass)
            result.fold(
                onSuccess = { _ ->
                    updateAuthState()
                    _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                    showSnackbar(getStringResource(R.string.success_account_created))
                    navigateTo(Screen.Home)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    showSnackbar(getStringResource(R.string.error_registration, error.localizedMessage ?: ""))
                }
            )
        }
    }

    /**
     * Meldet den aktuellen Benutzer von Firebase Auth ab.
     */
    fun signOut() {
        authRepository.signOut()
        updateAuthState()
        navigateTo(Screen.Home)
    }

    /**
     * Sendet eine E-Mail zum Zurücksetzen des Passworts.
     */
    fun sendPasswordReset(email: String, onResult: (Boolean, String?) -> Unit) {
        if (email.isBlank()) {
            onResult(false, getStringResource(R.string.error_empty_email))
            return
        }
        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(email)
            result.fold(
                onSuccess = { onResult(true, getStringResource(R.string.success_password_reset)) },
                onFailure = { error -> onResult(false, error.localizedMessage) }
            )
        }
    }

    /**
     * Aktualisiert den Anzeigenamen (Nickname) des angemeldeten Benutzers in Firebase Auth.
     */
    fun setNickname(newNickname: String) {
        viewModelScope.launch {
            if (authRepository.currentUser != null) {
                val result = authRepository.updateNickname(newNickname)
                if (result.isSuccess) {
                    updateAuthState()
                    _uiState.update { it.copy(snackbarMessage = getStringResource(R.string.success_nickname_updated)) }
                } else {
                    _uiState.update { it.copy(snackbarMessage = getStringResource(R.string.error_nickname_update, result.exceptionOrNull()?.localizedMessage ?: "")) }
                }
            } else {
                _uiState.update { it.copy(snackbarMessage = getStringResource(R.string.error_login_required_nickname)) }
            }
        }
    }

    // --- 2. PERSISTENT STATE (Disk storage via DataStore) ---
    val persistentUiState: StateFlow<PersistentUiState> = dataStoreManager.persistentUiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PersistentUiState()
        )

    // --- 3. DATABASE FILTERING & TRANSFORMATIONS (Room DB Reactive Pipeline) ---
    
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

    // --- 4. DATA POLLING ENGINE (Network & Auto-Persistence) ---
    private var pollingJob: Job? = null

    fun setMinBpmFilter(value: Int) { _uiState.update { it.copy(minBpmFilter = value) } }
    fun setTimeFilter(minutes: Int) { _uiState.update { it.copy(timeFilterMinutes = minutes) } }
    fun setGraphSource(isLive: Boolean) { _uiState.update { it.copy(showLiveGraph = isLive) } }
    fun setSelectedSessionId(sessionId: String?) { _uiState.update { it.copy(selectedSessionId = sessionId) } }
    
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
    // BOILERPLATE AREA: NAVIGATION HANDLING & HELPERS
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
            if (screen is Screen.Login) {
                backStack.add(screen)
            } else {
                backStack.clear()
                backStack.add(screen)
            }
            saveBackStack()
        }
    }

    fun popBackStack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
            saveBackStack()
        }
    }

    /**
     * Triggert das Anzeigen einer Snackbar in der UI.
     */
    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    /**
     * Setzt die Snackbar-Nachricht nach dem Anzeigen im UI zurück.
     */
    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    /**
     * Hilfsfunktion zum Auslesen von String-Ressourcen (vermeidet Boilerplate im ViewModel).
     */
    private fun getStringResource(@StringRes resId: Int): String {
        return getApplication<Application>().getString(resId)
    }

    /**
     * Hilfsfunktion zum Auslesen von String-Ressourcen mit Formatierungsparametern.
     */
    private fun getStringResource(@StringRes resId: Int, vararg formatArgs: Any): String {
        return getApplication<Application>().getString(resId, *formatArgs)
    }
}
