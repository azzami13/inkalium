package com.example.kaliumapp.data.database.dao

import androidx.room.*
import com.example.kaliumapp.data.database.entities.FoodIntake

@Dao
interface FoodIntakeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(foodIntake: FoodIntake): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMultiple(foodIntakes: List<FoodIntake>): List<Long>

    @Query("SELECT * FROM food_intake WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getFoodIntakesForDay(userId: Int, startTime: Long, endTime: Long): List<FoodIntake>

    @Query("SELECT * FROM food_intake WHERE id = :id")
    suspend fun getFoodIntakeById(id: Int): FoodIntake?

    @Query("SELECT SUM(calories) FROM food_intake WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalCaloriesForDay(userId: Int, startTime: Long, endTime: Long): Float?

    @Query("SELECT SUM(protein) FROM food_intake WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalProteinForDay(userId: Int, startTime: Long, endTime: Long): Float?

    @Query("SELECT SUM(fat) FROM food_intake WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalFatForDay(userId: Int, startTime: Long, endTime: Long): Float?

    @Query("SELECT SUM(carbs) FROM food_intake WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getTotalCarbsForDay(userId: Int, startTime: Long, endTime: Long): Float?

    @Query("SELECT COUNT(*) FROM food_intake WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun getFoodIntakeCountForDay(userId: Int, startTime: Long, endTime: Long): Int

    @Query("DELETE FROM food_intake WHERE userId = :userId AND timestamp < :startTime")
    suspend fun deleteOldIntakes(userId: Int, startTime: Long)

    @Query("SELECT * FROM food_intake WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllFoodIntakes(userId: Int): List<FoodIntake>

    @Query("SELECT * FROM food_intake WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentFoodIntakes(userId: Int, limit: Int): List<FoodIntake>

    @Update
    suspend fun updateFoodIntake(foodIntake: FoodIntake)

    @Query("DELETE FROM food_intake WHERE id = :id")
    suspend fun deleteFoodIntake(id: Int)

    @Query("DELETE FROM food_intake WHERE userId = :userId")
    suspend fun deleteAllUserFoodIntakes(userId: Int)

    @Query("DELETE FROM food_intake")
    suspend fun deleteAllFoodIntakes()

    @Query("DELETE FROM food_intake")
    suspend fun clearAllFoodIntakes()

    @Query("SELECT * FROM food_intake WHERE userId = :userId AND foodName LIKE '%' || :searchQuery || '%' ORDER BY timestamp DESC")
    suspend fun searchFoodIntakes(userId: Int, searchQuery: String): List<FoodIntake>
}