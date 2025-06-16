package com.example.kaliumapp.data.database.entities

import androidx.room.*

@Entity(
    tableName = "food_intake",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["userId"])]
)
data class FoodIntake(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val foodName: String,
    val calories: Float,
    val protein: Float,
    val fat: Float,
    val carbs: Float,
    val amountGrams: Float,
    val timestamp: Long = System.currentTimeMillis()
)