package com.example.main.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * LEARNING POINT: Room Entity mit UserId (Multi-User Data Isolation)
 * Das Feld 'userId' sorgt dafür, dass Messwerte datenschutzkonform dem jeweiligen 
 * angemeldeten Benutzer (oder 'guest') zugeordnet werden.
 */
@Entity(tableName = "fitness_log")
data class FitnessEntity(
    @PrimaryKey
    val timestamp: String,
    val userId: String = "guest",
    val sessionId: String = "Default Session",
    val heartRate: Int,
    val fitnessScore: Double,
    @ColumnInfo(defaultValue = "0")
    val isSynced: Boolean = false
)
