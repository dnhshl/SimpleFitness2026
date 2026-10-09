package com.example.main.repositories

import com.example.main.db.FitnessDao
import com.example.main.db.FitnessEntity
import kotlinx.coroutines.flow.Flow

/**
 * LEARNING POINT: Repository Pattern
 * This class abstracts the data source (Room DB) from the rest of the app.
 * The ViewModel doesn't need to know about SQL or DAOs.
 */
class LocalFitnessRepository(private val fitnessDao: FitnessDao) {

    // Expose data as a stream
    val allLogs: Flow<List<FitnessEntity>> = fitnessDao.getAllLogs()

    // Save a single reading
    suspend fun saveReading(timestamp: String, bpm: Int, fitness: Double) {
        val entity = FitnessEntity(timestamp, bpm, fitness)
        fitnessDao.insert(entity)
    }

    // Filter logic: Heart rate and Time
    fun getFilteredData(minBpm: Int, minTimestamp: String): Flow<List<FitnessEntity>> {
        return fitnessDao.getFilteredLogs(minBpm, minTimestamp)
    }

    // Clear everything
    suspend fun clearHistory() {
        fitnessDao.deleteAll()
    }
}
