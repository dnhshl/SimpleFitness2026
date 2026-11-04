package com.example.main.statemgmt

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.main.db.AppDatabase
import com.example.main.repositories.BleRepository
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
 * (Netzwerk via Ktor, Bluetooth via Kable und lokale Room Database) und verwaltet den UI-Zustand.
 */
class AppViewModel(application: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {

    // --- REPOSITORIES & DATA SOURCES ---
    private val dataStoreManager = DataStoreManager(application)
    private val httpRepository = HttpRepository(application)
    private val authRepository = FirebaseAuthRepository()
    private val firestoreRepository = FirestoreRepository()
    private val bleRepository = BleRepository()
    
    // LEARNING POINT: Database Initialization
    private val database = AppDatabase.getDatabase(application)
    private val localRepository = LocalFitnessRepository(database.fitnessDao())

    // --- BLE JOBS ---
    private var scanJob: Job? = null
    private var bleCollectJob: Job? = null

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
        // Reactive listener for Firebase Auth state
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

        if (_uiState.value.isPollingEnabled && _uiState.value.dataSource == DataSource.HTTP) {
            startPollingInternal()
        }
    }

    // --- BLE MANAGEMENT ---

    fun setDataSource(source: DataSource) {
        _uiState.update { it.copy(dataSource = source) }
        if (source == DataSource.HTTP && _uiState.value.isPollingEnabled) {
            startPollingInternal()
        } else {
            stopPollingInternal()
        }
    }

    fun startScanning() {
        if (_uiState.value.isScanning) return
        _uiState.update { it.copy(isScanning = true, foundDevices = emptyList(), errorMessage = null) }
        scanJob = viewModelScope.launch {
            try {
                bleRepository.advertisements.collect { device ->
                    _uiState.update { state ->
                        if (state.foundDevices.any { it.address == device.address }) state
                        else state.copy(foundDevices = state.foundDevices + device)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Scan Error: ${e.localizedMessage}", isScanning = false) }
            }
        }
    }

    fun stopScanning() {
        scanJob?.cancel()
        scanJob = null
        _uiState.update { it.copy(isScanning = false) }
    }

    fun connectToDevice(address: String) {
        stopScanning()
        _uiState.update { it.copy(connectionState = ConnectionState.Connecting, errorMessage = null) }
        viewModelScope.launch {
            try {
                val fitnessFlow = bleRepository.connect(address)
                _uiState.update { it.copy(connectionState = ConnectionState.Connected, dataSource = DataSource.BLE) }
                
                viewModelScope.launch { dataStoreManager.saveLastDevice(address) }

                bleCollectJob = viewModelScope.launch {
                    fitnessFlow.collect { fitnessData ->
                        handleNewFitnessData(fitnessData, rawJson = "Puls: ${fitnessData.puls}, Fitness: ${fitnessData.fitness}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(connectionState = ConnectionState.Disconnected, errorMessage = "Connection Error: ${e.localizedMessage}") }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            bleCollectJob?.cancel()
            bleCollectJob = null
            bleRepository.disconnect()
            _uiState.update { it.copy(connectionState = ConnectionState.Disconnected) }
        }
    }

    fun toggleAutoConnect(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreManager.saveAutoConnect(enabled)
        }
    }

    /**
     * LEARNING POINT: Unified Data Ingestion
     * Verarbeitet neue Fitness-Daten unabhängig von der Quelle (HTTP oder BLE),
     * aktualisiert den UI-Zustand, speichert lokal in Room und sichert in Firestore.
     */
    private fun handleNewFitnessData(data: FitnessData, rawJson: String) {
        _uiState.update { state -> 
            val newHistory = (state.fitnessHistory + data).takeLast(100)
            state.copy(
                lastReceivedData = data,
                fitnessHistory = newHistory,
                rawResponse = rawJson,
                isLoading = false,
                errorMessage = null
            )
        }

        // 1. Automatic Local Persistence (Room DB - Offline First)
        if (_uiState.value.isPollingEnabled) {
            viewModelScope.launch {
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
            }
        }
    }

    /**
     * LEARNING POINT: Explicit Cloud-to-Local Sync (Pull-Sync)
     * Wird vom Nutzer über den Home-Screen-Button (Sync with Cloud) ausgelöst.
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
                    setSelectedSessionId(null)
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
                    setSelectedSessionId(null)
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

    fun signOut() {
        authRepository.signOut()
        updateAuthState()
        setSelectedSessionId(null)
        navigateTo(Screen.Home)
    }

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

    val persistentUiState: StateFlow<PersistentUiState> = dataStoreManager.persistentUiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PersistentUiState()
        )

    data class HistoryFilter(val bpm: Int, val mins: Int, val sessionId: String?, val userId: String?)

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

    private var pollingJob: Job? = null

    fun setMinBpmFilter(value: Int) { _uiState.update { it.copy(minBpmFilter = value) } }
    fun setTimeFilter(minutes: Int) { _uiState.update { it.copy(timeFilterMinutes = minutes) } }
    fun setGraphSource(isLive: Boolean) { _uiState.update { it.copy(showLiveGraph = isLive) } }
    fun setSelectedSessionId(sessionId: String?) { _uiState.update { it.copy(selectedSessionId = sessionId) } }
    
    fun setPollingEnabled(enabled: Boolean) { 
        _uiState.update { it.copy(isPollingEnabled = enabled) } 
        if (enabled) {
            currentSessionId = generateNewSessionId()
            setSelectedSessionId(currentSessionId)
            
            if (_uiState.value.dataSource == DataSource.HTTP) {
                startPollingInternal()
            } else if (_uiState.value.dataSource == DataSource.BLE) {
                viewModelScope.launch {
                    try {
                        bleRepository.sendCommand(Esp32Command(LED = "H", LEDBlinken = true))
                    } catch (e: Exception) { /* Ignore */ }
                }
            }
        } else {
            stopPollingInternal()
            if (_uiState.value.dataSource == DataSource.BLE) {
                viewModelScope.launch {
                    try {
                        bleRepository.sendCommand(Esp32Command(LED = "L", LEDBlinken = false))
                    } catch (e: Exception) { /* Ignore */ }
                }
            }
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

    fun performHttpRequest() {
        viewModelScope.launch {
            if (_uiState.value.lastReceivedData == null) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            
            try {
                val data = httpRepository.fetchFitnessData()
                val raw = httpRepository.fetchRawJson()
                handleNewFitnessData(data, raw)
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

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbarMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun getStringResource(@StringRes resId: Int): String {
        return getApplication<Application>().getString(resId)
    }

    private fun getStringResource(@StringRes resId: Int, vararg formatArgs: Any): String {
        return getApplication<Application>().getString(resId, *formatArgs)
    }
}
