package com.example.kaliumapp.data.database.dao

import androidx.room.*
import com.example.kaliumapp.data.database.entities.CalorieExpenditure

@Dao
interface CalorieExpenditureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalorieExpenditure(expenditure: CalorieExpenditure): Long

    @Query("SELECT * FROM calorie_expenditure WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getCalorieExpenditureHistory(userId: Int): List<CalorieExpenditure>

    @Query("SELECT * FROM calorie_expenditure WHERE userId = :userId AND date = :date")
    suspend fun getCalorieExpenditureByDate(userId: Int, date: String): List<CalorieExpenditure>

    @Query("DELETE FROM calorie_expenditure WHERE userId = :userId")
    suspend fun deleteAllUserCalorieExpenditure(userId: Int)

    @Update
    suspend fun updateCalorieExpenditure(expenditure: CalorieExpenditure)

    @Delete
    suspend fun deleteCalorieExpenditure(expenditure: CalorieExpenditure)
}
