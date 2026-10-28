package com.example.main.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * LEARNING POINT: Room Database
 * The main access point to the local database.
 * Version 3: Added multi-user isolation (userId) and sync flags.
 */
@Database(entities = [FitnessEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun fitnessDao(): FitnessDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitness_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
