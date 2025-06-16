package com.example.kaliumapp.data.database.entities

import androidx.room.*

@Entity(
    tableName = "daily_water_summary",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "date"], unique = true) // Prevent duplicate entries for same user and date
    ]
)
data class DailyWaterSummary(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "summary_id")
    val summaryId: Int = 0,

    @ColumnInfo(name = "userId")
    val userId: Int,

    @ColumnInfo(name = "date")
    val date: String, // Format: "yyyy-MM-dd"

    @ColumnInfo(name = "totalIntakeMl")
    val totalIntakeMl: Int = 0,

    @ColumnInfo(name = "remainingMl")
    val remainingMl: Int = 0,

    @ColumnInfo(name = "status")
    val status: String = "Active", // Active, Completed, Paused

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)