package com.example.main.repositories

import android.content.Context
import com.example.main.statemgmt.FitnessData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * The HttpRepository handles network communication using the Ktor library.
 * 
 * Think of this as the "Network Card" of your app. Just like you used SensorRepository
 * to talk to hardware, you use this to talk to the Internet.
 */
class HttpRepository(context: Context) {

    companion object {
        /**
         * CONFIGURATION AREA:
         * You can change this URL to point to your own API or any other public data source.
         * Make sure the FitnessData class in UiState.kt matches the JSON structure of the API.
         */
        private const val BASE_URL = "https://fitness-api.dnoll.info"
    }

    // 1. Setup the Ktor Client
    // We configure it to automatically handle JSON data
    private val client = HttpClient(Android) {
        // Install ContentNegotiation to automatically convert JSON strings into Kotlin Objects
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true // Good practice: if the API adds new fields, the app won't crash
                prettyPrint = true
                isLenient = true
            })
        }
        
        // Logging helps you see the network traffic in the Logcat window
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    /**
     * Fetches fitness data from the configured API.
     * 
     * .body<FitnessData>() automatically parses the JSON into our data class.
     */
    suspend fun fetchFitnessData(): FitnessData {
        // We use the BASE_URL constant defined in the companion object.
        // We explicitly tell the body() function to decode into <FitnessData>.
        return client.get(BASE_URL).body<FitnessData>()
    }
    
    /**
     * If you want to see the "raw" JSON string (e.g., for debugging or 
     * before you have created a data class), you can use this method.
     */
    suspend fun fetchRawJson(): String {
        return client.get(BASE_URL).body<String>()
    }
}
