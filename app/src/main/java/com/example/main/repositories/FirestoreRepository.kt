package com.example.main.repositories

import android.util.Log
import com.example.main.db.FitnessEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * LEARNING POINT: Cloud Firestore Repository (NoSQL Database)
 * 
 * Dieses Repository kapselt die gesamte Kommunikation mit **Cloud Firestore**.
 * 
 * ARCHITEKTUR & KONZEPT:
 * - Im Gegensatz zu SQL (Room) ist Firestore eine **NoSQL-Dokumentendatenbank**. 
 * - Es gibt keine starren Tabellen oder Spalten, sondern **Collections** (Sammlungen) und **Documents** (Dokumente).
 * - Wir nutzen eine hierarchische Pfadstruktur, um Daten nutzer- und sitzungsspezifisch zu isolieren:
 *   `users/{uid}/sessions/{sessionId}/readings/{timestamp}`
 */
class FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "FirestoreRepository"

    /**
     * LEARNING POINT: Cloud-Speicherung (Push-Sync)
     * 
     * Speichert einen einzelnen Fitness-Messwert in Firestore.
     * 1. Erstellt/aktualisiert das übergeordnete Session-Dokument (`sessions/{sessionId}`).
     * 2. Speichert den Messwert als Dokument in der Subcollection `readings`.
     */
    suspend fun saveReadingToCloud(sessionId: String, timestamp: String, bpm: Int, fitness: Double): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: run {
                Log.d(TAG, "saveReadingToCloud: User not authenticated")
                return Result.failure(Exception("User not authenticated"))
            }
            
            // 1. Session-Dokument explizit anlegen (wichtig für Firestore-Abfragen!)
            val sessionRef = firestore.collection("users")
                .document(uid)
                .collection("sessions")
                .document(sessionId)
            
            sessionRef.set(mapOf("sessionId" to sessionId, "createdAt" to timestamp)).await()

            // 2. Messwert in der Subcollection 'readings' speichern
            val readingData = mapOf(
                "timestamp" to timestamp,
                "heartRate" to bpm,
                "fitnessScore" to fitness,
                "sessionId" to sessionId
            )

            sessionRef.collection("readings")
                .document(timestamp)
                .set(readingData)
                .await()

            Log.d(TAG, "saveReadingToCloud success: session=$sessionId, timestamp=$timestamp")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "saveReadingToCloud error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * LEARNING POINT: Batch-Writes (Massen-Synchronisation)
     * 
     * Wenn mehrere lokale Messwerte offline gesammelt wurden, nutzen wir einen **WriteBatch**.
     * Alle Schreibvorgänge werden atomar gebündelt und in einer einzigen Netzwerktransaktion 
     * an Firestore übertragen (spontane Performance und Bandbreitenschonung).
     */
    suspend fun syncReadings(readings: List<FitnessEntity>): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: run {
                Log.d(TAG, "syncReadings: User not authenticated")
                return Result.failure(Exception("User not authenticated"))
            }
            val batch = firestore.batch()

            for (reading in readings) {
                val sessionRef = firestore.collection("users")
                    .document(uid)
                    .collection("sessions")
                    .document(reading.sessionId)
                
                batch.set(sessionRef, mapOf("sessionId" to reading.sessionId, "createdAt" to reading.timestamp))

                val ref = sessionRef.collection("readings")
                    .document(reading.timestamp)

                val data = mapOf(
                    "timestamp" to reading.timestamp,
                    "heartRate" to reading.heartRate,
                    "fitnessScore" to reading.fitnessScore,
                    "sessionId" to reading.sessionId
                )
                batch.set(ref, data)
            }
            batch.commit().await()
            Log.d(TAG, "syncReadings success: ${readings.size} readings synced")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "syncReadings error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * LEARNING POINT: Inkrementeller Diff-Sync (Pull-Sync)
     * 
     * Lädt nur die Messwerte von solchen Sessions aus Firestore herunter, 
     * die lokal in der Room DB noch **nicht** existieren (`localSessions`-Abgleich).
     * Das verhindert unnötigen Traffic und schont das Datenvolumen.
     */
    suspend fun fetchMissingCloudReadings(localSessions: List<String>): List<FitnessEntity> {
        val readings = mutableListOf<FitnessEntity>()
        val uid = auth.currentUser?.uid ?: run {
            Log.d(TAG, "fetchMissingCloudReadings: User not authenticated")
            return emptyList()
        }
        
        try {
            Log.d(TAG, "fetchMissingCloudReadings: Querying sessions for uid=$uid, localSessions=$localSessions")
            val sessionsSnapshot = firestore.collection("users")
                .document(uid)
                .collection("sessions")
                .get()
                .await()

            for (sessionDoc in sessionsSnapshot.documents) {
                val sessionId = sessionDoc.id
                
                // DIFF-SYNC: Wenn die Session lokal bereits existiert, überspringen wir den Download!
                if (sessionId in localSessions) {
                    Log.d(TAG, "fetchMissingCloudReadings: Skipping already local session: $sessionId")
                    continue
                }

                Log.d(TAG, "fetchMissingCloudReadings: Fetching missing session: $sessionId")
                val readingsSnapshot = sessionDoc.reference
                    .collection("readings")
                    .get()
                    .await()

                for (readingDoc in readingsSnapshot.documents) {
                    val timestamp = readingDoc.getString("timestamp") ?: continue
                    val heartRate = readingDoc.getLong("heartRate")?.toInt() ?: 0
                    val fitnessScore = readingDoc.getDouble("fitnessScore") ?: 0.0

                    readings.add(
                        FitnessEntity(
                            timestamp = timestamp,
                            userId = uid,
                            sessionId = sessionId,
                            heartRate = heartRate,
                            fitnessScore = fitnessScore,
                            isSynced = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchMissingCloudReadings error: ${e.message}", e)
            throw e
        }
        Log.d(TAG, "fetchMissingCloudReadings: Total fetched missing readings = ${readings.size}")
        return readings
    }
}
