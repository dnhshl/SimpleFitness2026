package com.example.main.repositories

import com.example.main.db.FitnessDao
import com.example.main.db.FitnessEntity
import kotlinx.coroutines.flow.Flow

/**
 * LEARNING POINT: Repository Pattern
 * Abstracts the Room DB data source, supporting session-based queries.
 */
class LocalFitnessRepository(private val fitnessDao: FitnessDao) {

    val allSessions: Flow<List<String>> = fitnessDao.getAllSessions()

    suspend fun saveReading(timestamp: String, bpm: Int, fitness: Double, sessionId: String) {
        val entity = FitnessEntity(timestamp, sessionId, bpm, fitness)
        fitnessDao.insert(entity)
    }

    fun getFilteredDataBySession(sessionId: String, minBpm: Int, minTimestamp: String): Flow<List<FitnessEntity>> {
        return fitnessDao.getFilteredLogsBySession(sessionId, minBpm, minTimestamp)
    }

    suspend fun clearHistory() {
        fitnessDao.deleteAll()
    }
}
