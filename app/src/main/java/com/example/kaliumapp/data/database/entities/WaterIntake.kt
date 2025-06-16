package com.example.kaliumapp.data.database.entities

import androidx.room.*

@Entity(
    tableName = "water_intake",
    foreignKeys = [ForeignKey(
        entity = DailyWaterSummary::class,
        parentColumns = ["summary_id"],
        childColumns = ["summary_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["summary_id"])]
)
data class WaterIntake(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "intake_id")
    val intakeId: Int = 0,
    @ColumnInfo(name = "summary_id")
    val summaryId: Int,
    @ColumnInfo(name = "intake_ml")
    val intakeMl: Int,
    @ColumnInfo(name = "intake_time")
    val intakeTime: Long = System.currentTimeMillis()
)