package com.example.kaliumapp.data.database.entities

import androidx.room.*

@Entity(
    tableName = "calorie_expenditure",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["userId"])]
)
data class CalorieExpenditure(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val calories: Float,
    val date: String,
    val timestamp: Long = System.currentTimeMillis()
)