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
 * This class handles saving/loading the entire 'PersistentUiState' object 
 * as a single JSON string.
 */
class DataStoreManager(private val context: Context) {

    private val jsonKey = stringPreferencesKey("app_state_json")

    // Read the entire state object from DataStore
    val persistentUiState: Flow<PersistentUiState> = context.dataStore.data.map { preferences ->
        val json = preferences[jsonKey]
        if (json != null) {
            try {
                Json.decodeFromString<PersistentUiState>(json)
            } catch (e: Exception) {
                PersistentUiState() // Return default state on error
            }
        } else {
            PersistentUiState() // Return default state if no data exists
        }
    }

    // Write the entire state object to DataStore
    suspend fun updatePersistentState(update: (PersistentUiState) -> PersistentUiState) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[jsonKey]
            val current = if (currentJson != null) {
                try { Json.decodeFromString<PersistentUiState>(currentJson) } catch (e: Exception) { PersistentUiState() }
            } else {
                PersistentUiState()
            }
            
            val updated = update(current)
            preferences[jsonKey] = Json.encodeToString(updated)
        }
    }
}
