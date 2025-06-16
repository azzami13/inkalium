package com.example.kaliumapp.data.repository

import com.example.kaliumapp.data.database.dao.WalkSessionDao
import com.example.kaliumapp.data.database.entities.WalkSession
import com.example.kaliumapp.data.database.entities.LocationPoint
import javax.inject.Inject

class TrackingRepository @Inject constructor(
    private val dao: WalkSessionDao
) {

    suspend fun saveSession(session: WalkSession, points: List<LocationPoint>) {
        dao.insertSessionWithPoints(session, points)
    }

    suspend fun getAllSessions(userId: Int): List<WalkSession> {
        return dao.getAllSessions(userId)
    }

    suspend fun getPointsForSession(sessionId: Int): List<LocationPoint> {
        return dao.getPointsForSession(sessionId)
    }

    suspend fun clearUserTrackingData(userId: Int) {
        dao.deleteAllUserSessions(userId)
    }
}