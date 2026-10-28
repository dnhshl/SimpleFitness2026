package com.example.main.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * LEARNING POINT: DAO (Data Access Object)
 * 
 * Das DAO ist das Herzstück der Room Database. Hier definieren wir die SQL-Befehle 
 * als Kotlin-Funktionen inkl. Multi-User-Datenschutzfilterung (userId).
 */
@Dao
interface FitnessDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(fitness: FitnessEntity)

    @Query("SELECT DISTINCT sessionId FROM fitness_log WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllSessions(userId: String): Flow<List<String>>

    @Query("SELECT DISTINCT sessionId FROM fitness_log WHERE userId = :userId")
    suspend fun getAllSessionsList(userId: String): List<String>

    @Query("SELECT * FROM fitness_log WHERE userId = :userId AND sessionId = :sessionId AND heartRate >= :minBpm AND timestamp >= :minTimestamp ORDER BY timestamp DESC")
    fun getFilteredLogsBySession(userId: String, sessionId: String, minBpm: Int, minTimestamp: String): Flow<List<FitnessEntity>>

    @Query("SELECT * FROM fitness_log WHERE userId = :userId AND isSynced = 0")
    suspend fun getUnsyncedReadings(userId: String): List<FitnessEntity>

    @Query("UPDATE fitness_log SET isSynced = 1 WHERE timestamp IN (:timestamps)")
    suspend fun markAsSynced(timestamps: List<String>)

    @Query("DELETE FROM fitness_log")
    suspend fun deleteAll()
}
