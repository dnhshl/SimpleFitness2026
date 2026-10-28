package com.example.main.repositories

import com.example.main.db.FitnessDao
import com.example.main.db.FitnessEntity
import kotlinx.coroutines.flow.Flow

/**
 * LEARNING POINT: Repository Pattern
 * Abstracts the Room DB data source, supporting multi-user isolation (userId) and sync.
 */
class LocalFitnessRepository(private val fitnessDao: FitnessDao) {

    fun getAllSessions(userId: String): Flow<List<String>> = fitnessDao.getAllSessions(userId)

    suspend fun getAllSessionsList(userId: String): List<String> = fitnessDao.getAllSessionsList(userId)

    suspend fun saveReading(timestamp: String, bpm: Int, fitness: Double, sessionId: String, userId: String) {
        val entity = FitnessEntity(timestamp, userId, sessionId, bpm, fitness, isSynced = false)
        fitnessDao.insert(entity)
    }

    suspend fun saveReadingPreSynced(timestamp: String, sessionId: String, bpm: Int, fitness: Double, userId: String) {
        val entity = FitnessEntity(timestamp, userId, sessionId, bpm, fitness, isSynced = true)
        fitnessDao.insert(entity)
    }

    fun getFilteredDataBySession(userId: String, sessionId: String, minBpm: Int, minTimestamp: String): Flow<List<FitnessEntity>> {
        return fitnessDao.getFilteredLogsBySession(userId, sessionId, minBpm, minTimestamp)
    }

    suspend fun getUnsyncedReadings(userId: String): List<FitnessEntity> {
        return fitnessDao.getUnsyncedReadings(userId)
    }

    suspend fun markAsSynced(timestamps: List<String>) {
        fitnessDao.markAsSynced(timestamps)
    }

    suspend fun clearHistory() {
        fitnessDao.deleteAll()
    }
}
