package com.example.main.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * LEARNING POINT: Room Entity
 * This class represents a table in the database.
 * Each property is a column.
 */
@Entity(tableName = "fitness_log")
data class FitnessEntity(
    // We use the timestamp as the primary key to avoid duplicate entries
    @PrimaryKey
    val timestamp: String,
    val heartRate: Int,
    val fitnessScore: Double
)
