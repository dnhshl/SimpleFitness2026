package com.example.main.statemgmt

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Extension property to create the DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * A central DataStore manager that uses JSON serialization.
 * 
 * You usually don't need to change anything here.
 * This class handles saving/loading the entire 'PersistantUiState' object 
 * as a single JSON string.
 */
class DataStoreManager(private val context: Context) {

    private val jsonKey = stringPreferencesKey("app_state_json")

    // Read the entire state object from DataStore
    val persistantUiState: Flow<PersistantUiState> = context.dataStore.data.map { preferences ->
        val json = preferences[jsonKey]
        if (json != null) {
            try {
                Json.decodeFromString<PersistantUiState>(json)
            } catch (e: Exception) {
                PersistantUiState() // Return default state on error
            }
        } else {
            PersistantUiState() // Return default state if no data exists
        }
    }

    // Write the entire state object to DataStore
    suspend fun updatePersistantState(update: (PersistantUiState) -> PersistantUiState) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[jsonKey]
            val current = if (currentJson != null) {
                try { Json.decodeFromString<PersistantUiState>(currentJson) } catch (e: Exception) { PersistantUiState() }
            } else {
                PersistantUiState()
            }
            
            val updated = update(current)
            preferences[jsonKey] = Json.encodeToString(updated)
        }
    }
}
