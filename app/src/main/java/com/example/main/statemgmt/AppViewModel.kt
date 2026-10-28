package com.example.main.statemgmt

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.main.db.AppDatabase
import com.example.main.repositories.HttpRepository
import com.example.main.repositories.FirebaseAuthRepository
import com.example.main.repositories.FirestoreRepository
import com.example.main.repositories.LocalFitnessRepository
import com.example.main.ui.screens.Screen
import com.example.main.R
import com.google.firebase.auth.FirebaseAuth
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
    private val firestoreRepository = FirestoreRepository()
    
    // LEARNING POINT: Database Initialization
    private val database = AppDatabase.getDatabase(application)
    private val localRepository = LocalFitnessRepository(database.fitnessDao())

    // --- SESSION ID MANAGEMENT ---
    var currentSessionId: String = generateNewSessionId()
        private set

    private fun generateNewSessionId(): String {
        return "Session " + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").format(OffsetDateTime.now())
    }

    // --- 1. UI STATE & AUTHENTICATION ---
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    val currentUserId: String
        get() = authRepository.currentUser?.uid ?: "guest"

    @OptIn(ExperimentalCoroutinesApi::class)
    val availableSessions: StateFlow<List<String>> = uiState
        .map { it.currentUserId }
        .distinctUntilChanged()
        .flatMapLatest { uid ->
            localRepository.getAllSessions(uid ?: "guest")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Reactive listener for Firebase Auth state (handles async startup restoration & login/logout)
        val authListener = FirebaseAuth.AuthStateListener { _ ->
            updateAuthState()
            val user = authRepository.currentUser
            if (user != null) {
                syncOfflineDataToCloud()
            }
        }
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
        
        // Auto-select latest session when sessions load or user changes
        viewModelScope.launch {
            availableSessions.collect { sessions ->
                if (_uiState.value.selectedSessionId.isNullOrBlank() && sessions.isNotEmpty()) {
                    setSelectedSessionId(sessions.first())
                }
            }
        }

        if (_uiState.value.isPollingEnabled) {
            startPollingInternal()
        }
    }

    /**
     * LEARNING POINT: Explicit Cloud-to-Local Sync (Pull-Sync)
     * Wird vom Nutzer über den Home-Screen-Button (Sync with Cloud) ausgelöst.
     * Lädt historische Daten aus Firestore herunter und speichert sie in der lokalen Room Database.
     */
    fun syncFromCloud() {
        viewModelScope.launch {
            val user = authRepository.currentUser
            Log.d("AppViewModel", "syncFromCloud: currentUser = ${user?.uid}")
            if (user != null) {
                try {
                    _uiState.update { it.copy(isLoading = true) }
                    val localSessions = localRepository.getAllSessionsList(user.uid)
                    val cloudReadings = firestoreRepository.fetchMissingCloudReadings(localSessions)
                    Log.d("AppViewModel", "syncFromCloud: received ${cloudReadings.size} missing readings from cloud")
                    for (reading in cloudReadings) {
                        localRepository.saveReadingPreSynced(
                            timestamp = reading.timestamp,
                            sessionId = reading.sessionId,
                            bpm = reading.heartRate,
                            fitness = reading.fitnessScore,
                            userId = user.uid
                        )
                    }
                    _uiState.update { it.copy(isLoading = false) }
                    if (cloudReadings.isNotEmpty()) {
                        showSnackbar(getStringResource(R.string.success_cloud_synced))
                    } else {
                        showSnackbar("Already up to date!")
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false) }
                    Log.e("AppViewModel", "syncFromCloud exception: ${e.message}", e)
                    showSnackbar("Sync Error: ${e.localizedMessage}")
                }
            } else {
                showSnackbar(getStringResource(R.string.error_login_required_nickname))
            }
        }
    }

    /**
     * LEARNING POINT: Offline-First Synchronization (Push-Sync)
     * Prüft, ob es lokal in Room ungesyncte Einträge für diesen Nutzer gibt (isSynced = 0),
     * und lädt diese im Hintergrund nach Firestore hoch, sobald eine Verbindung besteht.
     */
    private fun syncOfflineDataToCloud() {
        viewModelScope.launch {
            val user = authRepository.currentUser
            if (user != null) {
                try {
                    val unsynced = localRepository.getUnsyncedReadings(user.uid)
                    if (unsynced.isNotEmpty()) {
                        val result = firestoreRepository.syncReadings(unsynced)
                        if (result.isSuccess) {
                            val timestamps = unsynced.map { it.timestamp }
                            localRepository.markAsSynced(timestamps)
                        }
                    }
                } catch (e: Exception) {
                    // Offline - retry later
                }
            }
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
                    setSelectedSessionId(null) // Reset session selection for new user
                    syncOfflineDataToCloud()
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
                    setSelectedSessionId(null) // Reset session selection for new user
                    syncOfflineDataToCloud()
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
        setSelectedSessionId(null) // Reset session selection for guest/new user
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
    
    data class HistoryFilter(val bpm: Int, val mins: Int, val sessionId: String?, val userId: String?)

    /**
     * LEARNING POINT: Reaktive Datenverarbeitung & Flow-Kombination mit Multi-User-Isolation
     * 
     * `fitnessHistoryLog` beobachtet den UI-Zustand (Filter wie Puls, Zeit, SessionID und UserID) 
     * und lädt Daten automatisch reaktiv aus der Room Database für den aktuell aktiven Nutzer.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val fitnessHistoryLog = uiState
        .map { state ->
            HistoryFilter(state.minBpmFilter, state.timeFilterMinutes, state.selectedSessionId, state.currentUserId)
        }
        .distinctUntilChanged()
        .flatMapLatest { filter ->
            val timestamp = if (filter.mins > 0) {
                OffsetDateTime.now(ZoneOffset.UTC)
                    .minusMinutes(filter.mins.toLong())
                    .format(DateTimeFormatter.ISO_DATE_TIME)
            } else {
                "" 
            }
            val activeSession = if (filter.sessionId.isNullOrBlank()) {
                availableSessions.value.firstOrNull() ?: currentSessionId
            } else {
                filter.sessionId
            }
            val activeUser = filter.userId ?: "guest"
            localRepository.getFilteredDataBySession(activeUser, activeSession, filter.bpm, timestamp)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * LEARNING POINT: Datenquellen-Umschaltung (Live vs. Historisch)
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

                // 1. Automatic Local Persistence (Room DB - Offline First) mit aktiver UserID
                localRepository.saveReading(
                    timestamp = data.isotimestamp,
                    bpm = data.puls,
                    fitness = data.fitness,
                    sessionId = currentSessionId,
                    userId = currentUserId
                )

                // 2. Cloud Backup (Firestore - Online Sync / Backup)
                if (authRepository.currentUser != null) {
                    val result = firestoreRepository.saveReadingToCloud(
                        sessionId = currentSessionId,
                        timestamp = data.isotimestamp,
                        bpm = data.puls,
                        fitness = data.fitness
                    )
                    if (result.isSuccess) {
                        localRepository.markAsSynced(listOf(data.isotimestamp))
                    }
                }
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
