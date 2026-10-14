package com.example.main.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * LEARNING POINT: Room Entity
 * This class represents a table in the database.
 * Each property is a column, including sessionId to group readings by training sessions.
 */
@Entity(tableName = "fitness_log")
data class FitnessEntity(
    @PrimaryKey
    val timestamp: String,
    val sessionId: String = "Default Session",
    val heartRate: Int,
    val fitnessScore: Double
)
