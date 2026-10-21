package com.example.main.statemgmt

import kotlinx.serialization.Serializable

/**
 * Data model for the Fitness API response.
 * This matches the JSON structure: {"fitness":57.5, "isotimestamp":"...", "puls":87}
 */
@Serializable
data class FitnessData(
    val fitness: Double,
    val isotimestamp: String,
    val puls: Int
)

/**
 * 1. UI STATE (Volatile)
 */
data class UiState(
    // Current Data
    val lastReceivedData: FitnessData? = null,
    
    // History for Graphing
    val fitnessHistory: List<FitnessData> = emptyList(),

    val rawResponse: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // NEW: Filtering and View Settings
    val minBpmFilter: Int = 0,
    val timeFilterMinutes: Int = 0,
    val showLiveGraph: Boolean = true,
    val isPollingEnabled: Boolean = false,
    val snackbarMessage: String? = null,
    val selectedSessionId: String? = null,

    // Provider-agnostic Authentication State
    val isAuthenticated: Boolean = false,
    val currentUserId: String? = null,
    val currentUserEmail: String? = null,
    val currentUserDisplayName: String? = null
)

/**
 * 2. PERSISTENT UI STATE (Saved to Disk)
 */
@Serializable
data class PersistentUiState(
    val lastUsedTimestamp: Long = 0L
)
