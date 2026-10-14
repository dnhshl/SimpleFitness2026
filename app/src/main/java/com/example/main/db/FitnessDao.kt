package com.example.main.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * LEARNING POINT: DAO (Data Access Object)
 * 
 * Das DAO ist das Herzstück der Room Database. Hier definieren wir die SQL-Befehle 
 * als Kotlin-Funktionen. Room generiert zur Compile-Zeit automatisch die Implementierung.
 * 
 * - 'suspend': Für Einmal-Operationen (Write/Delete), die in einer Coroutine laufen.
 * - 'Flow': Für Abfragen (Read), die sich reaktiv bei Änderungen in der Datenbank aktualisieren.
 */
@Dao
interface FitnessDao {

    /**
     * Speichert einen neuen Fitness-Datensatz in der Tabelle 'fitness_log'.
     * @OnConflictStrategy.IGNORE sorgt dafür, dass Duplikate (gleicher Timestamp) ignoriert werden.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(fitness: FitnessEntity)

    /**
     * SQL Abfrage: Liefert eine Liste aller eindeutigen Session-IDs (DISTINCT),
     * sortiert nach dem neuesten Timestamp absteigend.
     * Rückgabe als 'Flow', damit sich die UI bei neuen Sessions automatisch aktualisiert.
     */
    @Query("SELECT DISTINCT sessionId FROM fitness_log ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<String>>

    /**
     * Komplexe SQL Abfrage mit Filterung (WHERE) und Sortierung (ORDER BY):
     * - Filtert nach spezifischer SessionID
     * - Filtert nach minimalem Puls (heartRate >= :minBpm)
     * - Filtert nach Zeitpunkt (timestamp >= :minTimestamp)
     * - Sortiert absteigend nach Zeit (neueste zuerst)
     */
    @Query("SELECT * FROM fitness_log WHERE sessionId = :sessionId AND heartRate >= :minBpm AND timestamp >= :minTimestamp ORDER BY timestamp DESC")
    fun getFilteredLogsBySession(sessionId: String, minBpm: Int, minTimestamp: String): Flow<List<FitnessEntity>>

    /**
     * Löscht alle Datensätze in der 'fitness_log' Tabelle.
     */
    @Query("DELETE FROM fitness_log")
    suspend fun deleteAll()
}
