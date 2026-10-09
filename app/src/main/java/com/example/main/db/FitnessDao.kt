package com.example.main.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * LEARNING POINT: DAO (Data Access Object)
 * This interface defines the SQL queries mapping to Kotlin functions.
 */
@Dao
interface FitnessDao {

    // 1. STORE: Save new data. Ignore if the timestamp already exists.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(fitness: FitnessEntity)

    // 2. RETRIEVE: Get all logs, sorted by time (newest first).
    // We return a 'Flow' so the UI updates automatically when the DB changes.
    @Query("SELECT * FROM fitness_log ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<FitnessEntity>>

    // 3. FILTER: Get data based on heart rate AND time.
    // Students can learn how SQL 'AND' works here.
    @Query("SELECT * FROM fitness_log WHERE heartRate >= :minBpm AND timestamp >= :minTimestamp ORDER BY timestamp DESC")
    fun getFilteredLogs(minBpm: Int, minTimestamp: String): Flow<List<FitnessEntity>>

    // 4. DELETE: Clear history.
    @Query("DELETE FROM fitness_log")
    suspend fun deleteAll()
}
