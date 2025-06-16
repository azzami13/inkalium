package com.example.kaliumapp.data.database.entities

import androidx.room.*

@Entity(
    tableName = "water_reminder",
    foreignKeys = [ForeignKey(
        entity = DailyWaterSummary::class,
        parentColumns = ["summary_id"],
        childColumns = ["summary_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["summary_id"])]
)
data class WaterReminder(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "reminder_id")
    val reminderId: Int = 0,
    @ColumnInfo(name = "summary_id")
    val summaryId: Int,
    @ColumnInfo(name = "reminder_time")
    val reminderTime: Long,
    val status: String
)