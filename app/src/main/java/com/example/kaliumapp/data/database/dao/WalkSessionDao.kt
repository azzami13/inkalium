package com.example.kaliumapp.data.database.dao

import androidx.room.*
import com.example.kaliumapp.data.database.entities.WalkSession
import com.example.kaliumapp.data.database.entities.LocationPoint

@Dao
interface WalkSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WalkSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationPoints(points: List<LocationPoint>)

    @Transaction
    suspend fun insertSessionWithPoints(session: WalkSession, points: List<LocationPoint>) {
        val sessionId = insertSession(session)
        val pointsWithSessionId = points.map { it.copy(sessionId = sessionId.toInt()) }
        insertLocationPoints(pointsWithSessionId)
    }

    @Query("SELECT * FROM walk_sessions WHERE userId = :userId ORDER BY startTime DESC")
    suspend fun getAllSessions(userId: Int): List<WalkSession>

    @Query("SELECT * FROM location_points WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getPointsForSession(sessionId: Int): List<LocationPoint>

    @Query("DELETE FROM walk_sessions WHERE userId = :userId")
    suspend fun deleteAllUserSessions(userId: Int)

    @Update
    suspend fun updateSession(session: WalkSession)

    @Delete
    suspend fun deleteSession(session: WalkSession)
}