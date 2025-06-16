package com.example.kaliumapp.data.database.entities

import androidx.room.*

@Entity(
    tableName = "walk_sessions",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["userId"])]
)
data class WalkSession(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val startTime: Long,
    val endTime: Long,
    val totalDistance: Float, // in meters
    val avgSpeed: Float // in m/s or km/h
)